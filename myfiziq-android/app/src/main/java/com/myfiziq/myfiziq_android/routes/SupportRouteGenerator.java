package com.myfiziq.myfiziq_android.routes;

import android.content.Context;

import com.myfiziq.myfiziq_android.activities.ActivitySupport;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.SupportType;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class SupportRouteGenerator extends MyFiziqBroadcastReceiver<ParameterSet>
{
    public SupportRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    public ParameterSet generateParameterSet(@Nullable ParameterSet parcel)
    {
        if (parcel == null)
        {
            Timber.e("failed to generate support route from null parameterSet");
            return null;
        }
        ParameterSet.Builder ps = new ParameterSet.Builder(ActivitySupport.class);
        if (parcel.hasParam(com.myfiziq.sdk.R.id.TAG_ARG_VIEW))
        {
            ps.addParam(
                    new Parameter.Builder()
                            .setParamId(com.myfiziq.sdk.R.id.TAG_ARG_VIEW)
                            .setValue(parcel.getParam(com.myfiziq.sdk.R.id.TAG_ARG_VIEW).getValue())
                            .build()
            );
        }
        if (parcel.hasParam(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR))
        {
            ps.addParam(
                    new Parameter.Builder()
                            .setParamId(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR)
                            .setParcelableValue(parcel.getParam(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue())
                            .build()
            );
        }
        return ps.build();
    }
}
