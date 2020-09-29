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
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.myfiziq.sdk.R;

import androidx.annotation.ColorInt;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MYQGLTextureView extends GLTextureView {

    private volatile MYQGLRenderer mRenderer;

    public MYQGLTextureView(Context context) {
        super(context);
        init(context, null);
    }

    public MYQGLTextureView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        boolean bContinuous = false;

        if (null != attrs)
        {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs, R.styleable.MYQGLTextureView, 0, 0);

            try
            {
                if (a.hasValue(R.styleable.MYQGLTextureView_continuous))
                    bContinuous = a.getBoolean(R.styleable.MYQGLTextureView_continuous, false);
            }
            catch (Throwable t)
            {

            }
        }

        //getHolder().setFormat(PixelFormat.RGBA_8888);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(new ConfigChooser(8, 8, 8, 8, 16, 0));

        setPreserveEGLContextOnPause(true);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MYQGLRenderer(context);
        setRenderer(mRenderer);

        if (bContinuous)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        else
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void setBackgroundColor(@ColorInt int color)
    {
        if (null != mRenderer)
        {
            mRenderer.setClearColor(color);
        }
    }

    public MYQGLRenderer getRenderer()
    {
        return mRenderer;
    }

    @Override
    public void surfaceDestroyed(SurfaceTexture holder)
    {
        if (null != mRenderer)
        {
            mRenderer.onSurfaceDestroyed(null);
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        TextureMgr.getInstance().forceReload();
    }
}
