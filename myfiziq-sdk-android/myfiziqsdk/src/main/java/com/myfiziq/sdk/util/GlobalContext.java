package com.myfiziq.sdk.util;

import android.app.Application;
import android.content.Context;

/**
 * @hide
 */

public class GlobalContext
{
    final static Application mApp;

    public static Context getContext()
    {
        return mApp.getApplicationContext();
    }

    static
    {
        try
        {
            Class<?> c = Class.forName("android.app.ActivityThread");
            mApp = (Application)c.getDeclaredMethod("currentApplication").invoke(null);
        }
        catch (Throwable e)
        {
            throw new AssertionError(e);
        }
    }
}
