package com.myfiziq.sdk.db;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

/**
 * @hide
 */
public class ModelFieldCache
{
    private static ModelFieldCache mThis = null;

    HashMap<Class, List<Field>> mModelFields = new HashMap<>();

    public synchronized static ModelFieldCache getInstance()
    {
        if (null == mThis)
        {
            mThis = new ModelFieldCache();
        }

        return mThis;
    }

    private ModelFieldCache()
    {
        for (ORMTable table : ORMTable.values())
        {
            Class clazz = table.getTableClass();
            mModelFields.put(
                    clazz,
                    Reflection.getAllDeclaredNonStaticFieldsSorted(clazz));
        }
    }

    public List<Field> getFields(Class clazz)
    {
        List<Field> fields = mModelFields.get(clazz);
        if (null == fields)
        {
            fields = Reflection.getAllDeclaredNonStaticFieldsSorted(clazz);
            mModelFields.put(
                    clazz,
                    fields);
        }

        return fields;
    }
}
