package com.myfiziq.sdk.db;

import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Represents a measurement taken in the Stone (UK) units of measurement.
 */
// TODO ImperialUK instead of just Imperial?
public class StoneUK extends Weight implements Imperial, Measurement
{
    public static final String internalName = "STONE_UK";


    /**
     * Creates a new Stone object with 0 stone.
     */
    public StoneUK()
    {
        // Empty
    }

    /**
     * Creates an object holding stone.
     *
     * @param stone The number of stone to represent.
     * @param pounds The number of pounds to represent (a fraction of 1 stone).
     */
    public StoneUK(int stone, int pounds)
    {
        valueInKg += (stone * 6.35029318);
        valueInKg += (pounds * 0.45359237);
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getFormatted()
    {
        int[] values = getValueInStoneAndPounds();

        if (values[0] == 0 && values[1] == 0)
        {
            return "";
        }
        else if (values[0] == 0 && values[1] > 0)
        {
            return values[1]  + "lbs";
        }
        else if (values[0] > 0 && values[1] == 0)
        {
            return values[0] + "st";
        }
        else
        {
            return values[0] + "st " + values[1] + "lbs";
        }
    }

    @Override
    public void setFormat(DecimalFormat format)
    {
        throw new NotImplementedException("Not implemented for UK stone");
    }

    /**
     * Gets the value of this measurement in stone and pound components.
     *
     * The first value in the int[] array is the number of stone this measurement represents, the second value is the number of pounds this measurement represents.
     *
     * For example, for a measurement that is "1 stone and 2 pounds":
     *
     *      Index 0 = 1
     *      Index 1 = 2
     *
     * @return An integer array with the number of stones at index 0 and the number of pounds at index 1.
     */
    public int[] getValueInStoneAndPounds()
    {
        // The number of Stone this measurement represents
        double stoneDecimal = valueInKg * 0.1574730444;

        // The number of Stone this measurement represents as a whole number
        // Always round down
        int roundedDownStone = (int) stoneDecimal;

        // The remaining Stone
        double remainingStone = stoneDecimal - roundedDownStone;

        // Convert the remaining Stone into Pounds
        int roundedPounds = (int) Math.round(remainingStone * 14);

        if (roundedPounds == 14)
        {
            // If we're very close to having 1 whole stone, round up
            roundedDownStone += 1;
            roundedPounds = 0;
        }

        return new int[] {roundedDownStone, roundedPounds};
    }

    /**
     * Gets the total amount of pounds in this measurement.
     *
     * For example, the TOTAL number of pounds in "1 stone and 2 pounds" is 16 pounds.
     */
    public int getTotalValueInPounds()
    {
        return (int) Math.round(valueInKg * 2.2046226218);
    }

    @Override
    public BigDecimal getValueForComparison()
    {
        int valueInPounds = getTotalValueInPounds();

        BigDecimal returnObject = new BigDecimal(valueInPounds);
        return returnObject.setScale(getPrecision(), RoundingMode.HALF_DOWN);
    }

    @Override
    public void setTransformedValueFromComparison(BigDecimal comparisonValue)
    {
        valueInKg = comparisonValue.doubleValue() * 0.45359237;
    }

    @Override
    public int getPrecision()
    {
        return 0;
    }

    @Override
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        // TODO Imperial UK??
        return Imperial.class;
    }
}
