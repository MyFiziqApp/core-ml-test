package com.myfiziq.sdk.validators;

import android.content.Context;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Feet;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.validators.vo.TypeOfValidationError;
import com.myfiziq.sdk.validators.vo.ValidationErrorMessage;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Provides a service for validating measurements.
 */
public class LengthMeasurementValidatorService
{
    private Context context;

    public LengthMeasurementValidatorService(Context context)
    {
        this.context = context;
    }

    /**
     * Determines if a measurement is valid.
     */
    public boolean isValid(Length measurement)
    {
        MeasurementValidationCriteria validationCriteria = findValidationCriteria(measurement.getInternalName());
        return validationCriteria.isValid(context, measurement.getValueInCm());
    }

    /**
     * Retrieves error text for an invalid measurement.
     *
     * If the measurement is valid, a blank string is returned.
     */
    public String getErrorText(Length measurement)
    {
        if (isValid(measurement))
        {
            return "";
        }

        MeasurementValidationCriteria validationCriteria = findValidationCriteria(measurement.getInternalName());
        String valueConstraint;

        TypeOfValidationError typeOfValidationError;

        if (validationCriteria.isTooLow(context, measurement.getValueInCm()))
        {
            typeOfValidationError = TypeOfValidationError.TOO_LOW;
            valueConstraint = validationCriteria.getMinimumFormattedValue(context);
        }
        else if (validationCriteria.isTooHigh(context, measurement.getValueInCm()))
        {
            typeOfValidationError = TypeOfValidationError.TOO_HIGH;
            valueConstraint = validationCriteria.getMaximumFormattedValue(context);
        }
        else
        {
            throw new IllegalStateException("Measurement of " + measurement + " is neither too low nor too high.");
        }


        List<ValidationErrorMessage> validationErrorMessages = buildListOfValidationErrorMessages();

        for (ValidationErrorMessage errorMessage: validationErrorMessages)
        {
            if (errorMessage.getTypeOfValidationError() == typeOfValidationError)
            {
                int errorStringResourceId = errorMessage.getErrorStringResourceId();
                String rawErrorString = context.getResources().getString(errorStringResourceId);

                return String.format(rawErrorString, valueConstraint);
            }
        }


        throw new UnsupportedOperationException("Error text not found for " + validationCriteria);
    }

    private List<MeasurementValidationCriteria> buildListOfValidationCriteria()
    {
        return Arrays.asList(
                new MeasurementValidationCriteria(Centimeters.internalName, R.dimen.minimum_height, R.dimen.maximum_height),
                new MeasurementValidationCriteria(Feet.internalName, R.dimen.minimum_height, R.dimen.maximum_height)
        );
    }

    private List<ValidationErrorMessage> buildListOfValidationErrorMessages()
    {
        return Arrays.asList(
                new ValidationErrorMessage(TypeOfValidationError.TOO_LOW, R.string.error_height_too_low),
                new ValidationErrorMessage(TypeOfValidationError.TOO_HIGH, R.string.error_height_too_high)
        );
    }

    /**
     * Locates the validation criteria for a given unit of measurement and type of measurement.
     *
     * If no validation criteria can be found for the specified combination, null is returned.
     */
    @Nullable
    private MeasurementValidationCriteria findValidationCriteria(String internalTypeName)
    {
        List<MeasurementValidationCriteria> validationCriteria = buildListOfValidationCriteria();

        for (MeasurementValidationCriteria criteria : validationCriteria)
        {
            if (criteria.getInternalTypeName().equals(internalTypeName))
            {
                return criteria;
            }
        }

        return null;
    }
}
