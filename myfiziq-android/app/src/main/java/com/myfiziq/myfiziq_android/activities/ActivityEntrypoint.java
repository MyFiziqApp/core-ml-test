package com.myfiziq.myfiziq_android.activities;

import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.WindowManager;

import com.myfiziq.myfiziq_android.BuildConfig;
import com.myfiziq.myfiziq_android.Credentials;
import com.myfiziq.myfiziq_android.LoggingTree;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.MyFiziqCrashHelper;
import com.myfiziq.myfiziq_android.views.SplashScreenVideo;
import com.myfiziq.sdk.db.ResourceDownloadStatus;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.AwsUtils;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java9.util.concurrent.Flow;
import timber.log.Timber;

public class ActivityEntrypoint extends AppCompatActivity
{
    private SplashScreenVideo avatarView;
    private boolean sdkInitDone = false;
    private boolean hasCachedCredentials = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        MyFiziqCrashHelper.startCrashReporting(this);

        if (BuildConfig.DEBUG)
        {
            // Print to Logcat when we're performing blocking operations on the UI thread and slowing down the UI
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }

        LoggingTree.plantNewTree();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        avatarView = findViewById(R.id.splashScreenVideo);

        renderSplashScreenVideo();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        avatarView.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        avatarView.onResume();

        if (ensureRunningOnCompatibleDevice())
        {
            if (!ConnectivityHelper.isNetworkAvailable(this))
            {
                DialogHelper.showInternetDownDialog(this, this::finishAffinity);
                return;
            }

            getConfigurationAndSignIn();
        }
    }

    private boolean ensureRunningOnCompatibleDevice()
    {
        if (BuildConfig.DEBUG)
        {
            // Do not check for device compatibility if we're running in debug mode
            return true;
        }
        else
        {
            if (MyFiziqSdkManager.isDeviceCompatible())
            {
                return true;
            }
            else
            {
                new AlertDialog.Builder(this)
                        .setTitle("Sorry")
                        .setMessage("Your phone is not supported.")     // TODO Wording from Ryan
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        {
                            dialog.dismiss();
                            finish();
                        })
                        .show();

                return false;
            }
        }
    }

    /**
     * Retrieves any cached credentials and attempts to sign in the user.
     * <p>
     * If the sign in process was successful, the user will be sent to the main activity.
     * <p>
     * If the sign in process was unsuccessful, the user will be sent to the login screen.
     * <p>
     * If there are no cached credentials, the user will be sent to the Welcome screen.
     */
    private void getConfigurationAndSignIn()
    {
        MyFiziqSdkManager.assignConfiguration(
                Credentials.TOKEN,
                (responseCode, result) ->
                        onConfigurationAssigned(responseCode)
        );
    }

    /**
     * Once we have gotten the application's configuration and assigned it in the SDK.
     */
    private void onConfigurationAssigned(SdkResultCode responseCode)
    {
        if (responseCode.isInternetDown())
        {
            DialogHelper.showInternetDownDialog(this, this::finishAffinity);
            return;
        }

        if (!responseCode.isOk())
        {
            Timber.e("Failed to assign configuration");
            DialogHelper.showDialog(this, R.string.error, R.string.error_app_config, this::finishAffinity);
            return;
        }

        // We can only initialise feature flags after the configuration has been assigned and the database tables have been created.
        initialiseFeatureFlags();

        checkUserLoginState();

        // This MUST happen AFTER the configuration has been assigned, otherwise a race condition
        // can occur and the SDK initialisation will fail if we haven't assigned the configuration (initialised AWS).
        MyFiziqSdkManager.initialiseSdk((resultCode, result) ->
        {
            if (resultCode.isOk())
            {
                sdkInitDone = true;
                startActivityMainIfAllReady();
            }
            else
            {
                Timber.e("Received error when initializing AWS instance. Sending user to welcome screen.");
                startActivityWelcome();
            }
        });
    }

    private void checkUserLoginState()
    {
        String username = AwsUtils.getUsername();

        // If we have cached credentials
        if (!TextUtils.isEmpty(username))
        {
            hasCachedCredentials = true;
            startActivityMainIfAllReady();
        }
        else if (hasEnvironmentChanged())
        {
            Timber.i("Environment has changed. Sending user to the welcome screen.");

            // User has changed to another environment. Sign them out and send them to the welcome screen.
            MyFiziqSdkManager.signOut((responseCode1, result1) -> startActivityWelcome());
        }
        else
        {
            Timber.i("No cached credentials. Sending user to the welcome screen.");

            // Send the user to the welcome screen if there's no cached credentials
            startActivityWelcome();
        }
    }

    /**
     * Starts the main activity.
     * <p>
     * This should occur after the user has successfully been signed in.
     */
    private void startActivityMainIfAllReady()
    {
        if (!sdkInitDone || !hasCachedCredentials)
        {
            // Wait until both operations have completed before starting the main activity
            return;
        }

        if (isFinishing() || isDestroyed())
        {
            return;
        }

        Intent mainActivity = new Intent(this, ActivityMain.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(mainActivity);
    }

    /**
     * Starts the Welcome activity.
     * <p>
     * Since the Welcome activity is the first screen in the workflow, we'll clear the back stack
     * to ensure that the user won't go back to the Splash screen when they press the back button.
     */
    private void startActivityWelcome()
    {
        if (isFinishing() || isDestroyed())
        {
            return;
        }

        Intent welcomeActivity = new Intent(this, ActivityWelcome.class);
        welcomeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(welcomeActivity);
    }

    private void renderSplashScreenVideo()
    {
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loading);

        avatarView.setVideoFromUri(this, uri);
        avatarView.setLooping(true);
        avatarView.start();
    }

    /**
     * Determines if the user has changed to another environment since logging in.
     *
     * @return True if the environment has changed since login.
     */
    private boolean hasEnvironmentChanged()
    {
        //TODO: compare logged in env with app token env.
        //TODO: String loggedInEnvironment = ModelSetting.getSetting(ModelSetting.Setting.ENVIRONMENT, "");

        return false;//(!TextUtils.isEmpty(loggedInEnvironment) && !Credentials.ENV.equals(loggedInEnvironment));
    }

    /**
     * Setup any feature flags.
     */
    private void initialiseFeatureFlags()
    {
        /*
        AsyncHelper.run(() ->
        {
            // If we haven't configured any settings, give them a DEFAULT value here
            // Giving them a default value that's stored in the database lets them be cached and
            // reduces disk IO compared to having no value at all.
            // (Don't need to be constantly checking the database to see if the non-configured value exists).
        });
        */
    }
}
