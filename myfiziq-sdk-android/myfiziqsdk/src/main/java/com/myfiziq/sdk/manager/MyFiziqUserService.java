package com.myfiziq.sdk.manager;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignOutOptions;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.Token;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoNotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.ExpiredCodeException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.MyFiziqApiCallbackPayload;
import com.myfiziq.sdk.MyFiziqAvatarDownloadManager;
import com.myfiziq.sdk.billing.BillingManager;
import com.myfiziq.sdk.db.ORMDbCache;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.enums.BillingEventType;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.FilesystemHelpers;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import androidx.work.WorkInfo;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;


/**
 * Provides services to retrieve a user.
 */
// Package private. Should not be exposed to the customer app.
class MyFiziqUserService
{
    /**
     * Checks to see if we have a valid session for the given username.
     *
     * This method is synchronous.
     *
     * @return True if we have a valid session. False if we do not.
     */
    boolean checkUserSession()
    {
        UserState userState = AWSMobileClient.getInstance().currentUserState().getUserState();

        switch (userState) {
            case SIGNED_IN:
                return true;
            case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
            case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
            case GUEST:
            case SIGNED_OUT:
                return false;
            default:
                throw new IllegalStateException("Unknown user state, please report this exception");
        }
    }

    /**
     * Sign in through AWSMobile API with callback
     * @param username name of user
     * @param password user password
     * @param callback desired callback after login success/failed
     */
    void signIn(String username, String password, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        // If another SDK method checks to see if we're signed in or not, pause that SDK call until
        // this method has finished.
        lifecycleGuard.lockForSignIn();
        CompletableFuture<Void> unlockSignInFuture = new CompletableFuture<>();

        AwsUtils.setUsername(username);

        // Don't bother with ".currentUserState();" or ".refresh()" to refresh the token since
        // they take the same amount of time as signIn().
        // No need to have 2 different sets of code which take the same amount of time and have the same end result.
        AWSMobileClient.getInstance().signIn(username, password, null, new Callback<SignInResult>()
        {
            @Override
            public void onResult(SignInResult result)
            {
                try
                {
                    Tokens tokens = AWSMobileClient.getInstance().getTokens();
                    Token idToken = tokens.getIdToken();
                    String idTokenString = idToken.getTokenString();

                    AwsUtils.putIdToken(idTokenString);

                    // Try to unlock the database as soon as we have valid credentials
                    AwsUtils.tryToUnlockDatabase();
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot get AWS tokens");

                    unlockSignInFuture.complete(null);

                    MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
                    mgr.postCallback(callback, SdkResultCode.ERROR, e.getMessage());

                    return;
                }

                try
                {
                    // ---------- WARNING WARNING WARNING! ----------
                    // Federated Sign In refers to a third party provider (e.g. Google, Facebook, AWS Cognito, etc).
                    // The ID Token has an EXPIRY TIME in it.
                    // We need to do federatedSignIn() each time the user opens the app, otherwise the
                    // remote federated user pool will eventually have an expired token.
                    // ... according to Phillip Cooper
                    // ---------- WARNING WARNING WARNING! ----------
                    AWSMobileClient.getInstance().federatedSignIn(
                            AwsUtils.getProviderID(),
                            AwsUtils.getRawIdToken()
                    );
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot perform federated sign in");

                    unlockSignInFuture.complete(null);

                    MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
                    mgr.postCallback(callback, SdkResultCode.ERROR, e.getMessage());

                    return;
                }

                if (!StringUtils.isEmpty(password))
                {
                    // If the password is not empty, the user is logging in from the login screen
                    // Otherwise, we're trying to refresh the token
                    BillingManager.logEvent(BillingEventType.USER_LOGIN);
                }

                // Sign-in is now fully successful. Ensure lifecycleGuard knows before posting success callback.
                lifecycleGuard.setSignedIn(true);

                unlockSignInFuture.complete(null);

                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
                mgr.postCallback(callback, SdkResultCode.SUCCESS, null);
            }

            @Override
            public void onError(Exception exception)
            {
                Timber.e(exception, "Failed to sign in");

                Class<?> exceptionClass = exception.getClass();
                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

                if (exceptionClass.equals(UserNotFoundException.class))
                {
                    unlockSignInFuture.complete(null);
                    mgr.postCallback(callback, SdkResultCode.SDK_ERROR_USER_NOT_EXIST, exception.getMessage());
                }
                else if (exceptionClass.equals(CognitoNotAuthorizedException.class)
                        || exceptionClass.equals(com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException.class))
                {
                    unlockSignInFuture.complete(null);
                    mgr.postCallback(callback, SdkResultCode.SDK_ERROR_WRONG_PASSWORD, exception.getMessage());
                }
                else
                {
                    unlockSignInFuture.complete(null);
                    mgr.postCallback(callback, SdkResultCode.HTTP_INTERNAL_ERROR, exception.getMessage());
                }
            }
        });

        try
        {
            // BLOCK this thread until AWS has signed in and executed its callback.
            // Note that the AWS signIn() callback will get triggered on another thread that's managed
            // internally within AWS.
            // We want to keep this thread suspended until AWS has completed the sign in so we can unlock
            // the lock. If we try to unlock it from the AWS thread we will get a IllegalMonitorStateException
            // since unlock() is executed on a different thread.
            // We also don't want this thread to be made available in the MyFiziqSdkManager thread pool
            // where it executes another SDK call that is dependent on sign in being completed.
            // In that case, both SDK calls becomes blocked. We can't unlock from the original
            // thread since it is now suspended by the new lock. Therefore, we now have a deadlock.
            unlockSignInFuture.get();
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
        finally
        {
            // ALWAYS unlock the sign in, especially if an exception has occurred.
            // We don't want hold the sign in lock forever.
            lifecycleGuard.unlockSignIn();
        }
    }

    void refresh()
    {
        // Ensure AWS id token is valid before upload.
        // This will refresh the tokens as part of the method call
        UserStateDetails userStateDetails = AWSMobileClient.getInstance().currentUserState();

        if (userStateDetails.getUserState() == UserState.SIGNED_IN)
        {
            String idToken = userStateDetails.getDetails().get("token");
            AwsUtils.putIdToken(idToken);
        }
    }

    void signOut(MyFiziqApiCallback callback)
    {
        try
        {
            CompletableFuture<Void> future = BillingManager.logEvent(BillingEventType.USER_LOGOUT);

            // Wait until the billing event has completed before proceeding further
            future.get();
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception occurred when waiting for billing future to complete");
        }
        
        MyFiziqAvatarDownloadManager.getInstance().stopCheckingForAvatars();


        boolean downloadHoldSuccessful = waitUntilAvatarDownloadFinishes();

        if (!downloadHoldSuccessful)
        {
            MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

            Timber.e("Failed to hold while downloading avatars");

            // TODO Does the logout callback actually check for errors? What does it do when an error is encountered?
            mgr.postCallback(callback, SdkResultCode.ERROR, "");
            return;
        }


        SignOutOptions signOutOptions = SignOutOptions.builder().signOutGlobally(false).build();

        AWSMobileClient.getInstance().signOut(signOutOptions, new Callback<Void>()
        {
            @Override
            public void onResult(Void result)
            {
                AwsUtils.setUsername("");

                MyFiziqSdkManager.stopWorkers();
                ORMDbFactory.getInstance().signOut(GlobalContext.getContext());

                FilesystemHelpers.clearMyFiziqFiles(GlobalContext.getContext());

                ORMDbCache.getInstance().clearCache();

                MyFiziqSdkManager.restartSdk();
                MyFiziqSdkManager.startWorkers();

                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
                mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success");
            }

            @Override
            public void onError(Exception exception)
            {
                Timber.e(exception, "Failed to sign out");
                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

                if (exception.getClass().equals(UserNotFoundException.class))
                {
                    mgr.postCallback(callback, SdkResultCode.AUTH_EMPTY_USER, exception.getMessage());
                }
                else
                {
                    mgr.postCallback(callback, SdkResultCode.ERROR, exception.getMessage());
                }
            }
        });
    }

    void resetPasswordWithEmail(String username, MyFiziqApiCallbackPayload<UserCodeDeliveryDetails> callback)
    {
        AWSMobileClient.getInstance().forgotPassword(username, new Callback<ForgotPasswordResult>()
        {
            @Override
            public void onResult(ForgotPasswordResult result)
            {
                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

                switch (result.getState())
                {
                    case CONFIRMATION_CODE:
                        mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success", result.getParameters());
                        break;
                    default:
                        Timber.e("Unsupported forgot password state: '%s'", result.getState());
                        mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot reset password", null);
                        break;
                }
            }

            @Override
            public void onError(Exception e)
            {
                Timber.e(e, "Exception occurred when resetting password");

                MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
                mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot reset password", null);
            }
        });
    }

    void resetPasswordWithCode(String resetCode, String newPassword, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        AWSMobileClient.getInstance().confirmForgotPassword(newPassword, resetCode, new Callback<ForgotPasswordResult>()
        {
            @Override
            public void onResult(ForgotPasswordResult result)
            {
                switch (result.getState())
                {
                    case DONE:
                        mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success");
                        break;
                    default:
                        Timber.e("Un-supported forgot password state: '%s'", result.getState());
                        mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot reset password");
                        break;
                }
            }

            @Override
            public void onError(Exception exception)
            {
                Timber.e(exception, "Failed to reset password");

                if (exception instanceof ExpiredCodeException)
                {
                    mgr.postCallback(callback, SdkResultCode.AUTH_EXPIRED_TOKEN, "Token has expired");
                }
                else
                {
                    mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot reset password");
                }
            }
        });
    }

    private boolean waitUntilAvatarDownloadFinishes()
    {
        try
        {
            MyFiziqAvatarDownloadManager.getInstance().stopCheckingForAvatars();
            List<WorkInfo> runningDownloadWorkers = MyFiziqAvatarDownloadManager.getInstance().getRunningWorkers();

            for (WorkInfo runningDownloadWorker : runningDownloadWorkers)
            {
                if (!runningDownloadWorker.getState().isFinished())
                {
                    Timber.w("Avatar download worker is running! Pausing logout until download has finished.");

                    MyFiziqAvatarDownloadManager.getInstance().waitForWorkerCompletionSync(runningDownloadWorker.getId());

                    Timber.w("Avatar download worker has finished. Resuming logout.");
                }
            }

            return true;
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception occurred when waiting for avatars to finish downloading.");
            return false;
        }
    }
}
