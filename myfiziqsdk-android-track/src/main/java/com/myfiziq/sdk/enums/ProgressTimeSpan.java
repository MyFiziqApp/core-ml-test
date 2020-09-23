package com.myfiziq.sdk.enums;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public enum ProgressTimeSpan
{
    // iOS's first day of the week is always hard-coded to start on Monday. Let's copy iOS.
    DAYS(new SimpleDateFormat("yyyyMMdd"), 1),
    WEEKS(new SimpleDateFormat("yyyyww"), 7),
    MONTHS(new SimpleDateFormat("yyyyMM"), 30);


    // The first day of the week is ALWAYS Monday, just like iOS
    private static final int FIRST_DAY_OF_WEEK = Calendar.MONDAY;

    private SimpleDateFormat comparator;
    private long daysInTimeSpan;

    ProgressTimeSpan(SimpleDateFormat comparator, long daysInTimeSpan)
    {
        this.comparator = comparator;
        this.daysInTimeSpan = daysInTimeSpan;

        setFirstDayOfWeek(FIRST_DAY_OF_WEEK);
    }

    public SimpleDateFormat getComparator()
    {
        return comparator;
    }

    public long getDaysInTimeSpan()
    {
        return daysInTimeSpan;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(firstDayOfWeek);
        comparator.setCalendar(calendar);
    }
}
