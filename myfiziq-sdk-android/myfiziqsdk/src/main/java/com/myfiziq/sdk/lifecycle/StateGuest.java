package com.myfiziq.sdk.lifecycle;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.fragments.FragmentCreateGuest;
import com.myfiziq.sdk.fragments.FragmentSelectGuest;
import com.myfiziq.sdk.fragments.FragmentSelectOrCreateGuest;

public class StateGuest
{
    public static final String CREATE_GUEST_NAME = "CREATE-GUEST";
    public static final String SELECT_CREATE_GUEST_NAME = "SELECT-CREATE-GUEST";
    public static final String SELECT_GUEST_NAME = "SELECT-GUEST";

    private StateGuest()
    {
        // Empty hidden constructor for the utility class
    }

    public static ParameterSet getSelectOrCreateGuest()
    {
        return new ParameterSet.Builder(FragmentSelectOrCreateGuest.class)
                .setName(SELECT_CREATE_GUEST_NAME)
                .build();
    }

    public static ParameterSet getCreateGuest()
    {
        return new ParameterSet.Builder(FragmentCreateGuest.class)
                .setName(CREATE_GUEST_NAME)
                .build();
    }

    public static ParameterSet getSelectGuest()
    {
        return new ParameterSet.Builder(FragmentSelectGuest.class)
                .setName(SELECT_GUEST_NAME)
                .build();
    }
}
