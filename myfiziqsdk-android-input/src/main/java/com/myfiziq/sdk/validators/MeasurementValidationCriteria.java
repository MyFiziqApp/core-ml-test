package com.myfiziq.sdk.validators;

import android.content.Context;
import android.util.TypedValue;

import com.myfiziq.sdk.db.Length;

import androidx.annotation.DimenRes;

/**
 * Contains validation criteria for a measurement.
 */
public class MeasurementValidationCriteria
{

    private String internalTypeName;
    private @DimenRes
    int minimumValueDimensionId;
    private @DimenRes
    int maximumValueDimensionId;

    /**
     * Creates validation criteria for a measurement.
     *
     * @param internalTypeName        A internalName of a Length/Weight
     * @param minimumValueDimensionId A Dimension ID for a Dimension that contains the minimum value accepted.
     *                                The Dimension should have a format of "float" and a type of "vals".
     * @param maximumValueDimensionId A Dimension ID for a Dimension that contains the maximum value accepted.
     *                                The Dimension should have a format of "float" and a type of "vals".
     */
    public MeasurementValidationCriteria(String internalTypeName,
                                         @DimenRes int minimumValueDimensionId,
                                         @DimenRes int maximumValueDimensionId)
    {
        this.internalTypeName = internalTypeName;
        this.minimumValueDimensionId = minimumValueDimensionId;
        this.maximumValueDimensionId = maximumValueDimensionId;
    }

    /**
     * Determines if the supplied measurement is valid given the validation criteria.
     */
    public boolean isValid(Context context, double metricMeasurement)
    {
        return !isTooLow(context, metricMeasurement) && !isTooHigh(context, metricMeasurement);
    }

    /**
     * Determines if the supplied measurement is too low given the valdiation criteria.
     */
    public boolean isTooLow(Context context, double metricMeasurement)
    {
        double minimumValue = getDimension(context, minimumValueDimensionId);
        return minimumValue >= metricMeasurement;
    }

    /**
     * Determines if the supplied measurement is too high given the validation criteria.
     */
    public boolean isTooHigh(Context context, double metricMeasurement)
    {
        double maximumValue = getDimension(context, maximumValueDimensionId);
        return maximumValue <= metricMeasurement;
    }

    /**
     * Returns a Dimension ID for a Dimension that refers to the minimum value accepted.
     */
    public int getMinimumValueDimensionId()
    {
        return minimumValueDimensionId;
    }

    /**
     * Returns a Dimension ID for a Dimension that refers to the maximum value accepted.
     */
    public int getMaximumValueDimensionId()
    {
        return maximumValueDimensionId;
    }

    /**
     * Returns the minimum value accepted for this validation criteria.
     */
    public float getMinimumValue(Context context)
    {
        return getDimension(context, minimumValueDimensionId);
    }

    /**
     * Returns the formatted minimum value accepted for this validation criteria.
     */
    public String getMinimumFormattedValue(Context context)
    {
        float minimumValueInCentimeters = getDimension(context, minimumValueDimensionId);
        return Length.fromCentimeters(internalTypeName, minimumValueInCentimeters).getFormatted();
    }

    /**
     * Returns the maximum value accepted for this validation criteria.
     */
    public float getMaximumValue(Context context)
    {
        return getDimension(context, maximumValueDimensionId);
    }

    /**
     * Returns the formatted maximum value accepted for this validation criteria.
     */
    public String getMaximumFormattedValue(Context context)
    {
        float maximumValueInCentimeters = getDimension(context, maximumValueDimensionId);
        return Length.fromCentimeters(internalTypeName, maximumValueInCentimeters).getFormatted();
    }

    /**
     * Retrieves a float for a given Dimension ID.
     * <p>
     * A Dimension ID for a Dimension that contains the minimum value accepted.
     * The Dimension should have a format of "float" and a type of "vals".
     * <p>
     * <p>
     * For example:
     *
     * <dimen name="minimum_imperial_height" format="float" type="vals">19</dimen>
     */
    private float getDimension(Context context, @DimenRes int dimensionId)
    {
        TypedValue value = new TypedValue();
        context.getResources().getValue(dimensionId, value, true);
        return value.getFloat();
    }

    /**
     * Retrieves a string, internalName of a Length/Weight
     */
    public String getInternalTypeName()
    {
        return internalTypeName;
    }
}
