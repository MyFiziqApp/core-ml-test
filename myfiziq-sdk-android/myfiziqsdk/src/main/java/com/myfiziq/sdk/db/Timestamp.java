package com.myfiziq.sdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 */
public class Timestamp extends ModelBasicType
{
    public long mValue = System.currentTimeMillis();

    public Timestamp()
    {

    }

    public Timestamp(long value)
    {
        mValue = value;
    }

    public boolean isZero()
    {
        return (0 == Long.valueOf(mValue).compareTo(Long.valueOf(0)));
    }

    @Override
    public void fromJsonElement(JsonElement elem)
    {
        mValue = elem.getAsLong();
    }

    @Override
    public void toJSONObject(JSONObject dest, String name)
    {
        try
        {
            dest.put(name, mValue);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toJsonObject(JsonObject dest, String name)
    {
        dest.addProperty(name, mValue);
    }

    @Override
    public void readFromCursor(Cursor cursor, int column, Model model)
    {
        mValue = cursor.getLong(column);
    }

    @Override
    public boolean isModified(Cursor cursor, int column)
    {
        return cursor.isNull(column) || mValue != cursor.getLong(column);
    }

    @Override
    public void toContentValue(ContentValues values, String name)
    {
        values.put(name, System.currentTimeMillis());
    }

    @Override
    public String getTable(Persistent persistent, String name)
    {
        return String.format("%s INTEGER %s", name, persistent.appDb());
    }

    @Override
    public String getColumnType()
    {
        return "timestamp";
    }

    @Override
    public String getAlter(Persistent persistent, String name)
    {
        return String.format("ADD %s INTEGER %s DEFAULT '%s'", name, persistent.appDb(), toString());
    }

    @Override
    public void parcel(Parcel dest, int flags)
    {
        dest.writeLong(mValue);
    }

    @Override
    public void unparcel(Parcel source)
    {
        mValue = source.readLong();
    }

    @Override
    public Object getAsCursorRow()
    {
        return Long.valueOf(mValue);
    }

    @Override
    public String toString()
    {
        return String.valueOf(mValue);
    }
}
