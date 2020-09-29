package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.myfiziq.sdk.R;

import androidx.constraintlayout.widget.ConstraintLayout;

public class CaptureErrorUprightView extends ConstraintLayout
{
    private View container;


    public CaptureErrorUprightView(Context context)
    {
        super(context);
        init(context);
    }

    public CaptureErrorUprightView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public CaptureErrorUprightView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        container = LayoutInflater.from(context).inflate(getLayout(), this, true);
    }

    public int getLayout()
    {
        return R.layout.view_capture_error_upright;
    }
}
