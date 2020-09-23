package com.myfiziq.sdk.db;

import java.util.HashMap;

/**
 * Caching manager for <code>Cached</code> <code>Model</code> instances.
 */
public class ClassCache
{
    static ClassCache mThis;

    HashMap<String, Class> mCache = new HashMap<>();

    public synchronized static ClassCache getInstance()
    {
        if (null == mThis)
        {
            mThis = new ClassCache();
        }

        return mThis;
    }

    private ClassCache()
    {

    }

    public Class getCachedClass(String name)
    {
        return mCache.get(name);
    }

    public void putCachedClass(String name, Class clazz)
    {
        if (null != clazz)
        {
            mCache.put(name, clazz);
        }
    }
}
