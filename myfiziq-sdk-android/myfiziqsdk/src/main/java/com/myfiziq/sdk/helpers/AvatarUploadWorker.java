package com.myfiziq.sdk.helpers;

import android.content.Context;
import android.text.TextUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.billing.BillingManager;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelAvatarBatchList;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.BillingEventType;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.Stopwatch;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class AvatarUploadWorker extends Worker
{
    private static final String KEY_ID = "KEY_ID";


    public AvatarUploadWorker(@NonNull Context appContext,
                              @NonNull WorkerParameters workerParams)
    {
        super(appContext, workerParams);
    }

    @NonNull
    public static Operation createWorker(@NonNull ModelAvatar avatar)
    {
        Data.Builder builder = new Data.Builder();
        builder.putString(KEY_ID, avatar.getId());

        //Constraints constraints = new Constraints.Builder()
        //        .setRequiredNetworkType(NetworkType.CONNECTED)
        //        .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AvatarUploadWorker.class)
                //Don't wait for internet - try and fail. //.setConstraints(constraints)
                .setInputData(builder.build())
                .addTag(KEY_ID)
                .addTag(avatar.getId())
                .build();

        Operation operation = WorkManager.getInstance().enqueue(workRequest);

        return operation;
    }

    public static List<WorkInfo> getWorkersForAvatar(@NonNull ModelAvatar avatar)
    {
        ListenableFuture<List<WorkInfo>> workInfos = WorkManager.getInstance().getWorkInfosByTag(avatar.getId());

        try
        {
            return workInfos.get();
        }
        catch (Exception t)
        {
            Timber.e(t);
            return Collections.emptyList();
        }
    }

    public static LiveData<List<WorkInfo>> getLiveDataForAvatar(@NonNull ModelAvatar avatar)
    {
        return WorkManager.getInstance().getWorkInfosByTagLiveData(avatar.getId());
    }

    public static boolean isWorkerActive(@NonNull ModelAvatar avatar)
    {
        try
        {
            List<WorkInfo> workInfoList = getWorkersForAvatar(avatar);
            for(WorkInfo workInfo : workInfoList)
            {
                if (!workInfo.getState().isFinished())
                {
                    return true;
                }
            }
        }
        catch (Exception t)
        {
            Timber.e(t);
        }

        // Default not running.
        return false;
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Timber.i("Starting AvatarUploadWorker");

        String avatarId = getInputData().getString(KEY_ID);

        if (TextUtils.isEmpty(avatarId))
        {
            Timber.e("AvatarUploadWorker failed. Avatar id is empty.");
            return handleFailure(null);
        }

        if (!ConnectivityHelper.isNetworkAvailable(getApplicationContext()))
        {
            Timber.e("AvatarUploadWorker failed. Network is not available.");
            return handleFailure(avatarId);
        }

        Stopwatch stopwatch = new Stopwatch("AvatarUploadWorker");

        //MyFiziq.getInstance().setToken();

        ORMDbFactory.getInstance().openAllDatabases(getApplicationContext());
        String cachedModelKey = MyFiziq.getInstance().getKey(ModelSetting.Setting.MODEL);

        if (!TextUtils.isEmpty(cachedModelKey))
        {
            MyFiziq.getInstance().sdkSetup();
        }


        // Ensure AWS id token is valid before upload.
        // Ensure encrypted database is set-up
        MyFiziqSdkManager.refreshLogin();

        SdkResultCode result = SdkResultCode.ERROR;

        if (segment(avatarId, PoseSide.front))
        {
            if (segment(avatarId, PoseSide.side))
            {
                uploadExtendedAvatarSupportInfo(avatarId);

                result = upload(avatarId);
            }
        }

        stopwatch.print();

        if (result.isOk())
        {
            Timber.i("AvatarUploadWorker completed successfully");
            return handleSuccess();
        }
        else
        {
            Timber.e("AvatarUploadWorker exited with error code: %s", result);
            return handleFailure(avatarId);
        }
    }

    public boolean segment(String avatarId, PoseSide side)
    {
        SdkResultCode result;

        String basePath = GlobalContext.getContext().getFilesDir().getAbsolutePath();

        MyFiziq myFiziqSdk = MyFiziq.getInstance();
        return myFiziqSdk.segment(avatarId, side.ordinal(), basePath, "", false);
    }

    /**
     * Uploads the avatar synchronously to the cloud to be processed.
     *
     * @return A result code.
     */
    public SdkResultCode upload(String avatarId)
    {
        SdkResultCode result;

        BillingManager.logEvent(BillingEventType.NEW_MEASUREMENT_REQUESTED);

        //setState(State.Processing);

        String basePath = GlobalContext.getContext().getFilesDir().getAbsolutePath();

        MyFiziq myFiziqSdk = MyFiziq.getInstance();
        String nativeResult = myFiziqSdk.uploadAvatar(avatarId, basePath, null); //TODO: handle extra JSON data.

        if (!TextUtils.isEmpty(nativeResult))
        {
            ModelAvatarBatchList avatarList = Orm.newModel(ModelAvatarBatchList.class);
            try
            {
                avatarList.deserialize(nativeResult);

                if (avatarList.isValid())
                {
                    ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);
                    if (null != avatar)
                    {
                        avatar.setStatus(com.myfiziq.sdk.db.Status.Completed);
                    }
                    else
                    {
                        Timber.e("Unable to get model for Avatar for id:"+avatarId);
                    }

                    // Recalculates the adjusted values for all avatars and saves them into the database
                    //AdjustedValueCalculator.updateAllAvatarAdjustedValues();

                    result = SdkResultCode.SUCCESS;

                    BillingManager.logEvent(BillingEventType.NEW_MEASUREMENT_RESOLVED);
                }
                else
                {
                    ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);
                    if (null != avatar)
                    {
                        // Something went wrong - update status from error data.
                        avatar.setErrorStatus(avatarList);
                    }
                    else
                    {
                        Timber.e("Unable to get model for Avatar for id:"+avatarId);
                    }

                    result = SdkResultCode.ERROR;
                    Timber.e("Either we received an error from the server or the avatar data is invalid");
                }
            }
            catch (Throwable e)
            {
                ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);
                if (null != avatar)
                {
                    // Something went wrong - update status to failed.
                    avatar.setStatus(com.myfiziq.sdk.db.Status.FailedGeneral);
                }

                result = SdkResultCode.ERROR;
                Timber.e(e, "Cannot save avatar to database");
            }
        }
        else
        {
            ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);
            if (null != avatar)
            {
                // Something went wrong - update status to failed.
                avatar.setStatus(com.myfiziq.sdk.db.Status.FailedGeneral);
            }
            else
            {
                Timber.e("Unable to get model for Avatar for id:"+avatarId);
            }
            result = SdkResultCode.ERROR;
        }

        return result;
    }

    public static void announceProcessingError(@NonNull Context applicationContext, @NonNull ModelAvatar avatar)
    {
        Parameter modelParameter = new Parameter(R.id.TAG_MODEL_ID, avatar.getId());

        ParameterSet parameterSet = new ParameterSet.Builder(AvatarUploadWorker.class)
                .addParam(modelParameter)
                .build();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(applicationContext);
        intentManagerService.respond(IntentResponses.CAPTURE_PROCESSING_ERROR, parameterSet);
    }

    private Result handleSuccess()
    {
        return Result.success();
    }

    private Result handleFailure(@Nullable String avatarId)
    {
        if (!TextUtils.isEmpty(avatarId))
        {
            ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);
            if (avatar != null)
            {
                switch (avatar.getStatus())
                {
                    case FailedTimeout:
                    case FailedNoInternet:
                    case FailedServerErr:
                        // do nothing.
                        break;

                    default:
                        avatar.setStatus(Status.FailedGeneral);
                        break;
                }
                announceProcessingError(getApplicationContext(), avatar);
            }
        }

        return Result.failure();
    }

    private Result handleFailureWithRetry()
    {
        // TODO What should happen if the AWS upload fails? Should we retry when the internet is back again?
        return null;
    }

    /**
     * Uploads extended avatar support information to assist in diagnostics.
     * @param avatarId The avatar to upload extended support information for.
     */
    private void uploadExtendedAvatarSupportInfo(@NonNull String avatarId)
    {
        Context context = GlobalContext.getContext();
        boolean enableRgbCaptureUpload = context.getResources().getBoolean(R.bool.enable_rgb_capture_upload);

        if (!enableRgbCaptureUpload)
        {
            Timber.w("RGB upload is disabled");
            return;
        }

        String avatarDebuggingS3Bucket = generateRgbCaptureBucketString();

        if (TextUtils.isEmpty(avatarDebuggingS3Bucket))
        {
            Timber.i("Extended avatar support information is turned off. Will not upload.");
            return;
        }

        Timber.v("RGB capture will be uploaded to: %s", avatarDebuggingS3Bucket);

        ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);

        if (null != avatar)
        {
            MyFiziqSdkManager.uploadExtendedAvatarSupportData(
                    avatarDebuggingS3Bucket,
                    avatar,
                    success ->
                    {
                        if (Boolean.TRUE.equals(success))
                        {
                            Timber.i("Extended avatar support information was successfully uploaded.");
                        }
                        else
                        {
                            Timber.e("Failed to upload extended avatar support information.");
                        }
                    }
            );
        }
    }

    private String generateRgbCaptureBucketString()
    {
        ModelAppConf modelAppConf = ModelAppConf.getInstance();

        if (modelAppConf == null)
        {
            return "";
        }

        // Tel: Most environments won't have the RGB capture bucket available, even the dev environment. As agreed, we'll have to test that this works before giving it out to third parties
        // We'll try to upload the avatar to the RGB capture bucket on a best effort basis.
        return getApplicationContext().getResources().getString(R.string.avatar_extended_support_s3_bucket, modelAppConf.vendor, modelAppConf.env);
    }
}
