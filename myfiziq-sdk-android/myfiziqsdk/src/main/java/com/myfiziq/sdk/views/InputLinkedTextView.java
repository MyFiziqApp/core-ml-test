package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class InputLinkedTextView extends TextView
{
    public InputLinkedTextView(Context context)
    {
        super(context);
    }

    public InputLinkedTextView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public InputLinkedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public InputLinkedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
