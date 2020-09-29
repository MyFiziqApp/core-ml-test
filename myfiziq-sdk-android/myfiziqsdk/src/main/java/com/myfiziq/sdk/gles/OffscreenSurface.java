/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myfiziq.sdk.gles;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Off-screen EGL surface (pbuffer).
 * <p>
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
public class OffscreenSurface extends EglSurfaceBase {

    native long nativeCreateBuffer(int width, int height);
    native void nativeReleaseBuffer(long context);
    native Object nativeGetBuffer(long context);
    native void nativeSwapBuffers(long context);

    ByteBuffer mPixelBuf;
    long mContext = 0;

    /**
     * Creates an off-screen surface with the specified width and height.
     */
    public OffscreenSurface(EglCore eglCore, int width, int height) {
        super(eglCore);
        createOffscreenSurface(width, height);
        mContext = nativeCreateBuffer(width, height);
        mPixelBuf = (ByteBuffer)nativeGetBuffer(mContext);
        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void setPresentationTime(long nsecs)
    {

    }

    @Override
    public boolean swapBuffers()
    {
        mPixelBuf.rewind();
        GLES20.glReadPixels(0, 0, getWidth(), getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
        nativeSwapBuffers(mContext);
        return true;
    }

    /**
     * Releases any resources associated with the surface.
     */
    public void release() {
        releaseEglSurface();
        nativeReleaseBuffer(mContext);
    }

    @Override
    public Bitmap getFrame()
    {
        Bitmap outbmp = null;

        if (!mEglCore.isCurrent(mEGLSurface))
        {
            throw new RuntimeException("Expected EGL context/surface is not current");
        }

        // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
        // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
        // Bitmap "copy pixels" method wants the same format GL provides.
        //
        // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
        // here often.
        //
        // Making this even more interesting is the upside-down nature of GL, which means
        // our output will look upside down relative to what appears on screen if the
        // typical GL conventions are used.

        int width = getWidth();
        int height = getHeight();
        mPixelBuf.rewind();

        try
        {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1, width / 2f, height / 2f);
            bmp.copyPixelsFromBuffer(mPixelBuf);

            outbmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
            bmp.recycle();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        return outbmp;
    }
}
