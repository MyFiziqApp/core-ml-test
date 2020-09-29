package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

public class MyMeasurementDialogResultVO implements Parcelable
{
    private String chosenUnitsOfMeasurement;
    private int value;


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.chosenUnitsOfMeasurement);
        dest.writeInt(this.value);
    }

    public MyMeasurementDialogResultVO()
    {
    }

    protected MyMeasurementDialogResultVO(Parcel in)
    {
        this.chosenUnitsOfMeasurement = in.readString();
        this.value = in.readInt();
    }

    public static final Creator<MyMeasurementDialogResultVO> CREATOR = new Creator<MyMeasurementDialogResultVO>()
    {
        @Override
        public MyMeasurementDialogResultVO createFromParcel(Parcel source)
        {
            return new MyMeasurementDialogResultVO(source);
        }

        @Override
        public MyMeasurementDialogResultVO[] newArray(int size)
        {
            return new MyMeasurementDialogResultVO[size];
        }
    };
}
