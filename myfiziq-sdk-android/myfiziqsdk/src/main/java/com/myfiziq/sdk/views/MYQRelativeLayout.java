package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.myfiziq.sdk.lifecycle.Parameter;

public class MYQRelativeLayout extends RelativeLayout implements MYQViewInterface
{
    public MYQRelativeLayout(Context context)
    {
        super(context);
    }

    public MYQRelativeLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void applyParameter(Parameter parameter)
    {

    }
}
