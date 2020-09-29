package com.myfiziq.sdk.db;


import java.lang.annotation.Annotation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */

public class Orm
{
    public static final String SUFFIX = "_ORM";

    @Nullable
    public static <T> T newModel(@NonNull Class<T> clazz)
    {
        try
        {
            String name = clazz.getName() + SUFFIX;
            Class<?> processor = tryGetClass(name);

            if (null == processor)
            {
                processor = clazz;
                ClassCache.getInstance().putCachedClass(clazz.getName(), processor);
            }

            return (T) processor.newInstance();
        }
        catch (InstantiationException e)
        {
            Timber.e(e, "InstantiationException when creating newModel()");
        }
        catch (IllegalAccessException e)
        {
            Timber.e(e, "IllegalAccessException when creating newModel()");
        }

        return null;
    }

    @Nullable
    public static Class tryGetClass(@NonNull String name)
    {
        Class<?> processor = ClassCache.getInstance().getCachedClass(name);

        if (null == processor)
        {
            try
            {
                processor = Class.forName(name);
                ClassCache.getInstance().putCachedClass(name, processor);
            }
            catch (ClassNotFoundException e)
            {
                Timber.e(e, "Class not found for processor '%s'", name);
            }
        }

        return processor;
    }

    @Nullable
    public static String getModelName(@NonNull Class clazz)
    {
        String name = clazz.getName();
        Class<?> processor;

        if (name.endsWith(SUFFIX))
        {
            name = name.replace(SUFFIX, "");
        }

        processor = tryGetClass(name);

        if (null == processor)
        {
            return null;
        }


        return processor.getSimpleName();
    }

    public static boolean isInstance(@NonNull Class clazz1, @NonNull Class clazz2)
    {
        String name1 = clazz1.getName();
        String name2 = clazz2.getName();

        if (name1.endsWith(SUFFIX))
        {
            name1 = name1.replace(SUFFIX, "");
        }

        if (name2.endsWith(SUFFIX))
        {
            name2 = name2.replace(SUFFIX, "");
        }

        return name1.contentEquals(name2);
    }


    @Nullable
    public static <T extends Annotation> T getAnno(@NonNull Class clazz, @NonNull Class<T> anoClazz)
    {
        String name = clazz.getName();
        Class<?> processor;

        if (name.endsWith(SUFFIX))
        {
            name = name.replace(SUFFIX, "");
        }

        processor = tryGetClass(name);

        if (null != processor)
        {
            return processor.getAnnotation(anoClazz);
        }

        return null;
    }
}
