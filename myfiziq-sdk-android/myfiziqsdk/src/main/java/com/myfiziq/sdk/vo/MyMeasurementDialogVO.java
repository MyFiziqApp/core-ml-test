package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class MyMeasurementDialogVO implements Parcelable
{
    private String dialogTitle;
    private ArrayList<MyMeasurementDialogCategoryVO> categories;

    private MyMeasurementDialogCategoryVO initialCategory;
    private MyMeasurementDialogCategoryValueVO initialValue;
    private MyMeasurementDialogCategoryValueVO initialFractionValue;


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.dialogTitle);
        dest.writeTypedList(this.categories);
        dest.writeParcelable(this.initialCategory, flags);
        dest.writeParcelable(this.initialValue, flags);
        dest.writeParcelable(this.initialFractionValue, flags);
    }

    public MyMeasurementDialogVO()
    {
    }

    protected MyMeasurementDialogVO(Parcel in)
    {
        this.dialogTitle = in.readString();
        this.categories = in.createTypedArrayList(MyMeasurementDialogCategoryVO.CREATOR);
        this.initialCategory = in.readParcelable(MyMeasurementDialogCategoryVO.class.getClassLoader());
        this.initialValue = in.readParcelable(MyMeasurementDialogCategoryValueVO.class.getClassLoader());
        this.initialFractionValue = in.readParcelable(MyMeasurementDialogCategoryValueVO.class.getClassLoader());
    }

    public static final Creator<MyMeasurementDialogVO> CREATOR = new Creator<MyMeasurementDialogVO>()
    {
        @Override
        public MyMeasurementDialogVO createFromParcel(Parcel source)
        {
            return new MyMeasurementDialogVO(source);
        }

        @Override
        public MyMeasurementDialogVO[] newArray(int size)
        {
            return new MyMeasurementDialogVO[size];
        }
    };



    public String getDialogTitle()
    {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle)
    {
        this.dialogTitle = dialogTitle;
    }

    public ArrayList<MyMeasurementDialogCategoryVO> getCategories()
    {
        return categories;
    }

    public void setCategories(ArrayList<MyMeasurementDialogCategoryVO> categories)
    {
        this.categories = categories;
    }

    public MyMeasurementDialogCategoryVO getDefaultCategory()
    {
        return initialCategory;
    }

    public void setInitialCategory(MyMeasurementDialogCategoryVO initialCategory)
    {
        this.initialCategory = initialCategory;
    }

    public MyMeasurementDialogCategoryValueVO getDefaultValue()
    {
        return initialValue;
    }

    public void setInitialValue(MyMeasurementDialogCategoryValueVO initialValue)
    {
        this.initialValue = initialValue;
    }

    public MyMeasurementDialogCategoryValueVO getDefaultFractionValue()
    {
        return initialFractionValue;
    }

    public void setInitialFractionValue(MyMeasurementDialogCategoryValueVO initialFractionValue)
    {
        this.initialFractionValue = initialFractionValue;
    }
}
