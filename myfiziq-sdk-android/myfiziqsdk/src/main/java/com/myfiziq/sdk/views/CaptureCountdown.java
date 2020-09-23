package com.myfiziq.sdk.views;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.util.SecondsTimer;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @hide
 */
public class CaptureCountdown extends MYQFrameLayout
{
    private final static long TIMER_SECONDS_LENGTH = 9999;

    private CaptureCountdownCallback mCallback;
    private TextView mTextView;


    public interface CaptureCountdownCallback
    {
        void countDownHalfway();

        void countDownExpired();

        void countDown3Sec();
        void countDown2Sec();
        void countDown1Sec();
        void countDown0Sec();
    }


    SecondsTimer mSecondsTimer = new SecondsTimer(TIMER_SECONDS_LENGTH, 100)
    {
        @Override
        public void onSecondsTick(long millisUntilFinished)
        {
            int seconds = getSeconds();
            String textCountDown = String.valueOf(seconds + 1);

            setCountdownText(textCountDown, true);

            if (5 == seconds)
            {
                setTextSizeInSp(120);
                setMargin(
                        getResources().getInteger(R.integer.myfiziqsdk_capture_timer_top_margin),
                        getResources().getInteger(R.integer.myfiziqsdk_capture_timer_right_margin),
                        getResources().getInteger(R.integer.myfiziqsdk_capture_timer_bottom_margin),
                        getResources().getInteger(R.integer.myfiziqsdk_capture_timer_left_margin)
                );
                setGravity(Gravity.LEFT|Gravity.TOP, true);

                int textPaintCurrentColor = mTextView.getCurrentTextColor();

                ObjectAnimator colorAnim = ObjectAnimator.ofInt(mTextView, "textColor",
                        textPaintCurrentColor, Color.WHITE);
                colorAnim.setDuration(500);
                colorAnim.setEvaluator(new ArgbEvaluator());
                colorAnim.addUpdateListener(animation -> invalidate());
                colorAnim.start();


                if (null != mCallback)
                {
                    mCallback.countDownHalfway();
                }
            }
            else if (3 == seconds)
            {
                if (null != mCallback)
                {
                    mCallback.countDown3Sec();
                }
            }
            else if (2 == seconds)
            {
                if (null != mCallback)
                {
                    mCallback.countDown2Sec();
                }
            }
            else if (1 == seconds)
            {
                if (null != mCallback)
                {
                    mCallback.countDown1Sec();
                }
            }
            else if (0 == seconds)
            {
                if (null != mCallback)
                {
                    mCallback.countDown0Sec();
                }
            }

            invalidate();
        }

        @Override
        public void onMillisecondsTick(long millisUntilFinished)
        {

        }

        @Override
        public void onFinish()
        {
            setCountdownText("", false);
            invalidate();

            if (null != mCallback)
            {
                mCallback.countDownExpired();
            }
        }
    };

    public CaptureCountdown(@NonNull Context context)
    {
        super(context);
        init(context, null);
    }

    public CaptureCountdown(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public CaptureCountdown(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs)
    {
        LayoutInflater.from(context).inflate(getLayout(), this, true);
        mTextView = findViewById(R.id.textViewCountdown);

        setWillNotDraw(false);
    }

    public int getLayout()
    {
        return R.layout.view_capture_countdown;
    }

    public void setCallback(CaptureCountdownCallback callback)
    {
        mCallback = callback;
    }

    @Override
    public void setTextColor(@ColorInt int color)
    {
        mTextView.setTextColor(color);
    }

    public void initCounter()
    {
        setTextSizeInSp(250);
        setMargin(0, 0, 0, 0);
        setGravity(Gravity.CENTER, false);
    }

    public void start()
    {
        initCounter();
        mSecondsTimer.start();
    }

    public void cancel()
    {
        mSecondsTimer.reset();
        setCountdownText("", false);
    }

    private void setGravity(int gravity, boolean animate)
    {
        if (animate)
        {
            TransitionManager.beginDelayedTransition((ViewGroup) mTextView.getParent());
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTextView.getLayoutParams();
        layoutParams.gravity = gravity;

        mTextView.setLayoutParams(layoutParams);
    }

    private void setCountdownText(String text, boolean animate)
    {
        mTextView.setText(text);
    }

    private void setTextSizeInSp(float size)
    {
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private void setMargin(int topMargin, int rightMargin, int bottomMargin, int leftMargin)
    {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
        layoutParams.topMargin = topMargin;
        layoutParams.rightMargin = rightMargin;
        layoutParams.bottomMargin = bottomMargin;
        layoutParams.leftMargin = leftMargin;

        setLayoutParams(layoutParams);
    }
}
