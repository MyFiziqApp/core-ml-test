package com.myfiziq.sdk.manager;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;

import java.io.File;

import androidx.annotation.NonNull;
import timber.log.Timber;


// Package private. Should not be exposed to the customer app.
class MyFiziqAvatarUploadService
{
    /**
     * Uploads the zip files for an avatar to be processed by the MyFiziq AWS service.
     *
     * @param avatar The avatar to upload the zip files for.
     * @param callback A callback which will be called once the upload is either successful or has failed.
     */
    void uploadAvatarZips(@NonNull ModelAvatar avatar, @NonNull MyFiziqApiCallback callback)
    {
        Timber.i("Uploading avatar zip files...");

        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        final ModelAppConf conf = ModelAppConf.getInstance();
        if (conf == null)
        {
            Timber.e("ModelAppConf was empty");
            mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, null);
            return;
        }

        int filesToUpload = avatar.getUploadCount();

        // Create a listener that listens for when all the avatar zip files have been uploaded
        MyFiziqS3TransferListener zipListener = new MyFiziqS3TransferListener(
                filesToUpload,
                () ->
                {
                    MyFiziqAvatarMetadataService avatarMetadataService = new MyFiziqAvatarMetadataService();

                    // When all zip files have been successfully uploaded, upload the ModelAvatarStatus object as a file to S3
                    // Run it on a separate thread since we make some database calls in here. This prevents them from being made on the UI thread.
                    AsyncHelper.run(() -> avatarMetadataService.uploadStatusFileSync(avatar, conf, callback));
                },
                exception ->
                {
                    // If one or more avatar zip files couldn't be uploaded, return an error
                    Timber.e(exception, "Uploading the avatar zip files was unsuccessful");
                    mgr.postCallback(callback, SdkResultCode.SDK_ERROR_UPLOAD_ZIPS, null);
                });

        for (int fNo = 0; fNo < filesToUpload; fNo++)
        {
            try
            {
                File fileContents = avatar.getZipFile(fNo);
                String destFilename = fNo + ".zip";
                String destPath = avatar.getName(destFilename);

                AwsUtils.getTransferUtility().upload(conf.ingress, destPath, fileContents, new ObjectMetadata(), null, zipListener);
            }
            catch (Exception e)
            {
                Timber.e(e, "Error uploading avatar zips");
                mgr.postCallback(callback, SdkResultCode.SDK_ERROR_UPLOAD_ZIPS, null);
            }
        }
    }
}
