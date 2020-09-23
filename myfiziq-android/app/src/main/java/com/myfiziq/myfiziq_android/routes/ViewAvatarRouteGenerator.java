package com.myfiziq.myfiziq_android.routes;

import android.content.Context;

import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.fragments.FragmentViewAvatar;
import com.myfiziq.sdk.fragments.FragmentViewAvatarInsights;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.intents.parcels.ViewAvatarRouteRequest;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;

public class ViewAvatarRouteGenerator extends MyFiziqBroadcastReceiver<ViewAvatarRouteRequest>
{
    public ViewAvatarRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable ViewAvatarRouteRequest parcel)
    {
        String modelId = "";

        if (null != parcel)
        {
            modelId = parcel.getModelId();
        }
        Class<?> viewAvatarFragment = FragmentViewAvatar.class;

        if (ModelSetting.getSetting(ModelSetting.Setting.FEATURE_INSIGHTS_VIEW_AVATAR,false))
        {
            viewAvatarFragment = FragmentViewAvatarInsights.class;
        }
        return new ParameterSet.Builder(viewAvatarFragment)
                .addParam(new Parameter(com.myfiziqsdk_android_profile.R.id.TAG_MODEL, modelId))
                .addParam(new Parameter(com.myfiziqsdk_android_profile.R.id.TAG_ARG_VIEW, 1))
                .build();
    }
}