package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.fragments.FragmentProfileList;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;

public class ViewAllRouteGenerator extends MyFiziqBroadcastReceiver
{
    public ViewAllRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        return new ParameterSet.Builder(FragmentProfileList.class)
                .build();
    }
}