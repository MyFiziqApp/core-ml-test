package com.myfiziq.myfiziq_android;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.myfiziq.myfiziq_android.helpers.MyFiziqCrashHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class LoggingTree extends Timber.DebugTree
{
    private final String TAG = LoggingTree.class.getSimpleName();

    /**
     * Plants a new
     */
    public static void plantNewTree()
    {
        if (Timber.treeCount() == 0)
        {
            Timber.plant(new LoggingTree());
            Timber.i("A new Timber tree was planted");
        }
        else
        {
            Timber.i("No new Timber trees were planted since %s already exists", Timber.treeCount());
        }
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable exception)
    {
        if (BuildConfig.DEBUG)
        {
            logUsingAndroidLogger(priority, tag, message, exception);
        }
        else if (MyFiziqCrashHelper.shouldEnableCrashLogs())
        {
            logUsingCrashlytics(priority, tag, message, exception);
        }
    }

    private void logUsingCrashlytics(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable exception)
    {
        FirebaseCrashlytics.getInstance().log(message);

        if (exception != null)
        {
            FirebaseCrashlytics.getInstance().recordException(exception);
        }
    }

    @SuppressLint("LogNotTimber")
    private void logUsingAndroidLogger(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable exception)
    {
        switch (priority)
        {
            case Log.VERBOSE:
                Log.v(tag, message, exception);
                break;
            case Log.DEBUG:
                Log.d(tag, message, exception);
                break;
            case Log.INFO:
                Log.i(tag, message, exception);
                break;
            case Log.WARN:
                Log.w(tag, message, exception);
                break;
            case Log.ERROR:
                Log.e(tag, message, exception);
                break;
            case Log.ASSERT:
                Log.wtf(tag, message, exception);
                break;
            default:
                Log.e(TAG, "Unknown priority level: " + priority + ". Logging as ERROR");
                Log.e(TAG, message, exception);
                break;
        }
    }
}
