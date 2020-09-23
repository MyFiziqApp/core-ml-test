package com.myfiziq.sdk.db;

import android.text.TextUtils;

import com.myfiziq.sdk.helpers.AsyncHelper;

import timber.log.Timber;

public class CachedFloat
{
    private Float mFloat = null;

    public CachedFloat(String source)
    {
        try
        {
            if (!TextUtils.isEmpty(source))
                mFloat = Float.parseFloat(source);
        }
        catch (NumberFormatException e)
        {
            Timber.e(e);
        }
    }

    public Float get()
    {
        return mFloat;
    }

    public Integer getAsInteger()
    {
        if (!isNull())
        {
            return Math.round(mFloat);
        }

        return null;
    }

    public void apply(AsyncHelper.Callback<Float> callback)
    {
        if (null != mFloat)
        {
            callback.execute(mFloat);
        }
    }

    public boolean isNull()
    {
        return (null == mFloat);
    }

    public boolean nullOrMatch(float value)
    {
        if (null != mFloat)
        {
            if (value != mFloat)
                return false;
        }
        return true;
    }
}
