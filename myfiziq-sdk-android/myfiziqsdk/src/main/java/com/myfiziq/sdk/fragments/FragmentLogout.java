package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FragmentLogout extends BaseFragment
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        IntentManagerService intentManagerService = new IntentManagerService(getActivity());
        intentManagerService.respond(IntentResponses.HIDE_BOTTOM_NAVIGATION_BAR, null);

        logout();

        return view;
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_logout;
    }

    private void logout()
    {
        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (!ConnectivityHelper.isNetworkAvailable(activity))
        {
            DialogHelper.showInternetDownDialog(activity, activity::onBackPressed);
            return;
        }

        MyFiziqSdkManager.signOut(this::onSignOutResponse);
    }

    private void onSignOutResponse(SdkResultCode responseCode, String result)
    {
        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (!responseCode.isOk())
        {
            DialogHelper.showDialog(activity, R.string.myfiziqsdk_error_logout_title,  R.string.myfiziqsdk_error_logout_body, activity::onBackPressed);
            return;
        }

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.requestAndListenForResponse(
                IntentPairs.LOGOUT_ROUTE,
                parameterSet -> parameterSet.start(getMyActivity())
        );

    }
}
