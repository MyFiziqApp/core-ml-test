package com.myfiziq.sdk.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Provides helper methods to determine the current state of network connectivity.
 */
public class ConnectivityHelper
{
    /**
     * Determines if there's a current network connection available.
     *
     * Note, this only checks to see if there is a network connection is available, NOT if we're actually connected to the internet.
     *
     * @param context The current context.
     * @return True if there's a network connection available, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
