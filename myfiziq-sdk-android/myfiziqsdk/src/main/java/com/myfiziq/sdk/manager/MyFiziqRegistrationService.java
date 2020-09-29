package com.myfiziq.sdk.manager;

import android.text.TextUtils;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignInResult;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.billing.BillingManager;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ModelUserRegisterBody;
import com.myfiziq.sdk.db.ModelUserRegisterResponseBody;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.BillingEventType;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;

import timber.log.Timber;


// Package private. Should not be exposed to the customer app.
class MyFiziqRegistrationService
{
    /**
     * Provides a mechanism to register a user.
     * <p>
     * This method will trigger a series of async calls executed sequentially to p
     * <p>
     * <p>
     * NOTE! LIKE THE iOS IMPLEMENTATION, THIS OPERATION IS NOT TRANSACTIONAL!
     * <p>
     * IF NETWORK CONNECTIVITY IS LOST OR IF ONE OPERATION IS UNSUCCESSFUL,
     * THE USER MAY BE REGISTERED IN A CORRUPTED STATE (i.e. with the default password).
     * <p>
     * THEY MIGHT BE ABLE TO USE THE FORGOTTEN PASSWORD FEATURE TO RESET THEIR ACCOUNT.
     *
     * @param username    The username to register the user with.
     * @param password    The password the user has chosen to use.
     * @param userProfile The user's profile.
     * @param callback    The callback to execute once the user has successfully or unsuccessfully been registered.
     */
    // Package private. Should not be exposed to the customer app.
    void register(String clientId, String username, String password, ModelUserProfile userProfile, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback callback)
    {
        // The default password is the Client ID
        String defaultPassword = clientId;

        AwsUtils.setUsername(username);

        Runnable whenAllAreSuccessful = () -> onSuccess(callback);

        AsyncHelper.CallbackVoid logBillingEvent = () -> logBillingEvent(whenAllAreSuccessful, callback);
        AsyncHelper.CallbackVoid updateUserProfile = () -> updateUserProfile(userProfile, logBillingEvent, callback);
        AsyncHelper.CallbackVoid changePassword = () -> changePassword(defaultPassword, password, updateUserProfile, callback, lifecycleGuard);
        AsyncHelper.CallbackVoid signInUser = () -> signInUser(username, defaultPassword, changePassword, lifecycleGuard, callback);
        AsyncHelper.CallbackVoid signUpUser = () -> signUpUser(username, signInUser, callback);

        // Start registering the user
        signUpUser.execute();
    }

    /**
     * Signs up the user using a temporary password, which is sdkManager.mClientId.
     *
     * @param username        The username to register the user with.
     * @param nextMethod      The method to be executed after this operation has been completed successfully.
     * @param failureCallback The method to be executed if this operation is unsuccessful.
     */
    private void signUpUser(String username, AsyncHelper.CallbackVoid nextMethod, MyFiziqApiCallback failureCallback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        //ModelAppConf conf = ModelAppConf.getInstance();

        if (!MyFiziq.getInstance().hasTokens())
        {
            String message = "App tokens not configured";
            Timber.e(message);
            mgr.postCallback(failureCallback, SdkResultCode.SDK_EMPTY_CONFIGURATION, message);
            return;
        }

        ModelUserRegisterBody body = new ModelUserRegisterBody(MyFiziq.getInstance().getTokenAid(), username);

        String userRegisterPayload = body.serialize("id");
        String result = S3Helper.invoke("userregister", userRegisterPayload);

        if (TextUtils.isEmpty(result))
        {
            String message = "Received empty response from server when calling userregister.";
            Timber.e(message);
            mgr.postCallback(failureCallback, SdkResultCode.HTTP_NOT_FOUND, message);
        }

        ModelUserRegisterResponseBody responseBody = Orm.newModel(ModelUserRegisterResponseBody.class);

        if (responseBody == null || result == null)
        {
            String message = "Received an empty or invalid response from the server when calling userregister.";
            Timber.e(message);
            mgr.postCallback(failureCallback, SdkResultCode.HTTP_NOT_FOUND, message);
            return;
        }

        responseBody.deserialize(result);

        if (responseBody.body.status.equals("clash"))
        {
            String message = "Creating a new user was unsuccessful. The user already exists. Result: " + result;
            Timber.e(message);
            mgr.postCallback(failureCallback, SdkResultCode.HTTP_CONFLICT, message);
            return;
        }

        if (!responseBody.body.status.equals("new") || responseBody.statusCode != 200)
        {
            String message = "Creating a new user was unsuccessful. Result: " + result;
            Timber.e(message);

            SdkResultCode resultCode = SdkResultCode.valueOfHttpCode(responseBody.statusCode);
            mgr.postCallback(failureCallback, resultCode, message);

            return;
        }

        // Success
        nextMethod.execute();
    }

    /**
     * Logs in the user after signing up.
     *
     * @param username        The user that the user signed up with.
     * @param defaultPassword The default password that was used to sign up the user.
     * @param nextMethod      The method to be executed after this operation has been completed successfully.
     * @param failureCallback The method to be executed if this operation is unsuccessful.
     */
    private void signInUser(String username, String defaultPassword, AsyncHelper.CallbackVoid nextMethod, MFZLifecycleGuard lifecycleGuard, MyFiziqApiCallback failureCallback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        AWSMobileClient.getInstance().signIn(username, defaultPassword, null, new Callback<SignInResult>()
        {
            @Override
            public void onResult(final SignInResult signInResult)
            {
                Timber.d("Sign-in callback state: %s", signInResult.getSignInState());
                switch (signInResult.getSignInState())
                {
                    case DONE:
                    case NEW_PASSWORD_REQUIRED:
                        lifecycleGuard.setSignedIn(true);
                        nextMethod.execute();
                        break;
                    default:
                        Timber.e("Sign in failed");
                        mgr.postCallback(failureCallback, SdkResultCode.HTTP_UNAUTHORISED, " Sign in failed");
                        break;
                }
            }

            @Override
            public void onError(Exception e)
            {
                Timber.e(e, "Sign-in error");
                mgr.postCallback(failureCallback, SdkResultCode.ERROR, " Sign in failed");
            }
        });
    }

    /**
     * Changes the password for a user
     *
     * @param oldPassword     The default password that was used to sign up the user.
     * @param password        The new password to set for the user.
     * @param nextMethod      The method to be executed after this operation has been completed successfully.
     * @param failureCallback The method to be executed if this operation is unsuccessful.
     */
    private void changePassword(String oldPassword, String password, AsyncHelper.CallbackVoid nextMethod, MyFiziqApiCallback failureCallback, MFZLifecycleGuard lifecycleGuard)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        AWSMobileClient.getInstance().changePassword(oldPassword, password, new Callback<Void>()
        {
            @Override
            public void onResult(Void signInResult)
            {
                Timber.d("Sign-in callback state: Change Password Success");

                MyFiziqSdkManager.refreshLogin();

                try
                {
                    AWSMobileClient.getInstance().federatedSignIn(
                            AwsUtils.getProviderID(),
                            AwsUtils.getRawIdToken()
                    );
                }
                catch (Exception e)
                {
                    Timber.e(e, "Failed to assign the user a password");
                    mgr.postCallback(failureCallback, SdkResultCode.HTTP_ERROR, "Password change failed");
                }

                lifecycleGuard.setSignedIn(true);

                nextMethod.execute();
            }

            @Override
            public void onError(Exception exception)
            {
                Timber.e(exception, "Password change failed");
                mgr.postCallback(failureCallback, SdkResultCode.HTTP_ERROR, "Password change failed");
            }
        });
    }

    /**
     * Updates the User Profile for a newly created user.
     *
     * @param userProfile     The details for a user to set.
     * @param nextMethod      The method to be executed after this operation has been completed successfully.
     * @param failureCallback The method to be executed if this operation is unsuccessful.
     */
    private void updateUserProfile(ModelUserProfile userProfile, AsyncHelper.CallbackVoid nextMethod, MyFiziqApiCallback failureCallback)
    {
        try
        {
            MyFiziqUserProfileService userProfileService = new MyFiziqUserProfileService();
            userProfileService.updateUserSync(userProfile);

            nextMethod.execute();
        }
        catch (MyFiziqException exception)
        {
            Timber.e(exception, "Failed to register the user. Unable to update the user's profile. Code: %s. Message: %s", exception.getCode(), exception.getMessage());

            MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
            mgr.postCallback(failureCallback, exception.getCode(), exception.getMessage());
        }
    }

    /**
     * Logs the user registration billing event.
     *
     * @param nextMethod      The method to be executed after this operation has been completed successfully.
     * @param failureCallback The method to be executed if this operation is unsuccessful.
     */
    private void logBillingEvent(Runnable nextMethod, MyFiziqApiCallback failureCallback)
    {
        BillingManager.logEvent(BillingEventType.USER_REGISTERED);
        nextMethod.run();
    }


    /**
     * Called when all operations to register a user have been successful.
     */
    private void onSuccess(MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
        mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success");
    }
}
