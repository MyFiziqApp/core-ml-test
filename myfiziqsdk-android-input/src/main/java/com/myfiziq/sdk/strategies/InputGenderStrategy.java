package com.myfiziq.sdk.strategies;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Contains the business logic for determining the user's gender to be shown on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}..
 */
public class InputGenderStrategy
{
    private InputGenderStrategy()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Determines what Gender to show on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}.
     */
    @Nullable
    public static Gender determineGender(ParameterSet parameterSet, ModelUserProfile userProfile)
    {
        // If the gender has been passed through the ParameterSet
        if (parameterSet.hasParam(R.id.TAG_ARG_GENDER))
        {
            String parameterSetGender = parameterSet.getStringParamValue(R.id.TAG_ARG_GENDER, "");

            try
            {
                return Gender.valueOf(parameterSetGender);
            }
            catch (Exception e)
            {
                // Can't parse the gender from the ParameterSet? Log it and try a different strategy further down.
                Timber.w("Cannot parse Gender supplied in ParameterSet");
            }
        }

        // Use Gender from the User Profile if it's there
        if (userProfile != null
                && userProfile.getGender() != null)
        {
            return userProfile.getGender();
        }

        // Else return null
        return null;
    }
}
