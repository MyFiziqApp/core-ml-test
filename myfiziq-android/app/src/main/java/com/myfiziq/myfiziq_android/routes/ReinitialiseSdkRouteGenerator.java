package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.activities.ActivityEntrypoint;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;

public class ReinitialiseSdkRouteGenerator extends MyFiziqBroadcastReceiver
{
    public ReinitialiseSdkRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        return new ParameterSet.Builder(ActivityEntrypoint.class)
                .build();
    }
}