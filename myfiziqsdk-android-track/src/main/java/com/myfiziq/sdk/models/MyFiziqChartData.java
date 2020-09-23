package com.myfiziq.sdk.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.mikephil.charting.data.Entry;

import java.util.List;

public class MyFiziqChartData implements Parcelable
{
    private List<Entry> primaryDataSetEntries;
    private List<Entry> secondaryDataSetEntries;
    private float xAxisMin;
    private float xAxisMax;
    private float leftAxisMin;
    private float leftAxisMax;

    public MyFiziqChartData(List<Entry> primaryDataSetEntries)
    {
        this.primaryDataSetEntries = primaryDataSetEntries;
    }

    public MyFiziqChartData(List<Entry> primaryDataSetEntries, List<Entry> secondaryDataSetEntries)
    {
        this.primaryDataSetEntries = primaryDataSetEntries;
        this.secondaryDataSetEntries = secondaryDataSetEntries;
    }

    public List<Entry> getPrimaryDataSetEntries()
    {
        return primaryDataSetEntries;
    }

    public List<Entry> getSecondaryDataSetEntries()
    {
        return secondaryDataSetEntries;
    }

    public float getXAxisMin()
    {
        return xAxisMin;
    }

    public void setXAxisMin(float xAxisMin)
    {
        this.xAxisMin = xAxisMin;
    }

    public float getXAxisMax()
    {
        return xAxisMax;
    }

    public void setXAxisMax(float xAxisMax)
    {
        this.xAxisMax = xAxisMax;
    }

    public float getLeftAxisMin()
    {
        return leftAxisMin;
    }

    public void setLeftAxisMin(float leftAxisMin)
    {
        this.leftAxisMin = leftAxisMin;
    }

    public float getLeftAxisMax()
    {
        return leftAxisMax;
    }

    public void setLeftAxisMax(float leftAxisMax)
    {
        this.leftAxisMax = leftAxisMax;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeTypedList(this.primaryDataSetEntries);
        dest.writeFloat(this.xAxisMin);
        dest.writeFloat(this.xAxisMax);
        dest.writeFloat(this.leftAxisMin);
        dest.writeFloat(this.leftAxisMax);
    }

    protected MyFiziqChartData(Parcel in)
    {
        this.primaryDataSetEntries = in.createTypedArrayList(Entry.CREATOR);
        this.xAxisMin = in.readFloat();
        this.xAxisMax = in.readFloat();
        this.leftAxisMin = in.readFloat();
        this.leftAxisMax = in.readFloat();
    }

    public static final Creator<MyFiziqChartData> CREATOR = new Creator<MyFiziqChartData>()
    {
        @Override
        public MyFiziqChartData createFromParcel(Parcel source)
        {
            return new MyFiziqChartData(source);
        }

        @Override
        public MyFiziqChartData[] newArray(int size)
        {
            return new MyFiziqChartData[size];
        }
    };
}
