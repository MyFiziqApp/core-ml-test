package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.lifecycle.StateSettings;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class HomepageRouteGenerator extends MyFiziqBroadcastReceiver
{
    public HomepageRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        return StateSettings.getSettings();
    }
}
