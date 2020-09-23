package com.myfiziq.myfiziq_android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ActivityForgotPassword extends AppCompatActivity
{
    private EditText emailAddressEditText;
    private Button nextButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);


        emailAddressEditText = findViewById(R.id.emailAddressEditText);
        nextButton = findViewById(R.id.btnNext);

        bindListeners();


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarHelper.showBackButton(this);
        ActionBarHelper.setActionBarTitle(this, getString(R.string.forgot_password));

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));
    }

    public void onNextClicked()
    {
        if (!validate())
        {
            return;
        }


        Utils.hideSoftKeyboard(this);

        String emailAddress = emailAddressEditText.getText().toString();

        if (!ConnectivityHelper.isNetworkAvailable(this))
        {
            DialogHelper.showInternetDownDialog(this);
            return;
        }

        AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, getString(com.myfiziq.sdk.R.string.please_wait), null);

        MyFiziqSdkManager.requestPasswordResetWithEmail(emailAddress, (responseCode, result, deliveryDetails) ->
        {
            dialog.dismiss();
            handlePasswordResetResponse(responseCode, emailAddress, deliveryDetails);
        });
    }

    private boolean validate()
    {
        emailAddressEditText.setError(null);

        if (TextUtils.isEmpty(emailAddressEditText.getText()))
        {
            emailAddressEditText.setError(getString(R.string.error_emptyemailaddress));
            emailAddressEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void handlePasswordResetResponse(SdkResultCode responseCode, String emailAddress, UserCodeDeliveryDetails deliveryDetails)
    {
        if (!responseCode.isOk())
        {
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.forgot_password_error_checkEmailAddress), Toast.LENGTH_LONG).show());
            return;
        }

        Intent intent = new Intent(this, ActivityForgotPasswordReset.class);
        intent.putExtra("username", emailAddress);
        intent.putExtra("cognitoDestination", deliveryDetails.getDestination());
        intent.putExtra("cognitoDeliveryMedium", deliveryDetails.getDeliveryMedium());
        intent.putExtra("cognitoAttributeName", deliveryDetails.getAttributeName());

        startActivity(intent);
    }

    private void bindListeners()
    {
        nextButton.setOnClickListener(view -> onNextClicked());
    }
}
