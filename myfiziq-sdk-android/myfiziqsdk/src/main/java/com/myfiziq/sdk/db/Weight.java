package com.myfiziq.sdk.db;


import com.myfiziq.sdk.helpers.DoubleHelpers;

import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Represents a measurement by weight, such as how heavy a person is.
 */
public abstract class Weight implements TypeOfMeasurement, SystemOfMeasurement, Cloneable
{
    double valueInKg;

    @Nullable
    public static Weight fromKilograms(Class<? extends Weight> preferredUnitOfMeasurement, double kilograms)
    {
        try
        {
            String preferredUnitOfMeasurementString = preferredUnitOfMeasurement.newInstance().getInternalName();
            return fromKilograms(preferredUnitOfMeasurementString, kilograms);
        }
        catch(Exception e)
        {
            Timber.e( "Cannot create weight object using %s units of measurement", preferredUnitOfMeasurement.toString());
            return null;
        }
    }

    /**
     * Creates a new object with the user's preferred units of measurement from a value of kilograms.
     *
     * @param preferredUnitOfMeasurement The preferred unit of measurement to obtain the weight in.
     * @param kilograms A representation of the user's preferred units of measurement in kilograms.
     */
    public static Weight fromKilograms(String preferredUnitOfMeasurement, double kilograms)
    {
        Weight newObject;

        switch (preferredUnitOfMeasurement)
        {
            case Kilograms.internalName:
                newObject = new Kilograms();
                break;

            case Pounds.internalName:
                newObject = new Pounds();
                break;

            case StoneUK.internalName:
                newObject = new StoneUK();
                break;

            default:
                throw new NotImplementedException("Unknown Weight Unit of Measurement: " + preferredUnitOfMeasurement);
        }

        newObject.valueInKg = kilograms;

        return newObject;
    }

    public double getValueInKg()
    {
        return valueInKg;
    }

    public void setValueInKg(double valueInKg)
    {
        this.valueInKg = valueInKg;
    }

    /**
     * Gets a value representing a measurement with the highest possible precision for that particular unit of measurement.
     *
     * For example, the Stone and Pounds units of measurement supports up to 0 decimal places so a whole number is returned.
     *
     * The Kilograms units of measurement supports a decimal number so a rational number is returned.
     */
    public abstract BigDecimal getValueForComparison();

    /**
     * Sets a value for this particular measurement expressed with a specific unit of measurement. (e.g. 10 pounds)
     *
     * @param comparisonValue A value originally from {@link Weight#getValueForComparison()} that has been transformed.
     */
    public abstract void setTransformedValueFromComparison(BigDecimal comparisonValue);

    /**
     * Gets the precision for the units of measurement.
     *
     * For example, the Stone and Pounds units of measurement supports up to 0 decimal places so 0 is returned.
     *
     * The kilograms units of measurement supports a decimal number with 1 decimal place so 0.1 is returned.
     */
    public abstract int getPrecision();

    public boolean greaterThan(Weight measurement)
    {
        return compareTo(this, measurement) > 0;
    }

    public boolean lessThan(Weight measurement)
    {
        return compareTo(this, measurement) < 0;
    }

    public boolean equals(Weight measurement)
    {
        return compareTo(this, measurement) == 0;
    }

    public Weight add(Weight measurement)
    {
        double newWeight = valueInKg +  measurement.getValueInKg();

        return Weight.fromKilograms(getInternalName(), newWeight);
    }

    public Weight subtract(Weight measurement)
    {
        double newWeight = valueInKg - measurement.getValueInKg();

        return Weight.fromKilograms(getInternalName(), newWeight);
    }

    /**
     * Compares the two specified weight values.
     *
     * @param   weight1        the first {@code Weight} to compare
     * @param   weight2        the second {@code Weight} to compare
     * @return  the value {@code 0} if {@code weight1} is numerically equal to {@code weight2};
     *          a value less than {@code 0} if {@code weight1} is numerically less than {@code weight2};
     *          and a value greater than {@code 0} if {@code weight1} is numerically greater than {@code weight2}.
     */
    private int compareTo(Weight weight1, Weight weight2)
    {
        double weight1InKg = weight1.getValueInKg();
        double weight2InKg = weight2.getValueInKg();

        if (DoubleHelpers.greaterThan(weight1InKg, weight2InKg, 0.1))
        {
            return 1;
        }
        else if (DoubleHelpers.greaterThan(weight2InKg, weight1InKg, 0.1))
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public Weight clone()
    {
        try
        {
            return (Weight) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // Swallow the exception since this class implements the Cloneable interface
            // CloneNotSupportedException will probably never be thrown
            return null;
        }
    }
}
