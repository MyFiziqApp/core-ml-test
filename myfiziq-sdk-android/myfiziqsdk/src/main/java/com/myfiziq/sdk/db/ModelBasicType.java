package com.myfiziq.sdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

/**
 * @hide
 */
public abstract class ModelBasicType
{
    public abstract void fromJsonElement(JsonElement elem);
    public abstract void toJSONObject(JSONObject dest, String name);
    public abstract void toJsonObject(JsonObject dest, String name);
    public abstract void readFromCursor(Cursor cursor, int column, Model model);
    public abstract boolean isModified(Cursor cursor, int column);
    public abstract void toContentValue(ContentValues values, String name);
    public abstract String getTable(Persistent persistent, String name);
    public abstract String getColumnType();
    public abstract String getAlter(Persistent persistent, String name);
    public abstract void parcel(Parcel dest, int flags);
    public abstract void unparcel(Parcel source);
    public abstract Object getAsCursorRow();
    public abstract String toString();

    public ModelBasicType()
    {

    }
}
