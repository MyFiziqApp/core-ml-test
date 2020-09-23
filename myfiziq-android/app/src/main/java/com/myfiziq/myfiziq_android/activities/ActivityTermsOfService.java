package com.myfiziq.myfiziq_android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.views.ClickSpan;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import timber.log.Timber;

public class ActivityTermsOfService extends AppCompatActivity
{
    private TextView textView;
    private CheckBox iAgreeCheckbox;
    private Button continueButton;
    private String mVersion = "1";
    private ParameterSet mParameterSet = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termsofservice);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarHelper.showBackButton(this);

        textView = findViewById(R.id.textView);
        iAgreeCheckbox = findViewById(R.id.iAgreeCheckbox);
        continueButton = findViewById(R.id.continueButton);

        textView.setText(Html.fromHtml(getString(R.string.terms_of_service_text)));

        String tos = getString(R.string.i_agree_to_the_terms_of_service);
        String pp = getString(R.string.privacy_policy);
        SpannableString ss = new SpannableString(tos + " " + pp + ".");
        ss.setSpan(new URLSpan(pp), tos.length()+1, tos.length()+pp.length()+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        iAgreeCheckbox.setMovementMethod(LinkMovementMethod.getInstance());
        iAgreeCheckbox.setText(createClickableSpans(ss, url ->
        {
            Intent newActivity = new Intent(this, ActivityPrivacyPolicy.class);
            startActivity(newActivity);
        }));

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        Intent intent = getIntent();
        mParameterSet = intent.getParcelableExtra(BaseFragment.BUNDLE_PARAMETERS);
        if (mParameterSet != null)
        {
            ActionBarHelper.setActionBarTitle(this,
                    mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_TITLE, "Terms of Service"));
            textView.setText(Html.fromHtml(mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_CONTENT, getString(com.myfiziq.myfiziqsdk_android_input.R.string.terms_of_service_text))));
            mVersion = mParameterSet.getStringParamValue(com.myfiziq.myfiziqsdk_android_input.R.id.TAG_ARG_PAGE_VERSION, "");
        }
        else
        {
            ActionBarHelper.setActionBarTitle(this, "Terms of Service");
        }

        bindListeners();
    }

    static Spannable createClickableSpans(Spanned original, ClickSpan.OnClickListener listener)
    {
        SpannableString result = new SpannableString(original);
        URLSpan[] spans = result.getSpans(0, result.length(), URLSpan.class);

        for (URLSpan span : spans)
        {
            int start = result.getSpanStart(span);
            int end = result.getSpanEnd(span);
            int flags = result.getSpanFlags(span);

            result.removeSpan(span);
            result.setSpan(new ClickSpan(span.getURL(), listener), start, end, flags);
        }

        return result;
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

    private void bindListeners()
    {
        iAgreeCheckbox.setOnCheckedChangeListener(this::onToggleIAgreeCheckbox);
        continueButton.setOnClickListener(this::onContinueClicked);
    }

    private void onToggleIAgreeCheckbox(CompoundButton buttonView, boolean isChecked)
    {
        continueButton.setEnabled(isChecked);
    }

    private void processDestination()
    {
        if (mParameterSet == null)
        {
            Timber.e("Destination is null");
        }
        else
        {
            mParameterSet.start();
        }

        finish();
    }

    private void onContinueClicked(View view)
    {
        if (iAgreeCheckbox.isChecked())
        {
            AsyncHelper.run(
                    () -> ModelSetting.putSetting(ModelSetting.Setting.AGREED_TO_TOS, mVersion),
                    () -> processDestination(),
                    true
            );

        }
    }


}
