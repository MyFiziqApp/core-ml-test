package com.myfiziq.sdk.helpers;

import android.app.Activity;
import android.content.Context;

import com.myfiziq.sdk.R;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

public class DialogHelper
{
    /**
     * Shows a dialog indicating that there is no network connection available.
     *
     * @param activity The current activity.
     */
    public static void showInternetDownDialog(@NonNull Activity activity)
    {
        showInternetDownDialog(activity, () -> {});
    }

    /**
     * Shows a dialog indicating that there is no network connection available.
     *
     * @param activity The current activity.
     * @param onDialogClose A callback when the dialog is closed.
     */
    public static void showInternetDownDialog(@NonNull Activity activity, @NonNull AsyncHelper.CallbackVoid onDialogClose)
    {
        String title = activity.getString(R.string.myfiziqsdk_dialog_title_no_internet);
        String message = activity.getString(R.string.myfiziqsdk_dialog_message_no_internet);

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                {
                    dialog.dismiss();
                    onDialogClose.execute();
                })
                .show();
    }

    /**
     * Shows a dialog to the user.
     * @param context The current context.
     * @param titleResId A string resource that refers to the title of the dialog to use.
     * @param messageResId A string resource that refers to the message to use in the dialog.
     * @param onDialogClose A callback when the dialog is closed.
     */
    public static void showDialog(@NonNull Context context, @StringRes int titleResId, @StringRes int messageResId, @NonNull AsyncHelper.CallbackVoid onDialogClose)
    {
        String title = context.getString(titleResId);
        String message = context.getString(messageResId);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                {
                    dialog.dismiss();
                    onDialogClose.execute();
                })
                .show();
    }


}
