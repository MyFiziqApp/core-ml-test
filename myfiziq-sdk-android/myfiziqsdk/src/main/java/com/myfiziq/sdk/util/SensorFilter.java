package com.myfiziq.sdk.util;

/**
 * @hide
 */

public class SensorFilter
{
    static final double ALPHA = 0.2;

    double[] mData;
    double mAverage;
    double mFilteredData;
    int mIndex = 0;

    public SensorFilter(int size)
    {
        mData = new double[size];
    }

    public double average_value()
    {
        return mAverage;
    }

    public double filtered_value()
    {
        return mFilteredData;
    }

    public void newValue(double newVal)
    {
        int len = mData.length;
        mIndex = (mIndex + 1) % len;
        mData[mIndex] = newVal;
        mFilteredData = lowPass(newVal);

        mAverage = 0;

        for (int ix = 0; ix < len; ix++)
            mAverage += mData[ix];
        mAverage = mAverage / len;
    }

    public static float[] lowPass(float[] input, float[] output)
    {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++)
        {
            output[i] = output[i] + (float)ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public double lowPass(double input)
    {
        return input * ALPHA + mFilteredData * (1.0 - ALPHA);
    }

    public static float[] highPass(float[] input, float[] output)
    {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i] - ((float)ALPHA * output[i] + (1 - (float)ALPHA) * input[i]);
        }
        return output;
    }

    protected double highPass(double input)
    {
        return input - (ALPHA * mFilteredData + (1 - ALPHA) * input);
    }

    public boolean isInRange(double value, double range)
    {
        double abs = Math.abs(range);
        return (mAverage >= value - abs && mAverage <= value + abs);
    }
}
