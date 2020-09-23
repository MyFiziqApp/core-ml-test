package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import timber.log.Timber;


public class FragmentImageConsent extends BaseFragment implements FragmentInterface
{
    private CheckBox iAgreeCheckbox;
    private Button continueButton;
    private TextView textView;
    private String mVersion = "1";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (willSoonClose())
        {
            Timber.i("%s won't be rendered since the fragment will soon close", FragmentImageConsent.class.getSimpleName());
            return null;
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);
        textView = view.findViewById(R.id.textView);
        iAgreeCheckbox = view.findViewById(R.id.iAgreeCheckbox);
        continueButton = view.findViewById(R.id.continueButton);

        if (mParameterSet != null)
        {
            ActionBarHelper.setActionBarTitle(getActivity(),
                    mParameterSet.getStringParamValue(R.id.TAG_ARG_PAGE_TITLE, getString(R.string.myfiziqsdk_title_image_consent)));
            textView.setText(Html.fromHtml(mParameterSet.getStringParamValue(R.id.TAG_ARG_PAGE_CONTENT, getString(R.string.image_consent_conditions))));
            mVersion = mParameterSet.getStringParamValue(R.id.TAG_ARG_PAGE_VERSION, "");
        }
        else
        {
            ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_title_image_consent));
            textView.setText(Html.fromHtml(getString(R.string.image_consent_conditions)));
        }

        bindListeners();

        if (null != mParameterSet)
        {
            applyParameters(view);
        }

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ActionBarHelper.showBackButton(getActivity());
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.respond(IntentResponses.HIDE_BOTTOM_NAVIGATION_BAR, null);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.respond(IntentResponses.SHOW_BOTTOM_NAVIGATION_BAR, null);
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_image_consent;
    }


    /**
     * Binds listeners to views.
     */
    private void bindListeners()
    {
        iAgreeCheckbox.setOnCheckedChangeListener(this::onToggleIAgreeCheckbox);
        continueButton.setOnClickListener(v -> onContinueClicked());
    }

    private void onToggleIAgreeCheckbox(CompoundButton buttonView, boolean isChecked)
    {
        continueButton.setEnabled(isChecked);
    }

    /**
     * Executed when the user clicks the "Continue" button to move to the next screen in the wizard.
     */
    private void onContinueClicked()
    {
        if (iAgreeCheckbox.isChecked())
        {
            AsyncHelper.run(
                    () -> ModelSetting.putSetting(ModelSetting.Setting.AGREED_TO_IMAGE_CONSENT, mVersion),
                    () -> mParameterSet.startNext(getMyActivity(), false),
                    true
            );
        }
    }
}
