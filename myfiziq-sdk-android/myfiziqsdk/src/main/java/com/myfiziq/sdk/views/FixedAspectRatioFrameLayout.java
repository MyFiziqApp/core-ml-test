package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.util.UiUtils;

public class FixedAspectRatioFrameLayout extends FrameLayout
{
    enum ScaleType
    {
        SCALE_SQUARE,
        SCALE_16x9,
        SCALE_5x2;

        public static ScaleType fromInt(int val)
        {
            if (val >=SCALE_SQUARE.ordinal() && val <= SCALE_5x2.ordinal())
            {
                return ScaleType.values()[val];
            }
            return SCALE_SQUARE;
        }
    }

    boolean mRescaleW = false;
    boolean mRescaleH = false;
    ScaleType mScaleType = ScaleType.SCALE_SQUARE;

    public FixedAspectRatioFrameLayout(Context context)
    {
        super(context);
    }

    public FixedAspectRatioFrameLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public FixedAspectRatioFrameLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public void init(AttributeSet attrs)
    {
        if (null != attrs)
        {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs, R.styleable.FixedAspectRatioFrameLayout, 0, 0);

            try
            {
                if (a.hasValue(R.styleable.FixedAspectRatioFrameLayout_rescaleW))
                    mRescaleW = a.getBoolean(R.styleable.FixedAspectRatioFrameLayout_rescaleW, false);

                if (a.hasValue(R.styleable.FixedAspectRatioFrameLayout_rescaleH))
                    mRescaleH = a.getBoolean(R.styleable.FixedAspectRatioFrameLayout_rescaleH, false);

                if (a.hasValue(R.styleable.FixedAspectRatioFrameLayout_scaleRatio))
                    mScaleType = ScaleType.fromInt(a.getInt(R.styleable.FixedAspectRatioFrameLayout_scaleRatio, ScaleType.SCALE_SQUARE.ordinal()));
            }
            catch (Exception e)
            {
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (!mRescaleW && !mRescaleH)
        {
            // Don't make any measurement changes.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (mRescaleW)
        {
            widthMode = MeasureSpec.EXACTLY;
            switch (mScaleType)
            {
                case SCALE_SQUARE:
                    width = height;
                    break;

                case SCALE_16x9:
                    width = UiUtils.get16x9Width(height);
                    break;

                case SCALE_5x2:
                    width = UiUtils.get5x2Width(height);
                    break;
            }
        }

        if (mRescaleH)
        {
            heightMode = MeasureSpec.EXACTLY;
            switch (mScaleType)
            {
                case SCALE_SQUARE:
                    height = width;
                    break;

                case SCALE_16x9:
                    height = UiUtils.get16x9Height(width);
                    break;

                case SCALE_5x2:
                    height = UiUtils.get5x2Height(width);
                    break;
            }
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), MeasureSpec.makeMeasureSpec(height, heightMode));
    }
}
