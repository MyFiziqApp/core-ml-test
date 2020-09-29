package com.myfiziq.sdk.enums;

public enum IntentPairs
{
    HOMEPAGE_ROUTE(IntentRequests.NEW_HOMEPAGE_ROUTE, IntentResponses.NEW_HOMEPAGE_ROUTE),
    ONBOARDING_ROUTE(IntentRequests.NEW_ONBOARDING_ROUTE, IntentResponses.NEW_ONBOARDING_ROUTE),
    TRACK_ROUTE(IntentRequests.NEW_TRACK_ROUTE, IntentResponses.NEW_TRACK_ROUTE),
    SUPPORT_ROUTE(IntentRequests.NEW_SUPPORT_ROUTE, IntentResponses.NEW_SUPPORT_ROUTE),
    VIEW_ALL_ROUTE(IntentRequests.NEW_VIEW_ALL_ROUTE, IntentResponses.NEW_VIEW_ALL_ROUTE),
    VIEW_AVATAR_ROUTE(IntentRequests.NEW_VIEW_AVATAR_ROUTE, IntentResponses.NEW_VIEW_AVATAR_ROUTE),
    SETTINGS_ROUTE(IntentRequests.NEW_SETTINGS_ROUTE, IntentResponses.NEW_SETTINGS_ROUTE),
    LOGOUT_ROUTE(IntentRequests.NEW_LOGOUT_ROUTE, IntentResponses.NEW_LOGOUT_ROUTE),
    REINITIALISE_SDK(IntentRequests.REINITIALISE_SDK, IntentResponses.REINITIALISE_SDK),
    LOGOUT_CLICKED(IntentRequests.LOGOUT_CLICKED, IntentResponses.LOGOUT_CLICKED),
    AVATAR_SELECTOR(IntentRequests.AVATAR_SELECTOR, IntentResponses.AVATAR_SELECTOR),

    MYFIZIQ_ACTIVITY_FINISHING(IntentRequests.MYFIZIQ_ACTIVITY_FINISHING, IntentResponses.MYFIZIQ_ACTIVITY_FINISHING),

    AVATAR_ONE_SELECTED(IntentRequests.AVATAR_ONE_SELECTED, IntentResponses.AVATAR_ONE_SELECTED),
    AVATAR_TWO_SELECTED(IntentRequests.AVATAR_TWO_SELECTED, IntentResponses.AVATAR_TWO_SELECTED);


    private IntentRequests request;
    private IntentResponses response;

    IntentPairs(IntentRequests request, IntentResponses response)
    {
        this.request = request;
        this.response = response;
    }

    public IntentRequests getRequest()
    {
        return request;
    }

    public IntentResponses getResponse()
    {
        return response;
    }
}