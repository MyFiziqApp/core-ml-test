package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.myfiziq.sdk.R;

import androidx.constraintlayout.widget.ConstraintLayout;

public class CaptureErrorView extends ConstraintLayout
{
    private View container;
    private TextView errorText;
    public CaptureErrorButtonView errorButtons;


    public CaptureErrorView(Context context)
    {
        super(context);
        init(context);
    }

    public CaptureErrorView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public CaptureErrorView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        container = LayoutInflater.from(context).inflate(getLayout(), this, true);

        errorText = container.findViewById(R.id.errorText);
        errorButtons = container.findViewById(R.id.capErrorButtons);
    }

    public int getLayout()
    {
        return R.layout.view_capture_error;
    }

    public void setErrorText(String error)
    {
        errorText.setText(error);
    }
}
