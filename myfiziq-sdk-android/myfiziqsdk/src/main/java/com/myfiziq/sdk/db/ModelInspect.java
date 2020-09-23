package com.myfiziq.sdk.db;

public class ModelInspect extends Model
{
    @Persistent
    public ModelInspectRes result = Orm.newModel(ModelInspectRes.class);

    @Persistent
    public ModelJoints joints = Orm.newModel(ModelJoints.class);

    public boolean matches(ModelInspect inspect2)
    {
        if (null == result || null == inspect2 || null == inspect2.result)
            return false;

        return result.matches(inspect2.result);
    }
}
