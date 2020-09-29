package com.myfiziq.sdk.myfiziqsdk;

import com.myfiziq.sdk.util.TimeFormatUtils;

import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;


public class TimeFormatUtilsTest
{
    private final Date defaultDateTime = new Date(119, 1, 3, 12, 13, 14);
    private final TimeZone timeZoneCurrent = TimeZone.getTimeZone("GMT+8:00");
    private final TimeZone timeZoneTwoHoursAhead = TimeZone.getTimeZone("GMT+10:00");
    private final TimeZone timeZoneTwoHoursBehind = TimeZone.getTimeZone("GMT+6:00");
    private final TimeZone timeZoneUtc = TimeZone.getTimeZone("UTC");

    private final String outputDateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";


    @Test
    public void formatDateSameTimezone()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneCurrent, outputDateFormat, timeZoneCurrent);

        assertEquals("2019-02-03T12:13:14+0800", result);
    }

    @Test
    public void formatOutputTwoHoursAhead()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneCurrent, outputDateFormat, timeZoneTwoHoursAhead);

        assertEquals("2019-02-03T14:13:14+1000", result);
    }

    @Test
    public void formatOutputTwoHoursBehindUTC()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneCurrent, outputDateFormat, timeZoneTwoHoursBehind);

        assertEquals("2019-02-03T10:13:14+0600", result);
    }

    @Test
    public void formatInputTwoHoursAhead()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneTwoHoursAhead, outputDateFormat, timeZoneCurrent);

        assertEquals("2019-02-03T10:13:14+0800", result);
    }

    @Test
    public void formatInputTwoHoursBehind()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneTwoHoursBehind, outputDateFormat, timeZoneCurrent);

        assertEquals("2019-02-03T14:13:14+0800", result);
    }

    @Test
    public void formatInUTC()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneCurrent, outputDateFormat, timeZoneUtc);

        assertEquals("2019-02-03T04:13:14+0000", result);
    }

    @Test
    public void formatNoOutputTimeZone()
    {
        String result = TimeFormatUtils.formatDate(defaultDateTime, timeZoneTwoHoursAhead, outputDateFormat);

        assertEquals("2019-02-03T12:13:14+1000", result);
    }
}