package com.myfiziq.sdk.enums;

public enum IntentResponses
{
    NEW_HOMEPAGE_ROUTE,
    NEW_VIEW_ALL_ROUTE,
    NEW_VIEW_AVATAR_ROUTE,
    NEW_ONBOARDING_ROUTE,
    NEW_TRACK_ROUTE,
    NEW_SUPPORT_ROUTE,
    NEW_SETTINGS_ROUTE,
    NEW_LOGOUT_ROUTE,
    REINITIALISE_SDK,

    MYFIZIQ_ACTIVITY_FINISHING,
    SHOW_SWAP_MENU_BUTTON,
    HIDE_SWAP_MENU_BUTTON,
    SHOW_VIEW_SUPPORT_BUTTON,
    HIDE_VIEW_SUPPORT_BUTTON,
    CAPTURE_PROCESSING_ERROR,
    SWAP_AVATARS,
    SHOW_BOTTOM_NAVIGATION_BAR,
    HIDE_BOTTOM_NAVIGATION_BAR,
    NAVIGATE_HOME,
    LOGOUT_CLICKED,

    MESSAGE_MODEL_AVATAR,
    SEND_SUPPORT_DATA,

    AVATAR_ONE_SELECTED,
    AVATAR_TWO_SELECTED,

    AVATAR_SELECTOR;



    /**
     * Represents an intent being broadcast to respond to a request for information.
     */
    IntentResponses()
    {
    }

    /**
     * Gets a unique key that represents the action for the intent.
     */
    public String getActionKey()
    {
        return "com.myfiziq.sdk.RESPONSE_" + name();
    }

    /**
     * Gets a key which represents the parcel contained in the response.
     */
    public String getParcelKey()
    {
        return "com.myfiziq.sdk.RESPONSE_PARCEL_" + name();
    }
}
