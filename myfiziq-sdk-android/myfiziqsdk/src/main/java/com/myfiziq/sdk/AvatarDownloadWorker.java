package com.myfiziq.sdk;

import android.content.Context;
import android.text.TextUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AvatarUploadWorker;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;

/**
 * A worker to download the latest avatar metadata from the remote server and to start locally processing
 * any pending avatars that have finished processing remotely.
 */
public class AvatarDownloadWorker extends Worker
{
    /**
     * The number of seconds to wait before timing out an SDK operation.
     */
    private static final int TIMEOUT_IN_SECONDS = 60;

    // Ensure that only 1 AvatarDownloadWorker is running throughout the entire app at a time
    private static final Object lock = new Object();

    private static final String TAG = AvatarDownloadWorker.class.getSimpleName();


    public AvatarDownloadWorker(@NonNull Context appContext,
                                @NonNull WorkerParameters workerParams)
    {
        super(appContext, workerParams);
    }

    /**
     * Creates a new AvatarDownloadWorker.
     * <p>
     * If we try to create a worker when one is already running, the new worker will be queued up
     * and executed after the currently running worker has benn completed.
     *
     * @return The UUID of the worker that can be used to lookup its {@link androidx.work.WorkInfo}.
     */
    public static UUID createWorker()
    {
        Data.Builder builder = new Data.Builder();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AvatarDownloadWorker.class)
                .setConstraints(constraints)
                .setInputData(builder.build())
                .addTag(TAG)
                .build();

        // Ensures that only one AvatarDownloadWorker can run at a time
        // If we try to create another one, it will be queued up and executed after the currently
        // running worker has been completed.
        WorkManager.getInstance().enqueue(workRequest);

        return workRequest.getId();
    }

    @NonNull
    @Override
    public Result doWork()
    {
        // Let's ensure that only one worker is running at a time so we don't have concurrency issues when processing avatars.
        // We can use enqueueUniqueWork() with the WorkManager, but it's still early days and it's very unreliable.
        // We don't need anything fancy so lets just use a simple locking object.
        synchronized (lock)
        {
            try
            {
                Timber.i("Starting AvatarDownloadWorker");

                if (!MyFiziqSdkManager.isReady())
                {
                    Timber.w("Cannot download avatar before SDK is ready.");
                    return Result.failure();
                }

                // Ensure user session is valid - this also ensures the encrypted database is ready for use.
                MyFiziqSdkManager.refreshUserSessionSync();

                // The databases will only be opened again if it's not already open, so this is really fast!
                ORMDbFactory.getInstance().openAllDatabases(getApplicationContext());

                String cachedModelKey = MyFiziq.getInstance().getKey(ModelSetting.Setting.MODEL);

                if (!TextUtils.isEmpty(cachedModelKey))
                {
                    // The SDK will only be setup again if it's not already setup, so this is really fast!
                    MyFiziq.getInstance().sdkSetup();
                }

                // Downloads the status of all avatars from the remote server
                SdkResultCode getAvatarMetadataResultCode = getAllAvatarMetadataSync();

                if (getAvatarMetadataResultCode.isInternetDown())
                {
                    Timber.e("Cannot obtain latest avatars. Internet down.");
                    return Result.failure();
                }
                else if (!getAvatarMetadataResultCode.isOk())
                {
                    Timber.e("Error occurred when obtaining latest avatar metadata. Received response code: %s", getAvatarMetadataResultCode);
                    return Result.failure();
                }


                Timber.i("Obtained latest avatar metadata");


                // Downloads the results for any avatars that are pending
                SdkResultCode downloadPendingAvatarsResultCode = downloadPendingAvatarsSync();

                if (downloadPendingAvatarsResultCode.isInternetDown())
                {
                    Timber.e("Cannot obtain latest avatars. Internet down.");
                    return Result.failure();
                }
                else if (!downloadPendingAvatarsResultCode.isOk())
                {
                    Timber.e("Error occurred when downloading pending avatars. Received response code: %s", downloadPendingAvatarsResultCode);
                    return Result.failure();
                }

                Timber.d("Processed pending avatars");
                return Result.success();
            }
            catch (Exception t)
            {
                Timber.e(t, "Error occurred when downloading pending avatars.");
                return Result.failure();
            }
        }
    }

    public static ListenableFuture<List<WorkInfo>> getAllWork()
    {
        return WorkManager.getInstance().getWorkInfosByTag(TAG);
    }

    /**
     * Downloads the metadata for all avatars on the remote server. This includes their remote
     * processing status.
     * <p>
     * This operation is synchronous. The method will return a result once all metadata has been
     * downloaded.
     *
     * @return Whether the operation was successful or not.
     */
    private SdkResultCode getAllAvatarMetadataSync()
    {
        CompletableFuture<SdkResultCode> resultCode = new CompletableFuture<>();

        MyFiziqSdkManager.avatargetall(false, (responseCode, result) ->
        {
            resultCode.complete(responseCode);
        });

        try
        {
            // Wait until avatargetall() has been completed and returned a result.
            // This effectively makes the method synchronous.
            return resultCode.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception occurred while executing avatargetall()");
            return SdkResultCode.AVATARDOWNLOAD_EXCEPTION;
        }
    }

    /**
     * Downloads all pending avatars once they have finished processing remotely and then
     * initiates processing locally.
     * <p>
     * This operation is synchronous. The method will return a result once all metadata has been
     * downloaded.
     *
     * @return Whether the operation was successful or not. If an avatar failed to download, the
     * overall result will still be successful. A failure will only be returned if an exception
     * occurred that we cannot recover from such as an {@link java.util.concurrent.ExecutionException}
     * or an {@link InterruptedException}.
     */
    private SdkResultCode downloadPendingAvatarsSync()
    {
        SdkResultCode returnResultCode = SdkResultCode.SUCCESS;

        try
        {
            ArrayList<ModelAvatar> pendingAvatars = ORMTable.getModelList(
                    ModelAvatar.class,
                    ModelAvatar.getWhere(
                            String.format(
                                    "Status IN ('%s', '%s', '%s')",
                                    Status.Pending, Status.Processing, Status.Uploading
                            )
                    ),
                    ""
            );

            for (ModelAvatar avatar : pendingAvatars)
            {
                if (!AvatarUploadWorker.isWorkerActive(avatar))
                {
                    Timber.e("Failed Avatar detected...");
                    AvatarUploadWorker.createWorker(avatar);
                }
            }
        }
        catch (Exception t)
        {
            Timber.e(t);
        }

        try
        {
            ArrayList<ModelAvatar> pendingAvatars = ORMTable.getModelList(
                    ModelAvatar.class,
                    ModelAvatar.getWhere(
                            String.format("Status='%s'", Status.Pending)
                    ),
                    "");

            if (pendingAvatars == null)
            {
                Timber.i("There are no pending avatars");
                return SdkResultCode.SUCCESS;
            }


            List<CompletableFuture<SdkResultCode>> avatarsDownloading = new LinkedList<>();

            for (ModelAvatar avatar : pendingAvatars)
            {
                // Iterate through all avatars and look for pending ones
                if (avatar.isPending())
                {
                    CompletableFuture<SdkResultCode> completableFuture = new CompletableFuture<>();

                    // Download the avatar asynchronously
                    MyFiziqSdkManager.downloadAvatar(avatar, (responseCode, result) ->
                    {
                        completableFuture.complete(responseCode);
                    });

                    // Store a reference to the completableFuture which will be triggered once
                    // the async operation has completed.
                    avatarsDownloading.add(completableFuture);
                }
            }


            Timber.i("There are %s avatars waiting for a result", avatarsDownloading.size());

            // Iterate through all async operations that are running and wait until each one has been completed
            for (CompletableFuture<SdkResultCode> future : avatarsDownloading)
            {
                // Wait here until the avatar has been downloaded in the background.
                // This makes the method synchronous.
                SdkResultCode resultCode = future.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

                if (!resultCode.isOk())
                {
                    Timber.i("Avatar download result: %s", returnResultCode);

                    // If the avatar had problems downloading, return the individual failure as the overall result code
                    returnResultCode = resultCode;
                }
            }
        }
        catch (Exception t)
        {
            Timber.e(t, "Error occurred while checking for results");
            return SdkResultCode.AVATARDOWNLOAD_EXCEPTION;
        }

        return returnResultCode;
    }
}
