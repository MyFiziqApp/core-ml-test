package com.myfiziq.sdk.db;

import android.content.ContentValues;

/**
 *
 */

public class Synctime extends Timestamp
{
    public Synctime()
    {

    }

    public void disable()
    {
        mValue = Long.MAX_VALUE;
    }

    public void enable()
    {
        reset();
    }

    public void reset()
    {
        mValue = System.currentTimeMillis();
    }

    public boolean isEnabled()
    {
        return Long.valueOf(mValue).compareTo(Long.MAX_VALUE) < 0;
    }

    @Override
    public String getColumnType()
    {
        return "synctime";
    }

    @Override
    public void toContentValue(ContentValues values, String name)
    {
        values.put(name, mValue);
    }
}
