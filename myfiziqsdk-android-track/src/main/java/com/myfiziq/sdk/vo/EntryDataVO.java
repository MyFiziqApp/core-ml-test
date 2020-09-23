package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

/**
 * @hide
 */
public class EntryDataVO implements Parcelable
{
    /**
     * The label that should appear on the X-Axis.
     */
    private String xAxisLabel;

    /**
     * The label that should appear next to data point on the graph.
     */
    private String dataPointLabel;

    /**
     * Represents auxiliary data for an entry on a graph.
     *
     * @param xAxisLabel The label that should appear on the X-Axis.
     * @param dataPointLabel The label that should appear next to data point on the graph.
     */
    public EntryDataVO(String xAxisLabel, String dataPointLabel)
    {
        this.xAxisLabel = xAxisLabel;
        this.dataPointLabel = dataPointLabel;
    }

    @Nullable
    public String getXAxisLabel()
    {
        return xAxisLabel;
    }

    public void setXAxisLabel(String xAxisLabel)
    {
        this.xAxisLabel = xAxisLabel;
    }

    @Nullable
    public String getDataPointLabel()
    {
        return dataPointLabel;
    }

    public void setDataPointLabel(String yLabel)
    {
        this.dataPointLabel = yLabel;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.xAxisLabel);
        dest.writeString(this.dataPointLabel);
    }

    protected EntryDataVO(Parcel in)
    {
        this.xAxisLabel = in.readString();
        this.dataPointLabel = in.readString();
    }

    public static final Creator<EntryDataVO> CREATOR = new Creator<EntryDataVO>()
    {
        @Override
        public EntryDataVO createFromParcel(Parcel source)
        {
            return new EntryDataVO(source);
        }

        @Override
        public EntryDataVO[] newArray(int size)
        {
            return new EntryDataVO[size];
        }
    };
}
