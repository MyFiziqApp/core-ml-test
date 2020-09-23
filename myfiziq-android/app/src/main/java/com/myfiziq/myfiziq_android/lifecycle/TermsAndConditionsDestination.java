package com.myfiziq.myfiziq_android.lifecycle;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

public class TermsAndConditionsDestination implements Parcelable
{
    private Class destination;


    public TermsAndConditionsDestination()
    {
    }

    public TermsAndConditionsDestination(Class<? extends Activity> destination)
    {
        this.destination = destination;
    }

    public Class getDestination()
    {
        return destination;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeSerializable(this.destination);
    }

    protected TermsAndConditionsDestination(Parcel in)
    {
        this.destination = (Class) in.readSerializable();
    }

    public static final Parcelable.Creator<TermsAndConditionsDestination> CREATOR = new Parcelable.Creator<TermsAndConditionsDestination>()
    {
        @Override
        public TermsAndConditionsDestination createFromParcel(Parcel source)
        {
            return new TermsAndConditionsDestination(source);
        }

        @Override
        public TermsAndConditionsDestination[] newArray(int size)
        {
            return new TermsAndConditionsDestination[size];
        }
    };
}
