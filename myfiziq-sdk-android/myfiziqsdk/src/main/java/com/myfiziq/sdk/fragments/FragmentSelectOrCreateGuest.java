package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.GuestHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateGuest;
import com.myfiziq.sdk.views.ItemViewGuest;

import androidx.annotation.NonNull;

public class FragmentSelectOrCreateGuest extends FragmentSelectGuest
{
    private IntentManagerService<Void> intentManagerService;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View createdView = super.onCreateView(inflater, container, savedInstanceState);

        ActionBarHelper.setActionBarTitle(getActivity(), getString(com.myfiziq.sdk.R.string.myfiziqsdk_title_new_measurement));
        return createdView;
    }

    @Override
    public void onGuestSelected(View v)
    {
        if (getActivity() == null || getMyActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        ModelAvatar thisModel = (ModelAvatar) v.getTag(R.id.TAG_MODEL);
        String selectedGuestName = thisModel.getGuestName();

        AsyncHelper.run(
                () -> GuestHelper.persistGuestSelection(selectedGuestName),
                () ->
                {
                    GuestHelper.prepareGuestCapture(mParameterSet);
                    mParameterSet.startNext(getMyActivity(), true);
                },
                true
        );
    }

    protected void setRecyclerView()
    {
        mManager = new RecyclerManager(getActivity(), getLoaderManager(), this)
        {
            @Override
            public void bind(int holderId, int id, int position, View view)
            {
                if (view.getId() == R.id.guestHeader)
                {
                    View createGuestContainer = view.findViewById(R.id.createGuestContainer);
                    createGuestContainer.setOnClickListener(v -> {
                        ParameterSet createGuest = StateGuest.getCreateGuest();
                        createGuest.addNextSet(mParameterSet.getNext());
                        createGuest.start(getMyActivity(), false);
                    });
                }
            }
        };

        mManager.setupRecycler(
                this,
                recycler,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(new Parameter.Builder()
                                .addHeader(R.layout.view_guest_item_create_header)
                                .setModel(ModelAvatar.class)
                                .setView(ItemViewGuest.class)
                                .setWhere(
                                        ModelAvatar.getWhereWithGuestAvatars(
                                                "miscGuest IS NOT NULL AND TRIM (miscGuest, ' ') != '') GROUP BY (miscGuest"
                                        )
                                )
                                .build())
                        .build()
        );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (getActivity() == null || getMyActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (intentManagerService != null)
        {
            intentManagerService.unbindAll();
        }
    }
}