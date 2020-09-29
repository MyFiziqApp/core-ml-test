package com.myfiziq.sdk.lifecycle;

import android.os.Parcelable;

import com.myfiziq.sdk.enums.IntentResponses;

import java.util.EnumMap;

import androidx.annotation.Nullable;

public class PendingMessageRepository
{
    private static PendingMessageRepository instance = null;

    private EnumMap<IntentResponses, Parcelable> pendingMessages = new EnumMap<>(IntentResponses.class);


    private static PendingMessageRepository getInstance()
    {
        if (null == instance)
        {
            instance = new PendingMessageRepository();
        }

        return instance;
    }

    private PendingMessageRepository()
    {
        // Empty hidden constructor for the singleton
    }

    /**
     * Gets a pending message and removes it from the list of pending messages.
     *
     * This allows fragments to communicate with each other even though they might not be running.
     *
     * @param response The key of the message to receive.
     * @return An outstanding message, if any. If there is no outstanding message, null is returned.
     */
    @Nullable
    public static Parcelable getPendingMessage(IntentResponses response)
    {
        return getInstance().pendingMessages.remove(response);
    }

    /**
     * Determines whether there is a pending message for the given key.
     * @return Whether there is a pending message for the given key.
     */
    public static boolean hasPendingMessage(IntentResponses response)
    {
        return getInstance().pendingMessages.containsKey(response);
    }

    /**
     * Puts a message to be retrieved at a later point in time.
     * @param response The key to be associated with the messag.
     * @param message The message to be sent.
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    @Nullable
    public static Parcelable putMessage(IntentResponses response, Parcelable message)
    {
        return getInstance().pendingMessages.put(response, message);
    }

}
