package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.fragments.FragmentViewAvatar;
import com.myfiziq.sdk.fragments.FragmentViewAvatarInsights;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateTrack;

import androidx.annotation.Nullable;

public class TrackRouteGenerator extends MyFiziqBroadcastReceiver
{
    public TrackRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        ParameterSet trackSet = StateTrack.getTrack();

        Class<?> viewAvatarFragment = FragmentViewAvatar.class;

        if (ModelSetting.getSetting(ModelSetting.Setting.FEATURE_INSIGHTS_VIEW_AVATAR,false))
        {
            viewAvatarFragment = FragmentViewAvatarInsights.class;
        }

        trackSet.addNextSet(new ParameterSet.Builder(viewAvatarFragment)
                .setName(StateTrack.BUNDLE_VIEWAVATAR)
                .build());

        return trackSet;
    }
}