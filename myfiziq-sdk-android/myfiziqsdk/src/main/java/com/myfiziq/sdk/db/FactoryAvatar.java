package com.myfiziq.sdk.db;


import com.myfiziq.sdk.MyFiziqThreadFactory;
import com.myfiziq.sdk.gles.AvatarMesh;
import com.myfiziq.sdk.manager.MFZLifecycleGuard;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import timber.log.Timber;


/**
 * Provides a mechanism to generate a 3D mesh of an avatar.
 */
public class FactoryAvatar
{
    private static FactoryAvatar mThis = null;
    private FactoryAvatarExeuctor mThreadPool;


    private FactoryAvatar()
    {
        mThreadPool = new FactoryAvatarExeuctor(
                4, 4, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<>(),
                new MyFiziqThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Returns a singleton of this class that can be used to perform operations on it.
     */
    public synchronized static FactoryAvatar getInstance()
    {
        if (null == mThis)
        {
            mThis = new FactoryAvatar();
        }

        return mThis;
    }

    /**
     * Queues the generation of a 3D avatar mesh.
     */
    public synchronized boolean queueAvatarMesh(ModelAvatar avatar, AvatarMesh mesh, ModelAvatar.MeshReadyListener listener)
    {
        MFZLifecycleGuard lifecycleGuard = MyFiziqSdkManager.getInstance().getLifecycleGuard();

        lifecycleGuard.waitUntilSdkInit();
        lifecycleGuard.assertConfigurationAssigned();
        lifecycleGuard.assertSdkInitialised();


        CreateMeshRunnable existingItem = getExistingQueueItem(avatar);

        if (null == existingItem)
        {
            Timber.d("Added Avatar ID %s to the FactoryAvatar queue.", avatar.id);

            mThreadPool.execute(new CreateMeshRunnable(avatar, mesh, listener));
            return true;
        }
        else if (null != listener)
        {
            Timber.d("Avatar ID %s is already in the FactoryAvatar queue. Listening for completion...", avatar.id);
            existingItem.addMesh(mesh);
            existingItem.addListener(listener);
            return true;
        }
        else
        {
            Timber.d("Avatar ID %s is already in the FactoryAvatar queue.", avatar.id);
            return false;
        }
    }

    /**
     * Determines if the avatar is currently in the queue to have a 3D mesh generated.
     */
    public boolean isAvatarCurrentlyInQueue(ModelAvatar modelAvatar)
    {
        BlockingQueue<Runnable> queue = mThreadPool.getQueue();

        for (Runnable item : queue)
        {
            if (item instanceof CreateMeshRunnable)
            {
                CreateMeshRunnable meshRunnable = (CreateMeshRunnable) item;

                if (meshRunnable.hasAvatar(modelAvatar))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets an existing runnable in the queue of avatars to have 3D meshes generated.
     */
    @Nullable
    public CreateMeshRunnable getExistingQueueItem(ModelAvatar modelAvatar)
    {
        ConcurrentLinkedQueue<Runnable> queue = mThreadPool.getPendingRunnables();

        if (null == queue)
        {
            return null;
        }

        for (Runnable item : queue)
        {
            if (item instanceof CreateMeshRunnable)
            {
                CreateMeshRunnable meshRunnable = (CreateMeshRunnable) item;

                if (meshRunnable.hasAvatar(modelAvatar))
                {
                    return meshRunnable;
                }
            }
        }

        return null;
    }
}
