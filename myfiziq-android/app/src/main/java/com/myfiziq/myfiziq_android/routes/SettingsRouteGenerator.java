package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.lifecycle.StateSettings;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;

public class SettingsRouteGenerator extends MyFiziqBroadcastReceiver
{
    public SettingsRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        return StateSettings.getSettings();
    }
}
