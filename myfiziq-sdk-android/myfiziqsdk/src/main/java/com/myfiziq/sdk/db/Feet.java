package com.myfiziq.sdk.db;

import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Represents a measurement taken in the feet units of measurement.
 */
public class Feet extends Length implements Imperial, Measurement
{
    public static final String internalName = "FEET";


    /**
     * Creates a new Feet object with 0 feet.
     */
    public Feet()
    {
        // Empty
    }

    /**
     * Creates an object holding feet.
     *
     * @param feet The number of feet to represent.
     * @param inches The number of inches to represent (a fraction of 1 feet).
     *               Values greater than 12 (i.e. 1 foot) will automatically be converted to feet.
     */
    public Feet(int feet, int inches)
    {
        valueInCm += (feet * 30.48);
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
        int[] values = getValueInFeetAndInches();

        if (values[0] == 0 && values[1] == 0)
        {
            return "";
        }
        else if (values[0] == 0 && values[1] > 0)
        {
            return values[1] + "\"";
        }
        else if (values[0] > 0 && values[1] == 0)
        {
            return values[0] + "'";
        }
        else
        {
            return values[0] + "' " + values[1] + "\"";
        }
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        throw new NotImplementedException("Not implemented for feet");
    }

    /**
     * Gets the value of this measurement in feet and inches components.
     *
     * The first value in the int[] array is the number of feet this measurement represents, the second value is the number of inches this measurement represents.
     *
     * For example, for a measurement that is "1 foot and 2 inches":
     *
     *      Index 0 = 1
     *      Index 1 = 2
     *
     * @return An integer array with the number of feet at index 0 and the number of inches at index 1.
     */
    public int[] getValueInFeetAndInches()
    {
        // The number of Feet this measurement represents
        double feetDecimal = valueInCm * 0.032808399;

        // The number of Feet this measurement represents as a whole number
        // Always round down
        int feetRoundedDown = (int) feetDecimal;

        // The remaining Feet
        double remainingFeet = feetDecimal - feetRoundedDown;

        // Convert the remaining Feet into Inches
        int roundedInches = (int) Math.round(remainingFeet * 12);

        if (roundedInches == 12)
        {
            // If we're very close to having 1 whole foot, round up
            feetRoundedDown += 1;
            roundedInches = 0;
        }

        return new int[] {feetRoundedDown, roundedInches};
    }

    /**
     * Gets the total amount of inches in this measurement.
     *
     * For example, the TOTAL number of inches in "1 foot and 2 inches" is 14 inches.
     */
    public int getTotalValueInInches()
    {
        return (int) Math.round(valueInCm * 0.3937007874);
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        int valueInInches = getTotalValueInInches();

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
        return 0;
    }

    @Override
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        return Imperial.class;
    }
}
