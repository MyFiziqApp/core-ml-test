package com.myfiziq.sdk.views;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.myfiziq.sdk.gles.AvatarMesh;
import com.myfiziq.sdk.gles.MYQGLRenderer;
import com.myfiziq.sdk.gles.MYQGLTextureView;
import com.myfiziq.sdk.gles.Vector3D;
import com.myfiziq.sdk.helpers.ActivityHelper;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Custom <code>View</code> implementation for rendering an <code>AvatarMesh</code>.
 * <br>
 * Includes support for rotating, moving and zooming the mesh.
 * <br>
 * The simplest way to use this view is to include it in an xml layout and provide a mesh via
 * <code>setMesh</code>.
 * @see AvatarMesh
 */
public class AvatarView extends MYQGLTextureView implements AvatarMesh.AvatarMeshListener
{
    public enum ScrollState
    {
        None,
        Vertical,
        Horizontal
    }

    private AvatarView mLinkedView = null;
    private volatile AvatarMesh mAvatarMesh = null;
    private OnTouchHandler mTouchHandler;
    private boolean translateEnabled = true;
    private boolean scaleEnabled = true;
    private boolean rotateEnabled = true;
    private volatile Vector3D mAngle = new Vector3D();

    // Store a reference to the Application since we'll need it during onDetachedFromWindow().
    // Getting the Application may not be possible at that point if the activity is finishing.
    private Application application;
    private Application.ActivityLifecycleCallbacks lifecycleCallback;


    public AvatarView(Context context)
    {
        super(context);
        init(context, null);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

//    @Override
//    protected void onAttachedToWindow()
//    {
//        super.onAttachedToWindow();
//        MYQGLRenderer renderer = getRenderer();
//        if (null != renderer)
//        {
//            renderer.removeAllMesh();
//            renderer.addMesh(mAvatarMesh);
//        }
//    }

    public void linkView(AvatarView other)
    {
        mLinkedView = other;
    }

    /**
     * Sets the mesh that is to be rendered by the view.
     * @param mesh - The mesh to render.
     */
    public synchronized void setMesh(AvatarMesh mesh)
    {
        Timber.d("[AvatarView::setMesh]");

        // Release old mesh resources.
        if (null != mAvatarMesh)
        {
            mAvatarMesh.release();
        }

        mAvatarMesh = mesh;
        if (null != mAvatarMesh)
        {
            setRotate(mAngle.mX, mAngle.mY, mAngle.mZ);
        }

        MYQGLRenderer renderer = getRenderer();
        if (null != renderer)
        {
            renderer.removeAllMesh();
            renderer.addMesh(mAvatarMesh);
            requestRender();
        }
    }

    public AvatarMesh getMesh()
    {
        return mAvatarMesh;
    }

    @Override
    public synchronized void setRenderer(Renderer renderer)
    {
        super.setRenderer(renderer);
        setMesh(mAvatarMesh);
    }

    public void translate(float x, float y, float z)
    {
        if (null != mAvatarMesh && translateEnabled)
        {
            mAvatarMesh.translate(x, y, z);
            requestRender();
        }
    }

    public void rotate(float x, float y, float z)
    {
        mAngle.add(x, y, z);

        if (null != mAvatarMesh && rotateEnabled)
        {
            mAvatarMesh.rotate(0, x, 0);
            requestRender();
        }
    }

    public void setRotate(float x, float y, float z)
    {
        mAngle.set(x, y, z);

        if (null != mAvatarMesh && rotateEnabled)
        {
            mAvatarMesh.setRotate(0, x, 0);
            requestRender();
        }
    }

    public void scale(float scale)
    {
        if (null != mAvatarMesh && scaleEnabled)
        {
            mAvatarMesh.scale(scale);
            requestRender();
        }
    }

    public void click()
    {
        callOnClick();
    }

    private void init(Context context, AttributeSet attrs)
    {
        attachToActivityLifecycle(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (null != event && null != mTouchHandler)
        {
            boolean bHandled = mTouchHandler.onTouchEvent(event);

            if (!bHandled)
            {
                return super.onTouchEvent(event);
            }

            return true;
        }
        else
        {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public void meshLoading()
    {

    }

    @Override
    public boolean performClick()
    {
        return super.performClick();
    }

    @Override
    public void meshLoadComplete(AvatarMesh mesh)
    {
        setMesh(mesh);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if (null != application && null != lifecycleCallback)
        {
            // Unregister from the activity lifecycle so the view can be garbage collected when the fragment is destroyed
            application.unregisterActivityLifecycleCallbacks(lifecycleCallback);
        }
    }

    private void attachToActivityLifecycle(Context context)
    {
        Activity activity = ActivityHelper.getActivity(context);

        if (null == activity)
        {
            Timber.w("Cannot get the activity from inside the AvatarView." +
                    "We need to attach to the activity lifecycle to let the GLTextureView know " +
                    "when we should start and stop rendering. Without this, we will get strange results.");
            return;
        }

        application = activity.getApplication();

        lifecycleCallback = new Application.ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(Activity activity)
            {

            }

            @Override
            public void onActivityResumed(Activity activity)
            {
                // Let the GLTextureView renderer thread know that we should start rendering again
                // and immediately reload the TextureView.
                AvatarView.super.onResume();
            }

            @Override
            public void onActivityPaused(Activity activity)
            {
                // Let the GLTextureView renderer thread know that we should pause rendering.
                AvatarView.super.onPause();
            }

            @Override
            public void onActivityStopped(Activity activity)
            {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {

            }

            @Override
            public void onActivityDestroyed(Activity activity)
            {
                // Stop listening to prevent a memory leak.
                application.unregisterActivityLifecycleCallbacks(this);
            }
        };

        application.registerActivityLifecycleCallbacks(lifecycleCallback);
    }

    public void setTranslateEnabled(boolean translateEnabled)
    {
        this.translateEnabled = translateEnabled;
    }

    public void setScaleEnabled(boolean scaleEnabled)
    {
        this.scaleEnabled = scaleEnabled;
    }

    public void setRotateEnabled(boolean rotateEnabled)
    {
        this.rotateEnabled = rotateEnabled;
    }

    /**
     * Gets the X distance that the cursor is from the original starting point when the user touched the view.
     * The distance is relative to the initial starting point and is not the total distance.
     */
    public float getXDistanceTravelled()
    {
        return mTouchHandler.getXDistanceTravelled();
    }

    /**
     * Gets the Y distance that the cursor is from the original starting point when the user touched the view.
     * The distance is relative to the initial starting point and is not the total distance.
     */
    public float getYDistanceTravelled()
    {
        return mTouchHandler.getYDistanceTravelled();
    }

    public OnTouchHandler getTouchHandler()
    {
        return mTouchHandler;
    }

    public void setTouchHandler(OnTouchHandler mTouchHandler)
    {
        this.mTouchHandler = mTouchHandler;
    }

    public void setTouchHandlerEnabled(boolean bEnabled)
    {
        if (bEnabled && null == mTouchHandler)
        {
            mTouchHandler = new OnTouchHandler()
            {
                @Override
                public void onTranslate(float x, float y)
                {
                    translate(x, y, 0);

                    if (null != mLinkedView)
                        mLinkedView.translate(x, y, 0);
                }

                @Override
                public void onRotate(float x, float y)
                {
                    rotate(x, y, 0);

                    if (null != mLinkedView)
                        mLinkedView.rotate(x, y, 0);
                }

                @Override
                public void onScale(float scale)
                {
                    scale(scale);

                    if (null != mLinkedView)
                        mLinkedView.scale(scale);
                }

                @Override
                public void onClick()
                {
                    click();
                }

                @Override
                public void setDisableScrollView(boolean bDisabled)
                {

                }
            };
        }
        else if (!bEnabled)
        {
            mTouchHandler = null;
        }
    }

    public boolean isMeshReady()
    {
        return mAvatarMesh != null;
    }

    public AvatarMesh getAvatarMesh()
    {
        return mAvatarMesh;
    }
}

