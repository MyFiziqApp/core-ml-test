package com.myfiziq.sdk.manager;

import android.content.Context;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;

import java.util.List;

import androidx.annotation.Nullable;
import timber.log.Timber;


/**
 * Provides services to authenticate a user based on claims.
 */
// Package private. Should not be exposed to the customer app.
class MyFiziqUserClaimsService
{
    void signInWithClaims(String partnerUserId, List<String> claims, String salt, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        String username = generateCustomUsernameFromId(partnerUserId);
        String password = generateCustomPasswordForId(partnerUserId, claims, salt);

        if (hasUsernameChanged(username))
        {
            // We return an error here instead of signing out the user since the signOut() method will cause the SdkManager to stop all threads, including this one!!!
            mgr.postCallback(callback, SdkResultCode.AUTH_SIGNED_IN_DIFFERENT_USER, "Currently signed in with a different user. Please sign out first.");
            return;
        }

        MyFiziqUserService userService = new MyFiziqUserService();

        userService.signIn(username, password, lifecycleGuard, (responseCode, result) ->
                AsyncHelper.run(() ->
                        signInResult(username, password, lifecycleGuard, responseCode, result, callback)
                )
        );
    }

    void isSignedInWithClaims(String partnerUserId, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        String username = generateCustomUsernameFromId(partnerUserId);

        if (isUsernameEmpty())
        {
            mgr.postCallback(callback, SdkResultCode.AUTH_SIGNED_OUT, "Not signed in");
        }
        else if (hasUsernameChanged(username))
        {
            mgr.postCallback(callback, SdkResultCode.AUTH_SIGNED_IN_DIFFERENT_USER, "Currently signed in with a different user. Please sign out first.");
        }
        else
        {
            mgr.postCallback(callback, SdkResultCode.SUCCESS, "");
        }
    }

    /**
     * Handles the result of signing in.
     * <p>
     * If the user does not exist, they will be registered using the supplied username and password.
     * Otherwise, the sign in result will be returned to the method tha made the API call.
     */
    private void signInResult(String username, String password, MFZLifecycleGuard lifecycleGuard, SdkResultCode responseCode, String result, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        if (responseCode == SdkResultCode.SDK_ERROR_USER_NOT_EXIST)//wrong password should fail! || responseCode == SdkResultCode.SDK_ERROR_WRONG_PASSWORD)
        {
            Timber.i("The user does not exist. Trying to register...");

            MyFiziqRegistrationService userRegisterService = new MyFiziqRegistrationService();

            ModelUserProfile userProfile = Orm.newModel(ModelUserProfile.class);

            userRegisterService.register(MyFiziq.getInstance().getTokenCid(), username, password, userProfile, lifecycleGuard, (responseCode1, result1) ->
                    mgr.postCallback(callback, responseCode1, result1)
            );
        }
        else
        {
            // If we have a result from the sign in process, and it has either succeeded or failed
            // (except for when the user doesn't exist),
            // return it to the method that made the API call.
            mgr.postCallback(callback, responseCode, result);
        }
    }

    @Nullable
    public String generateCustomUsernameFromId(String partnerUserId)
    {
        Context context = GlobalContext.getContext();

        MyFiziq myq = MyFiziq.getInstance();

        if (!myq.hasTokens())
        {
            Timber.e("App tokens not configured");
            return null;
        }

        return context.getResources().getString(R.string.customUsernameFormat, partnerUserId, myq.getTokenVid(), myq.getTokenAid());
    }

    public String generateCustomPasswordForId(String partnerUserId, List<String> claims, String salt)
    {
        StringBuilder data = new StringBuilder(partnerUserId);

        for (String claim : claims)
        {
            data.append("-").append(claim);
        }

        return MyFiziq.getInstance().performHmac(salt, data.toString());
    }

    private boolean hasUsernameChanged(String newUsername)
    {
        String existingUsernameInDb = AwsUtils.getUsername();

        Timber.i("Username in settings: '%s'", existingUsernameInDb);

        // Return false if the username doesn't match the one we have in either the AWSMobileClient or the database
        return !TextUtils.isEmpty(existingUsernameInDb) && !newUsername.equals(existingUsernameInDb);
    }

    private boolean isUsernameEmpty()
    {
        String existingUsernameInDb = AwsUtils.getUsername();

        if (TextUtils.isEmpty(existingUsernameInDb))
        {
            Timber.i("Blank username in DB: '%s'", existingUsernameInDb);
            return true;
        }
        else
        {
            return false;
        }
    }
}
