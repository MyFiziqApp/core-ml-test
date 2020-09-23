package com.myfiziq.myfiziq_android;

import android.app.Activity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.AwsUtils;

import org.junit.Assert;

public class MyqEspressoUtils
{
    /**
     * Initialises the MyFiziq SDK to be used by Espresso tests.
     *
     * @param activity The Espresso test activity.
     * @param username The username to login as.
     * @param password The password to login as.
     * @param callback A callback once the SDK has been initialised.
     */
    public static void prepareSdk(Activity activity, String username, String password, AsyncHelper.Callback<SdkResultCode> callback)
    {
        MyFiziqSdkManager.assignConfiguration(
                Credentials.TOKEN,
                (responseCode, result) -> onConfigurationAssigned(activity, responseCode, username, password, callback)
        );
    }

    private static void onConfigurationAssigned(Activity activity, SdkResultCode responseCode, String username, String password, AsyncHelper.Callback<SdkResultCode> callback)
    {
        if (!responseCode.isOk())
        {
            Assert.fail("Response code for assignConfiguration was " + responseCode);
            callback.execute(responseCode);
            return;
        }

        AWSMobileClient.getInstance().initialize(activity.getApplicationContext(),
                AwsUtils.getAWSConfiguration(), new Callback<UserStateDetails>()
                {

                    @Override
                    public void onResult(UserStateDetails userStateDetails)
                    {
                        onAWSMobileClientInitialised(username, password, callback);
                    }

                    @Override
                    public void onError(Exception e)
                    {
                        Assert.fail("AWS Mobile Client failed to initialize");
                        callback.execute(SdkResultCode.ERROR);
                    }
                }
        );
    }

    private static void onAWSMobileClientInitialised(String username, String password, AsyncHelper.Callback<SdkResultCode> callback)
    {
        MyFiziqSdkManager.signIn(username,
                password,
                (responseCode, result1) ->
                {
                    onSignedIn(responseCode, callback);
                }
        );
    }

    private static void onSignedIn(SdkResultCode responseCode, AsyncHelper.Callback<SdkResultCode> callback)
    {
        if (!responseCode.isOk())
        {
            Assert.fail("Response code for assignConfiguration was " + responseCode);
            callback.execute(responseCode);
            return;
        }

        MyFiziqSdkManager.initialiseSdk((responseCode1, result1) -> callback.execute(SdkResultCode.SUCCESS));
    }
}
