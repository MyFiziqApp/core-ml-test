package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.myfiziq.sdk.lifecycle.Parameter;

import androidx.annotation.Nullable;

public class MYQLinearLayout extends LinearLayout implements MYQViewInterface
{
    public MYQLinearLayout(Context context)
    {
        super(context);
    }

    public MYQLinearLayout(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void applyParameter(Parameter parameter)
    {

    }
}
