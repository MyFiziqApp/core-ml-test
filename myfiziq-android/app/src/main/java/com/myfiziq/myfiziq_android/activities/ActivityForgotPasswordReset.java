package com.myfiziq.myfiziq_android.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.Utils;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.UiUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;

public class ActivityForgotPasswordReset extends AppCompatActivity
{
    private TextInputLayout emailCodeContainer;
    private EditText emailCodeEditText;
    private TextInputLayout passwordContainer;
    private EditText passwordEditText;
    private TextInputLayout confirmPasswordContainer;
    private EditText confirmPasswordEditText;
    private Button btnNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword_reset);

        emailCodeContainer = findViewById(R.id.emailCodeContainer);
        emailCodeEditText = emailCodeContainer.getEditText();
        passwordContainer = findViewById(R.id.passwordContainer);
        passwordEditText = passwordContainer.getEditText();
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        confirmPasswordEditText = confirmPasswordContainer.getEditText();
        btnNext = findViewById(R.id.btnNext);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarHelper.showBackButton(this);
        ActionBarHelper.setActionBarTitle(this, getString(R.string.forgot_password));

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        btnNext.setOnClickListener((view) -> onNextClicked());

        Utils.enablePasswordVisibilityButtonOnTextChange(passwordContainer);
        Utils.enablePasswordVisibilityButtonOnTextChange(confirmPasswordContainer);

        UiUtils.disablePasswordToggleButtonRipple(passwordContainer);
        UiUtils.disablePasswordToggleButtonRipple(confirmPasswordContainer);
    }

    private void onNextClicked()
    {
        if (!validate())
        {
            Timber.e("Form validation failed");
            return;
        }

        String resetCode = emailCodeEditText.getText().toString();
        String newPassword = passwordEditText.getText().toString();

        if (!ConnectivityHelper.isNetworkAvailable(this))
        {
            DialogHelper.showInternetDownDialog(this);
            return;
        }

        AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, getString(com.myfiziq.sdk.R.string.please_wait), null);

        MyFiziqSdkManager.resetPasswordWithResetCode(resetCode, newPassword, (responseCode, result) ->
        {
            dialog.dismiss();
            runOnUiThread(() -> handlePasswordResetResponse(responseCode, result));
        });
    }

    private boolean validate()
    {
        int minimumPasswordLength = getResources().getInteger(R.integer.minimum_password_length);

        emailCodeEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        if (TextUtils.isEmpty(emailCodeEditText.getText()))
        {
            emailCodeEditText.setError(getString(R.string.error_emptyemailcode));
            emailCodeEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(passwordEditText.getText()))
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            passwordEditText.setError(getString(R.string.error_emptypassword));
            passwordEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(confirmPasswordEditText.getText()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPasswordEditText.setError(getString(R.string.error_emptyconfirmpassword));
            confirmPasswordEditText.requestFocus();
            return false;
        }
        else if (passwordEditText.getText().toString().length() < minimumPasswordLength)
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            passwordEditText.setError(getString(R.string.error_tooshortpassword));
            passwordEditText.requestFocus();
            return false;
        }
        else if (!passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPasswordEditText.setError(getString(R.string.error_passwordnomatch));
            confirmPasswordEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void handlePasswordResetResponse(SdkResultCode responseCode, String result)
    {
        if (responseCode == SdkResultCode.HTTP_EXPIRED)
        {
            Toast.makeText(ActivityForgotPasswordReset.this, getString(R.string.forgot_password_reset_error_code_expired), Toast.LENGTH_LONG).show();
        }
        else if (responseCode.isOk())
        {
            new AlertDialog.Builder(ActivityForgotPasswordReset.this)
                .setTitle(getString(R.string.forgot_password_reset_successful_title))
                .setMessage(R.string.forgot_password_reset_successful_message)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> finishAffinity())
                .show();
        }
        else
        {
            Toast.makeText(ActivityForgotPasswordReset.this, getString(R.string.forgot_password_reset_error_generic), Toast.LENGTH_LONG).show();
        }
    }
}
