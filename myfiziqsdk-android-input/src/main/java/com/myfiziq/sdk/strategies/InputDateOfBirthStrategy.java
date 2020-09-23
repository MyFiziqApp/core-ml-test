package com.myfiziq.sdk.strategies;

import android.view.View;
import android.widget.EditText;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.util.Date;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Contains the business logic for determining the user's Date of Birth to be shown on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}.
 */
public class InputDateOfBirthStrategy
{
    private InputDateOfBirthStrategy()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Determines what date of birth to show on {@link com.myfiziq.sdk.fragments.FragmentCreateAvatar}.
     */
    @Nullable
    public static Date determineDateOfBirth(ParameterSet parameterSet)
    {
        if (parameterSet.hasParam(R.id.TAG_ARG_INPUT_DATE_OF_BIRTH))
        {
            String dateOfBirthString = parameterSet.getStringParamValue(R.id.TAG_ARG_INPUT_DATE_OF_BIRTH, "");
            Date parsedDateOfBirth = DateOfBirthCoordinator.parseDateOfBirth(dateOfBirthString);

            if (parsedDateOfBirth != null)
            {
                return parsedDateOfBirth;
            }
            else
            {
                Timber.w("Provided date of birth is not in the correct format. Cannot parse: %s", dateOfBirthString);
            }
        }

        try
        {
            return DateOfBirthCoordinator.getDateOfBirth();
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot parse date of birth");
        }

        return null;
    }

    /**
     * Determines if the date of birth should be enabled for the screen.
     */
    public static boolean isDateOfBirthEnabled(ParameterSet parameterSet, EditText dateOfBirthEditText, boolean defaultVal)
    {
        // If the layout has already set DoB to Invisible/Gone disable DoB.
        if (dateOfBirthEditText.getVisibility() != View.VISIBLE)
        {
            return false;
        }

        if (parameterSet.hasParam(R.id.TAG_ARG_INPUT_ENABLE_BIRTH_DATE))
        {
            return parameterSet.getBooleanParamValue(R.id.TAG_ARG_INPUT_ENABLE_BIRTH_DATE, defaultVal);
        }

        return defaultVal;
    }
}
