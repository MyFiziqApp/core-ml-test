package com.myfiziq.sdk.lifecycle;


import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.fragments.FragmentCreateAvatar;
import com.myfiziq.sdk.fragments.FragmentCreateAvatarInsights;
import com.myfiziq.sdk.fragments.FragmentImageConsent;
import com.myfiziq.sdk.helpers.SisterColors;

/**
 * Creates a route to visit the Input screen.
 */
public class StateInput
{
    // Don't put these in an XML resources file.
    // We don't want these to accidentally change or be modified by the customer. It would corrupt our logic.
    public static final String IMAGE_CONSENT_NAME = "IMAGE-CONSENT";
    public static final String INPUT_NAME = "INPUT";


    private StateInput()
    {
        // Empty hidden constructor for the utility class
    }

    public static ParameterSet getInput(ModelAvatar avatar, ParameterSet existingSet)
    {
        // DEMO APP REQUIREMENT: ALWAYS show the image consent screen regardless of how many times the user has seen it.
        boolean bShowTncPage = true;

        Class<?> fragmentClass = FragmentCreateAvatar.class;

        boolean insightsMode = ModelSetting.getSetting(ModelSetting.Setting.FEATURE_INSIGHTS_INPUT, false);

        if (insightsMode)
        {
            fragmentClass = FragmentCreateAvatarInsights.class;
        }

        ParameterSet inputSet = new ParameterSet.Builder(fragmentClass)
                .setName(INPUT_NAME)
                .build();

        if (SisterColors.getInstance().isSisterMode())
        {
            // don't show image consent for onDemand.
            bShowTncPage = false;
        }

        if (bShowTncPage)
        {
            // User hasn't accepted the conditions on the image consent screen yet
            // REQUIREMENT: ALWAYS show the image consent screen regardless of how many times the user has seen it.
            ParameterSet imageConsentScreen = new ParameterSet.Builder(FragmentImageConsent.class)
                    .setName(IMAGE_CONSENT_NAME)
                    // don't force DoB to be enabled.
                    //.addParam(new Parameter(R.id.TAG_ARG_INPUT_ENABLE_BIRTH_DATE, Boolean.TRUE.toString()))
                    .build();

            imageConsentScreen.insertNextSet(0, existingSet);
            inputSet.insertNextSet(0, imageConsentScreen);
        }
        else
            inputSet.insertNextSet(0, existingSet);

        return inputSet;
    }
}