package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.fragments.FragmentViewAvatarHome;
import com.myfiziq.sdk.fragments.FragmentViewAvatarInsights;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateProfile;

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
        ParameterSet parameterSet;
        ArrayList<ModelAvatar> avatars = ORMDbQueries.getLastXCompletedAvatars(1);

        if (null == avatars || avatars.size() == 0)
        {
            // If there is no avatar that has been created and has finished processing...
            parameterSet = StateProfile.getProfile();
        }
        else
        {
            // Else we have at least 1 avatar that has been created and has finished processing...
            ModelAvatar lastReadyAvatar = avatars.get(0);
            Class<?> homeFragment = FragmentViewAvatarHome.class;
            if (ModelSetting.getSetting(ModelSetting.Setting.FEATURE_INSIGHTS_VIEW_AVATAR,false))
            {
                homeFragment = FragmentViewAvatarInsights.class;
            }
            parameterSet = new ParameterSet.Builder(homeFragment)
                                .addParam(new Parameter(com.myfiziqsdk_android_profile.R.id.TAG_MODEL, lastReadyAvatar.id))
                                .build();
        }

        return parameterSet;
    }
}
