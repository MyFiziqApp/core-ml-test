package com.myfiziq.myfiziq_android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class ActivityPrivacyPolicy extends AppCompatActivity
{
    private TextView textView;
    private String mVersion = "1";
    private ParameterSet mParameterSet = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarHelper.showBackButton(this);

        textView = findViewById(R.id.textView);
        textView.setText(Html.fromHtml(getString(R.string.privacy_policy_text)));

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        Intent intent = getIntent();
        mParameterSet = intent.getParcelableExtra(BaseFragment.BUNDLE_PARAMETERS);
        if (mParameterSet != null)
        {
            ActionBarHelper.setActionBarTitle(this,
                    mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_TITLE, getString(R.string.privacy_policy)));
            textView.setText(Html.fromHtml(mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_CONTENT, getString(com.myfiziq.myfiziqsdk_android_input.R.string.terms_of_service_text))));
            mVersion = mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_VERSION, "");
        }
        else
        {
            ActionBarHelper.setActionBarTitle(this, R.string.privacy_policy);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
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
}
