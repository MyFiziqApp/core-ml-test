package com.myfiziq.sdk.lifecycle;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.MyFiziqActivity;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.ModelSetting;

/**
 * Creates a route to visit the Capture screen.
 */
public class StateCapture
{
    private StateCapture()
    {
        // Empty hidden constructor for the utility class
    }

    public static ParameterSet getCapture()
    {
        ParameterSet.Builder builder = new ParameterSet.Builder(MyFiziqActivity.class);

        return getCapture(builder);
    }

    /**
     * Create a parameter set for capture with known gender, height and weight.
     *
     * @param gender The gender of the user.
     * @param height The user's height.
     * @param weight The user's weight.
     * @return A {@link ParameterSet} that contains a route to the capture screen.
     */
    public static ParameterSet getCapture(Gender gender, String height, String weight)
    {
        return getCapture(gender, Float.parseFloat(height), Float.parseFloat(weight));
    }

    /**
     * Create a parameter set for capture with known gender, height and weight.
     *
     * @param gender The gender of the user.
     * @param height The user's height.
     * @param weight The user's weight.
     * @return A {@link ParameterSet} that contains a route to the capture screen.
     */
    public static ParameterSet getCapture(Gender gender, float height, float weight)
    {
        ParameterSet.Builder builder = new ParameterSet.Builder(MyFiziqActivity.class)
                .addParam(new Parameter(R.id.TAG_ARG_GENDER, gender.name()))
                .addParam(new Parameter(R.id.TAG_ARG_HEIGHT_IN_CM, String.valueOf(height)))
                .addParam(new Parameter(R.id.TAG_ARG_WEIGHT_IN_KG, String.valueOf(weight)))
                .addParam(new Parameter(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, Kilograms.internalName))
                .addParam(new Parameter(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, Centimeters.internalName));

        return getCapture(builder);
    }

    private static ParameterSet getCapture(ParameterSet.Builder builder)
    {
        return builder.build();

    }
}