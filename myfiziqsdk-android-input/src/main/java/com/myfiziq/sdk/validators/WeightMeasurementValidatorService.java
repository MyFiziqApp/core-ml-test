package com.myfiziq.sdk.validators;

import android.content.Context;
import androidx.annotation.Nullable;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.Imperial;
import com.myfiziq.sdk.db.Metric;
import com.myfiziq.sdk.db.SystemOfMeasurement;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.validators.vo.TypeOfValidationError;
import com.myfiziq.sdk.validators.vo.ValidationErrorMessage;

import java.util.Arrays;
import java.util.List;

/**
 * Provides a service for validating measurements.
 * TODO: CHANGE FORMAT LIKE LENGTH MEASUREMENT VALIDATOR
 */
public class WeightMeasurementValidatorService
{
    /*private Context context;

    public WeightMeasurementValidatorService(Context context)
    {
        this.context = context;
    }

    *//**
     * Determines if a measurement is valid.
     *//*
    public boolean isValid(Weight measurement)
    {
        Class<? extends SystemOfMeasurement> systemOfMeasurement = measurement.getSystemOfMeasurement();
        MeasurementValidationCriteria validationCriteria = findValidationCriteria(systemOfMeasurement);

        return validationCriteria.isValid(context, measurement.getValueInKg());
    }

    *//**
     * Retrieves error text for an invalid measurement.
     *
     * If the measurement is valid, a blank string is returned.
     *//*
    public String getErrorText(Weight measurement)
    {
        if (isValid(measurement))
        {
            return "";
        }


        Class<? extends SystemOfMeasurement> systemOfMeasurement = measurement.getSystemOfMeasurement();
        MeasurementValidationCriteria validationCriteria = findValidationCriteria(systemOfMeasurement);

        double valueConstraint;

        TypeOfValidationError typeOfValidationError;

        if (validationCriteria.isTooLow(context, measurement.getValueInKg()))
        {
            typeOfValidationError = TypeOfValidationError.TOO_LOW;
            valueConstraint = validationCriteria.getMinimumValue(context);
        }
        else if (validationCriteria.isTooHigh(context, measurement.getValueInKg()))
        {
            typeOfValidationError = TypeOfValidationError.TOO_HIGH;
            valueConstraint = validationCriteria.getMaximumValue(context);
        }
        else
        {
            throw new IllegalStateException("Measurement of " + measurement + " is neither too low nor too high.");
        }


        List<ValidationErrorMessage> validationErrorMessages = buildListOfValidationErrorMessages();

        for (ValidationErrorMessage errorMessage: validationErrorMessages)
        {
            if (errorMessage.getSystemOfMeasurement() == systemOfMeasurement && errorMessage.getTypeOfValidationError() == typeOfValidationError)
            {
                int errorStringResourceId = errorMessage.getErrorStringResourceId();
                String rawErrorString = context.getResources().getString(errorStringResourceId);

                return String.format(rawErrorString, valueConstraint);
            }
        }


        throw new UnsupportedOperationException("Error text not found for " + systemOfMeasurement);
    }

    private List<MeasurementValidationCriteria> buildListOfValidationCriteria()
    {
        return Arrays.asList(
                new MeasurementValidationCriteria(Metric.class, R.dimen.minimum_metric_weight, R.dimen.maximum_metric_weight),
                new MeasurementValidationCriteria(Imperial.class, R.dimen.minimum_imperial_weight, R.dimen.maximum_imperial_weight)
        );
    }

    private List<ValidationErrorMessage> buildListOfValidationErrorMessages()
    {
        return Arrays.asList(
                new ValidationErrorMessage(Metric.class, TypeOfValidationError.TOO_LOW, R.string.error_metric_weight_toolow),
                new ValidationErrorMessage(Metric.class, TypeOfValidationError.TOO_HIGH, R.string.error_metric_weight_toohigh),
                new ValidationErrorMessage(Imperial.class, TypeOfValidationError.TOO_LOW, R.string.error_imperial_weight_toolow),
                new ValidationErrorMessage(Imperial.class, TypeOfValidationError.TOO_HIGH, R.string.error_imperial_weight_toohigh)
        );
    }

    *//**
     * Locates the validation criteria for a given unit of measurement and type of measurement.
     *
     * If no validation criteria can be found for the specified combination, null is returned.
     *//*
    @Nullable
    private MeasurementValidationCriteria findValidationCriteria(Class<? extends SystemOfMeasurement> systemOfMeasurement)
    {
        List<MeasurementValidationCriteria> validationCriteria = buildListOfValidationCriteria();

        for (MeasurementValidationCriteria criteria : validationCriteria)
        {
            if (criteria.getSystemOfMeasurement() == systemOfMeasurement)
            {
                return criteria;
            }
        }

        return null;
    }*/
}
