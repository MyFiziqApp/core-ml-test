package com.myfiziq.sdk.db;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;

import timber.log.Timber;

/**
 * @hide
 */
public class ModelAvatarReqList extends Model
{
    @Persistent(escaped = true, inDb = false)
    public ArrayList<ModelAvatarReq> body = new ArrayList<>();

    /**
     * Creates avatars locally from the list of avatars received.
     *
     * @param bCacheMesh Whether we should generate a 3D mesh and cache it locally.
     */
    public void createAvatars(boolean bCacheMesh)
    {
        for (ModelAvatarReq req : body)
        {
            try
            {
                if (!TextUtils.isEmpty(req.error_id) && !req.error_id.equals("0"))
                {
                    Timber.i("Avatar with Attempt ID %s has error %s. It will not be saved.", req.attemptId, req.error_id);
                    continue;
                }

                ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, req.attemptId);
                if (null == avatar)
                {
                    // If the avatar stored on the remote server doesn't exist locally, then create it locally
                    Timber.i("Creating new avatar locally: %s", req.attemptId);

                    avatar = Orm.newModel(ModelAvatar.class);

                    avatar.set(req);

                    avatar.save();
                }
            }
            catch (Exception e)
            {
                Timber.e(e, "Cannot save avatar with Attempt ID %s. It will be ignored.", req.attemptId);
            }
        }

        if (bCacheMesh)
        {
            // Generate a 3D mesh of the avatar and cache it locally
            generateMeshes();
        }
    }

    private void generateMeshes()
    {
        ArrayList<ModelAvatar> sortedArrayList = new ArrayList<>();

        for(ModelAvatarReq modelAvatarReq : body)
        {
            ModelAvatar avatar = ORMTable.getModel(ModelAvatar.class, modelAvatarReq.attemptId);
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
