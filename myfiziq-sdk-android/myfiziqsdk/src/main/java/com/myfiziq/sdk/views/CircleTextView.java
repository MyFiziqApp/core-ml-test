package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.myfiziq.sdk.R;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class CircleTextView extends MYQTextView
{
    private float mStrokeWidth = 2.0f;
    public int mColor = Color.BLACK;

    public Paint circlePaint = new Paint();
    public Paint strokePaint = new Paint();

    public CircleTextView(Context context)
    {
        super(context);
        init(context, null);
    }

    public CircleTextView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, @Nullable AttributeSet attrs)
    {
        TypedArray a = null;

        if (null != attrs)
        {
            a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CircleTextView, 0, 0);

            try
            {
                int color = a.getResourceId(R.styleable.CircleTextView_circleColor, -1);
                if (color != -1)
                {
                    mColor = getResources().getColor(color);
                }

                mStrokeWidth = a.getDimensionPixelSize(R.styleable.CircleTextView_circleWidth, 0);

            }
            catch (Exception e)
            {
            }
            finally
            {
                a.recycle();
            }
        }

//        circlePaint.setColor(mColor);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        strokePaint.setColor(mColor);
        strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(mStrokeWidth);
    }

    @Override
    public void draw(Canvas canvas)
    {
        float h = this.getHeight()-(2*mStrokeWidth);
        float w = this.getWidth()-(2*mStrokeWidth);

        float diameter = ((h < w) ? h : w);
        float radius = diameter / 2;

        float cx = w/2+mStrokeWidth;
        float cy = h/2+mStrokeWidth;
        //this.setHeight(diameter);
        //this.setWidth(diameter);

        if (mStrokeWidth > 0)
        {
            canvas.drawCircle(cx, cy, radius, strokePaint);
            //canvas.drawCircle(diameter / 2, diameter / 2, radius - mStrokeWidth, circlePaint);
        }
        else
        {
            canvas.drawCircle(diameter / 2, diameter / 2, mStrokeWidth, circlePaint);
        }

        super.draw(canvas);
    }

    public void setStrokeWidth(int dp)
    {
        float scale = getContext().getResources().getDisplayMetrics().density;
        mStrokeWidth = dp * scale;
    }
}
