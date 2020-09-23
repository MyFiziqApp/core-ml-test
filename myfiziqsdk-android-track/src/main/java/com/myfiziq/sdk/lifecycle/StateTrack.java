package com.myfiziq.sdk.lifecycle;

import com.myfiziq.sdk.enums.ParameterSetName;
import com.myfiziq.sdk.fragments.FragmentNoAvatars;
import com.myfiziq.sdk.fragments.FragmentTrack;

/**
 * Creates a route to visit the Track screen.
 */
public class StateTrack
{
    public static final String BUNDLE_VIEWAVATAR = "VIEWAVATAR";
    public static final String BUNDLE_SELAVATAR = "SELWAVATAR";

    private StateTrack()
    {
        // Empty hidden constructor for the utility class
    }

    public static ParameterSet getTrack()
    {
        ParameterSet.Builder builder = new ParameterSet.Builder(FragmentTrack.class);

        builder.addNextSet(new ParameterSet.Builder(FragmentNoAvatars.class)
                            .setName(ParameterSetName.NO_AVATARS)
                            .build());

        return builder.build();
    }
}
