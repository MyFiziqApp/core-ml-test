package com.myfiziq.sdk.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Represents a measurement taken in the centimeters units of measurement.
 */
public class Centimeters extends Length implements Metric, Measurement
{
    public static final DecimalFormat heightFormat = new DecimalFormat("0 cm");
    public static final String internalName = "CENTIMETERS";

    private DecimalFormat defaultFormat = new DecimalFormat("0.0 cm");


    /**
     * Creates a new Centimeters object with 0 centimeters.
     */
    public Centimeters()
    {
        // Empty
    }

    /**
     * Creates an object holding centimeters.
     *
     * @param centimeters The number of centimeters to represent.
     */
    public Centimeters(double centimeters)
    {
        valueInCm = centimeters;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getFormatted()
    {
        return defaultFormat.format(valueInCm);
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        defaultFormat = format;
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        double valueInCm = getValueInCm();

        BigDecimal returnObject = new BigDecimal(valueInCm);
        return returnObject.setScale(getPrecision(), RoundingMode.HALF_DOWN);
    }

    @Override
    public void setTransformedValueFromComparison(BigDecimal comparisonValue)
    {
        valueInCm = comparisonValue.doubleValue();
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
