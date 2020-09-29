package com.myfiziq.sdk.helpers;

/**
 * Provides helper methods when comparing {@link Double} values.
 */
public class DoubleHelpers
{
    /**
     * Determines whether two double values are equal.
     * @param value1 The first value to compare.
     * @param value2 The second value to compare.
     * @param precision The precision to be used when comparing the values.
     *                  For example, if you have two values "1.00" and "1.03",
     *                  having a precision of "0.1" will return true but "0.01" will return false.
     *
     * @return Whether the two values are equal.
     */
    public static boolean equals(double value1, double value2, double precision)
    {
        return (Math.abs(value1 - value2) < precision);
    }

    /**
     * Determines if one value is greater than the other.
     * @param likelyBiggerValue The value we think is likely to be bigger.
     * @param likelySmallerValue The value we think is likely to be smaller.
     * @param precision The precision to be used when comparing the values.
     *                  For example, if you have two values "1.03" and "1.00",
     *                  having a precision of "0.1" will return false since they are "equal" but "0.01" will return true.
     * @return Whether the {@param likelyBiggerValue} is in fact larger than the {@param likelySmallerValue}.
     */
    public static boolean greaterThan(double likelyBiggerValue, double likelySmallerValue, double precision)
    {
        return ((likelyBiggerValue - likelySmallerValue) > precision);
    }

    /**
     * Determines if one value is less than than the other.
     * @param likelySmallerValue The value we think is likely to be smaller.
     * @param likelyBiggerValue The value we think is likely to be bigger.
     * @param precision The precision to be used when comparing the values.
     *                  For example, if you have two values "1.00" and "1.03",
     *                  having a precision of "0.1" will return false since they are "equal" but "0.01" will return true.
     * @return Whether the {@param likelyBiggerValue} is in fact larger than the {@param likelySmallerValue}.
     */
    public static boolean lessThan(double likelySmallerValue, double likelyBiggerValue, double precision)
    {
        return ((likelySmallerValue - likelyBiggerValue) < precision * -1);
    }

    /**
     * Determines if one value is greater than or equal to the other.
     * @param likelyBiggerValue The value we think is likely to be bigger.
     * @param likelySmallerValue The value we think is likely to be smaller.
     * @param precision The precision to be used when comparing the values.
     *                  For example, if you have two values "0.98" and "1.00",
     *                  having a precision of "0.1" will return true since they are "equal" but "0.01" will return false.
     * @return Whether the {@param likelyBiggerValue} is in fact larger than or equal to the {@param likelySmallerValue}.
     */
    public static boolean greaterThanOrEqualTo(double likelyBiggerValue, double likelySmallerValue, double precision)
    {
        return equals(likelyBiggerValue, likelySmallerValue, precision) || greaterThan(likelyBiggerValue, likelySmallerValue, precision);
    }

    /**
     * Determines if one value is less than or equal to the other.
     * @param likelySmallerValue The value we think is likely to be smaller.
     * @param likelyBiggerValue The value we think is likely to be bigger.
     * @param precision The precision to be used when comparing the values.
     *                  For example, if you have two values "1.00" and "0.98",
     *                  having a precision of "0.1" will return true since they are "equal" but "0.01" will return false.
     * @return Whether the {@param likelyBiggerValue} is in fact larger than or equal to the {@param likelySmallerValue}.
     */
    public static boolean lessThanOrEqualTo(double likelySmallerValue, double likelyBiggerValue, double precision)
    {
        return equals(likelyBiggerValue, likelySmallerValue, precision) || lessThanOrEqualTo(likelySmallerValue, likelyBiggerValue, precision);
    }
}
