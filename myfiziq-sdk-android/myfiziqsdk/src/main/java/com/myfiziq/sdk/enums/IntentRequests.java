package com.myfiziq.sdk.enums;

import android.os.Parcelable;

import com.myfiziq.sdk.intents.parcels.ViewAvatarRouteRequest;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;


public enum IntentRequests
{
    NEW_HOMEPAGE_ROUTE(),
    NEW_VIEW_ALL_ROUTE(),
    NEW_VIEW_AVATAR_ROUTE(ViewAvatarRouteRequest.class),
    NEW_ONBOARDING_ROUTE(ParameterSet.class),
    NEW_TRACK_ROUTE(),
    NEW_SUPPORT_ROUTE(ParameterSet.class),
    NEW_SETTINGS_ROUTE(),
    NEW_LOGOUT_ROUTE(),
    REINITIALISE_SDK(),
    LOGOUT_CLICKED(),
    SELECT_AVATAR(),

    MYFIZIQ_ACTIVITY_FINISHING(),

    AVATAR_ONE_SELECTED(),
    AVATAR_TWO_SELECTED(),

    AVATAR_SELECTOR(ParameterSet.class);



    private Class<? extends Parcelable> parcelableClass = null;


    /**
     * Represents an intent being broadcast to request information from a BroadcastReceiver that is listening for the request.
     */
    IntentRequests()
    {

    }

    /**
     * Represents an intent being broadcast to request information from a BroadcastReceiver that is listening for the request.
     * @param parcelableClass A parcelable class with information that will be passed as part of the request.
     */
    IntentRequests(Class<? extends Parcelable> parcelableClass)
    {
        this.parcelableClass = parcelableClass;
    }

    /**
     * Gets a unique key that represents the action for the intent.
     */
    public String getActionKey()
    {
        return "com.myfiziq.sdk.REQUEST_" + name();
    }

    /**
     * Gets a key which represents the parcel contained in the request.
     */
    public String getParcelKey()
    {
        return "com.myfiziq.sdk.REQUEST_PARCEL_" + name();
    }

    @Nullable
    public Class<? extends Parcelable> getParcelClass()
    {
        return parcelableClass;
    }
}
