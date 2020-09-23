package com.myfiziq.myfiziq_android.helpers;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.myfiziq.myfiziq_android.BuildConfig;
import com.myfiziq.sdk.util.GlobalContext;

import timber.log.Timber;

public class MyFiziqCrashHelper
{
    private MyFiziqCrashHelper()
    {
        // Empty hidden constructor for the utility class
    }

    public static void startCrashReporting(Context context)
    {
        if (!shouldEnableCrashLogs())
        {
            Timber.i("Crash reporting is disabled. Will not start listening for crashes.");
            return;
        }

        FirebaseApp.initializeApp(GlobalContext.getContext());
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }

    public static void assignUserForCrashReporting(String userIdentifier, String username, String userEmail)
    {
        if (!shouldEnableCrashLogs())
        {
            Timber.w("Crash reporting is disabled. Will not assign a user with crashes.");
            return;
        }

        FirebaseCrashlytics.getInstance().setUserId(userIdentifier);
    }

    public static boolean shouldEnableCrashLogs()
    {
        return !BuildConfig.DEBUG;
    }
}
