package com.myfiziq.sdk.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Represents a measurement taken in the pounds units of measurement.
 */
public class Pounds extends Weight implements Imperial, Measurement
{
    public static final String internalName = "POUNDS";

    private DecimalFormat defaultFormat = new DecimalFormat("0.0 lbs");


    /**
     * Creates a new Pounds object with 0 pounds.
     */
    public Pounds()
    {
        // Empty
    }

    /**
     * Creates an object holding pounds.
     *
     * @param pounds The number of pounds to represent.
     */
    public Pounds(double pounds)
    {
        valueInKg = (pounds * 0.45359237);
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getFormatted()
    {
        return defaultFormat.format(getValueInPounds());
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        defaultFormat = format;
    }

    /**
     * Gets the number of pounds this measurement represents.
     *
     * @return The number of pounds rounded to the nearest whole number.
     */
    public double getValueInPounds()
    {
        return valueInKg * 2.2046226218;
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        BigDecimal bigDecimalPounds = new BigDecimal(getValueInPounds());
        return bigDecimalPounds.setScale(getPrecision(), RoundingMode.HALF_DOWN);
    }

    @Override
    public void setTransformedValueFromComparison(BigDecimal comparisonValue)
    {
        valueInKg = comparisonValue.doubleValue() * 0.45359237;
    }

    @Override
    public int getPrecision()
    {
        return 1;
    }

    @Override
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        return Imperial.class;
    }
}