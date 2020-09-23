package com.myfiziq.myfiziq_android.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

/**
 * Provides helper methods to easily show notifications in the StatusBus to the user.
 */
public class NotificationHelper
{
    private NotificationHelper()
    {
        // Empty hidden constructor for the utility class
    }

    public static NotificationManager getNotificationManager(Context context)
    {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    }
    /**
     * Shows a notification in the StausBar.
     * @param context The context.
     * @param icon The icon to show next to the notification.
     * @param channelTitle The title of the channel. This will appear as the Notification Category Title in the "App Info" screen for the app in Android.
     * @param title The title of the message in the notification (i.e. the first line in the notification).
     * @param message The message to show in the notification (i.e. the second line in the notification).
     */
    public static void showNotification(Context context, int notificationId, @DrawableRes int icon, String channelTitle, String title, String message, NotificationCompat.Action[] actions)
    {
        NotificationManager notificationManager = getNotificationManager(context);

        NotificationChannel channel;
        String channelId = channelTitle.toUpperCase();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            channel = new NotificationChannel(channelId, channelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        for(NotificationCompat.Action action : actions)
        {
            builder.addAction(action);
        }

        notificationManager.notify(notificationId, builder.build());
    }
}
