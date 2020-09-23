package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.helpers.ActionBarHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FragmentTermsOfService extends BaseFragment
{
    @Override
    public void onResume()
    {
        super.onResume();

        ActionBarHelper.showBackButton(getActivity());
        ActionBarHelper.setActionBarTitle(getActivity(), R.string.myfiziqsdk_terms_of_service);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.textView3)).setText(Html.fromHtml(getString(R.string.terms_of_service_text)));
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_terms_of_service;
    }
}
