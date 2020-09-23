package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.myfiziq.sdk.lifecycle.Parameter;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class MYQTextView extends TextView implements MYQViewInterface
{
    public MYQTextView(Context context)
    {
        super(context);
    }

    public MYQTextView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void applyParameter(Parameter parameter)
    {
        //if (com.android.internal.R.styleable.TextView_text == parameter.getParamId())
        {

        }
    }
}
