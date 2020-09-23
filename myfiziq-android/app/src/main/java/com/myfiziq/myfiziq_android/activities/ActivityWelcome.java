package com.myfiziq.myfiziq_android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.myfiziq.myfiziq_android.BuildConfig;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.UiUtils;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityWelcome extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT, UiUtils.getThemePrimaryColor(this));

        bindButtons();

        TextView textViewVersion = findViewById(R.id.textViewVersion);
        if (null != textViewVersion)
        {
            textViewVersion.setText(String.format(Locale.getDefault(), "%s %s", BuildConfig.VERSION_NAME, com.myfiziq.sdk.BuildConfig.SDK_VERSION));
        }
    }

    private void bindButtons()
    {
        Button joinButton = findViewById(R.id.joinButton);
        Button loginButton = findViewById(R.id.loginButton);
        joinButton.setText("");
        loginButton.setOnClickListener(view -> onLoginClicked());
    }


    public void onLoginClicked()
    {
       // ParameterSet.Builder builder = new ParameterSet.Builder(ActivityLogin.class);

        Intent newActivity = new Intent(this, ActivityLogin.class);
        //newActivity.putExtra(BaseFragment.BUNDLE_PARAMETERS, builder.build());
        startActivity(newActivity);
    }
}
