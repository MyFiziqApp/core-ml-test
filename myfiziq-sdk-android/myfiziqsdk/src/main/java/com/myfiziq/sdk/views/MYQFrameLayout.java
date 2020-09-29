package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @hide
 */

public class MYQFrameLayout extends FrameLayout
{
    public MYQFrameLayout(@NonNull Context context)
    {
        super(context);
    }

    public MYQFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    // Can be overridden to allow custom text values by Parameter
    public void setText(CharSequence text)
    {

    }

    // Can be overridden to allow custom text color values by Parameter
    public void setTextColor(@ColorInt int color)
    {

    }
}
