package com.myfiziq.sdk.manager;

import android.text.TextUtils;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelAvatarReqList;
import com.myfiziq.sdk.db.ModelLambdaHeader;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.util.AwsUtils;

import androidx.annotation.NonNull;
import timber.log.Timber;

// Package private. Should not be exposed to the customer app.
class MyFiziqAvatarMetadataService
{
    /**
     * Gets the metadata for all avatars from the remote server.
     *
     * @param bCacheMesh Whether we should generate 3D meshes for the avatar and cache them locally.
     * @param callback A callback that will be made once the operation has succeeded or failed.
     */
    void avatargetall(boolean bCacheMesh, @NonNull MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        if (!MyFiziq.getInstance().hasTokens())
        {
            mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, null);
            return;
        }

        String idToken = AwsUtils.getRawIdToken();

        ModelLambdaHeader headers = new ModelLambdaHeader(
                MyFiziq.getInstance().getTokenAid(), idToken
        );

        MyFiziqSdkManager.refreshUserSessionSync();
        String result = S3Helper.invoke("avatargetall", headers.serialize("id"));

        if (TextUtils.isEmpty(result))
        {
            mgr.postCallback(callback, SdkResultCode.NO_INTERNET, null);
            return;
        }


        ModelAvatarReqList avatarList = Orm.newModel(ModelAvatarReqList.class);

        try
        {
            avatarList.deserialize(result);

            avatarList.createAvatars(bCacheMesh);
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot save avatar to database");
            mgr.postCallback(callback, SdkResultCode.DB_CANNOT_SAVE, null);
            return;
        }

        mgr.postCallback(callback, SdkResultCode.SUCCESS, null);
    }

    /**
     * Upload the {@link com.myfiziq.sdk.db.ModelAvatar} to S3 which is associated with the current processing state of the avatar.
     *
     * @param avatar The avatar to upload the status for.
     * @param conf The S3 configuration to upload to.
     * @param callback A callback which will be called once the upload is either successful or has failed.
     */
    void uploadStatusFileSync(@NonNull ModelAvatar avatar, @NonNull ModelAppConf conf, @NonNull MyFiziqApiCallback callback)
    {
        Timber.i("Uploading status for Avatar ID %s", avatar.id);

        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        try
        {
            TransferObserver observer = AwsUtils.getTransferUtility().upload(
                    conf.results,
                    avatar.getOutputsName(),
                    avatar.getOutputsFile());

            observer.setTransferListener(new MyFiziqS3TransferListener(
                            1, () ->
                    {
                        // Uploading the status file was successful
                        mgr.postCallback(callback, SdkResultCode.SUCCESS, null);
                    },
                    exception ->
                    {
                        // If one or more avatar zip files couldn't be uploaded, return an error
                        Timber.e(exception, "Uploading the status file was unsuccessful");
                        mgr.postCallback(callback, SdkResultCode.SDK_ERROR_UPLOAD_STATUS_FILE, null);
                    })
            );
        }
        catch (Exception e)
        {
            Timber.e("Error uploading status file");
            mgr.postCallback(callback, SdkResultCode.SDK_ERROR_UPLOAD_STATUS_FILE, null);
        }
    }
}
