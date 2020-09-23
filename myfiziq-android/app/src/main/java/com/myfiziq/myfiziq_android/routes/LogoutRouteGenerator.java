package com.myfiziq.myfiziq_android.routes;

import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.activities.ActivityEntrypoint;
import com.myfiziq.myfiziq_android.helpers.SignInHelper;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.SdkResultParcelable;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class LogoutRouteGenerator extends MyFiziqBroadcastReceiver
{
    public LogoutRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        return new ParameterSet.Builder(ActivityEntrypoint.class)
                .build();
    }

    /**
     * Executed when the user clicks on the "Logout" button.
     * @param activity The parent activity.
     * @param intentPair The intent that has been received and that we need to respond to.
     */
    public static void onLogoutClicked(Activity activity, IntentPairs intentPair)
    {
        IntentManagerService<SdkResultParcelable> intentManagerService = new IntentManagerService<>(activity);

        UiUtils.showAlertDialog(
                activity,
                "",
                activity.getString(com.myfiziq.sdk.R.string.myfiziqsdk_confirm_logout),
                activity.getString(com.myfiziq.sdk.R.string.myfiziqsdk_confirm),
                activity.getString(com.myfiziq.sdk.R.string.myfiziqsdk_cancel),
                (dialog, which) ->
                    delaySignoutIfSigningIn(activity, intentManagerService, intentPair),
                (dialog, which) -> {
                    SdkResultParcelable parcelable = new SdkResultParcelable(SdkResultCode.USER_CANCELLED);
                    intentManagerService.respond(intentPair.getResponse(), parcelable);
                }
        );
    }

    /**
     * Delays the signout if the user is signing in at the same time (probably in the background).
     */
    private static void delaySignoutIfSigningIn(Activity activity, IntentManagerService<SdkResultParcelable> intentManagerService, IntentPairs intentPair)
    {
        if (SignInHelper.getInstance().isRunning())
        {
            Timber.w("Background sign in is currently running. Pausing logout until background sign in has finished.");

            // If we're currently signing in, show a loading dialog which delays the prompt which asks you if you want to sign out
            String pleaseWaitString = activity.getString(R.string.please_wait);
            AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(activity, pleaseWaitString, true, false, null);

            // If the sign in running in the background, wait until it's done
            SignInHelper.getInstance().addListener((responseCode, result) ->
            {
                Timber.w("Background sign in has finished. Resuming logout.");

                dialog.dismiss();

                if (responseCode.isOk())
                {
                    // Signed in successfully, show the logout confirmation dialog
                    SdkResultParcelable parcelable = new SdkResultParcelable(SdkResultCode.SUCCESS);
                    intentManagerService.respond(intentPair.getResponse(), parcelable);
                }
                else
                {
                    // Unsuccessfully signed in, bail out
                    SdkResultParcelable parcelable = new SdkResultParcelable(responseCode, result);
                    intentManagerService.respond(intentPair.getResponse(), parcelable);
                }
            });
        }
        else
        {
            // No background sign in is happening at the moment, sign out immediately
            SdkResultParcelable parcelable = new SdkResultParcelable(SdkResultCode.SUCCESS);
            intentManagerService.respond(intentPair.getResponse(), parcelable);
        }
    }

}