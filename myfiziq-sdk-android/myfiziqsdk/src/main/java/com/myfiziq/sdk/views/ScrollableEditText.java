package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;

public class ScrollableEditText extends AppCompatEditText
{
    public ScrollableEditText(Context context)
    {
        super(context);
        init();
    }

    public ScrollableEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public ScrollableEditText(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init()
    {
        //Override Touch Event when it occurred on Edit Text
        setOnTouchListener((v, event) ->
        {
            //When the edit text has focus, override the touch event from a scroll view (If Exist)
            if (hasFocus())
            {
                return disableParentScrollOnChild(v, event);
            }
            return false;
        });
    }

    /**
     * This method is for disabling the parent scroll on a child view.
     * @param childView : Child View (edit text)
     * @param event : Motion event from the child view (edit text)
     */
    private boolean disableParentScrollOnChild(View childView, MotionEvent event){
        childView.getParent().requestDisallowInterceptTouchEvent(true);
        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_SCROLL:
                childView.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }
}
