package com.myfiziq.sdk.db;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @hide
 */
public class Reflection
{
    public static <T> T newInstance(Class<T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Unable to instantiate: " + clazz.getSimpleName(), e);
        }
    }

    public static List<Field> getAllDeclaredNonStaticFields(Class clazz)
    {
        List<Field> fields = new ArrayList<>();
        while (!clazz.equals(Object.class))
        {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    public static List<Field> getAllDeclaredNonStaticFieldsSorted(Class clazz)
    {
        List<Field> fields = new ArrayList<>();
        while (!clazz.equals(Object.class))
        {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        Collections.sort(fields, (lhs, rhs) ->
        {
            String lhsName = lhs.getName();
            String rhsName = rhs.getName();

            if (lhsName.contentEquals(rhsName))
                return 0;

            if (lhsName.contentEquals("id"))
                return -1;

            if (rhsName.contentEquals("id"))
                return 1;

            return lhsName.compareTo(rhsName);
        });
        return fields;
    }

    public static List<Field> getAllDeclaredNonStaticFieldsSorted(Class clazz, Comparator<Field> comparator)
    {
        List<Field> fields = new ArrayList<>();
        while (!clazz.equals(Object.class))
        {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        Collections.sort(fields, comparator);
        return fields;
    }

    public static Field getField(Class clazz, String name)
    {
        try
        {
            return clazz.getField(name);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getFieldValue(Object object, String field)
    {
        return getFieldValue(object, getField(object.getClass(), field));
    }

    public static Object getFieldValue(Object object, Field field)
    {
        try
        {
            field.setAccessible(true);
            return field.get(object);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Unable to get field value: " + field.getName(), e);
        }
    }

    public static void setFieldValue(Object object, String field, Object value)
    {
        Field fieldObj = getField(object.getClass(), field);
        if (null != fieldObj)
        {
            setFieldValue(object, fieldObj, value);
        }
        else
        {
            throw new IllegalArgumentException("Unable to set field value: " + field + " = " + value);
            //Timber.e("Unable to set field value: " + field + " = " + value);
        }
    }

    public static void setFieldValue(Object object, Field field, Object value)
    {
        try
        {
            field.setAccessible(true);
            field.set(object, value);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Unable to set field value: " + field.getName() + " = " + value, e);
        }
    }

    public static void setFieldEnumValue(Object object, Field field, String value)
    {
        try
        {
            field.setAccessible(true);
            field.set(object, Enum.valueOf((Class<Enum>) field.getType(), value));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Unable to set field value: " + field.getName() + " = " + value, e);
        }
    }
}
