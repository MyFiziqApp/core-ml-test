package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import androidx.annotation.Nullable;

/**
 * This is a model class to hold measurement category information
 *
 * You can insert fraction values to make fraction spinner (2 spinner in dialog)
 * or just leave the fraction values in null to create single spinner (1 spinner in dialog)
 *
 */
public class MyMeasurementDialogCategoryVO implements Parcelable
{
    private String key;
    private String label;
    private ArrayList<MyMeasurementDialogCategoryValueVO> values;
    private ArrayList<MyMeasurementDialogCategoryValueVO> fractionValues;
    private MyMeasurementFractionLimitValueVO fractionLimit;


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.key);
        dest.writeString(this.label);
        dest.writeTypedList(this.values);
        dest.writeTypedList(this.fractionValues);
        dest.writeParcelable(this.fractionLimit, flags);
    }

    /**
     * No-argument constructor initializes instance variables
     * to null
     * @see #setKey(String)
     * @see #setLabel(String)
     * @see #setValues(ArrayList)
     * @see #setFractionValues(ArrayList)
     * @see #setFractionLimit(MyMeasurementFractionLimitValueVO)
     */
    public MyMeasurementDialogCategoryVO()
    {
    }

    protected MyMeasurementDialogCategoryVO(Parcel in)
    {
        this.key = in.readString();
        this.label = in.readString();
        this.values = in.createTypedArrayList(MyMeasurementDialogCategoryValueVO.CREATOR);
        this.fractionValues = in.createTypedArrayList(MyMeasurementDialogCategoryValueVO.CREATOR);
        this.fractionLimit = in.readParcelable(MyMeasurementFractionLimitValueVO.class.getClassLoader());
    }

    public static final Creator<MyMeasurementDialogCategoryVO> CREATOR = new Creator<MyMeasurementDialogCategoryVO>()
    {
        @Override
        public MyMeasurementDialogCategoryVO createFromParcel(Parcel source)
        {
            return new MyMeasurementDialogCategoryVO(source);
        }

        @Override
        public MyMeasurementDialogCategoryVO[] newArray(int size)
        {
            return new MyMeasurementDialogCategoryVO[size];
        }
    };


    @Nullable
    public MyMeasurementDialogCategoryValueVO getValueVOFromInt(int intValue)
    {
        for (MyMeasurementDialogCategoryValueVO valueVO : getValues())
        {
            if (valueVO.getValue() == intValue)
            {
                return valueVO;
            }
        }

        return null;
    }

    @Nullable
    public MyMeasurementDialogCategoryValueVO getFractionValueVOFromInt(int intValue)
    {
        for (MyMeasurementDialogCategoryValueVO valueVO : getFractionValues())
        {
            if (valueVO.getValue() == intValue)
            {
                return valueVO;
            }
        }

        return null;
    }

    public boolean hasFractionLimit(){
        return this.fractionLimit != null;
    }


    public String getKey()
    {
        return key;
    }

    /**
     * Sets the key category
     * @param key: the category suffix
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    public String getLabel()
    {
        return label;
    }

    /**
     * Sets the label
     * @param label: the category label, implemented on spinner
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    public ArrayList<MyMeasurementDialogCategoryValueVO> getValues()
    {
        return values;
    }

    /**
     * Sets the values
     * @param values: this array defines the primary spinner range
     */
    public void setValues(ArrayList<MyMeasurementDialogCategoryValueVO> values)
    {
        this.values = values;
    }

    public ArrayList<MyMeasurementDialogCategoryValueVO> getFractionValues() {
        return fractionValues;
    }

    /**
     * Sets the fraction values
     * @param fractionValues this array defines the fraction spinner range. Just leave this
     *                       values null if you did not want a second spinner, and just insert it
     *                       if you want a single spinner.
     *
     */
    public void setFractionValues(ArrayList<MyMeasurementDialogCategoryValueVO> fractionValues) {
        this.fractionValues = fractionValues;
    }

    public MyMeasurementFractionLimitValueVO getFractionLimit() {
        return fractionLimit;
    }

    /**
     * Sets the fraction values
     * @param fractionLimit set limiter to fraction spinner
     *
     */
    public void setFractionLimit(MyMeasurementFractionLimitValueVO fractionLimit) {
        this.fractionLimit = fractionLimit;
    }


}