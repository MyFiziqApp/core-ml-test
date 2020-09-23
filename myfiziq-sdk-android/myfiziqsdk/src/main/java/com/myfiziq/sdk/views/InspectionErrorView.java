package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.helpers.AsyncHelper;

import androidx.constraintlayout.widget.ConstraintLayout;

public class InspectionErrorView extends ConstraintLayout implements CircleCountdown.CircleCountdownCallback
{
    private View container;
    private TextView errorText;
    private CircleCountdown viewCountdown;
    private AsyncHelper.CallbackVoid mCallback = null;
    public CaptureErrorButtonView errorButtons;

    public InspectionErrorView(Context context)
    {
        super(context);
        init(context);
    }

    public InspectionErrorView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public InspectionErrorView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        container = LayoutInflater.from(context).inflate(getLayout(), this, true);

        errorText = container.findViewById(R.id.errorText);
        viewCountdown = container.findViewById(R.id.viewCountdown);
        viewCountdown.setCallback(this);

        errorButtons = container.findViewById(R.id.inspectionErrorButtons);
    }

    public void setButtonsVisible(boolean visible)
    {
        int visibility;
        if (visible) visibility = View.VISIBLE;
        else visibility = View.GONE;
        errorButtons.setVisibility(visibility);
    }

    public void setViewCountdownVisible(boolean visible)
    {
        int visibility;
        if (visible) visibility = View.VISIBLE;
        else visibility = View.GONE;
        viewCountdown.setVisibility(visibility);
    }

    public int getLayout()
    {
        return R.layout.view_inspection_error;
    }

    public void setErrorText(String error)
    {
        errorText.setText(error);
    }

    @Override
    public void countDownExpired()
    {
        if (null != mCallback)
        {
            mCallback.execute();
        }
    }

    public void start(AsyncHelper.CallbackVoid callback)
    {
        mCallback = callback;
        viewCountdown.start();
    }

    public void cancelCallback()
    {
        mCallback = null;
    }
}
