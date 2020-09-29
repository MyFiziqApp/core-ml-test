package com.myfiziq.sdk.intents.parcels;

import android.os.Parcel;
import android.os.Parcelable;

public class SupportRouteRequest implements Parcelable
{
    private String modelId;
    public String supportType;

    public SupportRouteRequest(String modelId, String supportType)
    {
        this.modelId = modelId;
        this.supportType = supportType;
    }

    public String getModelId()
    {
        return modelId;
    }



    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.modelId);
        dest.writeString(this.supportType);
    }

    protected SupportRouteRequest(Parcel in)
    {
        this.modelId = in.readString();
    }

    public static final Creator<SupportRouteRequest> CREATOR = new Creator<SupportRouteRequest>()
    {
        @Override
        public SupportRouteRequest createFromParcel(Parcel source)
        {
            return new SupportRouteRequest(source);
        }

        @Override
        public SupportRouteRequest[] newArray(int size)
        {
            return new SupportRouteRequest[size];
        }
    };
}
