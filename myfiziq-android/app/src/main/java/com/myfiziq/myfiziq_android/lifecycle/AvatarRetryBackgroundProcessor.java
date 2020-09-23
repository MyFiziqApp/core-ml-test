package com.myfiziq.myfiziq_android.lifecycle;

import android.text.TextUtils;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.myfiziq.myfiziq_android.Credentials;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AvatarUploadWorker;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import timber.log.Timber;

/**
 * This class provides an example for how you can generate an avatar in the background without a user
 * interface present and how you can initialise the SDK.
 */
public class AvatarRetryBackgroundProcessor
{
    private Observer<List<WorkInfo>> avatarGenerationObserver;
    private long sdkSetupStartTime;


    public void retryAvatar(String avatarId)
    {
        Timber.i("Setting up SDK to retry Avatar ID: %s", avatarId);

        sdkSetupStartTime = System.currentTimeMillis();

        initialiseSdk(avatarId);
    }

    private void initialiseSdk(String avatarId)
    {
        MyFiziqSdkManager.assignConfiguration(
                Credentials.TOKEN,
                (responseCode, result) -> onConfigurationAssigned(responseCode, result, avatarId)
        );
    }

    /**
     * Once we have gotten the application's configuration and assigned it in the SDK.
     */
    private void onConfigurationAssigned(SdkResultCode responseCode, String result, String avatarId)
    {
        if (responseCode.isInternetDown())
        {
            Timber.e("Internet down. Avatar won't be regenerated in the background.");
            return;
        }

        if (!responseCode.isOk())
        {
            Timber.e("Failed to assign configuration. Avatar won't be regenerated in the background.");
            return;
        }

        String username = AWSMobileClient.getInstance().getUsername();

        // If we don't have cached credentials, we can't proceed
        if (TextUtils.isEmpty(username))
        {
            Timber.e("Not signed in. Avatar won't be regenerated in the background.");
            return;
        }


        // This MUST happen AFTER the configuration has been assigned, otherwise a race condition
        // can occur and the SDK initialisation will fail if we haven't assigned the configuration (initialised AWS).
        MyFiziqSdkManager.initialiseSdk(Credentials.TOKEN, (resultCode1, result1) ->
        {
            if (resultCode1.isOk())
            {
                signInUser(username, avatarId);
            }
            else
            {
                Timber.e("Received error %s when initializing AWS instance. Avatar won't be regenerated in the background.", resultCode1.toString());
            }
        });
    }

    private void signInUser(String username, String avatarId)
    {
        MyFiziqSdkManager.signIn(username, null, (responseCode, result1) ->
        {
            if (responseCode.isOk())
            {
                startRegeneratingAvatar(avatarId);
            }
            else if (responseCode.isInternetDown())
            {
                Timber.e("Internet down. Avatar won't be regenerated in the background.");
            }
            else
            {
                Timber.e("Session invalid. Avatar won't be regenerated in the background.");
            }
        });

    }

    private void startRegeneratingAvatar(String avatarId)
    {
        long sdkSetupEndTime = System.currentTimeMillis();
        Timber.i("Finished setting up SDK. Took %sms. Will now start regenerating Avatar ID: %s", (sdkSetupEndTime - sdkSetupStartTime), avatarId);

        ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, avatarId);

        if (avatar == null)
        {
            Timber.e("Cannot find avatar with ID: %s. Avatar won't be regenerated in the background.", avatarId);
            return;
        }

        // Mark the avatar as pending to be executed when the app next opens
        avatar.setStatus(Status.Pending);
        avatar.save();

        AvatarUploadWorker.createWorker(avatar);


        LiveData<List<WorkInfo>> liveData = AvatarUploadWorker.getLiveDataForAvatar(avatar);

        avatarGenerationObserver = workInfoList ->
        {
            if (workInfoList != null && !workInfoList.isEmpty())
            {
                WorkInfo workInfo = workInfoList.get(0);
                Timber.i("WorkInfo State: %s", workInfo.getState());

                if (workInfo.getState().isFinished())
                {
                    Timber.i("Removing observer for AvatarUploadWorker");
                    liveData.removeObserver(avatarGenerationObserver);
                }
            }
        };

        liveData.observeForever(avatarGenerationObserver);


        // Launch the app where the avatar will then be regenerated
        //launchApp(context);
    }
}
