package com.myfiziq.sdk.lifecycle;

import com.myfiziq.sdk.fragments.FragmentViewAvatarHome;

/**
 * Creates a route to visit the Profile screen.
 */
public class StateProfile
{
    private StateProfile()
    {
        // Empty hidden constructor for the utility class
    }

    public static ParameterSet getProfile()
    {
        return new ParameterSet.Builder(FragmentViewAvatarHome.class)
                .build();
    }
}
