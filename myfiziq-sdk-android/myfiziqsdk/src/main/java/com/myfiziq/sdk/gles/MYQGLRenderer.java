/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.opengl.GLES20;
import android.view.View;

import com.myfiziq.sdk.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.ColorInt;
import timber.log.Timber;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MYQGLRenderer implements GLTextureView.Renderer
{
    private final static long FRAME_RATE = 60;
    private final static long DELTA_FRAME_RATE = 1000 / FRAME_RATE;

    private WeakReference<View> mView;

    private WeakReference<Context> mContext;
    private float mWidth = 0;
    private float mHeight = 0;

    @ColorInt
    private int mClearColor = 0x00FFFFFF;

    @ColorInt
    private int mLastClearColor = 0x00FFFFFF;

    private float x = 0;
    private float y = 0;
    private float z = 0;

    private boolean mObjectsCreated = false;
    private ArrayList<WeakReference<Mesh>> mMeshList = new ArrayList<>();

    private Camera mCamera = new Camera();

    /**
     * @hide
     */
    public static class Bounds
    {
        public float mX;
        public float mY;
        public float mW;
        public float mH;

        Bounds()
        {

        }

        public Bounds(float x, float y, float w, float h)
        {
            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }

        public void set(Bounds bounds)
        {
            mX = bounds.mX;
            mY = bounds.mY;
            mW = bounds.mW;
            mH = bounds.mH;
        }
    }

    public MYQGLRenderer(Context context)
    {
        mContext = new WeakReference<>(context);
        // Draw background color
        int[] attributes = {android.R.attr.background};
        TypedArray styledAttributes = getContext().obtainStyledAttributes(R.style.MFViewAvatarAvatar, attributes);
        mClearColor = styledAttributes.getInt(0, getContext().getResources().getColor(R.color.myfiziqsdk_white));
        mLastClearColor = mClearColor;
        styledAttributes.recycle();
    }

    public Context getContext()
    {
        return mContext.get();
    }

    public static float getRed(int color)
    {
        int r = ((color & 0x00FF0000) >> 16);
        return r / 255.0f;
    }

    public static float getGreen(int color)
    {
        int g = ((color & 0x0000FF00) >> 8);
        return g / 255.0f;
    }

    public static float getBlue(int color)
    {
        int b = ((color & 0x000000FF));
        return b / 255.0f;
    }

    public static float getAlpha(int color)
    {
        int a = ((color & 0xFF000000) >> 24);
        return a / 255.0f;
    }

    public void initRenderer()
    {
        // Set the background frame color
        //Set clear color as a float from 0.0f-1.0f
        //GLES20.glClearColor(0xF1/255f, 0xF1/255f, 0xF1/255f, 1.0f);
        GLES20.glClearColor(getRed(mLastClearColor), getGreen(mLastClearColor), getBlue(mLastClearColor), getAlpha(mLastClearColor));
        GLES20.glClearDepthf(1.0f);

        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glDepthMask(true);

        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        GLES20.glFrontFace(GL10.GL_CCW);
        GLES20.glCullFace(GL10.GL_BACK);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_SMOOTH);

        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); //GL_DST_ALPHA
        GLES20.glDisable(GL10.GL_BLEND);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGlError("initRenderer");

        TextureMgr.getInstance().reloadTextures();
    }

    public void initViewport(int width, int height)
    {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;

        mCamera.perspective(
                45f,
                mWidth / mHeight,
                1f, 1000f);

        applyValues();

        TextureMgr.getInstance().forceReload();

        checkGlError("initViewport");
    }

    public void initObjects()
    {
        if (!mObjectsCreated)
        {
            applyValues();

            mObjectsCreated = true;
        }
    }

    public void setClearColor(@ColorInt int color)
    {
        mClearColor = color;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        initRenderer();
        initObjects();
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {
        long timing = System.currentTimeMillis();

        GLES20.glClearColor(getRed(mClearColor), getGreen(mClearColor), getBlue(mClearColor), 1.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        //mQuad.draw(mCamera);

        synchronized (this)
        {
            for (WeakReference<Mesh> meshRef : mMeshList)
            {
                Mesh mesh = meshRef.get();
                if (null != mesh)
                {
                    mesh.draw(mCamera);
                }
            }
        }

        TextureMgr.getInstance().unloadTextures();

        checkGlError("onDrawFrame");

        try
        {
            long timeTaken = System.currentTimeMillis() - timing;
            if (timeTaken < DELTA_FRAME_RATE)
                Thread.sleep(DELTA_FRAME_RATE - timeTaken);
        }
        catch (InterruptedException e)
        {
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        initViewport(width, height);
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     * <p>
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
        {
            Timber.e(glOperation + ": glError " + error);
            //throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    private float normaliseG(float val)
    {
        return Math.abs(1.0f - (val / 9.81f));
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getZ()
    {
        return z;
    }

    public void onSurfaceDestroyed(GL10 holder)
    {

        TextureMgr.getInstance().forceReload();

        //if (null != mShaderTexture)
        //    mShaderTexture.resetShader();
    }

    private void applyValues()
    {
        //if (null != mViewTexture && null != mView)
        //    mViewTexture.setView(mView.get());
    }

    public void setView(View view)
    {
        mView = new WeakReference<>(view);
        applyValues();
    }

    public synchronized void removeAllMesh()
    {
        mMeshList.clear();
    }

    public synchronized void addMesh(Mesh mesh)
    {
        if (null != mesh)
        {
            mMeshList.add(new WeakReference<>(mesh));
        }
    }
}
