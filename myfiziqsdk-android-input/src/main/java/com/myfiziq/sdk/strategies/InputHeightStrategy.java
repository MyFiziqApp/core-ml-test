package com.myfiziq.sdk.strategies;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Contains the business logic for determining the user's height to be shown on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}..
 */
public class InputHeightStrategy
{
    private InputHeightStrategy()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Determines what Height and Units of Measurement to show on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}.
     */
    @Nullable
    public static Length determineHeight(ParameterSet parameterSet, ModelUserProfile userProfile)
    {
        Double height = null;

        // If the height has been passed through the ParameterSet
        if (parameterSet.hasParam(R.id.TAG_ARG_HEIGHT_IN_CM))
        {
            String heightString = parameterSet.getStringParamValue(R.id.TAG_ARG_HEIGHT_IN_CM, "");

            try
            {
                height = Double.parseDouble(heightString);
            }
            catch (Exception e)
            {
                // Can't parse the height from the ParameterSet? Log it and try a different strategy further down.
                Timber.e(e, "Height was passed in the ParameterSet but it could not be converted to a double. Received: %s", heightString);
            }
        }

        // If we can't get a height from the ParameterSet, try to get one from the profile.
        if (height == null
                && userProfile != null
                && userProfile.getHeight() != null
                && userProfile.getHeight() > 0.1)
        {
            height = userProfile.getHeight();
        }

        // No height found? Return null.
        if (height == null)
        {
            return null;
        }

        // Return the height with the desired units of measurement
        Class<? extends Length> unitsOfMeasurement = determineUnitsOfMeasurement(parameterSet);
        return Length.fromCentimeters(unitsOfMeasurement, height);
    }

    public static Class<? extends Length> determineUnitsOfMeasurement(ParameterSet parameterSet)
    {
        // If the height units of measurement has been passed through the ParameterSet
        if (parameterSet.hasParam(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS))
        {
            try
            {
                String preferredHeightUnit = parameterSet.getStringParamValue(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, "");
                Length lengthObject = Length.fromCentimeters(preferredHeightUnit, 0);

                return lengthObject.getClass();
            }
            catch (Exception e)
            {
                // Can't parse the height units of measurement from the ParameterSet? Log it and try a different strategy further down.
                Timber.e(e, "Unrecognised height unit of measurement supplied in ParameterSet");
            }
        }

        return AppWideUnitSystemHelper.getAppWideHeightUnit();
    }
}
