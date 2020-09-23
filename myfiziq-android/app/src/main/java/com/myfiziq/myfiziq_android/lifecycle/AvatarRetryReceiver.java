package com.myfiziq.myfiziq_android.lifecycle;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.LoggingTree;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.NotificationHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import timber.log.Timber;

/**
 * This Broadcast Receiver listens for when the user presses the "Retry" button on the
 * avatar generation failure notification.
 *
 * It provides an example for how you can generate an avatar in the background without a user
 * interface present and how you can initialise the SDK.
 *
 * To test the SDK initialisation process, create a failed avatar and Force Close the MyFiziq app
 * (e.g. by swiping it away in the "Recent Apps" list on your Android device.
 *
 * When you press the "Retry" button in the Notification, it will initialise the SDK and start
 * regenerating the avatar.
 *
 * You can monitor its progress in Logcat.
 */
public class AvatarRetryReceiver extends BroadcastReceiver
{
    public static final String ACTION = "com.myfiziq.myfiziq_android.AvatarRetryReceiver";


    @Override
    public void onReceive(Context context, Intent intent)
    {
        LoggingTree.plantNewTree();

        AsyncHelper.run(() -> handleIntent(context, intent));
    }

    private void handleIntent(Context context, Intent intent)
    {
        Timber.i("Executing AvatarRetryReceiver");

        if (intent == null)
        {
            Timber.e("The received Intent was null. Avatar generation will not be retried.");
            return;
        }

        Parcelable parcel = intent.getParcelableExtra(String.valueOf(R.id.TAG_MODEL_ID));
        int notificationId = intent.getIntExtra(String.valueOf(R.id.TAG_NOTIFICATION_ID), -1);

        if (!(parcel instanceof ParameterSet))
        {
            Timber.e("Cannot get parcelable contained in the intent.");
            return;
        }

        if (notificationId > 0)
        {
            NotificationManager notificationManager = NotificationHelper.getNotificationManager(context);

            // Cancels/dismisses the notification now that the user has clicked on it
            notificationManager.cancel(notificationId);
        }

        ParameterSet parameterSet = (ParameterSet) parcel;
        Parameter param = parameterSet.getParam(R.id.TAG_MODEL_ID);

        if (param == null)
        {
            Timber.e("No Avatar ID was passed to the AvatarRetryReceiver.");
            return;
        }

        String avatarId = param.getValue();

        Timber.i("Received request to regenerate Avatar ID: %s", avatarId);

        AvatarRetryBackgroundProcessor processor = new AvatarRetryBackgroundProcessor();
        processor.retryAvatar(avatarId);
    }

    private void launchApp(Context context)
    {
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();

        Intent startIntent = packageManager.getLaunchIntentForPackage(packageName);

        startIntent.setFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        );

        context.startActivity(startIntent);
    }
}
