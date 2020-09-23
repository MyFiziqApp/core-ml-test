package com.myfiziq.sdk.intents;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;

import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentRequests;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.lifecycle.PendingMessageRepository;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

/**
 * Manages intents sent between the MyFiziq SDK and the application.
 * @param <T> The type of data we're managing in this lifecycle.
 */
// TODO Rename this to something more meaningful.
// TODO Make these methods static?
// TODO Move generic from class level to method level? Or specify in the method signature? (e.g. Class<T> ???)
public class IntentManagerService<T>
{
    private WeakReference<Activity> activity;
    private LocalBroadcastManager localBroadcastManager;

    private List<BroadcastReceiver> broadcastReceiverList = new LinkedList<>();

    public IntentManagerService(Context context)
    {
        this.activity = null;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public IntentManagerService(Activity activity)
    {
        this.activity = new WeakReference<>(activity);
        this.localBroadcastManager = LocalBroadcastManager.getInstance(activity.getApplication());
    }

    /**
     * Unbinds all listeners to ensure there's no memory leaks.
     *
     * This should be called in the onDestroy method.
     */
    public void unbindAll()
    {
        Iterator<BroadcastReceiver> iterator = broadcastReceiverList.iterator();

        while (iterator.hasNext())
        {
            BroadcastReceiver broadcastReceiver = iterator.next();

            localBroadcastManager.unregisterReceiver(broadcastReceiver);

            // Remove the unbound Broadcast Receiver from the list of registered receivers in case we
            // want to re-use this object again for some reason.
            iterator.remove();
        }
    }

    /**
     * Broadcasts an intent requesting information.
     * @param action The intent action to perform.
     */
    public void request(IntentRequests action)
    {
        request(action, null);
    }

    /**
     * Broadcasts an intent requesting information.
     * @param parcel A parcel to send with the intent.
     */
    public void request(IntentRequests request, Parcelable parcel)
    {
        if (null != parcel && !parcel.getClass().equals(request.getParcelClass()))
        {
            if (request.getParcelClass() != null)
            {
                Timber.w(
                        "The class of the parcel we're sending is %s but we're actually expecting to send %s. No parcel will be sent!",
                        parcel.getClass().getSimpleName(),
                        request.getParcelClass().getSimpleName()
                );
            }
            else
            {
                Timber.w(
                        "The class of the parcel we're sending is %s but we're actually expecting to send a different one. No parcel will be sent!",
                        parcel.getClass().getSimpleName()
                );
            }

            parcel = null;
        }

        String requestActionKey = request.getActionKey();
        String requestParcelKey = request.getParcelKey();

        Intent sendIntent = new Intent();
        sendIntent.setAction(requestActionKey);

        if (null != parcel)
        {
            sendIntent.putExtra(requestParcelKey, parcel);
        }

        localBroadcastManager.sendBroadcast(sendIntent);
    }

    public void respond(IntentResponses response, Parcelable parcel)
    {
        respond(response, parcel, false);
    }

    public void respond(IntentResponses response, Parcelable parcel, boolean bPending)
    {
        String responseActionKey = response.getActionKey();
        String responseParcelKey = response.getParcelKey();

        Intent sendIntent = new Intent();
        sendIntent.setAction(responseActionKey);

        if (null != parcel && null != responseParcelKey)
        {
            sendIntent.putExtra(responseParcelKey, parcel);
        }

        if (!bPending)
        {
            localBroadcastManager.sendBroadcast(sendIntent);
        }
        else
        {
            if (activity == null)
            {
                throw new IllegalStateException("Cannot put a pending message without passing an activity in the constructor.");
            }

            PendingMessageRepository.putMessage(response, parcel);
        }
    }

    /**
     * Listens for an intent from the customer app.
     *
     * We will continue to listen until {@link #unbindAll} has been called.
     *
     * @param request A key that represents the type of request (i.e. the Intent "Action" in Android).
     * @param callback A callback to be executed with the data received being passed in as a parameter.
     *                 If we received a response but either no data or invalid data was recevied, null is supplied.
     */
    public void listenIndefinitely(IntentRequests request, AsyncHelper.Callback<T> callback)
    {
        listen(request, callback, false);
    }

    /**
     * Listens for an intent from the customer app.
     *
     * Once a request has been received, we will stop listening.
     *
     * @param request A key that represents the type of request (i.e. the Intent "Action" in Android).
     * @param callback A callback to be executed with the data received being passed in as a parameter.
     *                 If we received a response but either no data or invalid data was recevied, null is supplied.
     */
    public void listenOnce(IntentRequests request, AsyncHelper.Callback<T> callback)
    {
        listen(request, callback, true);
    }

    /**
     * Listens for an intent from the customer app.
     *
     * We will continue to listen until {@link #unbindAll} has been called.
     *
     * @param response A key that represents the type of response (i.e. the Intent "Action" in Android).
     * @param callback A callback to be executed with the data received being passed in as a parameter.
     *                 If we received a response but either no data or invalid data was recevied, null is supplied.
     */
    public void listenIndefinitely(IntentResponses response, AsyncHelper.Callback<T> callback)
    {
        listen(response, callback, false);
    }

    /**
     * Listens for an intent from the customer app.
     *
     * Once a response has been received, we will stop listening.
     *
     * @param response A key that represents the type of response (i.e. the Intent "Action" in Android).
     * @param callback A callback to be executed with the data received being passed in as a parameter.
     *                 If we received a response but either no data or invalid data was recevied, null is supplied.
     */
    public void listenOnce(IntentResponses response, AsyncHelper.Callback<T> callback)
    {
        listen(response, callback, true);
    }

    private void listen(IntentRequests request, AsyncHelper.Callback<T> callback, boolean unregisterAfterReceivingResponse)
    {
        String requestActionKey = request.getActionKey();
        String requestParcelKey = request.getParcelKey();

        listen(requestActionKey, requestParcelKey, callback, unregisterAfterReceivingResponse);
    }

    private void listen(IntentResponses response, AsyncHelper.Callback<T> callback, boolean unregisterAfterReceivingResponse)
    {
        String responseActionKey = response.getActionKey();
        String responseParcelKey = response.getParcelKey();

        listen(responseActionKey, responseParcelKey, callback, unregisterAfterReceivingResponse);
    }

    private void listen(String actionKey, String parcelKey, AsyncHelper.Callback<T> callback, boolean unregisterAfterReceivingResponse)
    {
        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (unregisterAfterReceivingResponse)
                {
                    localBroadcastManager.unregisterReceiver(this);
                }

                if (activity == null)
                {
                    Timber.w("Did not send the received message to the callback since the activity was null");
                    return;
                }

                if (null != intent)
                {
                    T result = intent.getParcelableExtra(parcelKey);
                    Activity activityInst = activity.get();
                    if (null != activityInst)
                    {
                        try
                        {
                        activityInst.runOnUiThread(() -> callback.execute(result));
                        }
                        catch (Exception e)
                        {
                            Timber.e(e);
                        }
                    }
                }
                else
                {
                    Activity activityInst = activity.get();
                    if (null != activityInst)
                    {
                        activityInst.runOnUiThread(() -> callback.execute(null));
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(actionKey);

        broadcastReceiverList.add(receiver);
        localBroadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * Request an action to be performed and listenOnce for the response.
     * @param intentPairs The type of action to be performed.
     * @param callback The callback to be executed once the action has been performed. The result (if any) is passed in as a parameter to the calblack.
     */
    public void requestAndListenForResponse(IntentPairs intentPairs, AsyncHelper.Callback<T> callback)
    {
        requestAndListenForResponse(intentPairs, null, callback);
    }

    /**
     * Request an action to be performed and listenOnce for the response.
     * @param intentPairs The type of action to be performed.
     * @param parcel A parcel to send with the intent.
     * @param callback The callback to be executed once the action has been performed. The result (if any) is passed in as a parameter to the calblack.
     */
    public void requestAndListenForResponse(IntentPairs intentPairs, Parcelable parcel, AsyncHelper.Callback<T> callback)
    {
        listenOnce(
                intentPairs.getResponse(),
                callback);

        request(intentPairs.getRequest(), parcel);
    }
}
