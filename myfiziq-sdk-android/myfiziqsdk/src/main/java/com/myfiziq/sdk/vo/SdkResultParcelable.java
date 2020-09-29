package com.myfiziq.sdk.vo;

import android.os.Parcel;
import android.os.Parcelable;

import com.myfiziq.sdk.enums.SdkResultCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains the {@link SdkResultCode} and associated message inside a {@link Parcelable} so
 * it can be transported around.
 */
public class SdkResultParcelable implements Parcelable
{
    @NonNull
    private SdkResultCode resultCode;

    @Nullable
    private String result;

    // We need this constructor for serialisation in Android, but don't call this yourself!
    // Use another constructor below.
    // I've made this constructor "deprecated" so the programmer sees the warning and crossed out method
    // when they try to use it instead of the other ones below.
    @Deprecated
    public SdkResultParcelable()
    {
        // If something (e.g. Android Framework) tries to create this object but never sets
        // the result code, ensure that the code is an error for when the consumer tries to parse
        // it later.
        this.resultCode = SdkResultCode.SDK_ERROR_PARCELABLE_SERIALISATION;
    }

    public SdkResultParcelable(@NonNull SdkResultCode resultCode)
    {
        this.resultCode = resultCode;
    }

    public SdkResultParcelable(@NonNull SdkResultCode resultCode, @Nullable  String result)
    {
        this.resultCode = resultCode;
        this.result = result;
    }

    @NonNull
    public SdkResultCode getResultCode()
    {
        return resultCode;
    }

    @Nullable
    public String getResult()
    {
        return result;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    // WARNING WARNING WARNING!
    // The Parcelable auto-generator does a null check and sets resultCode to null if it equals -1. But our -1 means and error.
    // Change it so never does the null check and always assumes non-null.
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.resultCode.ordinal());
        dest.writeString(this.result);
    }

    // WARNING WARNING WARNING!
    // The Parcelable auto-generator does a null check and sets resultCode to null if it equals -1. But our -1 means and error.
    // Change it so never does the null check and always assumes non-null.
    protected SdkResultParcelable(Parcel in)
    {
        int tmpResultCode = in.readInt();
        this.resultCode = SdkResultCode.values()[tmpResultCode];
        this.result = in.readString();
    }

    public static final Creator<SdkResultParcelable> CREATOR = new Creator<SdkResultParcelable>()
    {
        @Override
        public SdkResultParcelable createFromParcel(Parcel source)
        {
            return new SdkResultParcelable(source);
        }

        @Override
        public SdkResultParcelable[] newArray(int size)
        {
            return new SdkResultParcelable[size];
        }
    };
}
