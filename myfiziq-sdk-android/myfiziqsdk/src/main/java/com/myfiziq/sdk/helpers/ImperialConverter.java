package com.myfiziq.sdk.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * @deprecated Do not use this class. Instead use the converters built into each units of measurement,
 *              such as {@link com.myfiziq.sdk.db.Pounds#fromKilograms(String, double)}
 */
@Deprecated
public class ImperialConverter
{
    /**
     * Converts a string representation of imperial height into inches.
     *
     * The following string representations are valid:
     *
     *      1'           (1 foot)
     *      1' 5"        (1 foot 5 inches)
     *      1"           (1 inch)
     *
     *
     * @return An integer of inches.
     */
    public static int convertImperialHeightToInches(String imperialHeight)
    {
        int result = 0;

        // Matches against the following representations of imperial height:
        // 1'           (1 foot)
        // 1' 5"        (1 foot 5 inches)
        // 1"           (1 inch)
        //
        Pattern pattern = Pattern.compile(RegexHelpers.imperialHeightFormat);
        Matcher matcher = pattern.matcher(imperialHeight);

        if (matcher.matches())
        {
            // Start at index 1, the first index is the full string match which we don't want
            for (int i=1; i<=matcher.groupCount(); i++)
            {
                String match = matcher.group(i);

                // If it is feet
                if (match.contains("'"))
                {
                    String feet = match.replaceAll(RegexHelpers.onlyNumbers, "");
                    result += Integer.parseInt(feet) * 12;
                }
                // If it is inches
                else if (match.contains("\""))
                {
                    String inches = match.replaceAll(RegexHelpers.onlyNumbers, "");
                    result += Integer.parseInt(inches);
                }
                // Else unknown capture group
                else
                {
                    Timber.w("Unknown imperial height capture group: '%s'", match);
                }
            }
        }
        else
        {
            Timber.w("No imperial height found in the string: '%s'", imperialHeight);
        }

        return result;
    }

    /**
     * Converts inches to one of the below representations of imperial height:
     *
     *      1'           (1 foot)
     *      1' 5"        (1 foot 5 inches)
     *      1"           (1 inch)
     *                   (0 inches)
     *
     */
    public static String convertInchesToImperialHeight(double input)
    {
        int feet = (int) (input / 12);
        int inches = (int) (input % 12);

        if (feet == 0 && inches == 0)
        {
            return "";
        }
        else if (feet == 0 && inches > 0)
        {
            return inches + "\"";
        }
        else if (feet > 0 && inches == 0)
        {
            return feet + "'";
        }
        else
        {
            return feet + "' " + inches + "\"";
        }
    }

    public static double convertInchesToCentimeters(double inches)
    {
        return inches * 2.54;
    }

    public static double convertPoundsToKilograms(double pounds)
    {
        return pounds * 0.45359237;
    }

    public static double convertStonesToKilograms(double stones)
    {
        return stones * 6.35029;
    }

    public static double convertPoundsToStones(double pounds)
    {
        return pounds / 14;
    }

    public static double convertStonesToPounds(double stones)
    {
        return stones * 14;
    }

    public static double convertStonesToKilogramsWithFraction(double stones, double pounds)
    {
        double kilograms = ImperialConverter.convertStonesToKilograms(stones);
        double kilogramsFraction = ImperialConverter.convertPoundsToKilograms(pounds);
        return kilograms + kilogramsFraction;
    }
}
