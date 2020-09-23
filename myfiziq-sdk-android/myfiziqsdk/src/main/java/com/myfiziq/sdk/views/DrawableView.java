package com.myfiziq.sdk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawableView extends View
{
    public interface Draw
    {
        void doDraw(Canvas canvas);
    }

    Draw mDraw = null;

    public DrawableView(Context context)
    {
        super(context);
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void setDraw(Draw draw)
    {
        mDraw = draw;
        setWillNotDraw(null == mDraw);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if (null != mDraw)
        {
            mDraw.doDraw(canvas);
        }
    }
}
