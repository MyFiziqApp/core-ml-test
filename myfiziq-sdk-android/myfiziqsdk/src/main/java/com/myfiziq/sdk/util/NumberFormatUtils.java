package com.myfiziq.sdk.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This is a helper class for number format utility / conversion
 */
public class NumberFormatUtils
{
    /**
     * Return a Big Decimal Value, rounded to the specified precision, where .5 is rounded up (half up rounding). Example :
     * 1. 0 precision will return "0" format,
     * 2. 1 precision will return "0.0" format.
     */
    public static BigDecimal roundDoubleHalfUp(double number, int precision)
    {
        BigDecimal bigDecimal = new BigDecimal(number);
        return bigDecimal.setScale(precision , RoundingMode.HALF_UP);
    }
}
