package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds the upper and lower bounds of the Measurement Fraction. Specify the desired lowerValue and upperValue
 * by calling this class and insert it into MyMeasurementDialogCategoryVO.
 */
public class MyMeasurementFractionLimitValueVO implements Parcelable
{
    private int lowerMinValue;
    private int lowerMaxValue;
    private int upperMaxValue;
    private int upperMinValue;

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.lowerMinValue);
        dest.writeInt(this.lowerMaxValue);
        dest.writeInt(this.upperMaxValue);
        dest.writeInt(this.upperMinValue);
    }

    public MyMeasurementFractionLimitValueVO(int lowerMinValue, int lowerMaxValue,
                                             int upperMinValue, int upperMaxValue)
    {
        this.lowerMinValue = lowerMinValue;
        this.lowerMaxValue = lowerMaxValue;
        this.upperMinValue = upperMinValue;
        this.upperMaxValue = upperMaxValue;
    }

    protected MyMeasurementFractionLimitValueVO(Parcel in)
    {
        this.lowerMinValue = in.readInt();
        this.lowerMaxValue = in.readInt();
        this.upperMaxValue = in.readInt();
        this.upperMinValue = in.readInt();
    }

    public static final Creator<MyMeasurementFractionLimitValueVO> CREATOR = new
            Creator<MyMeasurementFractionLimitValueVO>()
    {
        @Override
        public MyMeasurementFractionLimitValueVO createFromParcel(Parcel source)
        {
            return new MyMeasurementFractionLimitValueVO(source);
        }

        @Override
        public MyMeasurementFractionLimitValueVO[] newArray(int size)
        {
            return new MyMeasurementFractionLimitValueVO[size];
        }
    };

    public int getLowerMinValue()
    {
        return lowerMinValue;
    }

    public void setLowerMinValue(int lowerMinValue)
    {
        this.lowerMinValue = lowerMinValue;
    }

    public int getLowerMaxValue() {
        return lowerMaxValue;
    }

    public void setLowerMaxValue(int lowerMaxValue)
    {
        this.lowerMaxValue = lowerMaxValue;
    }

    public int getUpperMaxValue()
    {
        return upperMaxValue;
    }

    public void setUpperMaxValue(int upperMaxValue)
    {
        this.upperMaxValue = upperMaxValue;
    }

    public int getUpperMinValue() {
        return upperMinValue;
    }

    public void setUpperMinValue(int upperMinValue)
    {
        this.upperMinValue = upperMinValue;
    }


}
