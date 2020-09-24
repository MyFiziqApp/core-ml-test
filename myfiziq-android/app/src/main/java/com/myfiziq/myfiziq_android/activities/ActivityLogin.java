package com.myfiziq.myfiziq_android.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.autofill.AutofillManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.SignOutOptions;
import com.google.android.material.textfield.TextInputLayout;
import com.myfiziq.myfiziq_android.Credentials;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.MyFiziqCrashHelper;
import com.myfiziq.myfiziq_android.helpers.Utils;
import com.myfiziq.sdk.MyFiziqAvatarDownloadManager;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.activities.DebugActivity;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.RegexHelpers;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.ClockAccuracyUtils;
import com.myfiziq.sdk.util.UiUtils;

import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.WorkInfo;
import timber.log.Timber;


public class ActivityLogin extends AppCompatActivity
{
    private EditText emailAddress;
    private TextInputLayout passwordContainer;
    private EditText password;
    private Button btnLogin;
    private TextView forgotPassword;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AsyncProgressDialog mDlg;
    boolean mSdkInitDone = false;
    boolean mAvatarReqDone = false;

    private boolean isPaused = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ActionBarHelper.showBackButton(this);
        ActionBarHelper.setActionBarTitle(this, "Login");


        emailAddress = findViewById(R.id.etxtEmailAddress);
        passwordContainer = findViewById(R.id.etxtPasswordContainer);
        password = passwordContainer.getEditText();
        btnLogin = findViewById(R.id.btnLogin);
        forgotPassword = findViewById(R.id.forgotPassword);

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));


        ModelSetting.getSettingAsync(
                ModelSetting.Setting.USERNAME,
                "",
                emailAddressText -> emailAddress.setText(emailAddressText)
        );


        btnLogin.setOnClickListener(view -> onLoginClicked());
        forgotPassword.setOnClickListener(view -> onForgotPasswordClicked());


        Utils.enablePasswordVisibilityButtonOnTextChange(passwordContainer);
        UiUtils.disablePasswordToggleButtonRipple(passwordContainer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        isPaused = false;

        startMainActivityIfAllDone();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (null != mDlg)
        {
            mDlg.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onLoginClicked()
    {
        boolean passedValidation = validate();

        if (!passedValidation)
        {
            return;
        }

        Utils.hideSoftKeyboard(this);

        if (!ConnectivityHelper.isNetworkAvailable(this))
        {
            DialogHelper.showInternetDownDialog(this);
            return;
        }

        mDlg = AsyncProgressDialog.showProgress(this, getString(R.string.signing_in), true, false, null);

        MyFiziqSdkManager.isConfigurationEmpty((responseCode1, result1) ->
        {
            if (!responseCode1.isConfigurationEmpty())
            {
                startLogin();
            }
            else
            {
                Timber.w("ModelAppConf is null. Assigning configuration");

                MyFiziqSdkManager.assignConfiguration(
                        Credentials.TOKEN,
                        (responseCode, result) ->
                        {
                            if (!responseCode.isOk())
                            {
                                Timber.e("Cannot assign configuration. Response Code: %s. Result: %s", responseCode.getCode(), result);

                                mDlg.dismiss();
                                Toast.makeText(ActivityLogin.this, getString(R.string.sign_in_failed), Toast.LENGTH_LONG).show();
                            }
                            else
                            {
                                startLogin();
                            }
                        }
                );
            }
        });
    }

    private void startLogin()
    {
        String usernameText = emailAddress.getText().toString();
        String passwordText = password.getText().toString();

        MyFiziqSdkManager.signIn(
                usernameText,
                passwordText,
                (responseCode, result) -> loginCallback(responseCode, usernameText)
        );
    }

    private void onForgotPasswordClicked()
    {
        Intent intent = new Intent(this, ActivityForgotPassword.class);
        startActivity(intent);
    }

    private boolean validate()
    {
        emailAddress.setError(null);
        password.setError(null);

        int minimumPasswordLength = getResources().getInteger(R.integer.minimum_password_length);

        if (TextUtils.isEmpty(emailAddress.getText()))
        {
            emailAddress.setError(getString(R.string.error_emptyemailaddress));
            emailAddress.requestFocus();
            return false;
        }
        else if (!Pattern.compile(RegexHelpers.emailPattern).matcher(emailAddress.getText()).matches())
        {
            emailAddress.setError(getString(R.string.error_notvalidemailaddress));
            emailAddress.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(password.getText()) || password.getText().length() < minimumPasswordLength)
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            password.setError(getString(R.string.error_tooshortpassword));
            password.requestFocus();
            return false;
        }

        return true;
    }

    private void loginCallback(SdkResultCode responseCode, String username)
    {
        if (responseCode.isOk())
        {
            commitAutofillAction();

            MyFiziqAvatarDownloadManager.Callbacks downloadCallbacks = new MyFiziqAvatarDownloadManager.Callbacks()
            {
                @Override
                public void refreshStart()
                {

                }

                @Override
                public void refreshEnd(WorkInfo.State state)
                {
                    if (state == WorkInfo.State.SUCCEEDED)
                    {
                        mAvatarReqDone = true;
                        MyFiziqCrashHelper.assignUserForCrashReporting(username, username, username);

                        startMainActivityIfAllDone();
                    }
                    else if (state == WorkInfo.State.FAILED)
                    {
                        signOutAwsOnFailedLogin();

                        mDlg.dismiss();
                        DialogHelper.showDialog(ActivityLogin.this, R.string.sign_in_failed_title, R.string.sign_in_failed_unknown, () -> {});
                    }
                }
            };

            MyFiziqSdkManager.initialiseSdk((resultCode, result) ->
            {
                if (resultCode.isOk())
                {
                    mSdkInitDone = true;

                    // We shouldn't download the avatars in parallel
                    // If initialiseSdk fails but the avatar download succeeds,
                    // then we need to abort the login process but we would've
                    // already downloaded all the avatars :(
                    MyFiziqAvatarDownloadManager.getInstance().getAvatarsNow(downloadCallbacks);
                }
                else
                {
                    signOutAwsOnFailedLogin();

                    mDlg.dismiss();
                    DialogHelper.showDialog(this, R.string.sign_in_failed_title, R.string.sign_in_failed_unknown, () -> {});
                }
            });


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
        else if (responseCode.isConfigurationEmpty())
        {
            Timber.w("Configuration is empty. Signing out the user and relaunching MyFiziq.");

            mDlg.dismiss();

            // Session is invalid, sign out the user and send them to the welcome screen
            MyFiziqSdkManager.signOut((responseCode1, result1) -> startActivityEntrypoint());
        }
        else if (responseCode.isInternetDown())
        {
            Timber.w("No network connection");

            mDlg.dismiss();
            DialogHelper.showInternetDownDialog(this);
        }
        else if (responseCode == SdkResultCode.SDK_ERROR_USER_NOT_EXIST)
        {
            mDlg.dismiss();
            emailAddress.setError(getString(R.string.sign_in_failed_wrong_username));
            emailAddress.requestFocus();
        }
        else if (responseCode == SdkResultCode.SDK_ERROR_WRONG_PASSWORD)
        {
            mDlg.dismiss();
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            password.setError(getString(R.string.sign_in_failed_wrong_password));
            password.requestFocus();
        }
        else
        {
            Timber.w("Sign in failed");

            mDlg.dismiss();

            ClockAccuracyUtils.getClockAccuracy(this::handleClockAccuracyResponse);
        }
    }

    private void handleClockAccuracyResponse(long minutesOffBy)
    {
        runOnUiThread(() ->
        {
            if (Math.abs(minutesOffBy) > 60)
            {
                DialogHelper.showDialog(this, R.string.sign_in_failed_title, R.string.sign_in_failed_wrong_time, () -> {});
            }
            else
            {
                DialogHelper.showDialog(this, R.string.sign_in_failed_title, R.string.sign_in_failed, () -> {});
            }
        });
    }

    /**
     * Prompt the user to save their username and password to the Android Autofill service if
     * they're running Android Oreo or higher.
     */
    private void commitAutofillAction()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOFILL))
        {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            autofillManager.commit();
        }
    }

    private void startMainActivityIfAllDone()
    {
        if (mAvatarReqDone && mSdkInitDone && !isPaused)
        {
            mDlg.dismiss();
            mHandler.post(() ->
            {
                Intent mainActivity = new Intent(ActivityLogin.this, DebugActivity.class);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                startActivity(mainActivity);
            });
        }
    }

    private void startActivityEntrypoint()
    {
        if (isFinishing() || isDestroyed())
        {
            return;
        }

        mDlg.dismiss();

        Intent loginActivity = new Intent(this, ActivityEntrypoint.class);
        loginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(loginActivity);
    }

    private void signOutAwsOnFailedLogin()
    {
        SignOutOptions signOutOptions = SignOutOptions.builder().signOutGlobally(false).build();

        try
        {
            AWSMobileClient.getInstance().signOut(signOutOptions);
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception occurred when signing out after a failed login");
        }
    }
}
