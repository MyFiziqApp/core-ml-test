package com.myfiziq.sdk.util;

import com.myfiziq.sdk.db.ModelLog;

/**
 * Represents a performance metric that is being measured and the time that we started measuring it.
 */
public class Stopwatch
{
    private String mTitle;
    private long mStartTime;

    /**
     * Creates a new Stopwatch instance to log performance metrics.
     *
     * The point in time this instance is created is the starting time of the performance metric.
     *
     * @param title The title of the performance metric.
     */
    public Stopwatch(String title)
    {
        // TODO Firebase performance monitoring
        mTitle = title;
        mStartTime = System.currentTimeMillis();
    }

    /**
     * Outputs the number of milliseconds from the time this instance was created
     * until the time this method was called.
     */
    public void print()
    {
        long tookTime = System.currentTimeMillis() - mStartTime;
        ModelLog.d("STOPWATCH: " + mTitle + " took: " + tookTime + " ms");
    }
}
