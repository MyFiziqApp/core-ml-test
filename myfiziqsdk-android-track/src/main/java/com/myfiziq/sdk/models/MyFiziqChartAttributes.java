package com.myfiziq.sdk.models;

import android.content.res.TypedArray;

import com.myfiziq.myfiziqsdk_android_track.R;

public class MyFiziqChartAttributes
{
    private int primaryLineColour;
    private int primaryLineCircleColour;
    private float primaryLineWidth;
    private int primaryLineDashLength;
    private int primaryLineLength;
    private int primaryLineCircleRadius;

    private int secondaryLineColour;
    private int secondaryLineCircleColour;
    private float secondaryLineWidth;
    private int secondaryLineDashLength;
    private int secondaryLineLength;
    private int secondaryLineCircleRadius;

    private int xAxisTextSize;
    private int xAxisTextColor;
    private int xAxisTextFontFamily;                    // Resource ID
    private int xAxisLineColor;
    private float xAxisLineWidth;

    private boolean leftAxisDrawGridLines;
    private int leftAxisGridColor;

    private int lineDataValueTextColor;
    private int lineDataValueTextSize;
    private int lineDataValueTextFontFamily;            // Resource ID
    private boolean lineDataValueTypefaceIsBold;

    private String lineDataValueFormat;

    private int maxVisibleValues;

    private int desiredMaxXAxisLabels;


    public int getPrimaryLineColour()
    {
        return primaryLineColour;
    }

    public int getPrimaryLineCircleColour()
    {
        return primaryLineCircleColour;
    }

    public float getPrimaryLineWidth()
    {
        return primaryLineWidth;
    }

    public float getPrimaryLineDashLength()
    {
        return primaryLineDashLength;
    }

    public float getPrimaryLineLength()
    {
        return primaryLineLength;
    }

    public int getPrimaryLineCircleRadius()
    {
        return primaryLineCircleRadius;
    }

    public int getSecondaryLineColour()
    {
        return secondaryLineColour;
    }

    public int getSecondaryLineCircleColour()
    {
        return secondaryLineCircleColour;
    }

    public float getSecondaryLineWidth()
    {
        return secondaryLineWidth;
    }

    public float getSecondaryLineDashLength()
    {
        return secondaryLineDashLength;
    }

    public float getSecondaryLineLength()
    {
        return secondaryLineLength;
    }

    public int getSecondaryLineCircleRadius()
    {
        return secondaryLineCircleRadius;
    }

    public int getXAxisTextSize()
    {
        return xAxisTextSize;
    }

    public int getXAxisTextColor()
    {
        return xAxisTextColor;
    }

    public int getXAxisTextFontFamily()
    {
        // Resource ID
        return xAxisTextFontFamily;
    }

    public int getxAxisLineColor()
    {
        return xAxisLineColor;
    }

    public float getxAxisLineWidth()
    {
        return xAxisLineWidth;
    }

    public boolean isLeftAxisDrawGridLines()
    {
        return leftAxisDrawGridLines;
    }

    public int getLeftAxisGridColor()
    {
        return leftAxisGridColor;
    }

    public int getLineDataValueTextColor()
    {
        return lineDataValueTextColor;
    }

    public int getLineDataValueTextSize()
    {
        return lineDataValueTextSize;
    }

    public int getLineDataValueTextFontFamily()
    {
        // Resource ID
        return lineDataValueTextFontFamily;
    }

    public boolean isLineDataValueTypefaceIsBold()
    {
        return lineDataValueTypefaceIsBold;
    }

    public String getLineDataValueFormat()
    {
        return lineDataValueFormat;
    }

    public int getMaxVisibleValues()
    {
        return maxVisibleValues;
    }

    public int getDesiredMaxXAxisLabels()
    {
        return desiredMaxXAxisLabels;
    }

    public MyFiziqChartAttributes fromTypedArray(TypedArray typedArray)
    {
        primaryLineColour = typedArray.getColor(R.styleable.MyFiziqChart_primaryLineColour, 0);
        primaryLineCircleColour = typedArray.getColor(R.styleable.MyFiziqChart_primaryLineCircleColour,0 );
        primaryLineWidth = typedArray.getFloat(R.styleable.MyFiziqChart_primaryLineWidth, 0);
        primaryLineDashLength = typedArray.getInteger(R.styleable.MyFiziqChart_primaryLineDashLength, 0);
        primaryLineLength = typedArray.getInteger(R.styleable.MyFiziqChart_primaryLineLength, 0);
        primaryLineCircleRadius = typedArray.getInteger(R.styleable.MyFiziqChart_primaryCircleRadius, 0);

        secondaryLineColour = typedArray.getColor(R.styleable.MyFiziqChart_secondaryLineColour, 0);
        secondaryLineCircleColour = typedArray.getColor(R.styleable.MyFiziqChart_secondaryLineCircleColour,0 );
        secondaryLineWidth = typedArray.getFloat(R.styleable.MyFiziqChart_secondaryLineWidth, 0);
        secondaryLineDashLength = typedArray.getInteger(R.styleable.MyFiziqChart_secondaryLineDashLength, 0);
        secondaryLineLength = typedArray.getInteger(R.styleable.MyFiziqChart_secondaryLineLength, 0);
        secondaryLineCircleRadius = typedArray.getInteger(R.styleable.MyFiziqChart_secondaryCircleRadius, 0);

        xAxisTextSize = typedArray.getInteger(R.styleable.MyFiziqChart_xAxisTextSize, 0);
        xAxisTextColor = typedArray.getColor(R.styleable.MyFiziqChart_xAxisTextColor, 0);
        xAxisTextFontFamily = typedArray.getResourceId(R.styleable.MyFiziqChart_xAxisTextFontFamily, 0);
        xAxisLineColor = typedArray.getColor(R.styleable.MyFiziqChart_xAxisLineColor, 0);
        xAxisLineWidth = typedArray.getFloat(R.styleable.MyFiziqChart_xAxisLineWidth, 0);

        leftAxisDrawGridLines = typedArray.getBoolean(R.styleable.MyFiziqChart_leftAxixDrawGridLines, false);
        leftAxisGridColor = typedArray.getColor(R.styleable.MyFiziqChart_leftAxisGridColor, 0);

        lineDataValueTextColor = typedArray.getColor(R.styleable.MyFiziqChart_lineDataValueTextColor, 0);
        lineDataValueTextSize = typedArray.getInteger(R.styleable.MyFiziqChart_lineDataValueTextSize, 0);
        lineDataValueTextFontFamily = typedArray.getResourceId(R.styleable.MyFiziqChart_lineDataValueTextFontFamily, 0);
        lineDataValueTypefaceIsBold = typedArray.getBoolean(R.styleable.MyFiziqChart_lineDataValueTypefaceIsBold, false);

        lineDataValueFormat = typedArray.getString(R.styleable.MyFiziqChart_lineDataValueFormat);

        maxVisibleValues = typedArray.getInteger(R.styleable.MyFiziqChart_maxVisibleValues, 0);

        desiredMaxXAxisLabels = typedArray.getInteger(R.styleable.MyFiziqChart_desiredMaxXAxisLabels, 0);


        return this;
    }
}
