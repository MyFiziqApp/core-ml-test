package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * This ScrollView allows us to disable scrolling when required.
 */
public class ScrollViewDisableable extends ScrollView implements ScrollViewLock
{
    private boolean disableScrollView = false;

    public ScrollViewDisableable(Context context)
    {
        super(context);
    }

    public ScrollViewDisableable(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ScrollViewDisableable(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        boolean result = false;

        if (!disableScrollView)
        {
            result = super.onInterceptTouchEvent(ev);
        }

        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        boolean result = false;

        if (!disableScrollView)
        {
            result = super.onTouchEvent(ev);
        }

        return result;
    }

    public void setDisableScrollView(boolean disableScrollView)
    {
        this.disableScrollView = disableScrollView;
    }
}
