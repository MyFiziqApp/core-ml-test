package com.myfiziq.sdk.activities;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentManager;

/**
 * Simple interface to abstract common <code>Activity</code> functionality.
 * @hide
 */
public interface ActivityInterface
{
    Activity getActivity();
    Context getContext();
    void finish();

    FragmentManager getSupportFragmentManager();
}
