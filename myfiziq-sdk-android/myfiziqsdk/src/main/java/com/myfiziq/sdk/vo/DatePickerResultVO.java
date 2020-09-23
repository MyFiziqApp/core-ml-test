package com.myfiziq.sdk.vo;

/**
 * Holds the result of the user's date selection.
 */
public class DatePickerResultVO
{
    private int year;
    private int month;
    private int day;

    public DatePickerResultVO(int year, int month, int day)
    {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getYear()
    {
        return year;
    }

    public int getMonth()
    {
        return month;
    }

    public int getDay()
    {
        return day;
    }
}
