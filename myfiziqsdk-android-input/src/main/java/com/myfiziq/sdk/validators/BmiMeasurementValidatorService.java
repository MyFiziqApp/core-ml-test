package com.myfiziq.sdk.validators;

import android.content.Context;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.Weight;

import androidx.annotation.NonNull;

public class BmiMeasurementValidatorService
{
    @NonNull
    private Context context;

    public BmiMeasurementValidatorService(@NonNull Context context)
    {
        this.context = context;
    }

    /**
     * Determines if the BMI is valid.
     */
    public boolean isValid(@NonNull Length height, @NonNull Weight weight)
    {
        return !isBmiTooLow(height, weight) && !isBmiTooHigh(height, weight);
    }

    /**
     * Retrieves error text for an invalid BMI.
     */
    public String getErrorText(@NonNull Length height, @NonNull Weight weight)
    {
        if (isValid(height, weight))
        {
            return "";
        }

        if (isBmiTooLow(height, weight))
        {
            return context.getResources().getString(R.string.error_bmi_too_low);
        }

        if (isBmiTooHigh(height, weight))
        {
            return context.getResources().getString(R.string.error_bmi_too_high);
        }

        throw new IllegalStateException("BMI is neither too high nor too low but yet it is invalid.");
    }

    public int getMinimumBmi()
    {
        return context.getResources().getInteger(R.integer.minimum_bmi);
    }

    public int getMaximumBmi()
    {
        return context.getResources().getInteger(R.integer.maximum_bmi);
    }

    public boolean isBmiTooLow(@NonNull Length height, @NonNull Weight weight)
    {
        double bmi = calculateBmi(height, weight);
        return bmi < getMinimumBmi();
    }

    public boolean isBmiTooHigh(@NonNull Length height, @NonNull Weight weight)
    {
        double bmi = calculateBmi(height, weight);
        return bmi > getMaximumBmi();
    }

    public double calculateBmi(@NonNull Length height, @NonNull Weight weight)
    {
        double heightInCentimeters = height.getValueInCm();
        double heightInMeters = heightInCentimeters / 100;
        double heightInMetersSquare = heightInMeters * heightInMeters;

        double weightInKg = weight.getValueInKg();

        return weightInKg / heightInMetersSquare;
    }
}
