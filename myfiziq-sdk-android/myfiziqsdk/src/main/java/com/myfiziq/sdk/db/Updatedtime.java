package com.myfiziq.sdk.db;

/**
 *
 */
public class Updatedtime extends Timestamp
{
    public Updatedtime()
    {

    }

    @Override
    public String getColumnType()
    {
        return "updatedtime";
    }
}
