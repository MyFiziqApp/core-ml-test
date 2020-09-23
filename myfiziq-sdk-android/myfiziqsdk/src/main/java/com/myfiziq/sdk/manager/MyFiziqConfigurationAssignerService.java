package com.myfiziq.sdk.manager;

import android.annotation.SuppressLint;
import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

// Package private. Should not be exposed to the customer app.
class MyFiziqConfigurationAssignerService
{
    /**
     * Gets the application's configuration and assigns it to the SDK.
     *
     * @param key          A key representing the environment's configuration. It contains the URL, App Id, Vendor Id and Client ID.
     * @param environment  The name of the environment to get the application configuration for.
     * @param callback     A callback after the operation is performed.
     */
    void assignConfiguration(String key, String environment, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        Timber.i("Assigning app configuration:"+key);

        ORMDbFactory.getInstance().openAllDatabases(GlobalContext.getContext());

        MyFiziq.getInstance().setKeySecretEnv(key, "", environment);

        if (!MyFiziq.getInstance().hasTokens())
        {
            throw new IllegalStateException("Client ID is null. This is required for the MyFiziq app to continue running");
        }

        Timber.i("Environment: %s", MyFiziq.getInstance().getTokenEnv());

        ModelAppConf appConf = ModelAppConf.getInstance();
        if (null == appConf)
        {
            // No existing app conf -> use cached config.
            getAppConfSync(lifecycleGuard, callback);
        }
        else
        {
            // Use cached app config and update in the background.
            AsyncHelper.run(()->getAppConfSync(lifecycleGuard, null));
            onAppConfReceived(callback, lifecycleGuard, SdkResultCode.SUCCESS, "");
        }
    }

    /**
     * Gets the application's configuration and assigns it to the SDK.
     *
     * @param token       A key representing the environment's configuration. It contains the URL, App Id, Vendor Id and Client ID.
     * @param callback    A callback after the operation is performed.
     */
    void assignConfiguration(String token, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        Timber.i("Assigning app configuration:" + token);

        ORMDbFactory.getInstance().openAllDatabases(GlobalContext.getContext());

        MyFiziq.getInstance().setToken(token);

        if (!MyFiziq.getInstance().hasTokens())
        {
            throw new IllegalStateException("Client ID is null. This is required for the MyFiziq app to continue running");
        }

        Timber.i("Environment: %s", MyFiziq.getInstance().getTokenEnv());

        ModelAppConf appConf = ModelAppConf.getInstance();
        if (null == appConf)
        {
            // No existing app conf -> use cached config.
            getAppConfSync(lifecycleGuard, callback);
        }
        else
        {
            // Use cached app config and update in the background.
            AsyncHelper.run(()->getAppConfSync(lifecycleGuard, null));
            onAppConfReceived(callback, lifecycleGuard, SdkResultCode.SUCCESS, "");
        }
    }

    /**
     * Gets the application configuration from the remote server and stores it in the database.
     *
     * @param lifecycleGuard .
     * @param callback    A callback once the configuration has been received.
     */
    private void getAppConfSync(@NonNull MFZLifecycleGuard lifecycleGuard, @Nullable MyFiziqApiCallback callback)
    {
        MyFiziq mfz = MyFiziq.getInstance();

        //
        // !!!!!!!!! WARNING WARNING WARNING! !!!!!!!!!
        // !!!!!!!!!!!!!! DANGER DANGER !!!!!!!!!!!!!!!
        //
        // DO NOT CHANGE THE BELOW LINE TO ANYTHING EXCEPT FOR
        // "Integer responseCode = new Integer(0);"
        // THE IDE WILL TEMPT YOU. IT WILL TELL YOU IT CAN BE DONE MORE EFFICIENTLY, BUT DON'T BELIEVE IT
        // EVEN TRYING TO DO "Integer responseCode = 0" WILL CAUSE THE JNI LAYER TO BECOME
        // CORRUPTED IN AN UNIMAGINABLE WAY. FUTURE JNI CALLS WILL FAIL THAT HAVE NOTHING
        // TO DO WITH NETWORKING!
        //
        // Note, the responseCode is passed by reference to the C++ code.
        // The HTTP response will appear in this variable after the C++ code has performed the request.
        @SuppressLint("UseValueOf")
        Integer responseCode = new Integer(0);                                      // NOSONAR

        // Note, the C++ code puts the ModelAppConf into the ModelAppConf database table automatically
        // as part of "apiGet()" since we pass the ModelAppConf.class parameter to it.

        String result = mfz.apiGet(
                ModelAppConf.class.getSimpleName(),
                String.format("/app/appconf?vid=%s&aid=%s&env=%s",
                        mfz.getTokenVid(), mfz.getTokenAid(), mfz.getTokenEnv()),
                responseCode,
                new Integer(0),
                0,
                FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS)
        );

        if (callback != null)
        {
            onAppConfReceived(callback, lifecycleGuard, SdkResultCode.valueOfHttpCode(responseCode), result);
        }
    }

    /**
     * Continues assigning the configuration after the application configuration has been received.
     */
    private void onAppConfReceived(MyFiziqApiCallback callback, MFZLifecycleGuard lifecycleGuard, SdkResultCode responseCode, String result)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        if (!responseCode.isOk())
        {
            mgr.postCallback(callback, responseCode, result);
            return;
        }

        AWSConfiguration awsConfig = AwsUtils.getAWSConfiguration();

        if (awsConfig == null)
        {
            Timber.e("Cannot get AWS configuration");
            mgr.postCallback(callback, SdkResultCode.AUTH_CANNOT_GET_AWS_CONFIG, result);
            return;
        }

        AWSMobileClient awsInstance = AWSMobileClient.getInstance();
        Context context = GlobalContext.getContext();

        awsInstance.initialize(context, awsConfig, new Callback<UserStateDetails>()
                {
                    @Override
                    public void onResult(UserStateDetails userStateDetails)
                    {
                        Timber.i("AWS initialize onResult: %s", userStateDetails.getUserState());

                        // If we're signed in, try to update the token that we receive from AWS in the database
                        if (userStateDetails.getUserState() == UserState.SIGNED_IN)
                        {
                            String token = userStateDetails.getDetails().get("token");
                            AwsUtils.putIdToken(token);
                        }

                        AwsUtils.tryToUnlockDatabase();

                        lifecycleGuard.setConfigurationAssigned(true);
                        mgr.postCallback(callback, SdkResultCode.SUCCESS, "");
                    }

                    @Override
                    public void onError(Exception e)
                    {
                        Timber.e(e, "AWS Mobile Client failed to initialize");
                        mgr.postCallback(callback, SdkResultCode.AUTH_CANNOT_INIT_AWS, e.getMessage());
                    }
                }
        );
    }
}
