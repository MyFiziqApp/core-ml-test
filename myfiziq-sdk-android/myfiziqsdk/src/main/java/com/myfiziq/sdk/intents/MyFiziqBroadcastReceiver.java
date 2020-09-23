package com.myfiziq.sdk.intents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;

import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.lang.reflect.ParameterizedType;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

/**
 * This class responds to requests from the MyFiziq SDK to generate customised ParameterSets that indicate where the user should be sent to next.
 *
 * It should be initialised before the MyFiziq activity is created.
 */
public abstract class MyFiziqBroadcastReceiver<T extends Parcelable> extends BroadcastReceiver
{
    private Context rootContext;
    private IntentPairs intentPairs;

    private LocalBroadcastManager localBroadcastManager;


    public MyFiziqBroadcastReceiver(Context rootContext, IntentPairs intentPairs)
    {
        this.rootContext = rootContext;
        this.intentPairs = intentPairs;
    }

    /**
     * Prepares the MyFiziqBroadcastReceiver to start accepting requests from the MyFiziq SDK.
     */
    public void startListening()
    {
        localBroadcastManager = LocalBroadcastManager.getInstance(rootContext);

        IntentFilter intentFilter = new IntentFilter(intentPairs.getRequest().getActionKey());
        localBroadcastManager.registerReceiver(this, intentFilter);
    }

    public void stopListening()
    {
        if (null != localBroadcastManager)
        {
            localBroadcastManager.unregisterReceiver(this);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        stopListening();
        super.finalize();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        String requestActionKey = intentPairs.getRequest().getActionKey();
        String requestParcelKey = intentPairs.getRequest().getParcelKey();

        T parcel = getParcelFromIntent(intent, requestParcelKey);

        if (null != action && action.equals(requestActionKey))
        {
            String responseActionKey = intentPairs.getResponse().getActionKey();
            String responseParcelKey = intentPairs.getResponse().getParcelKey();

            // Run this on a separate thread in case "generateParameterSet()" runs any database operations on the UI thread.
            AsyncHelper.run(() -> {
                ParameterSet set = generateParameterSet(parcel);

                Intent sendIntent = new Intent();
                sendIntent.setAction(responseActionKey);
                sendIntent.putExtra(responseParcelKey, set);

                localBroadcastManager.sendBroadcast(sendIntent);
            });
        }
    }

    /**
     * This method is called when the Broadcast Receiver requests a customised ParameterSet from the application.
     * @return A ParameterSet which may (or may not) be customised.
     */
    public abstract ParameterSet generateParameterSet(@Nullable T parcel);


    private T getParcelFromIntent(Intent intent, String requestParcelKey)
    {
        T parcel = null;

        // Determine if the parcel exists in the intent at all
        if (intent.hasExtra(requestParcelKey))
        {
            // Get the type of generic class we're implementing. This is the type of parcel the customer app is EXPECTING to receive.
            Class<T> tClass = getClassOfGenericT();

            // Get the type of class specified in the intent configuration. This is the type of parcel the SDK is ACTUALLY sending.
            Class<? extends Parcelable> expectedClass = intentPairs.getRequest().getParcelClass();

            String tClassName = (null != tClass) ? tClass.getSimpleName() : null;
            String expectedClassName = (null != expectedClass) ? expectedClass.getSimpleName() : null;

            if (null == tClassName || !tClassName.equals(expectedClassName))
            {
                Timber.w("Unexpected generic class was specified. Expected %s but instead %s was implemented", expectedClassName, tClassName);
            }
            else
            {
                parcel = intent.getParcelableExtra(requestParcelKey);

                if (!parcel.getClass().equals(tClass))
                {
                    parcel = null;
                    Timber.w("Unexpected parcel received. Expected %s but instead %s was sent", expectedClassName, parcel.getClass());
                }
            }
        }

        return parcel;
    }

    private Class<T> getClassOfGenericT()
    {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
