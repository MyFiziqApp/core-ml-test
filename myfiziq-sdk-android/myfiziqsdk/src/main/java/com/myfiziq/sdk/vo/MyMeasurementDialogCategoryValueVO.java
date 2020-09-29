package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

public class MyMeasurementDialogCategoryValueVO implements Parcelable
{
    private String label;
    private int value;




    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.label);
        dest.writeInt(this.value);
    }

    public MyMeasurementDialogCategoryValueVO()
    {
    }

    protected MyMeasurementDialogCategoryValueVO(Parcel in)
    {
        this.label = in.readString();
        this.value = in.readInt();
    }

    public static final Creator<MyMeasurementDialogCategoryValueVO> CREATOR = new Creator<MyMeasurementDialogCategoryValueVO>()
    {
        @Override
        public MyMeasurementDialogCategoryValueVO createFromParcel(Parcel source)
        {
            return new MyMeasurementDialogCategoryValueVO(source);
        }

        @Override
        public MyMeasurementDialogCategoryValueVO[] newArray(int size)
        {
            return new MyMeasurementDialogCategoryValueVO[size];
        }
    };


    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public int getValue()
    {
        return value;
    }

    public void setValue(int value)
    {
        this.value = value;
    }
}
