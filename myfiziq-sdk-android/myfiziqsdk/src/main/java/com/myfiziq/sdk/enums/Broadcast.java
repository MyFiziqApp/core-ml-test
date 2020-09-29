package com.myfiziq.sdk.enums;

import android.content.Intent;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.GlobalContext;

import androidx.annotation.Keep;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

@Keep
public enum Broadcast
{
    ACTION_UNKNOWN(""),

    ACTION_PARAMETER(),
    ACTION_PARAMETER_SET(),
    ACTION_RESOURCE_DL_START(),
    ACTION_RESOURCE_DL_PROGRESS(),
    ACTION_RESOURCE_DL_FAILED(),
    ACTION_RESOURCE_DL_SUCCESS(),

    CONNECTIVITY_CHANGE();

    String mAction;

    Broadcast()
    {
        mAction = BuildConfig.LIBRARY_PACKAGE_NAME + name();
    }

    Broadcast(String action)
    {
        mAction = BuildConfig.LIBRARY_PACKAGE_NAME + action;
    }

    public static Intent prepare(Broadcast broadcast)
    {
        return new Intent(broadcast.mAction);
    }

    public static void send(Intent intent)
    {
        LocalBroadcastManager.getInstance(GlobalContext.getContext()).sendBroadcast(intent);
    }

    public static void send(Broadcast broadcast)
    {
        LocalBroadcastManager.getInstance(GlobalContext.getContext()).sendBroadcast(new Intent(broadcast.mAction));
    }

    public static void send(Broadcast broadcast, String... args)
    {
        Intent intent = new Intent(broadcast.mAction);
        int ix = 0;
        for (String arg : args)
        {
            intent.putExtra(String.valueOf(ix), arg);
            ix++;
        }
        LocalBroadcastManager.getInstance(GlobalContext.getContext()).sendBroadcast(intent);
    }

    public static void send(Parameter parameter)
    {
        Intent intent = new Intent(ACTION_PARAMETER.mAction);
        intent.putExtra(ACTION_PARAMETER.name(), parameter);
        LocalBroadcastManager.getInstance(GlobalContext.getContext()).sendBroadcast(intent);
    }

    public static void send(ParameterSet parameterSet)
    {
        Intent intent = new Intent(ACTION_PARAMETER_SET.mAction);
        intent.putExtra(ACTION_PARAMETER_SET.name(), parameterSet);
        LocalBroadcastManager.getInstance(GlobalContext.getContext()).sendBroadcast(intent);
    }

    public static Broadcast parse(Intent intent)
    {
        String action = "";
        if (null != intent)
        {
            action = intent.getAction();
        }

        Broadcast result = ACTION_UNKNOWN;
        try
        {
            for (Broadcast bc : Broadcast.values())
            {
                if (bc.mAction.contentEquals(action))
                {
                    result = bc;
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "");
        }
        return result;
    }

    public static Parameter getParameter(Intent intent)
    {
        Broadcast broadcast = parse(intent);
        if (ACTION_PARAMETER == broadcast)
        {
            return intent.getParcelableExtra(ACTION_PARAMETER.name());
        }

        return null;
    }

    public static ParameterSet getParameterSet(Intent intent)
    {
        Broadcast broadcast = parse(intent);
        if (ACTION_PARAMETER_SET == broadcast)
        {
            return intent.getParcelableExtra(ACTION_PARAMETER_SET.name());
        }

        return null;
    }
}
