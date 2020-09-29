package com.myfiziq.sdk.helpers;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.myfiziq.sdk.db.LocalUserDataKey;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class DateOfBirthCoordinator
{
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private DateOfBirthCoordinator()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Gets the date of birth if one has been set.
     *
     * Returns null if no date of birth has been set before.
     *
     * NOTE! The date of birth is stored locally on the device and is deleted when the user logs out.
     */
    @Nullable
    public static Date getDateOfBirth()
    {
        String dateOfBirthString = MyFiziqLocalUserDataHelper.getValue(LocalUserDataKey.DATE_OF_BIRTH, "");

        if (TextUtils.isEmpty(dateOfBirthString))
        {
            Timber.w("No date of birth is available");
            return null;
        }

        return parseDateOfBirth(dateOfBirthString);
    }

    /**
     * Gets the date of birth if one has been set.
     *
     * Returns an empty string if no date of birth has been set before.
     *
     * NOTE! The date of birth is stored locally on the device and is deleted when the user logs out.
     */
    public static String getDateOfBirthAsString()
    {
        Date dateOfBirth = getDateOfBirth();

        if (dateOfBirth == null)
        {
            return "";
        }

        return dateFormat.format(dateOfBirth);
    }

    /**
     * Sets the date of birth locally on the device.
     */
    public static void setDateOfBirth(Date dateOfBirth)
    {
        if (dateOfBirth == null)
        {
            Timber.w("Supplied date of birth is null");
            return;
        }

        String dateOfBirthString = dateFormat.format(dateOfBirth);

        MyFiziqLocalUserDataHelper.setValue(LocalUserDataKey.DATE_OF_BIRTH, dateOfBirthString);
    }

    /**
     * Parses the date of birth given the provided string.
     */
    public static Date parseDateOfBirth(String dateOfBirthString)
    {
        try
        {
            return dateFormat.parse(dateOfBirthString);
        }
        catch (Exception e)
        {
            Timber.e("Cannot parse date of birth");
            return null;
        }
    }

    /**
     * Converts the provided Date into a String
     *
     * Output is in the format expected by DateOfBirthCoordinator.parseDateOfBirth()
     *
     * Returns an empty String if dateToFormat is null
     */
    public static String formatDate(Date dateToFormat)
    {
        if (dateToFormat == null)
        {
            return "";
        }

        return dateFormat.format(dateToFormat);
    }
}
