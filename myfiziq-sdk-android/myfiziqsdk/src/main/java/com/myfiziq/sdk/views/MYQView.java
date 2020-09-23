package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.myfiziq.sdk.lifecycle.Parameter;

import androidx.annotation.Nullable;

public class MYQView extends View implements MYQViewInterface
{
    public MYQView(Context context)
    {
        super(context);
    }

    public MYQView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void applyParameter(Parameter parameter)
    {

    }
}
