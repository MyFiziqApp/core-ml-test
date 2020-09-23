package com.myfiziq.sdk.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.myfiziq.sdk.R;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import timber.log.Timber;

/**
 * @hide
 */

public class TimeFormatUtils
{
    // Beware! The letter "X" in SimpleDateFormat's doesn't work in versions of Android below Nougat (7.0)
    public static final String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String PATTERN_ISO8601_2 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String PATTERN_ISO8601_3 = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String PATTERN_ISO8601_4 = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String PATTERN_REQUEST_SHORT = "yyyyMMdd";
    public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";

    private static final DateFormat ISO8601 = new SimpleDateFormat(PATTERN_ISO8601, Locale.getDefault());
    private static final DateFormat ISO8601_2 = new SimpleDateFormat(PATTERN_ISO8601_2, Locale.getDefault());
    private static final DateFormat ISO8601_3 = new SimpleDateFormat(PATTERN_ISO8601_3, Locale.getDefault());
    private static final DateFormat ISO8601_4 = new SimpleDateFormat(PATTERN_ISO8601_4, Locale.getDefault());
    private static final DateFormat REQUEST = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    private static final DateFormat FORMAT_MONTH = new SimpleDateFormat("MMM yy", Locale.getDefault());
    private static final DateFormat FORMAT_MINUTES = new SimpleDateFormat("m:ss", Locale.getDefault());
    private static final DateFormat FORMAT_SHORT_DATE = new SimpleDateFormat("dd MMM ''yy", Locale.getDefault());
    private static final DateFormat FORMAT_DAY = new SimpleDateFormat("dd MMM", Locale.getDefault());

    private static boolean isInitialised = false;


    private static synchronized void ensureInitialised()
    {
        if (!TimeFormatUtils.isInitialised)
        {
            Timber.v("Initialising Joda-Time");

            Context globalContext = GlobalContext.getContext();
            JodaTimeAndroid.init(globalContext);

            isInitialised = true;
        }
    }

    public static Long getUnixTimestampInUtcWithSeconds()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(new Date());

        return calendar.getTimeInMillis() / 1000;
    }

    /**
     * Formats a date.
     *
     * @param inputDate     The date to format.
     * @param inputTimeZone The timezone that the input date is currently in.
     * @param outputPattern The date format to output in. Refer to {@link DateTimeFormat} for the pattern syntax.
     * @return A formatted date using the specified pattern.
     */
    public static String formatDate(Date inputDate, TimeZone inputTimeZone, String outputPattern)
    {
        return TimeFormatUtils.formatDate(inputDate, inputTimeZone, outputPattern, inputTimeZone);
    }

    /**
     * Formats a date in the specified timezone.
     *
     * @param inputDate      The date to format.
     * @param inputTimeZone  The timezone that the input date is currently in.
     * @param outputPattern  The date format to output in. Refer to {@link DateTimeFormat} for the pattern syntax.
     * @param outputTimeZone The timezone that we should convert the time to and format it in.
     * @return A formatted date using the specified pattern in the desired timezone.
     */
    public static String formatDate(Date inputDate, TimeZone inputTimeZone, String outputPattern, TimeZone outputTimeZone)
    {
        ensureInitialised();

        DateTimeZone inputDateTimeZone = DateTimeZone.forTimeZone(inputTimeZone);
        DateTimeZone outputDateTimeZone = DateTimeZone.forTimeZone(outputTimeZone);

        // Parse the date time with the specified timezone
        DateTime inputDateTime = new DateTime(inputDate.getTime());
        inputDateTime = inputDateTime.withZoneRetainFields(inputDateTimeZone);

        // Convert the date time to one in the desired timezone
        DateTime outputDateTime = inputDateTime.withZone(outputDateTimeZone);


        DateTimeFormatter outputDateTimeFormat = DateTimeFormat.forPattern(outputPattern);

        return outputDateTimeFormat.print(outputDateTime);
    }

    /**
     * Parsed a date contained in a string into a Date object with the desired timezone.
     *
     * @param dt             The string representation of the date to be parsed.
     * @param inputTimeZone  The timezone of the date in the string.
     * @param outputTimeZone The timezone of the desired date object.
     * @return A date object representing the date contained in the string displayed in the desired output timezone.
     */
    @Nullable
    public static Date parseDateTime(String dt, TimeZone inputTimeZone, TimeZone outputTimeZone)
    {
        Date cachedDate = TimeFormatParseCache.getDateFromCache(dt, inputTimeZone, outputTimeZone);

        if (cachedDate != null)
        {
            return cachedDate;
        }

        ensureInitialised();
        Date dateTime = null;

        if (!TextUtils.isEmpty(dt))
        {
            try
            {
                dateTime = ISO8601.parse(dt);
            }
            catch (Exception e)
            {
                // Ignore, try the next parsing strategy
            }

            // We couldn't parse the date using the previous strategy, try another one
            if (dateTime == null)
            {
                try
                {
                    dateTime = ISO8601_2.parse(dt);
                }
                catch (Exception ignored)
                {
                    // Ignore, try the next parsing strategy
                }
            }

            // We couldn't parse the date using the previous strategy, try another one
            if (dateTime == null)
            {
                try
                {
                    String newDt = dt.replace("Z", "");
                    dateTime = ISO8601_3.parse(newDt);
                }
                catch (Exception ignored)
                {
                    // Ignore, try the next parsing strategy
                }
            }

            // We couldn't parse the date using the previous strategy, try another one
            if (dateTime == null)
            {
                try
                {
                    String newDt = dt.replace("Z", "");
                    dateTime = ISO8601_4.parse(newDt);
                }
                catch (Exception ignored)
                {
                    // Ignore, try the next parsing strategy
                }
            }

            // We couldn't parse the date using the previous strategy, try another one
            if (dateTime == null)
            {
                try
                {
                    dateTime = REQUEST.parse(dt);
                }
                catch (Exception ignored)
                {
                    // Ignore, try the next parsing strategy
                }
            }

            if (null == dateTime)
            {
                Timber.e("Cannot parse date %s using any possible date format", dt);
            }
        }

        if (dateTime != null)
        {
            DateTime inputDateTime = new DateTime(dateTime.getTime());
            inputDateTime = inputDateTime.withZoneRetainFields(DateTimeZone.forTimeZone(inputTimeZone));
            inputDateTime = inputDateTime.withZone(DateTimeZone.forTimeZone(outputTimeZone));
            dateTime = inputDateTime.toDate();
        }

        TimeFormatParseCache.putDateInCache(dateTime, dt, inputTimeZone, outputTimeZone);

        return dateTime;
    }

    public static String formatDay(Date time)
    {
        return FORMAT_DAY.format(time);
    }

    public static String formatWeek(Date time)
    {
        return FORMAT_DAY.format(time);
    }

    public static String formatMonth(Date time)
    {
        return FORMAT_MONTH.format(time);
    }

    public static String formatShortDate(Date time)
    {
        String shortDate = FORMAT_SHORT_DATE.format(time);
        return shortDate.replaceAll("\\.", "");
    }

    public static String formatMinutes(long time)
    {
        return FORMAT_MINUTES.format(new Date(time));
    }

    public static long calculateDaysBetween(Date startDate, Date endDate)
    {
        long diff = TimeFormatUtils.getDateInDayMillis(endDate) - TimeFormatUtils.getDateInDayMillis(startDate);
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    public static long calculateWeeksBetween(Date startDate, Date endDate)
    {
        long daysBetween = TimeFormatUtils.calculateDaysBetween(startDate, endDate);
        return (long) Math.ceil(daysBetween / 7.0);
    }

    public static long calculateMonthsBetween(Date startDate, Date endDate)
    {
        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTime(startDate);
        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(endDate);

        long m1 = startCalendar.get(Calendar.YEAR) * 12L + startCalendar.get(Calendar.MONTH);
        long m2 = endCalendar.get(Calendar.YEAR) * 12L + endCalendar.get(Calendar.MONTH);
        return m2 - m1;
    }

    public static long calculateYearsBetween(Date startDate, Date endDate)
    {
        long monthsBetween = TimeFormatUtils.calculateMonthsBetween(startDate, endDate);
        return monthsBetween / 12L;
    }

    /**
     * Get current day in millis
     */
    public static long getDateInDayMillis(Date currentDate)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        calendar.clear();
        calendar.set(year, month, date);
        return calendar.getTimeInMillis();
    }

    /**
     * Formats the user's date in their local style.
     * Medium length is chosen as it the most similar to iOS.
     *
     * @param chosenDate The date that the user has selected.
     * @return A date format specific to the user's region e.g. "3 Jan. 1989"
     */
    public static String formatDateForDisplay(Date chosenDate)
    {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        return dateFormat.format(chosenDate);
    }

    public static String generateDateSinceLabel(Resources resources, long unixStartDate, long unixEndDate)
    {
        Date startDate = new Date(unixStartDate);
        Date endDate = new Date(unixEndDate);

        long unixTimeDifference = endDate.getTime() - startDate.getTime();


        long secondsSince = TimeUnit.SECONDS.convert(unixTimeDifference, TimeUnit.MILLISECONDS);
        long minutesSince = TimeUnit.MINUTES.convert(unixTimeDifference, TimeUnit.MILLISECONDS);
        long hoursSince = TimeUnit.HOURS.convert(unixTimeDifference, TimeUnit.MILLISECONDS);
        long daysSince = TimeUnit.DAYS.convert(unixTimeDifference, TimeUnit.MILLISECONDS);
        long weeksSince = TimeFormatUtils.calculateWeeksBetween(startDate, endDate);
        long monthsSince = TimeFormatUtils.calculateMonthsBetween(startDate, endDate);
        long yearsSince = TimeFormatUtils.calculateYearsBetween(startDate, endDate);

        String dateSinceLabel = "";

        if (yearsSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, yearsSince, R.plurals.year_ago);
        }
        else if (monthsSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, monthsSince, R.plurals.month_ago);
        }
        else if (weeksSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, weeksSince, R.plurals.week_ago);
        }
        else if (daysSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, daysSince, R.plurals.day_ago);
        }
        else if (hoursSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, hoursSince, R.plurals.hour_ago);
        }
        else if (minutesSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, minutesSince, R.plurals.minute_ago);
        }
        else if (secondsSince >= 1)
        {
            dateSinceLabel = generateDateSinceString(resources, secondsSince, R.plurals.second_ago);
        }
        else
        {
            dateSinceLabel = "Now";
        }

        return dateSinceLabel;
    }

    private static String generateDateSinceString(Resources resources, long dateSince, @PluralsRes int resourceId)
    {
        if (dateSince >= Integer.MAX_VALUE)
        {
            // If the date difference is too long to fit in an int, make it the integer's max value instead
            dateSince = Integer.MAX_VALUE;
        }

        int dateSinceInt = (int) dateSince;

        String format = resources.getQuantityString(resourceId, dateSinceInt);

        return String.format(format, dateSince);
    }

    public static int yearsBetweenTwoDatesJoda(Date dob, Date completed)
    {
        DateTime dobDateTime = new DateTime(dob.getTime());
        DateTime completedDateTime = new DateTime(completed.getTime());
        Years yearsBetween = Years.yearsBetween(dobDateTime, completedDateTime);
        return yearsBetween.getYears();
    }
}
