package com.myfiziq.sdk.db;

import android.os.Handler;
import android.os.Looper;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.gles.AvatarMesh;
import com.myfiziq.sdk.gles.MeshCache;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.vo.MYQNativeHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import timber.log.Timber;


public class CreateMeshRunnable implements Runnable
{
    final static private int BUFFER_SIZE = 10240;

    private final ArrayList<WeakReference<ModelAvatar.MeshReadyListener>> mListeners = new ArrayList<>();
    private final ArrayList<WeakReference<AvatarMesh>> mMeshList = new ArrayList<>();
    private WeakReference<ModelAvatar> mAvatar;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private File mVerticesFile = null;
    private File mFacesFile = null;

    CreateMeshRunnable(ModelAvatar avatar, AvatarMesh mesh, ModelAvatar.MeshReadyListener listener)
    {
        mAvatar = new WeakReference<>(avatar);
        addMesh(mesh);
        addListener(listener);
    }

    @Override
    public void run()
    {
        getVerticesFile();
        getFacesFile();

        getMeshData();
    }

    public void cancel()
    {
        mHandler.removeCallbacksAndMessages(null);
    }

    private void notifyListeners()
    {
        mHandler.post(() ->
        {
            synchronized (mListeners)
            {
                for (WeakReference<ModelAvatar.MeshReadyListener> listener : mListeners)
                {
                    ModelAvatar.MeshReadyListener l = listener.get();
                    if (null != l)
                    {
                        Timber.d("Notifying listener: %s", l.toString());
                        l.onMeshReady();
                    }
                }

                mListeners.clear();
            }
        });
    }

    private void notifyMeshListeners(MeshCache meshCache)
    {
        mHandler.post(() ->
        {
            ModelAvatar avatar = mAvatar.get();
            if (null != avatar)
            {
                synchronized (mMeshList)
                {
                    for (WeakReference<AvatarMesh> meshRef : mMeshList)
                    {
                        AvatarMesh mesh = meshRef.get();
                        if (null != mesh)
                        {
                            Timber.d("Notifying mesh listener: %s", mesh.toString());
                            mesh.setMeshData(new MeshCache(meshCache));
                        }
                    }

                    if (null != meshCache)
                    {
                        meshCache.release();
                    }

                    mMeshList.clear();
                }
            }
        });
    }

    /**
     * Creates a Mesh for this Avatar.
     */
    private void getMeshData()
    {
        ModelAvatar avatar = mAvatar.get();
        if (null != avatar)
        {
            if (avatar.isCompleted())
            {
                if (!hasVertices() || !hasFaces())
                {
                    Timber.i("A brand new AvatarMesh will now be generated for Avatar ID: %s", avatar.id);

                    MeshCache cache = generateMeshData();
                    if (null != cache)
                    {
                        notifyMeshListeners(cache);
                        notifyListeners();
                    }
                }
                else
                {
                    Timber.i("Loading AvatarMesh from cache for Avatar ID: %s", avatar.id);

                    MeshCache cache = loadFileMesh();
                    if (null != cache)
                    {
                        notifyMeshListeners(cache);
                        notifyListeners();
                    }
                }
            }
        }
    }

    private MeshCache generateMeshData()
    {
        ModelAvatar avatar = mAvatar.get();
        if (avatar == null)
        {
            Timber.e("Avatar is null. Cannot generate mesh data.");
            return null;
        }

        MyFiziq myFiziqSdk = MyFiziq.getInstance();

        MYQNativeHandle handle = myFiziqSdk.getMesh(
                avatar.height, avatar.weight, avatar.gender.ordinal(),
                avatar.chest, avatar.waist, avatar.hip,
                avatar.inseam, avatar.fitness, avatar.thigh);

        if (!handle.isInitialised())
        {
            Timber.e("Handle was not initialised when generating mesh data");
            return null;
        }

        ByteBuffer vertBufCache = myFiziqSdk.getMeshVerts(handle);
        ByteBuffer faceBufCache = myFiziqSdk.getMeshFaces(handle);

        if (vertBufCache == null || faceBufCache == null)
        {
            Timber.e("Either the verts or faces buffer was null. Cannot generate mesh data");
            handle.close();

            return null;
        }

        int vertBufSize = vertBufCache.remaining() + vertBufCache.position();
        int facesBufSize = faceBufCache.remaining() + faceBufCache.position();

        Timber.d("Generated vertices byte buffer is %s bytes", vertBufSize);
        Timber.d("Generated faces byte buffer is %s bytes", facesBufSize);

        writeFile(getVerticesFile(), vertBufCache);
        writeFile(getFacesFile(), faceBufCache);

        return new MeshCache(handle, vertBufCache, faceBufCache);
    }

    boolean hasAvatar(ModelAvatar avatar)
    {
        ModelAvatar myavatar = mAvatar.get();
        if (null != avatar && null != myavatar)
        {
            return myavatar.id.contentEquals(avatar.id);
        }

        return false;
    }

    private boolean hasMesh(AvatarMesh mesh)
    {
        synchronized (mMeshList)
        {
            for (WeakReference<AvatarMesh> meshRef : mMeshList)
            {
                if (meshRef.get() == mesh)
                    return true;
            }
        }

        return false;
    }

    void addMesh(AvatarMesh mesh)
    {
        synchronized (mMeshList)
        {
            if (null != mesh && !hasMesh(mesh))
            {
                Timber.d("Adding mesh: %s", mesh.toString());
                mMeshList.add(new WeakReference<>(mesh));
            }
        }
    }

    private boolean hasListener(ModelAvatar.MeshReadyListener listener)
    {
        synchronized (mListeners)
        {
            for (WeakReference<ModelAvatar.MeshReadyListener> listenerRef : mListeners)
            {
                if (listenerRef.get() == listener)
                    return true;
            }
        }

        return false;
    }

    void addListener(ModelAvatar.MeshReadyListener listener)
    {
        synchronized (mListeners)
        {
            if (null != listener && !hasListener(listener))
            {
                Timber.d("Adding listener: %s", listener.toString());
                mListeners.add(new WeakReference<>(listener));
            }
        }
    }

    private boolean hasVertices()
    {
        return getVerticesFile().exists();
    }

    private File getVerticesFile()
    {
        if (null == mVerticesFile)
        {
            ModelAvatar avatar = mAvatar.get();
            if (null != avatar)
            {
                mVerticesFile = new File(GlobalContext.getContext().getFilesDir(), avatar.getAttemptId() + ".vert");
            }
        }

        return mVerticesFile;
    }

    private boolean hasFaces()
    {
        return getFacesFile().exists();
    }

    private File getFacesFile()
    {
        if (null == mFacesFile)
        {
            ModelAvatar avatar = mAvatar.get();
            if (null != avatar)
            {
                mFacesFile = new File(GlobalContext.getContext().getFilesDir(), avatar.getAttemptId() + ".face");
            }
        }

        return mFacesFile;
    }

    private MeshCache loadFileMesh()
    {
        ByteBuffer vertBufCache = null;
        ByteBuffer faceBufCache = null;

        File vertFile = getVerticesFile();
        if (vertFile.exists())
        {
            int bufferSize = (int) vertFile.length();

            Timber.d("Allocating buffer of size: %s bytes", bufferSize);

            vertBufCache = ByteBuffer.allocateDirect(bufferSize);
            vertBufCache.order(ByteOrder.nativeOrder());
            readFile(vertFile, vertBufCache);
        }

        File faceFile = getFacesFile();
        if (faceFile.exists())
        {
            faceBufCache = ByteBuffer.allocateDirect((int) faceFile.length());
            faceBufCache.order(ByteOrder.nativeOrder());
            readFile(faceFile, faceBufCache);
        }

        return new MeshCache(vertBufCache, faceBufCache);
    }

    private void readFile(File input, ByteBuffer dest)
    {
        int bytesRemainingInBuffer = 0;
        int bytesRead = 0;

        try
        {
            if (null != input && null != dest)
            {
                Timber.d("Started reading %s", input.getName());

                FileInputStream fis = new FileInputStream(input);
                byte[] data = new byte[BUFFER_SIZE];
                while ((bytesRead = fis.read(data)) != -1)
                {
                    bytesRemainingInBuffer = dest.remaining();
                    dest.put(data, 0, bytesRead);
                }
                fis.close();
                dest.rewind();

                Timber.d("Finished reading %s", input.getName());
            }
        }
        catch (Throwable t)
        {
            String message = "Error occurred reading file. Tried to put " + bytesRead + " bytes into the buffer when only " + bytesRemainingInBuffer + " remain.";
            Timber.e(t, message);
        }
    }

    private void writeFile(File output, ByteBuffer src)
    {
        try
        {
            if (null != output && null != src)
            {
                FileOutputStream fos = new FileOutputStream(output);
                byte[] data = new byte[BUFFER_SIZE];
                src.rewind();
                while (src.hasRemaining())
                {
                    int read = Math.min(src.remaining(), BUFFER_SIZE);
                    src.get(data, 0, read);
                    fos.write(data, 0, read);
                }
                fos.close();
                src.rewind();
            }
        }
        catch (Throwable t)
        {
            Timber.e(t, "Error when writing to file");
        }
    }

}
