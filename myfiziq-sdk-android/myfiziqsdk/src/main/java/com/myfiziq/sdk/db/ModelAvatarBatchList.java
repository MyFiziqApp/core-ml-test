package com.myfiziq.sdk.db;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @hide
 */

public class ModelAvatarBatchList extends Model
{
    @Persistent(inDb = false)
    public ModelAvatarReq body = null;

    public void updateAvatar(ModelAvatar avatar)
    {
        if (null != body && null != avatar)
        {
            //Timber.e("Create NEW Avatar:" + req);



            avatar.set(body);

            avatar.save();
        }
    }

    public boolean isValid()
    {
        if (null == body)
            return false;

        return body.isValid();
    }

    public boolean isError()
    {
        if (null == body)
            return false;

        return body.isError();
    }

    private void generateMeshes()
    {
        ArrayList<ModelAvatar> sortedArrayList = new ArrayList<>();

        if (null != body)
        {
            ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, body.requestdate);
            sortedArrayList.add(avatar);
        }

        Collections.sort(sortedArrayList, (o1, o2) -> {
            long o1Time = o1.getRequestTime();
            long o2Time = o2.getRequestTime();
            return Long.compare(o1Time, o2Time);
        });

        for (ModelAvatar model : sortedArrayList)
        {
            FactoryAvatar.getInstance().queueAvatarMesh(model, null, null);
        }
    }
}
