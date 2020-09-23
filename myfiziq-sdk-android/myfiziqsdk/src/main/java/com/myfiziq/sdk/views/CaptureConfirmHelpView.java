package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.myfiziq.sdk.R;

import androidx.constraintlayout.widget.ConstraintLayout;

public class CaptureConfirmHelpView extends ConstraintLayout
{
    public CaptureConfirmHelpView(Context context)
    {
        super(context);
        init(context);
    }

    public CaptureConfirmHelpView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public CaptureConfirmHelpView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View container = LayoutInflater.from(context).inflate(getLayout(), this, true);

        Button continueButton = container.findViewById(R.id.continueButton);
        continueButton.setOnClickListener((v) -> container.setVisibility(GONE));
    }

    public int getLayout()
    {
        return R.layout.view_capture_confirm_help;
    }
}
