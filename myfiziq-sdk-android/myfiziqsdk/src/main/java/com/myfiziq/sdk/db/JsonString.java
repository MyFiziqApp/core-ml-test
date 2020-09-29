package com.myfiziq.sdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myfiziq.sdk.util.BaseUtils;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * Stores raw JSON as a B91 encoded string in the database.
 * Exposes the value as a JSON string in the model.
 */
public class JsonString extends ModelBasicType
{
    public String mValue = "";

    public JsonString()
    {

    }

    @Override
    public void fromJsonElement(JsonElement elem)
    {
        // whole JSON object as a string.
        mValue = elem.getAsString();
    }

    @Override
    public void toJSONObject(JSONObject dest, String name)
    {
        try
        {
            // Parse JSON string and store as an object.
            JSONObject object = new JSONObject(mValue);
            dest.put(name, object);
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }
    }

    @Override
    public void toJsonObject(JsonObject dest, String name)
    {
        // Parse JSON string and store as an object.
        JsonElement rootElement = JsonParser.parseString(mValue);
        dest.add(name, rootElement);
    }

    @Override
    public void readFromCursor(Cursor cursor, int column, Model model)
    {
        // Stored value in DB is B91(string) -> decode value.
        String value = cursor.getString(column);
        if (!TextUtils.isEmpty(value))
        {
            mValue = new String(BaseUtils.decode(value, BaseUtils.Format.string));
        }
        else
        {
            mValue = "";
        }
    }

    @Override
    public boolean isModified(Cursor cursor, int column)
    {
        if (!TextUtils.isEmpty(mValue))
        {
            if (cursor.isNull(column))
                return true;

            return (!cursor.getString(column).contentEquals(BaseUtils.encode(mValue.getBytes(), BaseUtils.Format.string)));
        }
        return false;
    }

    @Override
    public void toContentValue(ContentValues values, String name)
    {
        if (!TextUtils.isEmpty(mValue))
        {
            values.put(name, BaseUtils.encode(mValue.getBytes(), BaseUtils.Format.string));
        }
    }

    @Override
    public String getTable(Persistent persistent, String name)
    {
        return String.format("%s BLOB %s", name, persistent.appDb());
    }

    @Override
    public String getColumnType()
    {
        return "b91json";
    }

    @Override
    public String getAlter(Persistent persistent, String name)
    {
        return String.format("ADD %s BLOB %s DEFAULT '%s'", name, persistent.appDb(), toString());
    }

    @Override
    public void parcel(Parcel dest, int flags)
    {
        dest.writeString(mValue);
    }

    @Override
    public void unparcel(Parcel source)
    {
        mValue = source.readString();
    }

    @Override
    public Object getAsCursorRow()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mValue;
    }

    public String getElement(String name)
    {
        JsonElement rootElement = JsonParser.parseString(mValue);
        if (rootElement.isJsonObject())
        {
            JsonObject object = rootElement.getAsJsonObject();
            if (object.has(name))
            {
                JsonElement element = object.get(name);
                return element.getAsString();
            }
        }

        return null;
    }

    public void setElement(String name, String value)
    {
        JsonElement rootElement;

        if (!TextUtils.isEmpty(mValue))
        {
            rootElement = JsonParser.parseString(mValue);
        }
        else
        {
            rootElement = new JsonObject();
        }

        if (rootElement.isJsonObject())
        {
            JsonObject object = rootElement.getAsJsonObject();
            object.addProperty(name, value);
            mValue = object.toString();
        }
    }
}
