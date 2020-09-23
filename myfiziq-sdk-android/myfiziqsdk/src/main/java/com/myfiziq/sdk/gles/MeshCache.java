package com.myfiziq.sdk.gles;

import com.myfiziq.sdk.vo.MYQNativeHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import timber.log.Timber;

public class MeshCache
{
    private ByteBuffer mVerticesBuffer;
    private ByteBuffer mIndicesBuffer;
    private MYQNativeHandle mHandle;

    @SuppressWarnings("CopyConstructorMissesField")
    public MeshCache(MeshCache src)
    {
        Timber.d("[MeshCache::MeshCache] %s", src);

        if (null != src && null != src.mVerticesBuffer && null != src.mIndicesBuffer)
        {
            int capacity = src.mVerticesBuffer.capacity();
            mVerticesBuffer = ByteBuffer.allocateDirect(capacity);
            mVerticesBuffer.order(ByteOrder.nativeOrder());
            mVerticesBuffer.position(0);
            src.mVerticesBuffer.position(0);
            mVerticesBuffer.put(src.mVerticesBuffer);

            capacity = src.mIndicesBuffer.capacity();
            mIndicesBuffer = ByteBuffer.allocateDirect(capacity);
            mIndicesBuffer.order(ByteOrder.nativeOrder());
            mIndicesBuffer.position(0);
            src.mIndicesBuffer.position(0);
            mIndicesBuffer.put(src.mIndicesBuffer);
        }
        else
        {
            Timber.w("[MeshCache::MeshCache] NULL src");
        }
    }

    public MeshCache(ByteBuffer vertBuf, ByteBuffer faceBuf)
    {
        Timber.d("[MeshCache::MeshCache] %s %s", vertBuf, faceBuf);
        mVerticesBuffer = vertBuf;
        mIndicesBuffer = faceBuf;
    }

    public MeshCache(MYQNativeHandle handle, ByteBuffer vertBuf, ByteBuffer faceBuf)
    {
        Timber.d("[MeshCache::MeshCache] %s %s %s", handle, vertBuf, faceBuf);
        mHandle = handle;
        mVerticesBuffer = vertBuf;
        mIndicesBuffer = faceBuf;
    }

    public boolean isValid()
    {
        return (null != mVerticesBuffer && null != mIndicesBuffer);
    }

    public void release()
    {
        Timber.d("[MeshCache::release]");
        mVerticesBuffer = null;
        mIndicesBuffer = null;
        if (null != mHandle)
        {
            mHandle.close();
        }
    }

    public ByteBuffer getVerticesBuffer()
    {
        return mVerticesBuffer;
    }

    public ByteBuffer getIndicesBuffer()
    {
        return mIndicesBuffer;
    }

    public MYQNativeHandle getHandle()
    {
        return mHandle;
    }
}
