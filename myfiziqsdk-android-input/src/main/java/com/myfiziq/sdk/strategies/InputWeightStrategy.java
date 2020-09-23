package com.myfiziq.sdk.strategies;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import timber.log.Timber;

/**
 * Contains the business logic for determining the user's height to be shown on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}..
 */
public class InputWeightStrategy
{
    private InputWeightStrategy()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Determines what Weight and Units of Measurement to show on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}.
     */
    public static Weight determineWeight(ParameterSet parameterSet, ModelUserProfile userProfile)
    {
        Double weight = null;

        // If the weight has been passed through the ParameterSet
        if (parameterSet.hasParam(R.id.TAG_ARG_WEIGHT_IN_KG))
        {
            String weightString = parameterSet.getStringParamValue(R.id.TAG_ARG_WEIGHT_IN_KG, "");

            try
            {
                weight = Double.parseDouble(weightString);
            }
            catch (Exception e)
            {
                // Can't parse the weight from the ParameterSet? Log it and try a different strategy further down.
                Timber.e(e, "Weight was passed in the ParameterSet but it could not be converted to a double. Received: %s", weight);
            }
        }

        // If we can't get a weight from the ParameterSet, try to get one from the profile.
        if (weight == null
                && userProfile != null
                && userProfile.getWeight() != null
                && userProfile.getWeight() > 0.1)
        {
            weight = userProfile.getWeight();
        }

        // No weight found? Return null.
        if (weight == null)
        {
            return null;
        }

        // Return the weight with the desired units of measurement
        Class<? extends Weight> unitsOfMeasurement = determineUnitsOfMeasurement(parameterSet);
        return Weight.fromKilograms(unitsOfMeasurement, weight);
    }

    public static Class<? extends Weight> determineUnitsOfMeasurement(ParameterSet parameterSet)
    {
        // If the weight units of measurement has been passed through the ParameterSet
        if (parameterSet.hasParam(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS))
        {
            try
            {
                String preferredWeightUnit = parameterSet.getStringParamValue(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, "");
                Weight weightObject = Weight.fromKilograms(preferredWeightUnit, 0);

                return weightObject.getClass();
            }
            catch (Exception e)
            {
                // Can't parse the weight units of measurement from the ParameterSet? Log it and try a different strategy further down.
                Timber.e(e, "Unrecognised weight unit of measurement supplied in ParameterSet");
            }
        }

        return AppWideUnitSystemHelper.getAppWideWeightUnit();
    }
}
