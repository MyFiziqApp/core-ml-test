package com.myfiziq.sdk.db;

import android.graphics.Color;
import android.text.TextUtils;

import com.myfiziq.sdk.helpers.AsyncHelper;

import java.util.ArrayList;

import timber.log.Timber;

public class CachedColorArray
{
    private int[] mColors = null;

    public CachedColorArray(ArrayList<String> sources)
    {
        try
        {
            if (null != sources && sources.size() > 0)
            {
                mColors = new int[sources.size()];
                for (int i=0; i<sources.size(); i++)
                {
                    mColors[i] = Color.parseColor(sources.get(i));
                }
            }
        }
        catch (IllegalArgumentException e)
        {
            Timber.e(e);
        }
    }

    public void apply(AsyncHelper.Callback<int[]> callback)
    {
        if (null != mColors)
        {
            callback.execute(mColors);
        }
    }
}
