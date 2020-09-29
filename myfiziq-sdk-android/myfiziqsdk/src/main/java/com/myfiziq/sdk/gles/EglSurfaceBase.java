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
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Common base class for EGL surfaces.
 * <p>
 * There can be multiple surfaces associated with a single context.
 *
 * @hide
 */
public class EglSurfaceBase
{
    // EglCore object we're associated with.  It may be associated with multiple surfaces.
    protected EglCore mEglCore;

    EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private int mWidth = -1;
    private int mHeight = -1;

    protected EglSurfaceBase(EglCore eglCore)
    {
        mEglCore = eglCore;
    }

    /**
     * Creates a window surface.
     * <p>
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    public void createWindowSurface(Object surface)
    {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE)
        {
            throw new IllegalStateException("Surface already created");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);

        // Don't cache width/height here, because the size of the underlying surface can change
        // out from under us (see e.g. HardwareScalerActivity).
        //mWidth = mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        //mHeight = mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
    }

    /**
     * Creates an off-screen surface.
     */
    public void createOffscreenSurface(int width, int height)
    {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE)
        {
            throw new IllegalStateException("surface already created");
        }
        mEGLSurface = mEglCore.createOffscreenSurface(width, height);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the surface's width, in pixels.
     * <p>
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    public int getWidth()
    {
        if (mWidth < 0)
        {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        }
        else
        {
            return mWidth;
        }
    }

    /**
     * Returns the surface's height, in pixels.
     */
    public int getHeight()
    {
        if (mHeight < 0)
        {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
        }
        else
        {
            return mHeight;
        }
    }

    /**
     * Release the EGL surface.
     */
    public void releaseEglSurface()
    {
        mEglCore.releaseSurface(mEGLSurface);
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mWidth = mHeight = -1;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent()
    {
        mEglCore.makeCurrent(mEGLSurface);
    }

    /**
     * Makes our EGL context and surface current for drawing, using the supplied surface
     * for reading.
     */
    public void makeCurrentReadFrom(EglSurfaceBase readSurface)
    {
        mEglCore.makeCurrent(mEGLSurface, readSurface.mEGLSurface);
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers()
    {
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if (!result)
        {
            //Timber.d("WARNING: swapBuffers() failed");
        }
        return result;
    }

    /**
     * Sends the presentation time stamp to EGL.
     *
     * @param nsecs Timestamp, in nanoseconds.
     */
    public void setPresentationTime(long nsecs)
    {
        mEglCore.setPresentationTime(mEGLSurface, nsecs);
    }

    /**
     * Saves the EGL surface to a file.
     * <p>
     * Expects that this object's EGL surface is current.
     */
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
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        GlUtil.checkGlError("glReadPixels");
        buf.rewind();

        byte[] bytes = buf.array();
        if (null != bytes)
        {
            for (int i=0; i<width * height * 4; i+=4)
            {
                if ((byte)0xFF == bytes[i] && (byte)0xFF == bytes[i+1] && (byte)0xFF == bytes[i+2])
                {
                    bytes[i+3] = 0;
                }
            }
        }

        BufferedOutputStream bos = null;
        try
        {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1, width / 2f, height / 2f);
            bmp.copyPixelsFromBuffer(buf);

            outbmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
            bmp.recycle();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        return outbmp;
    }

    public void saveFrame(File file) throws IOException
    {
        String filename = file.toString();
        BufferedOutputStream bos = null;
        try
        {
            bos = new BufferedOutputStream(new FileOutputStream(filename));
            Bitmap outbmp = getFrame();
            outbmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            outbmp.recycle();
        }
        finally
        {
            if (bos != null) bos.close();
        }
        //Timber.d("Saved " + width + "x" + height + " frame as '" + filename + "'");
    }
}
