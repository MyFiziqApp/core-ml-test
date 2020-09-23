package com.myfiziq.sdk.db;

import android.graphics.Color;
import android.text.TextUtils;

import com.myfiziq.sdk.helpers.AsyncHelper;

import timber.log.Timber;

public class CachedColor
{
    private Integer mColor = null;

    public CachedColor(String source)
    {
        try
        {
            if (!TextUtils.isEmpty(source))
                mColor = Color.parseColor(source);
        }
        catch (IllegalArgumentException e)
        {
            Timber.e(e);
        }
    }

    public void apply(AsyncHelper.Callback<Integer> callback)
    {
        if (null != mColor)
        {
            callback.execute(mColor);
        }
    }

    public boolean isNull()
    {
        return (null == mColor);
    }

    public boolean nullOrMatch(int color)
    {
        if (null != mColor)
        {
            if (color != mColor)
                return false;
        }
        return true;
    }
}
