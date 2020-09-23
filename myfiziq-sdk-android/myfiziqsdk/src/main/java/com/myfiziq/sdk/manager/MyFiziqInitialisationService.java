package com.myfiziq.sdk.manager;

import android.text.TextUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.billing.BillingManager;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.BillingEventType;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;

import timber.log.Timber;

// Package private. Should not be exposed to the customer app.
class MyFiziqInitialisationService
{
    void initialiseSdk(String secret, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        MyFiziq.getInstance().setSecret(secret);

        String cachedModelKey = MyFiziq.getInstance().getKey(ModelSetting.Setting.MODEL);

        if (!TextUtils.isEmpty(cachedModelKey))
        {
            setupSdk(lifecycleGuard, callback);
        }
        else
        {
            downloadModelKeyAndSetupSdk(lifecycleGuard, callback);
        }

        BillingManager.logEvent(BillingEventType.SDK_INITIALIZED);
    }

    void initialiseSdk(MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        String cachedModelKey = MyFiziq.getInstance().getKey(ModelSetting.Setting.MODEL);

        if (!TextUtils.isEmpty(cachedModelKey))
        {
            setupSdk(lifecycleGuard, callback);
        }
        else
        {
            downloadModelKeyAndSetupSdk(lifecycleGuard, callback);
        }

        BillingManager.logEvent(BillingEventType.SDK_INITIALIZED);
    }

    /**
     * Downloads the Model Key from the remote server and then sets up the SDK.
     */
    private void downloadModelKeyAndSetupSdk(MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        final ModelAppConf conf = ModelAppConf.getInstance();
        if (conf == null)
        {
            mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, null);
            return;
        }

        String modelKey = "";

        try
        {
            AmazonS3Client s3Client = AwsUtils.getS3Client();
            S3Object object = s3Client.getObject(conf.confbucket, conf.getModelKeyId());

            modelKey = S3Helper.readS3ObjectAsString(object);
        }
        catch (Exception e)
        {
            Timber.e(e, "Unable to get S3 object");
        }

        if (TextUtils.isEmpty(modelKey))
        {
            Timber.e("SDK not initialised");
            mgr.postCallback(callback, SdkResultCode.ERROR, "");
        }
        else
        {
            MyFiziq.getInstance().setKey(ModelSetting.Setting.MODEL, modelKey);

            // Setup the SDK and let the method execute the callback
            setupSdk(lifecycleGuard, callback);
        }
    }

    /**
     * Sets up the SDK using the given secret and model key.
     *
     * @param callback The callback to execute once the operation has been completed.
     */
    private void setupSdk(MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        AsyncHelper.run(()-> MyFiziq.getInstance().sdkSetup());
        lifecycleGuard.setSdkInitialised(true);
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
        mgr.postCallback(callback, SdkResultCode.SUCCESS, "");
    }
}
