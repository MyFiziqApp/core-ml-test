package com.myfiziq.sdk.helpers;

import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.vo.BodyFatCategory;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Contains methods to help determine which body fat category a person belongs to.
 *
 * The logic in this class should mirror that of the iOS implementation in:
 *
 *      MyFiziqAvatar+MeasurementFormatting.m
 *      https://github.com/MyFiziqApp/BCT/blob/1c68b77f84d6f87423a131bad24a0ff904e3b961/MyFiziq/MyFiziqAvatar%2BMeasurementFormatting.m
 *
 */
public class BodyFatCategoryCalculator
{
    private BodyFatCategoryCalculator()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Determines a person's body fat percentage.
     * @param gender The person's gender.
     * @param dateOfBirth The person's date of birth.
     * @param dateTaken The date the body fat measurement was taken.
     * @param bodyFatPercent The body fat percentage of the person.
     * @return The person's body fat category.
     */
    public static BodyFatCategory determineBodyFatCategory(Gender gender, Date dateOfBirth, Date dateTaken, double bodyFatPercent)
    {
        int age = getYearsBetweenTwoDates(dateOfBirth, dateTaken);

        if (gender == Gender.M)
        {
            return determineBodyFatCategoryForMale(age, bodyFatPercent);
        }
        else if (gender == Gender.F)
        {
            return determineBodyFatCategoryForFemale(age, bodyFatPercent);
        }
        else
        {
            throw new IllegalArgumentException("Unknown gender: " + gender);
        }
    }

    /**
     * Determines the body fat category for a male.
     * @param age The male's age.
     * @param bodyFatPercent The male's body fat percentage.
     * @return The male's body fat category.
     */
    private static BodyFatCategory determineBodyFatCategoryForMale(int age, double bodyFatPercent)
    {
        if (age > 17 && age < 30)
        {
            if (bodyFatPercent < 13.83333)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 13.83333 && bodyFatPercent < 24.43333)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 24.4333 && bodyFatPercent < 29.4)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else if (age >= 30 && age < 50)
        {
            if (bodyFatPercent < 15.03333)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 15.0333 && bodyFatPercent < 24.8)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 24.8 && bodyFatPercent < 29.4)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else if (age >= 50 && age < 85)
        {
            if (bodyFatPercent < 17.866666)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 17.86666 && bodyFatPercent < 27.1)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 27.1 && bodyFatPercent < 31.46666)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else
        {
            return BodyFatCategory.UNKNOWN;
        }

    }

    /**
     * Determines the body fat category for a female.
     * @param age The female's age.
     * @param bodyFatPercent The female's body fat percentage.
     * @return The body fat percentage for the female.
     */
    private static BodyFatCategory determineBodyFatCategoryForFemale(int age, double bodyFatPercent)
    {
        if (age > 17 && age < 30)
        {
            if (bodyFatPercent < 26.66666)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 26.66666 && bodyFatPercent < 36.6666)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 36.6666 && bodyFatPercent < 41.4)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else if (age >= 30 && age < 50)
        {
            if (bodyFatPercent < 27.7)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 27.7 && bodyFatPercent < 37.2)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 37.2 && bodyFatPercent < 41.7)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else if (age >= 50 && age < 85)
        {
            if (bodyFatPercent < 30.433333)
            {
                return BodyFatCategory.UNDERWEIGHT;
            }
            else if (bodyFatPercent >= 30.433333 && bodyFatPercent < 39.3333)
            {
                return BodyFatCategory.HEALTHY;
            }
            else if (bodyFatPercent >= 39.3333 && bodyFatPercent < 43.53333)
            {
                return BodyFatCategory.OVERWEIGHT;
            }
            else
            {
                return BodyFatCategory.OBESE;
            }
        }
        else
        {
            return BodyFatCategory.UNKNOWN;
        }
    }

    private static int getYearsBetweenTwoDates(Date first, Date last)
    {
        Calendar a = getCalendar(first);
        Calendar b = getCalendar(last);

        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);

        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
                (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE)))
        {
            diff--;
        }

        return diff;
    }

    private static Calendar getCalendar(Date date)
    {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(date);
        return cal;
    }
}
