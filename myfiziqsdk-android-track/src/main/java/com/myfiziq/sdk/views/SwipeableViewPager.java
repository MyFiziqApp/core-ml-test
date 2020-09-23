package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * A ViewPager that you can turn on and off swiping for.
 */
public class SwipeableViewPager extends ViewPager
{
    private boolean swipeable;

    public SwipeableViewPager(Context context)
    {
        super(context);
    }

    public SwipeableViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.swipeable = true;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event)
    {
        if (this.swipeable)
        {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        if (this.swipeable)
        {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public void setSwipeable(boolean swipeable)
    {
        this.swipeable = swipeable;
    }
}
