package com.myfiziq.sdk.helpers;

/**
 * @deprecated Do not use this class. Instead use the converters built into each units of measurement,
 *              such as {@link com.myfiziq.sdk.db.Pounds#fromKilograms(String, double)}
 */
public class MetricConverter
{
    public static double convertCentimetersToInches(double centimeters)
    {
        return centimeters / 2.54;
    }

    public static double convertKilogramsToPounds(double kilograms)
    {
        return kilograms / 0.45359237;
    }

    public static double convertKilogramsToStone(double kilograms)
    {
        return kilograms / 6.35029;
    }
}
