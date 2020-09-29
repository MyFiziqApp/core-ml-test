package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.GuestHelper;
import com.myfiziq.sdk.util.UiUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

public class FragmentCreateGuest extends BaseFragment implements FragmentInterface
{
    private TextInputEditText guestName;
    private Button continueButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_title_create_guest));


        if (mParameterSet != null)
        {
            applyParameters(view);
        }

        guestName = view.findViewById(R.id.guestName);
        continueButton = view.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(v -> onCreateGuestClicked());

        AsyncHelper.run(
                this::generateRecommendedName,
                recommendedName -> guestName.setText(recommendedName),
                true
        );

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ActionBarHelper.showBackButton(getActivity());
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_create_guest;
    }

    private void onCreateGuestClicked()
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (validate())
        {
            UiUtils.hideSoftKeyboard(getActivity());

            String selectedGuestName = guestName.getText().toString();

            AsyncHelper.run(
                    () ->
                    {
                        // Save the guest name to settings so we can see it when viewing all the avatars
                        // Do this regardless of whether we're in Settings or creating an avatar
                        GuestHelper.persistGuestSelection(selectedGuestName);
                    },
                    () ->
                    {
                        GuestHelper.prepareGuestCapture(mParameterSet);
                        mParameterSet.startNext(getMyActivity(), true);
                    },
                    true
            );
        }
    }

    private boolean validate()
    {
        guestName.setError(null);

        String enteredGuestName = guestName.getText().toString();

        if (TextUtils.isEmpty(enteredGuestName))
        {
            guestName.setError(getString(R.string.error_emptyguestname));
            guestName.requestFocus();
            return false;
        }

        Pattern guestNamePattern = Pattern.compile("([a-zA-Z0-9 ]+)");
        Matcher guestNameMatcher = guestNamePattern.matcher(enteredGuestName);

        if (!guestNameMatcher.matches())
        {
            guestName.setError(getString(R.string.error_invalidguestname));
            guestName.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Generates a recommended name for the guest based on the number of guests that have been created.
     * (e.g. "Guest 2" if one other guest has been created).
     */
    private String generateRecommendedName()
    {
        ArrayList<ModelAvatar> guests = ORMTable.getModelList(
                ModelAvatar.class,
                ModelAvatar.getWhereWithGuestAvatars(
                        "miscGuest IS NOT NULL AND TRIM (miscGuest, ' ') != '') GROUP BY (miscGuest"
                ),
                ""
        );

        int guestCount = (guests == null) ? 0 : guests.size();
        int numberOfAvatarsForRecommendedName;
        String recommendedName;

        // Try to find a unique recommended name
        // (e.g. if "Guest 3" is already in use, try to recommend "Guest 4" if it's not in use, etc)
        do
        {
            recommendedName = "Guest " + (guestCount + 1);

            numberOfAvatarsForRecommendedName = ORMDbQueries.countAvatarsCreatedByGuest(recommendedName);

            guestCount++;
        }
        while (numberOfAvatarsForRecommendedName > 0);

        return recommendedName;
    }

}