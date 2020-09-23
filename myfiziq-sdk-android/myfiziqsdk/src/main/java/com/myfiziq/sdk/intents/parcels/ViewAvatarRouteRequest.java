package com.myfiziq.sdk.intents.parcels;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This parcelable contains information that can be sent with a request for a ViewAvatar Route.
 */
public class ViewAvatarRouteRequest implements Parcelable
{
    private String modelId;

    public ViewAvatarRouteRequest(String modelId)
    {
        this.modelId = modelId;
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
    }

    protected ViewAvatarRouteRequest(Parcel in)
    {
        this.modelId = in.readString();
    }

    public static final Creator<ViewAvatarRouteRequest> CREATOR = new Creator<ViewAvatarRouteRequest>()
    {
        @Override
        public ViewAvatarRouteRequest createFromParcel(Parcel source)
        {
            return new ViewAvatarRouteRequest(source);
        }

        @Override
        public ViewAvatarRouteRequest[] newArray(int size)
        {
            return new ViewAvatarRouteRequest[size];
        }
    };
}
