package com.myfiziq.sdk.manager;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.MyFiziqApiCallbackPayload;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.util.AwsUtils;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Provides services to manager the user's profile
 */
// Package private. Should not be exposed to the customer app.
class MyFiziqUserProfileService
{
    /**
     * Gets a user's profile from the remote S3 profile store and performs checks against it to ensure that it is valid.
     *
     * @param callback The callback to trigger on success or failure.
     */
    void getUser(MyFiziqApiCallbackPayload<ModelUserProfile> callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        try
        {
            ModelUserProfile userProfile = getUserSync();

            if (userProfile == null)
            {
                mgr.postCallback(callback, SdkResultCode.AUTH_EMPTY_USER_DETAILS, "User Profile is empty", null);
                return;
            }

            mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success", userProfile);
        }
        catch (MyFiziqException exception)
        {
            Timber.e(exception);
            mgr.postCallback(callback, exception.getCode(), exception.getMessage(), null);
        }
        catch (Exception exception)
        {
            Timber.e(exception, "Cannot obtain user profile");
            mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot obtain user profile", null);
        }
    }

    /**
     * Updates a user's profile in the remote S3 user profile store.
     *
     * @param userProfile The user profile to update in the remote S3 user profile.
     * @param callback The callback to trigger on success or failure.
     */
    void updateUser(ModelUserProfile userProfile, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();

        try
        {
            updateUserSync(userProfile);

            mgr.postCallback(callback, SdkResultCode.SUCCESS, "Success");
        }
        catch (MyFiziqException exception)
        {
            Timber.e(exception);
            mgr.postCallback(callback, exception.getCode(), exception.getMessage());
        }
        catch (Exception exception)
        {
            Timber.e(exception, "Cannot update user profile");
            mgr.postCallback(callback, SdkResultCode.ERROR, "Cannot update user profile");
        }
    }

    /**
     * Obtain the user's profile from the remote server.
     *
     * @return The user's profile.
     * @throws MyFiziqException Throws MyFiziqException if an exception occurs that can be transmitted back to the calling method using
     *                          {@link MyFiziqSdkManager#postCallback(MyFiziqApiCallback, SdkResultCode, String)}.
     */
    @Nullable
    ModelUserProfile getUserSync() throws MyFiziqException
    {
        ModelAppConf conf = ModelAppConf.getInstance();
        if (null == conf)
        {
            Timber.e("ModelAppConf is null");
            throw new MyFiziqException(SdkResultCode.SDK_EMPTY_CONFIGURATION, "Failed to update user profile");
        }

        if (TextUtils.isEmpty(conf.interface_url))
        {
            Timber.e("Unknown interface URL");
            throw new MyFiziqException(SdkResultCode.ERROR);
        }


        MyFiziq mfz = MyFiziq.getInstance();

        //
        // !!!!!!!!! WARNING WARNING WARNING! !!!!!!!!!
        // !!!!!!!!!!!!!! DANGER DANGER !!!!!!!!!!!!!!!
        //
        // DO NOT CHANGE THE BELOW LINE TO ANYTHING EXCEPT FOR
        // "Integer responseCode = new Integer(0);"
        // THE IDE WILL TEMPT YOU. IT WILL TELL YOU IT CAN BE DONE MORE EFFICIENTLY, BUT DON'T BELIEVE IT
        // EVEN TRYING TO DO "Integer responseCode = 0" WILL CAUSE THE JNI LAYER TO BECOME
        // CORRUPTED IN AN UNIMAGINABLE WAY. FUTURE JNI CALLS WILL FAIL THAT HAVE NOTHING
        // TO DO WITH NETWORKING!
        //
        // Note, the responseCode is passed by reference to the C++ code.
        // The HTTP response will appear in this variable after the C++ code has performed the request.
        @SuppressLint("UseValueOf")
        Integer responseCode = new Integer(0);                                  // NOSONAR

        String endpoint = conf.interface_url + "/" + conf.env + "/profile/get";


        // Note, the C++ code puts the ModelUserProfile into the ModelUserProfile database table automatically
        // as part of "apiGet()" since we pass the ModelUserProfile.class parameter to it.

        String responseBody = mfz.apiGet(
                "",                 // WARNING! WARNING! WARNING! Don't specify a model name here. The ORM relies on userid being present as the primary key, which is an optional field in the profile. If it doesn't exist (e.g. iOS doesn't put it in) we won't be able to download the user profile and the user will get stuck. A primary key MUST exist in order to use the automapping on this line :(
                endpoint,
                responseCode,
                0,
                0,
                FLAG.getFlags(FLAG.FLAG_NOBASE, FLAG.FLAG_RESPONSE)
        );

        Timber.i("Received user profile from server.");


        SdkResultCode resultCode = SdkResultCode.valueOfHttpCode(responseCode);

        if (resultCode.isInternetDown())
        {
            Timber.e("Cannot get user profile. Internet down.");
            throw new MyFiziqException(SdkResultCode.NO_INTERNET, "Internet down");
        }
        else if (!resultCode.isOk())
        {
            Timber.e("Cannot get user profile. Received HTTP Code: %s. Message: %s", resultCode, responseBody);
            throw new MyFiziqException(SdkResultCode.ERROR);
        }
        // WARNING! DON'T RELY ON THE HTTP RESPONSE CODE. IT CAN RETURN HTTP 200 EVEN WHEN THERE IS AN ERROR LIKE BELOW.
        else if (responseContainsErrorXml(responseBody) && responseBody.contains("NoSuchKey"))
        {
            Timber.i("User profile does not exist. Creating a new profile");

            // The user profile doesn't exist! Create a brand new one
            ModelUserProfile userProfile = Orm.newModel(ModelUserProfile.class);

            injectUserIdIfEmpty(userProfile);

            return userProfile;
        }
        // WARNING! DON'T RELY ON THE HTTP RESPONSE CODE. IT CAN RETURN HTTP 200 EVEN WHEN THERE IS AN ERROR LIKE BELOW.
        else if (responseContainsErrorXml(responseBody))
        {
            Timber.e("Cannot get user profile. Received unrecognised error from server. Received HTTP Code: %s. Message: %s", resultCode, responseBody);
            throw new MyFiziqException(SdkResultCode.ERROR, "Received unrecognised error from server");
        }
        else
        {
            try
            {
                ModelUserProfile userProfile = Orm.newModel(ModelUserProfile.class);
                userProfile.deserialize(responseBody);

                injectUserIdIfEmpty(userProfile);

                return userProfile;
            }
            catch (Exception e)
            {
                Timber.e("Cannot parse received user profile");
                throw new MyFiziqException(SdkResultCode.ERROR, "Cannot parse received user profile");
            }
        }
    }

    /**
     * Updates a user's profile in the remote S3 user profile store.
     *
     * @param userProfile The user profile to update in S3 remotely.
     * @throws MyFiziqException Throws MyFiziqException if an exception occurs that can be transmitted back to the calling method using
     *                          {@link MyFiziqSdkManager#postCallback(MyFiziqApiCallback, SdkResultCode, String)}.
     */
    void updateUserSync(ModelUserProfile userProfile) throws MyFiziqException
    {
        ModelAppConf conf = ModelAppConf.getInstance();
        if (null == conf)
        {
            Timber.e(new Throwable(), "ModelAppConf is null");
            throw new MyFiziqException(SdkResultCode.SDK_EMPTY_CONFIGURATION, "Failed to update user profile");
        }

        if (TextUtils.isEmpty(conf.interface_url))
        {
            Timber.e("Unknown interface URL");
            throw new MyFiziqException(SdkResultCode.ERROR);
        }

        injectUserIdIfEmpty(userProfile);

        String jsonBody = userProfile.serialize();

        MyFiziq mfz = MyFiziq.getInstance();


        //
        // !!!!!!!!! WARNING WARNING WARNING! !!!!!!!!!
        // !!!!!!!!!!!!!! DANGER DANGER !!!!!!!!!!!!!!!
        //
        // DO NOT CHANGE THE BELOW LINE TO ANYTHING EXCEPT FOR
        // "Integer responseCode = new Integer(0);"
        // THE IDE WILL TEMPT YOU. IT WILL TELL YOU IT CAN BE DONE MORE EFFICIENTLY, BUT DON'T BELIEVE IT
        // EVEN TRYING TO DO "Integer responseCode = 0" WILL CAUSE THE JNI LAYER TO BECOME
        // CORRUPTED IN AN UNIMAGINABLE WAY. FUTURE JNI CALLS WILL FAIL THAT HAVE NOTHING
        // TO DO WITH NETWORKING!
        //
        // Note, the responseCode is passed by reference to the C++ code.
        // The HTTP response will appear in this variable after the C++ code has performed the request.
        @SuppressLint("UseValueOf")
        Integer responseCode = new Integer(0);                                      // NOSONAR

        String endpoint = conf.interface_url + "/" + conf.env + "/profile/update";

        String responseBody = mfz.apiPut(
                "",
                endpoint,
                responseCode,
                FLAG.getFlags(FLAG.FLAG_NOBASE, FLAG.FLAG_DATA, FLAG.FLAG_RESPONSE),
                jsonBody
        );

        SdkResultCode resultCode = SdkResultCode.valueOfHttpCode(responseCode);

        if (resultCode.isInternetDown())
        {
            Timber.e("Cannot update user profile. Internet down.");
            throw new MyFiziqException(SdkResultCode.NO_INTERNET, "Internet down");
        }
        else if (!resultCode.isOk())
        {
            Timber.e("Cannot update user profile. Received HTTP Code: %s. Message: %s", resultCode, responseBody);
            throw new MyFiziqException(SdkResultCode.ERROR);
        }
        // WARNING! DON'T RELY ON THE HTTP RESPONSE CODE. IT CAN RETURN HTTP 200 EVEN WHEN THERE IS AN ERROR LIKE BELOW.
        else if (responseContainsErrorXml(responseBody))
        {
            Timber.e("Cannot update user profile. Received unrecognised error from server. Received HTTP Code: %s. Message: %s", resultCode, responseBody);
            throw new MyFiziqException(SdkResultCode.ERROR, "Received unrecognised error from server");
        }

        Timber.i("Successfully updated user profile.");
    }

    /**
     * Injects the User ID from the JWT token if it's not present in the User Profile.
     */
    private void injectUserIdIfEmpty(@Nullable ModelUserProfile userProfile)
    {
        if (userProfile != null &&
                (userProfile.getUserId() == null || userProfile.getUserId() == 0))
        {
            Timber.i("User ID is empty in ModelUserProfile. Injecting User ID into it from the JWT token.");

            // Ensure that we ALWAYS have a User ID set
            String userIdString = AwsUtils.getCognitoUsernameNumber();

            try
            {
                // Do this safely in a try-catch in case the User ID isn't a number for whatever reason
                Integer userId = Integer.parseInt(userIdString);
                userProfile.setUserId(userId);
            }
            catch (NumberFormatException e)
            {
                Timber.e("Cannot parse User ID. It's not an integer. Tried to parse: '%s'", userIdString);
            }
        }
    }

    /**
     * Checks to see if the HTTP response body from the server contains an XML node called "Error">
     */
    private boolean responseContainsErrorXml(String response)
    {
        return response != null && response.contains("<Error>");
    }
}
