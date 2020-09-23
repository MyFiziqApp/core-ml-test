package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class QuantityTextView extends TextView
{

    public QuantityTextView(Context context)
    {
        super(context);
    }

    public QuantityTextView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public QuantityTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public QuantityTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
