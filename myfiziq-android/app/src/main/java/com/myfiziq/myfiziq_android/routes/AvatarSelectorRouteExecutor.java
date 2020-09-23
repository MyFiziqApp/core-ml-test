package com.myfiziq.myfiziq_android.routes;

import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class AvatarSelectorRouteExecutor extends MyFiziqBroadcastReceiver<ParameterSet>
{
    private WeakReference<ActivityInterface> activity;

    public AvatarSelectorRouteExecutor(ActivityInterface activity, IntentPairs intentPairs)
    {
        super(activity.getContext(), intentPairs);

        this.activity = new WeakReference<>(activity);
    }

    @Override
    @Nullable
    public ParameterSet generateParameterSet(@Nullable ParameterSet parameterSet)
    {
        if (activity.get() == null)
        {
            Timber.e("Route executor has detached from activity");
            return null;
        }

        if (parameterSet == null)
        {
            Timber.e("ParameterSet received by route executor was null");
            return null;
        }

        parameterSet.addNextSet(parameterSet);
        parameterSet.startNext(activity.get(), false);

        // Don't send back anything
        return null;
    }
}
