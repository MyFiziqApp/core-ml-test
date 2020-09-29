package com.myfiziq.sdk.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.Nullable;

public class ActivityHelper
{
    /**
     * Gets an activity from any context (e.g. a {@link android.view.View}.
     *
     * @param context The current context, probably from {@link View#getContext()}.
     * @return The activity attached to the context. May return null if no activity was found.
     */
    // Uses dark magic to get the activity.
    // Code is from the Android Support library
    // See: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/marshmallow-release/v7/mediarouter/src/android/support/v7/app/MediaRouteButton.java#262
    @Nullable
    public static Activity getActivity(Context context)
    {
        while (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }

            ContextWrapper contextWrapper = (ContextWrapper) context;
            context = contextWrapper.getBaseContext();
        }

        return null;
    }
}
