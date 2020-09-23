package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.util.SecondsTimer;
import com.myfiziq.sdk.util.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */

public class CircleCountdown extends FrameLayout
{
    long TIMER_SECONDS_LENGTH = 3000;
    long TIMER_RING_LENGTH = 2000;

    TextView textViewCountdown;
    int mWidth = 0;
    int mHeight = 0;
    public int mColor = 0;
    boolean mRingSync = false;
    float mLineWidth = 0f;
    long mTimeRemaining = TIMER_RING_LENGTH;
    RectF mCircleBounds = new RectF();
    Paint mStartPaint = new Paint();
    Paint mEndPaint = new Paint();
    Path mStartPath = new Path();
    Path mEndPath = new Path();
    CircleCountdownCallback mCallback;

    private int desiredFps = 30;

    public interface CircleCountdownCallback
    {
        void countDownExpired();
    }

    SecondsTimer mSecondsTimer;
    SecondsTimer mRingTimer;

    public CircleCountdown(@NonNull Context context)
    {
        super(context);
        init(context, null);
    }

    public CircleCountdown(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleCountdown(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs)
    {
        //if (isInEditMode())
        //    return;

        LayoutInflater.from(context).inflate(getLayout(), this, true);
        textViewCountdown = findViewById(R.id.textViewCountdown);
        mColor = getResources().getColor(R.color.myfiziqsdk_colorPrimary);
        TypedArray a = null;

        if (null != attrs)
        {
            a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CircleCountdown, 0, 0);

            try
            {
                int color = a.getResourceId(R.styleable.CircleCountdown_countdownColor, -1);
                if (color != -1)
                {
                    mColor = color;
                }

                int time = a.getInt(R.styleable.CircleCountdown_countdownTime, -1);
                if (time != -1)
                {
                    TIMER_SECONDS_LENGTH = time*1000;
                    TIMER_RING_LENGTH = TIMER_SECONDS_LENGTH;
                }

                mRingSync = a.getBoolean(R.styleable.CircleCountdown_countdownRingSync, false);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
            finally
            {
                a.recycle();
            }
        }

        float circleStrokeWidth = getResources().getDimension(R.dimen.myfiziqsdk_circle_stroke_width);
        mLineWidth = UiUtils.convertDpToPixel(context, circleStrokeWidth);
        mStartPaint.setColor(mColor);
        mStartPaint.setStyle(Paint.Style.STROKE);
        mStartPaint.setStrokeWidth(circleStrokeWidth);

        mEndPaint.setColor(getResources().getColor(R.color.myfiziqsdk_white));
        mEndPaint.setStyle(Paint.Style.STROKE);
        mEndPaint.setStrokeWidth(circleStrokeWidth);

        setWillNotDraw(false);

        mSecondsTimer = new SecondsTimer(TIMER_SECONDS_LENGTH, 100)
        {
            @Override
            public void onSecondsTick(long millisUntilFinished)
            {
                textViewCountdown.setText(String.valueOf(getSeconds()+1));
            }

            @Override
            public void onMillisecondsTick(long millisUntilFinished)
            {
                invalidate();
            }

            @Override
            public void onFinish()
            {
                textViewCountdown.setText("");
                invalidate();
                if (null != mCallback)
                {
                    mCallback.countDownExpired();
                }
            }
        };

        mRingTimer = new SecondsTimer(TIMER_RING_LENGTH, 1000 / desiredFps)
        {
            @Override
            public void onSecondsTick(long millisUntilFinished)
            {

            }

            @Override
            public void onMillisecondsTick(long millisUntilFinished)
            {
                mTimeRemaining = millisUntilFinished;
                invalidate();
            }

            @Override
            public void onFinish()
            {
                mTimeRemaining = 0;
                invalidate();
                if (!mRingSync)
                {
                    mSecondsTimer.start();
                }
            }
        };
    }

    public int getLayout()
    {
        return R.layout.view_circle_countdown;
    }

    public void setCallback(CircleCountdownCallback callback)
    {
        mCallback = callback;
    }

    public void start()
    {
        mRingTimer.start();
        if (mRingSync)
        {
            mSecondsTimer.start();
        }
    }

    public void cancel()
    {
        mSecondsTimer.reset();
        mRingTimer.cancel();
        textViewCountdown.setText("");
        mTimeRemaining = TIMER_RING_LENGTH;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mCircleBounds.set(mLineWidth/2, mLineWidth/2, mWidth-mLineWidth/2, mHeight-mLineWidth/2);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        mStartPaint.setColor(mColor);
        float progressAngle = mTimeRemaining/(float) TIMER_RING_LENGTH * 360;
        if (progressAngle >= 360)
        {
            mStartPath.rewind();
            mStartPath.addCircle(mCircleBounds.centerX(), mCircleBounds.centerY(), mCircleBounds.width()/2, Path.Direction.CW);
            canvas.drawPath(mStartPath, mStartPaint);
        }
        else if (progressAngle > 0)
        {
            mStartPath.rewind();
            mStartPath.arcTo(mCircleBounds, 270, progressAngle, true);
            canvas.drawPath(mStartPath, mStartPaint);
            mEndPath.rewind();
            mEndPath.arcTo(mCircleBounds, 270+progressAngle, 360-progressAngle, true);
            canvas.drawPath(mEndPath, mEndPaint);
        }
        else
        {
            mEndPath.rewind();
            mEndPath.addCircle(mCircleBounds.centerX(), mCircleBounds.centerY(), mCircleBounds.width()/2, Path.Direction.CW);
            canvas.drawPath(mEndPath, mEndPaint);
        }
    }
}
