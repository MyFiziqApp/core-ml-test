package com.myfiziq.myfiziq_android.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.myfiziq.myfiziq_android.LoggingTree;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.MyFiziqCrashHelper;
import com.myfiziq.myfiziq_android.helpers.SignInHelper;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.MyFiziqAvatarDownloadManager;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Feet;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.ModelSisterStyle;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Pounds;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.SisterAppStyleDownloader;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StyleInterceptor;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.UiUtils;

import org.adrianwalker.multilinestring.Multiline;

import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import timber.log.Timber;

public class ActivityMyFiziq extends AppCompatActivity implements ActivityInterface
{
    // Phillip Cooper: The salt between iOS and Android will always be this (6/5/2020 3:51pm)
    // NEVER CHANGE THIS.
    private static final String SALT = "0c0d16a9-857d-47f9-b627-dc352e184c5e";

    private AsyncProgressDialog dialog;

    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        MyFiziqCrashHelper.startCrashReporting(this);

        super.onCreate(savedInstanceState);

        LoggingTree.plantNewTree();

        SisterColors.getInstance().setSisterMode(true);

        /**
         *      t - REQUIRED - the jwt string token, URL Encoded (of course), base64.
         * u - REQUIRED - the partner's user id, which we use for custom auth. The use of the jwt token and salt only know by sister app will be plenty of user security, so claims not needed.
         * p - OPTIONAL - the user's preferred measurement type, either "kg" or "lb", less characters than writing "imperial" and "metric", particularly when it's just enum for program to interpret.
         * h - OPTIONAL - user's height, in centimetres, expressed as float.
         * w - OPTIONAL - user's weight, in kilograms, expressed as float.
         * g - OPTIONAL - user's gender, either "M" for male, or "F" for female.
         * l - OPTIONAL - language override, in the ISO standard format (see Apple 'Language Designator' doc), should the partner choose to force language over the device setting.
         *                  iOS sister app will try to locate respective language resource bundle, by first attempting
         *                  bundle-<lang>-<region> or fallback to bundle-<lang> or finally falling back to bundle should no language specific version of the resource exist for
         *                  the the given language descriptor.
         */
        Intent intent = getIntent();
        String userId = intent.getStringExtra("u");
        String jwtStringToken = intent.getStringExtra("t");
        String preMeasurementType = intent.getStringExtra("p");
        String userGender = intent.getStringExtra("g");
        String userHeight = intent.getStringExtra("h");
        String userWeight = intent.getStringExtra("w");
        String displayMode = intent.getStringExtra("m");

        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(jwtStringToken))
        {
            Toast.makeText(this, "Required inputs are missing", Toast.LENGTH_LONG).show();
            Timber.e("Missing inputs, UserID: %s, Token: %s", userId, jwtStringToken);
            return;
        }

        MyFiziqSdkManager.assignConfiguration(
                jwtStringToken,
                (responseCode, result) ->
                {
                    login(userId, jwtStringToken, Collections.singletonList("12"), SALT, (responseCode1, result1) ->
                    {
                        if (!responseCode1.isOk())
                        {
                            // TODO Handle this error somehow
                            dialog.dismiss();
                            Toast.makeText(this, "Cannot sign in. Response Code: " + responseCode1, Toast.LENGTH_LONG).show();
                            Timber.e("Cannot sign in. Response Code: %s", responseCode1);
                            return;
                        }

                        ParameterSet.Builder builder = new ParameterSet.Builder(ActivityMain.class);
                        if (!TextUtils.isEmpty(userGender))
                            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_GENDER, userGender));
                        if (!TextUtils.isEmpty(userWeight))
                            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_WEIGHT_IN_KG, userWeight));
                        if (!TextUtils.isEmpty(userHeight))
                            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_HEIGHT_IN_CM, userHeight));
                        if (!TextUtils.isEmpty(preMeasurementType))
                        {
                            String prefMeasType = preMeasurementType.toLowerCase();
                            if (prefMeasType.contentEquals("kg"))
                            {
                                builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, Kilograms.internalName));
                                builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, Centimeters.internalName));
                            }
                            else if (prefMeasType.contentEquals("lb"))
                            {
                                builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, Pounds.internalName));
                                builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, Feet.internalName));
                            }
                        }

                        builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_ACTIVITY_FLAGS,
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_NO_ANIMATION));

                        MyFiziqSdkManager.initialiseSdk((responseCode3, result3) ->
                                handleRemoteStyling(builder.build()));
                    });
                });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (null != dialog)
        {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        isPaused = false;

        // Only start checking for avatars if we have finished the sign in process.
        // If we haven't finished signing in, we will start checking in the callback for
        // {@code SignInHelper} once we have signed in.
        if (MyFiziqSdkManager.isSignedIn())
        {
            MyFiziqAvatarDownloadManager.getInstance().startCheckingForAvatars();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        isPaused = true;

        // Ensure that we stop polling for new avatars if we go to the background
        MyFiziqAvatarDownloadManager.getInstance().stopCheckingForAvatars();
    }

    private void showPleaseWait()
    {
        dialog = AsyncProgressDialog.showProgress(this, getString(R.string.please_wait), true, false, null);
        UiUtils.setAlertDialogColours(this, dialog);
    }

    private void login(String partnerUserId, String jwtStringToken, List<String> claims, String salt, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager.isCurrentlySignedInWithClaims(partnerUserId, claims, salt, (responseCode, result) ->
        {
            if (responseCode.isOk())
            {
                Timber.i("Currently signed in. Able to refresh sign in state in the background. Starting now.");

                startBackgroundSignIn();

                callback.apiResult(SdkResultCode.SUCCESS, "");
            }
            // If we're signed out, sign in.
            else if (responseCode == SdkResultCode.AUTH_SIGNED_OUT)
            {
                showPleaseWait();
                Timber.i("User is signed out. Will now authenticate.");
                MyFiziqSdkManager.userCustomAuthenticateForId(partnerUserId, claims, salt, callback);
            }
            // If we're logged in as another user, sign out and try again
            else if (responseCode == SdkResultCode.AUTH_SIGNED_IN_DIFFERENT_USER)
            {
                showPleaseWait();
                Timber.i("Currently signed in using a different user. Need to sign out and authenticate.");
                signOutAndRelogin(partnerUserId, jwtStringToken, claims, salt, callback);
            }
            else
            {
                // Error. Pass the response back to the calling method
                callback.apiResult(responseCode, result);
            }
        });
    }

    private void signOutAndRelogin(String partnerUserId, String jwtStringToken, List<String> claims, String salt, MyFiziqApiCallback callback)
    {
        Timber.i("User has changed. Signing out");

        MyFiziqSdkManager.signOut((responseCode, result) ->
        {
            if (!responseCode.isOk())
            {
                Timber.e("Cannot sign out");
                callback.apiResult(responseCode, result);
                return;
            }

            // Need to reassign configuration after signing out
            MyFiziqSdkManager.assignConfiguration(
                    jwtStringToken,
                    (responseCode1, result1) ->
                    {
                        if (!responseCode1.isOk())
                        {
                            callback.apiResult(responseCode1, result1);
                            return;
                        }

                        // Authenticate and pass the result directly to the callback.
                        // If we're still logged into another account at this point, something really bad has gone wrong.
                        MyFiziqSdkManager.userCustomAuthenticateForId(partnerUserId, claims, salt, callback);
                    });
        });
    }

    private void handleRemoteStyling(ParameterSet parameterSet)
    {
        SisterAppStyleDownloader.getStyling((responseCode1, result1, payload) ->
        {
            if (!responseCode1.isOk())
            {
                Timber.e("Cannot download styling. Code: %s. Message: %s.", responseCode1, result1);
            }
            else
            {
                StyleInterceptor mStyleInterceptor = new StyleInterceptor(payload);
                mStyleInterceptor.init();

                SisterColors sisterColors = SisterColors.getInstance();
                sisterColors.setStyle(mStyleInterceptor);
                sisterColors.init(payload);

                Integer col = SisterColors.getInstance().getAlertBackgroundColor();
                if (col != null)
                {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_placeholder);
                    ActivityManager.TaskDescription td = new ActivityManager.TaskDescription("Body Meansurements", icon, col);
                    setTaskDescription(td);
                }

                ViewPump.init(ViewPump.builder()
                        .addInterceptor(mStyleInterceptor)
                        .build());
            }

            parameterSet.start(this);
            finish();
        });
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    private void startBackgroundSignIn()
    {
        SignInHelper.getInstance().refreshSignInState(this, (responseCode, result) ->
        {
            if (responseCode.isOk())
            {
                if (!isPaused)
                {
                    // Only start checking for avatars if the window has focus. i.e. don't start checking for avatars if we're in the background...
                    // If we don't have focus, we'll start checking in onResume()
                    MyFiziqAvatarDownloadManager.getInstance().startCheckingForAvatars();
                }
            }
            else if (responseCode.isInternetDown())
            {
                // No internet
                DialogHelper.showInternetDownDialog(this, this::finishAffinity);
            }
            else
            {
                // Session is invalid, sign out the user and send them somewhere
                MyFiziqSdkManager.signOut((responseCode1, result1) ->
                {
                    // TODO Error handling
                    dialog.dismiss();
                    // Note, use the code which CAUSED the error, not the likely success code from signing out
                    Toast.makeText(this, "Cannot sign in. Response Code: " + responseCode, Toast.LENGTH_LONG).show();
                    Timber.e("Cannot sign in. Response Code: %s", responseCode);
                });
            }
        });
    }

    @Override
    public Activity getActivity()
    {
        return this;
    }

    @Override
    public Context getContext()
    {
        return this;
    }
}
