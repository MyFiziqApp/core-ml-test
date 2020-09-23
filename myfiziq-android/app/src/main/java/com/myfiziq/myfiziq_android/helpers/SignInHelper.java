package com.myfiziq.myfiziq_android.helpers;

import android.app.Activity;
import android.content.Intent;

import com.myfiziq.myfiziq_android.activities.ActivityWelcome;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.MyFiziqAvatarDownloadManager;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.AwsUtils;

import java.util.Iterator;
import java.util.LinkedList;

import timber.log.Timber;

import java.util.Iterator;
import java.util.LinkedList;

import timber.log.Timber;


/**
 * A helper class to assist in signing in the user in the background as opposed to the splash screen/entrypoint.
 */
public class SignInHelper
{
    private LinkedList<MyFiziqApiCallback> callbacks = new LinkedList<>();

    private boolean signInRunning = false;

    private static SignInHelper INSTANCE;


    private SignInHelper()
    {
        // Empty hidden constructor for the singleton
    }

    public static SignInHelper getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new SignInHelper();
        }

        return INSTANCE;
    }

    /**
     * Signs in the user.
     *
     * If the sign in process was successful, the user will be sent to the main activity.
     *
     * If the sign in process was unsuccessful, the user will be sent to the login screen.
     */
    public void refreshSignInState(Activity activity, MyFiziqApiCallback callback)
    {
        callbacks.add(callback);

        if (signInRunning)
        {
            Timber.e("Cannot run multiple sign in attempts at the same time. Listening for completion of the initial sign in.");
            return;
        }

        signInRunning = true;

        String username = AwsUtils.getUsername();

        MyFiziqSdkManager.signIn(username, null, (responseCode1, result1) ->
        {
            if (responseCode1.isOk())
            {
                onSuccessfullySignedIn(username);

                signInRunning = false;
                executeAndClearCallbacks(responseCode1, result1);
            }
            else
            {
                // e.g. internet down or a sign in error occurred
                signInRunning = false;
                executeAndClearCallbacks(responseCode1, result1);
            }
        });
    }

    public boolean isRunning()
    {
        return signInRunning;
    }

    public void addListener(MyFiziqApiCallback callback)
    {
        callbacks.add(callback);
    }

    private void onSuccessfullySignedIn(String username)
    {
        MyFiziqCrashHelper.assignUserForCrashReporting(username, username, username);
        refreshUserProfile();

        MyFiziqAvatarDownloadManager downloadManager = MyFiziqAvatarDownloadManager.getInstance();

        // Get latest avatars when starting the app, instead of waiting 30 seconds
        downloadManager.getAvatarsNow();
    }

    private void refreshUserProfile()
    {
        // Setting up the SDK takes a long time, so lets try to cache the user profile on a best-effort
        // basis while it starts
        MyFiziqSdkManager.getUserProfile((userProfileResponseCode, userProfileResult, userProfile) ->
        {
            if (userProfileResponseCode.isOk() || userProfile != null)
            {
                AsyncHelper.run(userProfile::save);
            }
        });
    }

    /**
     * Starts the Welcome activity.
     *
     * Since the Welcome activity is the first screen in the workflow, we'll clear the back stack
     * to ensure that the user won't go back to the Splash screen when they press the back button.
     */
    private void startActivityWelcome(Activity activity)
    {
        Intent welcomeActivity = new Intent(activity, ActivityWelcome.class);
        welcomeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        activity.startActivity(welcomeActivity);
    }

    private void executeAndClearCallbacks(SdkResultCode resultCode, String message)
    {
        Iterator<MyFiziqApiCallback> iterator = callbacks.iterator();

        while (iterator.hasNext())
        {
            MyFiziqApiCallback callback = iterator.next();
            callback.apiResult(resultCode, message);

            iterator.remove();
        }
    }
}
