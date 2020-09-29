package com.myfiziq.sdk.db;


import com.myfiziq.sdk.helpers.DoubleHelpers;

import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Represents a measurement by length, such as a person's height or arm length.
 */
public abstract class Length implements TypeOfMeasurement, SystemOfMeasurement, Cloneable
{
    double valueInCm;


    @Nullable
    public static Length fromCentimeters(Class<? extends Length> preferredUnitOfMeasurement, double centimeters)
    {
        try
        {
            String preferredUnitOfMeasurementString = preferredUnitOfMeasurement.newInstance().getInternalName();
            return fromCentimeters(preferredUnitOfMeasurementString, centimeters);
        }
        catch(Exception e)
        {
            Timber.e(Length.class.getSimpleName(), "Cannot create length object using %s units of measurement", preferredUnitOfMeasurement.toString());
            return null;
        }
    }

    /**
     * Creates a new object with the user's preferred units of measurement from a value of centimeters.
     *
     * @param preferredUnitOfMeasurement The preferred unit of measurement to obtain the length in.
     * @param centimeters A representation of the user's preferred units of measurement in centimeters.
     */
    public static Length fromCentimeters(String preferredUnitOfMeasurement, double centimeters)
    {
        Length newObject;

        switch (preferredUnitOfMeasurement)
        {
            case Centimeters.internalName:
                newObject = new Centimeters();
                break;

            case Feet.internalName:
                newObject = new Feet();
                break;

            case Inches.internalName:
                newObject = new Inches();
                break;

            default:
                throw new NotImplementedException("Unknown Length Unit of Measurement: " + preferredUnitOfMeasurement);
        }

        newObject.valueInCm = centimeters;

        return newObject;
    }

    public double getValueInCm()
    {
        return valueInCm;
    }

    public void setValueInCm(double valueInCm)
    {
        this.valueInCm = valueInCm;
    }

    /**
     * Gets a value representing a measurement with the highest possible precision for that particular unit of measurement.
     *
     * For example, the Feet units of measurement supports up to 0 decimal places so a whole number is returned.
     *
     * The inches units of measurement supports a decimal number so a rational number is returned.
     */
    public abstract BigDecimal getValueForComparison();

    /**
     * Sets a value for this particular measurement expressed with a specific unit of measurement. (e.g. 10 inches)
     *
     * @param comparisonValue A value originally from {@link Length#getValueForComparison()} that has been transformed.
     */
    public abstract void setTransformedValueFromComparison(BigDecimal comparisonValue);

    /**
     * Gets the precision for the units of measurement.
     *
     * For example, the Feet units of measurement supports up to 0 decimal places so 0 is returned.
     *
     * The centimeters units of measurement supports a decimal number with 1 decimal place so 1 is returned.
     *
     * The inches units of measurement supports a decimal number with 2 decimal place so 2 is returned.
     */
    public abstract int getPrecision();

    public boolean greaterThan(Length measurement)
    {
        return compareTo(this, measurement) > 0;
    }

    public boolean lessThan(Length measurement)
    {
        return compareTo(this, measurement) < 0;
    }

    public boolean equals(Length measurement)
    {
        return compareTo(this, measurement) == 0;
    }

    public Length add(Length measurement)
    {
        double newLength = valueInCm +  measurement.getValueInCm();

        return Length.fromCentimeters(getInternalName(), newLength);
    }

    public Length subtract(Length measurement)
    {
        double newLength = valueInCm - measurement.getValueInCm();

        return Length.fromCentimeters(getInternalName(), newLength);
    }

    /**
     * Compares the two specified length values.
     *
     * @param length1 the first {@code Length} to compare.
     * @param length2 the second {@code Length} to compare.
     *
     * @return  the value {@code 0} if {@code length1} is numerically equal to {@code length2};
     *          a value less than {@code 0} if {@code length1} is numerically less than {@code length2};
     *          and a value greater than {@code 0} if {@code length1} is numerically greater than {@code length2}.
     */
    private int compareTo(Length length1, Length length2)
    {
        double length1InCm = length1.getValueInCm();
        double length2InCm = length2.getValueInCm();

        if (DoubleHelpers.greaterThan(length1InCm, length2InCm, 0.1))
        {
            return 1;
        }
        else if (DoubleHelpers.greaterThan(length2InCm, length1InCm, 0.1))
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public Length clone()
    {
        try
        {
            return (Length) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // Swallow the exception since this class implements the Cloneable interface
            // CloneNotSupportedException will probably never be thrown
            return null;
        }
    }
}
