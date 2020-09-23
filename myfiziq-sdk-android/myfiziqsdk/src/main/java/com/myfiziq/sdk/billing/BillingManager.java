package com.myfiziq.sdk.billing;

import com.myfiziq.sdk.enums.BillingEventType;
import com.myfiziq.sdk.helpers.AsyncHelper;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;

/**
 * Manages billing events.
 *
 * @hide
 */
public class BillingManager
{
    private BillingManager()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Log a billing event.
     *
     * @param eventType The type of event to log.
     * @return A future that will be completed when the operation completes.
     */
    public static CompletableFuture<Void> logEvent(@NonNull BillingEventType eventType)
    {
        return logEvent(eventType, new HashMap<>());
    }

    /**
     * Log a billing event.
     *
     * @param eventType The type of event to log.
     * @param eventMisc Additional information that can be sent with the billing event.
     * @return A future that will be completed when the operation completes.
     */
    public static CompletableFuture<Void> logEvent(@NonNull BillingEventType eventType, @Nullable Map<String, String> eventMisc)
    {
        return logEvent(eventType.getEventId(), eventType.getEventSource().getId(), eventMisc);
    }

    /**
     * Log a billing event.
     *
     * @param eventId The ID of the event to log.
     * @param source The source of the billing event.
     * @param eventMisc Additional information that can be sent with the billing event.
     * @return A future that will be completed when the operation completes.
     */
    public static CompletableFuture<Void> logEvent(int eventId, @NonNull String source, @Nullable Map<String, String> eventMisc)
    {
        String loggingMessage = String.format("Logging billing event. ID: %s, Source: %s", eventId, source);

        if (eventMisc != null && !eventMisc.isEmpty())
        {
            loggingMessage = String.format("%s, Misc: %s", loggingMessage, eventMisc.toString());
        }

        loggingMessage += ".";

        Timber.i(loggingMessage);


        CompletableFuture<Void> future = new CompletableFuture<>();

        AsyncHelper.run(() ->
        {
            BillingFactory.logEvent(eventId, source, eventMisc);
            future.complete(null);
        });

        return future;
    }
}
