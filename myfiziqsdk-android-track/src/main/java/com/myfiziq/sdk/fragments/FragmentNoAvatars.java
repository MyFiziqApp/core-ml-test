package com.myfiziq.sdk.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;

public class FragmentNoAvatars extends BaseFragment implements FragmentInterface
{
    private Button newMeasurement;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        newMeasurement = view.findViewById(R.id.newMeasurement);
        newMeasurement.setOnClickListener(this::onNewMeasurementClicked);

        return view;
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_no_avatars;
    }

    private void onNewMeasurementClicked(View view)
    {
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.requestAndListenForResponse(
                IntentPairs.ONBOARDING_ROUTE,
                result -> result.start(getMyActivity())
        );
    }
}
