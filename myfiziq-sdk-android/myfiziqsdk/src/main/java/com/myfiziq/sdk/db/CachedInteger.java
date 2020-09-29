package com.myfiziq.sdk.db;

import android.graphics.Color;
import android.text.TextUtils;

import com.myfiziq.sdk.helpers.AsyncHelper;

import timber.log.Timber;

public class CachedInteger
{
    private Integer mInteger = null;

    public CachedInteger()
    {
    }

    public CachedInteger(String source)
    {
        try
        {
            if (!TextUtils.isEmpty(source))
                mInteger = Integer.parseInt(source);
        }
        catch (IllegalArgumentException e)
        {
            Timber.e(e);
        }
    }

    public CachedInteger(int source)
    {
        mInteger = source;
    }

    public void apply(AsyncHelper.Callback<Integer> callback)
    {
        if (null != mInteger)
        {
            callback.execute(mInteger);
        }
    }
}
