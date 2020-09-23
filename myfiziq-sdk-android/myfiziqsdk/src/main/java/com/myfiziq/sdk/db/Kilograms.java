package com.myfiziq.sdk.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Represents a measurement taken in the kilograms units of measurement.
 */
public class Kilograms extends Weight implements Metric, Measurement
{
    public static final String internalName = "KILOGRAMS";

    private DecimalFormat defaultFormat = new DecimalFormat("0.0 kg");


    /**
     * Creates a new Kilograms object with 0 kilograms.
     */
    public Kilograms()
    {
        // Empty
    }

    /**
     * Creates an object holding kilograms.
     *
     * @param kilograms The number of kilograms to represent.
     */
    public Kilograms(double kilograms)
    {
        valueInKg = kilograms;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getFormatted()
    {
        return defaultFormat.format(valueInKg);
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        defaultFormat = format;
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        double valueInKilograms = getValueInKg();

        BigDecimal returnObject = new BigDecimal(valueInKilograms);
        return returnObject.setScale(getPrecision(), RoundingMode.HALF_DOWN);
    }

    @Override
    public void setTransformedValueFromComparison(BigDecimal comparisonValue)
    {
        valueInKg = comparisonValue.doubleValue();
    }

    @Override
    public int getPrecision()
    {
        return 1;
    }

    @Override
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        return Metric.class;
    }
}
