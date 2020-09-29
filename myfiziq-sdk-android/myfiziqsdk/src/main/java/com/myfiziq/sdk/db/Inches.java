package com.myfiziq.sdk.db;

import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Represents a measurement taken in the inches units of measurement.
 */
public class Inches extends Length implements Imperial, Measurement
{
    public static final String internalName = "INCHES";


    /**
     * Creates a new Inches object with 0 inches.
     */
    public Inches()
    {
        // Empty
    }

    /**
     * Creates an object holding inches.
     *
     * @param inches The number of inches to represent. This may be a decimal number.
     */
    public Inches(double inches)
    {
        valueInCm += (inches * 2.54);
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getFormatted()
    {
        double value = getValueInInches();

        if (value == 0)
        {
            return "";
        }
        else
        {
            return String.format(Locale.getDefault(), "%.2f\"", value);
        }
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        throw new NotImplementedException("Not implemented for inches");
    }

    /**
     * Gets the total amount of inches in this measurement.
     *
     * For example, the TOTAL number of inches in "1 foot and 2 inches" is 14 inches.
     */
    public double getValueInInches()
    {
        return valueInCm * 0.3937007874;
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        double valueInInches = getValueInInches();

        BigDecimal returnObject = new BigDecimal(valueInInches);
        return returnObject.setScale(getPrecision(), RoundingMode.HALF_DOWN);
    }

    @Override
    public void setTransformedValueFromComparison(BigDecimal comparisonValue)
    {
        valueInCm = comparisonValue.doubleValue() * 2.54;
    }

    @Override
    public int getPrecision()
    {
        return 2;
    }

    @Override
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        return Imperial.class;
    }
}
