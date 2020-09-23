package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

@SuppressLint("AppCompatCustomView")
public class TintableButton extends Button
{

    public TintableButton(Context context)
    {
        super(context);
    }

    public TintableButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TintableButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public TintableButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
