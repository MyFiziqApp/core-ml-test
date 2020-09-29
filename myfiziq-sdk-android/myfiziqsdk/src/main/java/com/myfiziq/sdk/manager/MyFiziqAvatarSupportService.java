package com.myfiziq.sdk.manager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelDeviceData;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ModelSupportData;
import com.myfiziq.sdk.db.ModelSupportImages;
import com.myfiziq.sdk.db.ModelSupportMeasurements;
import com.myfiziq.sdk.db.ModelSupportQuery;
import com.myfiziq.sdk.db.ModelSupportUser;
import com.myfiziq.sdk.db.ModelSupportVersions;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.ZipUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import androidx.annotation.NonNull;
import timber.log.Timber;


// Package private. Should not be exposed to the customer app.
@SuppressWarnings("ResultOfMethodCallIgnored")
class MyFiziqAvatarSupportService
{
    private String getSupportImageForSide(PoseSide side, ModelAvatar avatar)
    {
        int imagesPerSide = ModelAvatar.getCaptureFrames() - 1;
        String sideFilename = side.getSideImageFilename(avatar.getAttemptId(), imagesPerSide);
        String filePath = GlobalContext.getContext().getFilesDir().getAbsolutePath() + "/" + sideFilename;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap == null)
        {
            return "No image file exists for this avatar";
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] bytes = outputStream.toByteArray();
        String b64Encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
        return b64Encoded;
    }

    void uploadSupportData(String reason, String message, ModelAvatar avatar, AsyncHelper.Callback<Boolean> onComplete)
    {
        String appVersion = "";
        try
        {
            // We want the version of the application (not the SDK) so we get package info from the application context
            Context appContext = GlobalContext.getContext().getApplicationContext();
            PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            appVersion = pInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Timber.e(e, "Cannot find application package name");
        }

        String username = AwsUtils.getUsername();
        String tokenItem = AwsUtils.getCognitoUsernameNumber();
        ModelSupportQuery modelSupportQuery = new ModelSupportQuery(reason, message, true);
        ModelSupportVersions modelSupportVersions = new ModelSupportVersions(appVersion, String.valueOf(BuildConfig.VERSION_CODE));
        ModelDeviceData modelDeviceData = new ModelDeviceData(Build.MODEL, Build.MANUFACTURER, String.valueOf(Build.VERSION.SDK_INT));
        ModelSupportImages modelSupportImages = null;
        ModelSupportMeasurements modelSupportMeasurements = null;
        ModelSupportUser modelSupportUser = null;
        String fileName = "General-Feedback-" + AwsUtils.getCognitoUsernameNumber();

        if (avatar != null)
        {
            ArrayList<String> front = new ArrayList<>();
            front.add(getSupportImageForSide(PoseSide.front, avatar));
            ArrayList<String> side = new ArrayList<>();
            side.add(getSupportImageForSide(PoseSide.side, avatar));
            modelSupportImages = new ModelSupportImages(0, front, side, avatar.getAttemptId(), ".jpg");
            modelSupportMeasurements = new ModelSupportMeasurements(avatar);
            modelSupportUser = new ModelSupportUser(avatar, username, tokenItem, modelSupportMeasurements);
            fileName = avatar.getAttemptId();
        }

        ModelSupportData sdata = new ModelSupportData(
                modelSupportQuery,
                modelSupportVersions,
                modelDeviceData,
                username,
                tokenItem,
                reason,
                reason,
                message,
                modelSupportUser,
                modelSupportImages
        );

        final byte[] supportData = sdata.serialize().getBytes();


        File feedbackFile = new File(GlobalContext.getContext().getFilesDir().getAbsolutePath(), fileName);

        try (FileOutputStream fOut = new FileOutputStream(feedbackFile))
        {
            fOut.write(supportData);
        }
        catch (Exception e)
        {
            Timber.e(e, "Could not write support data to file");
            onComplete.execute(false);
            return;
        }

        ModelAppConf conf = ModelAppConf.getInstance();
        if (conf == null)
        {
            Timber.e("ModelAppConf is null");
            feedbackFile.delete();
            onComplete.execute(false);
            return;
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String uploadKey = String.format("%s/%s/%s-%s-%s-%s.json", MyFiziq.getInstance().getTokenAid(), AwsUtils.getCognitoUsernameNumber(), fileName, year, month, day);

        try
        {
            AwsUtils.getTransferUtility().upload(
                    conf.feedback,
                    uploadKey,
                    feedbackFile,
                    new ObjectMetadata(),
                    null,
                    new MyFiziqS3TransferListener(
                            1,
                            () ->
                            {
                                Timber.i("Feedback file upload success");
                                feedbackFile.delete();
                                onComplete.execute(true);

                            },
                            exception ->
                            {
                                Timber.e(exception, "Uploading the feedback file was unsuccessful");
                                feedbackFile.delete();
                                onComplete.execute(false);
                            }
                    )
            );
        }
        catch (Exception e)
        {
            Timber.e(e, "AWS Transfer Utility failed to upload support data");
            feedbackFile.delete();
            onComplete.execute(false);
        }
    }

    void writeLogsToFile(FileOutputStream fout)
    {
        try
        {
            Cursor cursor = ORMTable.dbFromModel(ModelLog.class).getModelCursor(ModelLog.class, null, "pk");
            if (null != cursor)
            {
                if (cursor.moveToFirst())
                {
                    ModelLog log = Orm.newModel(ModelLog.class);

                    do
                    {
                        log.readFromCursor(cursor);
                        fout.write(log.serialize().getBytes());
                    }
                    while (cursor.moveToNext());
                }

                cursor.close();
            }
        }
        catch (Throwable t)
        {
            Timber.e(t);
        }
    }

    /**
     * Uploads additional information about an avatar capture to help with support.
     *
     * @param bucketToUploadTo The S3 bucket to upload to.
     * @param avatar           The avatar to upload extended support information for.
     * @param callback         The callback to trigger on success or failure.
     */
    void uploadExtendedAvatarSupportInfo(String bucketToUploadTo, ModelAvatar avatar, @NonNull AsyncHelper.Callback<Boolean> callback)
    {
        if (avatar == null)
        {
            Timber.e("Cannot upload extended avatar support data. Input avatar is null");
            callback.execute(false);
            return;
        }

        if (TextUtils.isEmpty(avatar.getAttemptId()))
        {
            Timber.e("The attempt ID is empty. Extended avatar support information will not be uploaded.");
            callback.execute(false);
            return;
        }

        if (TextUtils.isEmpty(bucketToUploadTo))
        {
            Timber.e("No bucket specified to upload extended avatar support information to");
            callback.execute(false);
            return;
        }

        if (!AwsUtils.getS3Client().doesBucketExist(bucketToUploadTo))
        {
            Timber.w("S3 bucket %s does not exist. Will not upload extended support information.", bucketToUploadTo);
            callback.execute(false);
            return;
        }


        Timber.i("Generating extended avatar support information...");


        String appDirectory = GlobalContext.getContext().getFilesDir().getAbsolutePath();

        // See if there's already a saved frame from the video...
        String frontImageZipName = "front.photo" + PoseSide.CAPTURE_IMAGE_EXTENSION;
        File frontOutputFile = new File(appDirectory, PoseSide.front.getSideImageFilename(avatar.getAttemptId(), 0));

        // See if there's already a saved frame from the video...
        String sideImageZipName = "side.photo" + PoseSide.CAPTURE_IMAGE_EXTENSION;
        File sideOutputFile = new File(appDirectory, PoseSide.side.getSideImageFilename(avatar.getAttemptId(), 0));

        if (!frontOutputFile.exists() && !sideOutputFile.exists())
        {
            Timber.e("Could not generate extended support information for both the front and side images");
            callback.execute(false);
            return;
        }

        String zipOutputName = GlobalContext.getContext().getFilesDir().getAbsolutePath() + "/extended-support-" + avatar.getAttemptId() + ".zip";

        HashMap<String, File> zipFileInput = new HashMap<>();
        zipFileInput.put(frontImageZipName, frontOutputFile);
        zipFileInput.put(sideImageZipName, sideOutputFile);

        File zipOutputFile = ZipUtils.createZipFiles(zipOutputName, zipFileInput);

        if (zipOutputFile == null || !zipOutputFile.exists())
        {
            Timber.e("Cannot create zip file that contains extended avatar support information");
            callback.execute(false);
            return;
        }


        String remoteFilename = avatar.getNameForZip();

        uploadedExtendedSupportDataToS3(bucketToUploadTo, remoteFilename, zipOutputFile, callback);
    }

    /**
     * Saves a bitmap to the filesystem.
     *
     * @param outputFile The file to save the bitmap to.
     * @param bitmap     The bitmap to save.
     */
    private void saveBitmapToFilesystem(File outputFile, Bitmap bitmap)
    {
        if (outputFile.exists())
        {
            // This MUST be synchronous
            outputFile.delete();
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputFile))
        {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot save bitmap to filesystem.");
        }
    }

    /**
     * Uploads extended support data for an avatar to S3.
     *
     * @param bucketToUploadTo The bucket to uploaded to.
     * @param remoteFilename   The filename to call the file on S3.
     * @param zipFile          The zip file to upload.
     * @param callback         A callback indicating if the operation is successful or not.
     */
    private void uploadedExtendedSupportDataToS3(@NonNull String bucketToUploadTo, @NonNull String remoteFilename,
                                                 @NonNull File zipFile, @NonNull AsyncHelper.Callback<Boolean> callback)
    {
        if (!zipFile.exists())
        {
            Timber.e("Cannot upload extended avatar support information to S3 bucket. Zip file does not exist.");
            callback.execute(false);
            return;
        }

        ModelAppConf conf = ModelAppConf.getInstance();
        if (conf == null)
        {
            Timber.e("ModelAppConf is null");
            callback.execute(false);
            return;
        }

        Timber.d("Starting to upload extended avatar support information to S3");

        TransferObserver observer = AwsUtils.getTransferUtility().upload(
                bucketToUploadTo,
                remoteFilename,
                zipFile);

        observer.setTransferListener(new MyFiziqS3TransferListener(
                1,
                () ->
                {
                    // Clean up the zip file
                    deleteFileAsync(zipFile);

                    callback.execute(true);
                },
                exception ->
                {
                    Timber.e(exception, "Uploading extended avatar support information was unsuccessful");

                    // Clean up the zip file
                    deleteFileAsync(zipFile);

                    callback.execute(false);
                })
        );
    }

    /**
     * Deletes a file asynchronously if it exists.
     *
     * @param file The file to delete.
     */
    private void deleteFileAsync(File file)
    {
        AsyncHelper.run(() ->
        {
            if (file.exists())
            {
                file.delete();
            }
        });
    }
}
