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
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Process;
import android.view.TextureView;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MYQGLTxRenderer extends MYQGLRenderer
        implements TextureView.SurfaceTextureListener,
        Runnable
{
    // Experiment with allowing TextureView to release the SurfaceTexture from the callback vs.
    // releasing it explicitly ourselves from the draw loop.  The latter seems to be problematic
    // in 4.4 (KK) -- set the flag to "false", rotate the screen a few times, then check the
    // output of "adb shell ps -t | grep `pid grafika`".
    //
    // Must be static or it'll get reset on every Activity pause/resume.
    private static volatile boolean sReleaseInCallback = true;

    Thread mThread;
    Object mLock = new Object();        // guards mSurfaceTexture, mDone
    SurfaceTexture mSurfaceTexture;
    EglCore mEglCore;
    boolean mDone;
    boolean mOffscreen = false;
    int mWidth = 1024;
    int mHeight = 1024;

    public MYQGLTxRenderer(Context context) {
        super(context);
    }

    public MYQGLTxRenderer(Context context, boolean bOffscreen, int width, int height) {
        super(context);
        mOffscreen = bOffscreen;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void run() {
        int tid = android.os.Process.myTid();
        android.os.Process.setThreadPriority(tid, Process.THREAD_PRIORITY_URGENT_DISPLAY);

        while (true) {
            SurfaceTexture surfaceTexture = null;

            // Latch the SurfaceTexture when it becomes available.  We have to wait for
            // the TextureView to create it.
            synchronized (mLock) {
                while (!mDone && (surfaceTexture = mSurfaceTexture) == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) {
                        break;
                        //throw new RuntimeException(ie);     // not expected
                    }
                }
                if (mDone) {
                    break;
                }
            }
            //Timber.d("Got surfaceTexture=" + surfaceTexture);

            // Create an EGL surface for our new SurfaceTexture.  We're not on the same
            // thread as the SurfaceTexture, which is a concern for the *consumer*, which
            // wants to call updateTexImage().  Because we're the *producer*, i.e. the
            // one generating the frames, we don't need to worry about being on the same
            // thread.
            mEglCore = EglCore.getSharedInstance(EglCore.FLAG_TRY_GLES3);//new EglCore(null, EglCore.FLAG_TRY_GLES3);

            if (mOffscreen)
            {
                OffscreenSurface windowSurface = new OffscreenSurface(mEglCore, mWidth, mHeight);
                windowSurface.makeCurrent();

                initRenderer();
                initViewport(windowSurface.getWidth(), windowSurface.getHeight());
                initObjects();

                // Render frames until we're told to stop or the SurfaceTexture is destroyed.
                while (!mDone)
                {
                    // Check to see if the TextureView's SurfaceTexture is still valid.
                    synchronized (mLock)
                    {
                        SurfaceTexture s = mSurfaceTexture;
                        if (s == null)
                        {
                            //Timber.d("doAnimation exiting");
                            return;
                        }
                    }

                    onDrawFrame(null);

                    // Publish the frame.  If we overrun the consumer, frames will be dropped,
                    // so on a sufficiently fast device the animation will run at faster than
                    // the display refresh rate.
                    //
                    // If the SurfaceTexture has been destroyed, this will throw an exception.
                    windowSurface.swapBuffers();
                }

                windowSurface.release();
            }
            else
            {
                WindowSurface windowSurface = new WindowSurface(mEglCore, mSurfaceTexture);
                windowSurface.makeCurrent();

                initRenderer();
                initViewport(windowSurface.getWidth(), windowSurface.getHeight());
                initObjects();

                // Render frames until we're told to stop or the SurfaceTexture is destroyed.
                while (!mDone)
                {
                    // Check to see if the TextureView's SurfaceTexture is still valid.
                    synchronized (mLock)
                    {
                        SurfaceTexture s = mSurfaceTexture;
                        if (s == null)
                        {
                            //Timber.d("doAnimation exiting");
                            return;
                        }
                    }

                    onDrawFrame(null);

                    // Publish the frame.  If we overrun the consumer, frames will be dropped,
                    // so on a sufficiently fast device the animation will run at faster than
                    // the display refresh rate.
                    //
                    // If the SurfaceTexture has been destroyed, this will throw an exception.
                    windowSurface.swapBuffers();
                }

                windowSurface.release();
            }
            mEglCore.release();
            if (!sReleaseInCallback) {
                //Timber.i("Releasing SurfaceTexture in renderer thread");
                surfaceTexture.release();
            }
        }

        mThread = null;
        //Timber.d("Renderer thread exiting");
    }

    public void start()
    {
        //if (null == mThread || !mThread.isAlive())
        {
            if (null != mThread)
            {
                mThread.interrupt();
            }
            mDone = false;
            mThread = new Thread(this);
            mThread.setPriority(Thread.MAX_PRIORITY);
            mThread.start();
        }
    }

    /**
     * Tells the thread to stop running.
     */
    public void halt() {
        synchronized (mLock) {
            mDone = true;
            mLock.notify();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        //Timber.d("onSurfaceTextureAvailable(" + width + "x" + height + ")");
        start();
        synchronized (mLock) {
            mSurfaceTexture = surface;
            mLock.notify();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        //Timber.d("onSurfaceTextureSizeChanged(" + width + "x" + height + ")");
        initViewport(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        //Timber.d("onSurfaceTextureDestroyed");

        // We set the SurfaceTexture reference to null to tell the Renderer thread that
        // it needs to stop.  The renderer might be in the middle of drawing, so we want
        // to return false here so that the caller doesn't try to release the ST out
        // from under us.
        //
        // In theory.
        //
        // In 4.4, the buffer queue was changed to be synchronous, which means we block
        // in dequeueBuffer().  If the renderer has been running flat out and is currently
        // sleeping in eglSwapBuffers(), it's going to be stuck there until somebody
        // tears down the SurfaceTexture.  So we need to tear it down here to ensure
        // that the renderer thread will break.  If we don't, the thread sticks there
        // forever.
        //
        // The only down side to releasing it here is we'll get some complaints in logcat
        // when eglSwapBuffers() fails.
        synchronized (mLock) {
            mSurfaceTexture = null;
        }
        if (sReleaseInCallback) {
            //Timber.i("Allowing TextureView to release SurfaceTexture");
        }

        onSurfaceDestroyed(null);

        halt();

        return sReleaseInCallback;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
    }
}