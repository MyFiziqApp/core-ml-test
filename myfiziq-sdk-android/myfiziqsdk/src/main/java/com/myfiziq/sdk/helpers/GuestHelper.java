package com.myfiziq.sdk.helpers;

import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.LocalUserDataKey;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.util.Date;

public class GuestHelper
{
    private GuestHelper()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Adds the guest name to the future avatar that will be created (if a future one will be created)
     * and saves the guest name to the database as the currently selected guest.
     *
     * It also recalculates the adjusted values for all avatars with (or without) the guest taken into account
     * depending
     *
     * @param selectedGuestName The name of the guest. Pass a blank value for no selected guest.
     */
    public static void persistGuestSelection(String selectedGuestName)
    {
        MyFiziq.getInstance().setGuestUser(selectedGuestName);

        MyFiziq.getInstance().updateAllAvatarAdjustedValues();
    }

    /**
     * Determines if a guest has currently been selected.
     */
    public static boolean isGuestSelected()
    {
        String selectedGuest = MyFiziq.getInstance().getGuestUser();
        return !TextUtils.isEmpty(selectedGuest);
    }

    /**
     * Prepares for guest mode capture using the ParameterSet after guest is selected.
     *
     * Sets the avatar's guest name and the guest's date of birth.
     */
    public static void prepareGuestCapture(ParameterSet set)
    {
        ParameterSet captureSet = set.getSubSet("CAPSIDE");
        if (captureSet != null)
        {
            String stringDob = captureSet.getParam(R.id.TAG_ARG_DOB).getValue();
            Date dateDob = DateOfBirthCoordinator.parseDateOfBirth(stringDob);
            DateOfBirthCoordinator.setDateOfBirth(dateDob);

            if (captureSet.hasParam(R.id.TAG_ARG_ETHNICITY))
            {
                String stringeth = captureSet.getParam(R.id.TAG_ARG_ETHNICITY).getValue();
                MyFiziqLocalUserDataHelper.setValue(LocalUserDataKey.ETHNICITY, stringeth);
            }

            ModelAvatar avatar = (ModelAvatar) captureSet.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
            avatar.setGuestToCurrent();
            captureSet.setParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR, avatar));
        }
    }
}
