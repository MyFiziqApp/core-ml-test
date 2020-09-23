package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.myfiziq.sdk.gles.Vector3D;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class AvatarLayout extends LinearLayout
{
    //private AvatarRotationService rotationService = new AvatarRotationService(this);
    private OnTouchHandler mTouchHandler;
    private boolean translateEnabled = false;
    private boolean scaleEnabled = false;
    private boolean rotateEnabled = true;
    private ArrayList<AvatarView> mAvatars = new ArrayList<>();
    private ScrollViewLock mScrollView = null;
    private Vector3D mAngle = null;
    private AvatarRotationService rotationService = new AvatarRotationService(this);
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    public AvatarLayout(Context context)
    {
        super(context);
        init();
    }

    public AvatarLayout(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public AvatarLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AvatarRotationService getRotationService()
    {
        return rotationService;
    }

    public Vector3D getAngle()
    {
        return mAngle;
    }

    public void translate(float x, float y, float z)
    {
        if (translateEnabled)
        {
            for (AvatarView avatarView : mAvatars)
            {
                avatarView.translate(x, y, 0);
            }
        }
    }

    public void rotate(float x, float y, float z)
    {
        if (rotateEnabled)
        {
            if (null != mAngle)
            {
                mAngle.add(x, y, z);
            }

            for (AvatarView avatarView : mAvatars)
            {
                if (null == mAngle && avatarView.isMeshReady())
                {
                    // Copy rotation from 3d model.
                    mAngle = new Vector3D(avatarView.getAvatarMesh().getAngle());
                }
                if (null != mAngle)
                {
                    avatarView.setRotate(mAngle.mX, mAngle.mY, mAngle.mZ);
                }
            }
        }
    }

    public void scale(float scale)
    {
        if (scaleEnabled)
        {
            for (AvatarView avatarView : mAvatars)
            {
                avatarView.scale(scale);
            }
        }
    }

//    public AvatarRotationService getRotationService()
//    {
//        return rotationService;
//    }

    private void findChildAvatars()
    {
        mAvatars.clear();
        traverseChildren(this);
        unbindViewTreeObserver();
    }

    private void traverseChildren(ViewGroup vg)
    {
        for (int i = 0; i < vg.getChildCount(); i++)
        {
            View newView = vg.getChildAt(i);
            if (newView instanceof ViewGroup)
            {
                traverseChildren((ViewGroup)newView);
            }
            else if (newView instanceof AvatarView)
            {
                mAvatars.add((AvatarView)newView);
            }
        }
    }

    private void init()
    {
        bindViewTreeObserver();

        mTouchHandler = new OnTouchHandler()
        {
            @Override
            public void onTranslate(float x, float y)
            {
                translate(x, y, 0);
            }

            @Override
            public void onRotate(float x, float y)
            {
                rotate(x, y, 0);
            }

            @Override
            public void onScale(float scale)
            {
                scale(scale);
            }

            @Override
            public void onClick()
            {
                click();
            }

            @Override
            public void setDisableScrollView(boolean bDisabled)
            {
                if (null != mScrollView)
                {
                    mScrollView.setDisableScrollView(bDisabled);
                }
            }
        };
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        if (null != event && null != mTouchHandler)
        {
            boolean bHandled = mTouchHandler.onTouchEvent(event);

            if (!bHandled)
            {
                return super.onInterceptTouchEvent(event);
            }

            return false;
        }
        else
        {
            return super.onInterceptTouchEvent(event);
        }
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

            return bHandled;
        }
        else
        {
            return super.onTouchEvent(event);
        }
    }

    public void click()
    {
//        for (AvatarView avatarView : mAvatars)
//        {
//            avatarView.click();
//        }
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

    public void setScrollingParent(ScrollViewLock scrollView)
    {
        mScrollView = scrollView;
    }

    public void destroy()
    {
        unbindViewTreeObserver();
    }

    private void bindViewTreeObserver()
    {
        onGlobalLayoutListener = this::findChildAvatars;
        getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    private void unbindViewTreeObserver()
    {
        if (onGlobalLayoutListener != null)
        {
            getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            onGlobalLayoutListener = null;
        }
    }
}
