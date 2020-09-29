package com.myfiziq.sdk.manager;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallback;
import com.myfiziq.sdk.MyFiziqApiCallbackPayload;
import com.myfiziq.sdk.MyFiziqSdk;
import com.myfiziq.sdk.MyFiziqThreadFactory;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ResourceDownloadStatus;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.AwsUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java9.util.concurrent.Flow;
import timber.log.Timber;

/**
 * This class provides the primary SDK methods for MyFiziq.
 * <p>
 * Many of the methods in this class are asynchronous in that they will return a void result to the
 * caller immediately. Once processing has finished, the provided callback will be triggered.
 */
// The keep annotation ensures that Proguard doesn't obfuscate this class
@Keep
public class MyFiziqSdkManager
{
    private static final String TAG = MyFiziqSdkManager.class.getSimpleName();

    private static MyFiziqSdkManager mThis;


    // ----------------------------------------- START ------------------------------------------ //
    //
    // These instance variables must NEVER be declared as static
    // Otherwise they will not longer be a singleton
    // They should be accessed via the getInstance() method
    //
    // ------------------------------------------------------------------------------------------ //

    /**
     * A pool of threads to execute {@link MyFiziqSdkManager} operations on.
     */
    private ThreadPoolExecutor mThreadPool;

    /**
     * A handle representing the UI thread.
     */
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static MFZLifecycleGuard lifecycleGuard = new MFZLifecycleGuard();

    // ------------------------------------------------------------------------------------------ //
    // ----------------------------------------- END -------------------------------------------- //
    // ------------------------------------------------------------------------------------------ //


    public static synchronized MyFiziqSdkManager getInstance()
    {
        Timber.d("Getting SDK Manager instance");
        if (null == mThis)
        {
            mThis = new MyFiziqSdkManager();
        }

        return mThis;
    }

    public static boolean isLoaded()
    {
        // Do not call this through "getInstance()" which may do initialisation and crash the app if it isn't loaded.
        // isLoaded returns the result of a static variable, which is always a singleton.
        return MyFiziqSdk.isLoaded();
    }

    public static synchronized boolean isSdkInitialised()
    {
        return MyFiziq.getInstance().hasTokens();
    }

    private MyFiziqSdkManager()
    {
        //Timber.w(new Throwable(), "SDK Manager is initialising");

        // Boots up the C++ layer
        MyFiziq.getInstance();

        mThreadPool = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<>(),
                new MyFiziqThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Determines if the SDK has been setup in the C++ layer.
     */
    public static boolean isSdkSetup()
    {
        return MyFiziq.getInstance().isSdkSetup();
    }

    public static boolean isSignedIn()
    {
        return lifecycleGuard.isSignedIn();
    }

    public static boolean isReady()
    {
        return lifecycleGuard.isReady();
    }

    public static boolean isCaptureEnabled()
    {
        return MyFiziq.getInstance().isCaptureEnabled();
    }

    /**
     * Initialises the SDK the provided credentials.
     *
     * This method must be called before calling many other methods inside this class.
     *
     * @param secret The credential string to initialise the SDK with.
     * @param callback A callback once the SDK has been initialised.
     */
    public static void initialiseSdk(@NonNull String secret, @NonNull MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be initialised before we can use it to get the ModelKey
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqInitialisationService initialisationService = new MyFiziqInitialisationService();
            initialisationService.initialiseSdk(secret, lifecycleGuard, callback);
        });
    }

    /**
     * Initialises the SDK the provided credentials.
     *
     * This method must be called before calling many other methods inside this class.
     *
     * @param callback A callback once the SDK has been initialised.
     */
    public static void initialiseSdk(@NonNull MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be initialised before we can use it to get the ModelKey
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqInitialisationService initialisationService = new MyFiziqInitialisationService();
            initialisationService.initialiseSdk(lifecycleGuard, callback);
        });
    }

    /**
     * Starts the ThreadPool to start performing operations in the SDK.
     * <p>
     * The ThreadPool will automatically be started when the SDK is initialised, unless it is explicitly stopped.
     */
    public static void startWorkers()
    {
        ModelLog.i(TAG, "Starting ThreadPool.");

        MyFiziqSdkManager manager = getInstance();
        manager.mThreadPool = new ThreadPoolExecutor(
                2, 2, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<>(),
                new MyFiziqThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Stops the ThreadPool that performs operations in the SDK.
     * <p>
     * All actively executing tasks MAY be terminated soon. No new tasks will be started.
     */
    public static void stopWorkers()
    {
        ModelLog.i(TAG, "Stopping ThreadPool. All actively executing tasks MAY be terminated soon. No new tasks will be started.");

        // Force a ThreadPool stop
        getInstance().mThreadPool.shutdownNow();
    }

    public static void restartSdk()
    {
        MyFiziq.getInstance().restartSdk();
    }


    /**
     * Triggers the requested callback on the UI thread.
     *
     * This is necessary to prevent the callback from being made on the {@link MyFiziqSdkManager}
     * executor pool. If lots of callbacks are made on the executor pool and don't finish processing
     * in a timely manner, it could starve the executor pool of available threads.
     *
     * @param callback The callback to make.
     * @param code An {@link SdkResultCode} representing the status of the operation.
     * @param result A string providing further details of the status.
     */
    void postCallback(@Nullable MyFiziqApiCallback callback, SdkResultCode code, String result)
    {
        if (null != callback)
        {
            mHandler.post(() -> callback.apiResult(code, result));
        }
    }

    /**
     * Triggers the requested callback on the UI thread.
     *
     * This is necessary to prevent the callback from being made on the {@link MyFiziqSdkManager}
     * executor pool. If lots of callbacks are made on the executor pool and don't finish processing
     * in a timely manner, it could starve the executor pool of available threads.
     *
     * @param callback The callback to make.
     * @param code An {@link SdkResultCode} representing the status of the operation.
     * @param result A string providing further details of the status.
     */
    // Identical to "postCallback()" except this allows us to make callbacks without initialising the JNI layer
    static void postCallbackStatic(@Nullable MyFiziqApiCallback callback, SdkResultCode code, String result)
    {
        if (null != callback)
        {
            new Handler(Looper.getMainLooper()).post(() -> callback.apiResult(code, result));
        }
    }

    /**
     * Triggers the requested callback on the UI thread.
     *
     * This is necessary to prevent the callback from being made on the {@link MyFiziqSdkManager}
     * executor pool. If lots of callbacks are made on the executor pool and don't finish processing
     * in a timely manner, it could starve the executor pool of available threads.
     *
     * @param callback The callback to make.
     * @param code An {@link SdkResultCode} representing the status of the operation.
     * @param result A string providing further details of the status.
     * @param payload A payload to return.
     */
    <T> void postCallback(@Nullable MyFiziqApiCallbackPayload<T> callback, SdkResultCode code, String result, @Nullable T payload)
    {
        if (null != callback)
        {
            mHandler.post(() -> callback.apiResult(code, result, payload));
        }
    }

    /**
     * Triggers the requested callback on the UI thread.
     *
     * This is necessary to prevent the callback from being made on the {@link MyFiziqSdkManager}
     * executor pool. If lots of callbacks are made on the executor pool and don't finish processing
     * in a timely manner, it could starve the executor pool of available threads.
     *
     * @param callback The callback to make.
     * @param code An {@link SdkResultCode} representing the status of the operation.
     * @param result A string providing further details of the status.
     * @param payload A payload to return.
     */
    // Identical to "postCallback()" except this allows us to make callbacks without initialising the JNI layer
    static <T> void postCallbackStatic(@Nullable MyFiziqApiCallbackPayload<T> callback, SdkResultCode code, String result, @Nullable T payload)
    {
        if (null != callback)
        {
            new Handler(Looper.getMainLooper()).post(() -> callback.apiResult(code, result, payload));
        }
    }

    /* ************************************************************************ */
    /* ************************ APPLICATION SPECIFIC CALLS ******************** */
    /* ************************************************************************ */

    /**
     * Returns whether the SDK configuration is empty and needs to be initialised with
     * {@link #assignConfiguration(String, String, MyFiziqApiCallback)}
     *
     * @param callback The callback that will be executed with the result of whether the stored
     *                 SDK configuration is currently empty.
     */
    public static void isConfigurationEmpty(MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            ModelAppConf conf = ModelAppConf.getInstance();

            if (conf == null)
            {
                Timber.w(new Throwable(), "SDK configuration is empty");
                mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, null);
            }
            else
            {
                mgr.postCallback(callback, SdkResultCode.SUCCESS, null);
            }
        });
    }

    /**
     * Gets the application's configuration and assigns it to the SDK.
     *
     * @param key        A token representing the environment's configuration. It contains the URL, App Id, Vendor Id and Client ID.
     * @param callback A callback after the operation is performed.
     */
    public static void assignConfiguration(String key, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            MyFiziqConfigurationAssignerService configurationAssignerService = new MyFiziqConfigurationAssignerService();
            configurationAssignerService.assignConfiguration(key, lifecycleGuard, callback);
        });
    }

    /**
     * Gets the application's configuration and assigns it to the SDK.
     *
     * @param key        A token representing the environment's configuration. It contains the URL, App Id, Vendor Id and Client ID.
     * @param environment  The name of the environment to get the application configuration for.
     * @param callback A callback after the operation is performed.
     */
    public static void assignConfiguration(String key, String environment, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            MyFiziqConfigurationAssignerService configurationAssignerService = new MyFiziqConfigurationAssignerService();
            configurationAssignerService.assignConfiguration(key, environment, lifecycleGuard, callback);
        });
    }

    public static boolean refreshUserSessionSync()
    {
        // AWS must be setup before we can refresh a user's session
        lifecycleGuard.assertConfigurationAssigned();

        // Try to unlock the database, just in case...
        AwsUtils.tryToUnlockDatabase();

        try
        {
            MyFiziqUserService userService = new MyFiziqUserService();
            String username = AwsUtils.getUsername();

            if (!TextUtils.isEmpty(username))
            {
                userService.refresh();
            }

            return userService.checkUserSession();
        }
        catch (Exception e)
        {
            Timber.e("Cannot refresh user session");
            return false;
        }
    }

    /**
     * Checks to see if we have a valid session for the given username.
     * <p>
     * This method is synchronous.
     *
     * @return True if we have a valid session. False if we do not.
     */
    public static boolean checkUserSession()
    {
        // AWS must be setup before we can check to see if a user session is valid
        lifecycleGuard.assertConfigurationAssigned();

        MyFiziqUserService userService = new MyFiziqUserService();
        return userService.checkUserSession();
    }

    /**
     * Registers the user asynchronously.
     *
     * @param username    The username to register with.
     * @param password    The password to register with.
     * @param userProfile The user's profile to register with.
     * @param callback    A callback that will be made once the operation has succeeded or failed.
     */
    public static void register(String username, String password, ModelUserProfile userProfile, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can register
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqRegistrationService registrationService = new MyFiziqRegistrationService();
            registrationService.register(MyFiziq.getInstance().getTokenCid(), username, password, userProfile, lifecycleGuard, callback);
        });
    }

    /**
     * Gets the user's profile asynchronously.
     *
     * @param callback A callback that will be made once the operation has succeeded or failed. If successful, the callback will also include ModelUserProfileCache.
     */
    public static void getUserProfile(MyFiziqApiCallbackPayload<ModelUserProfile> callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // We need to be logged in to get a user's profile, but we don't need to have the SDK initialised
            lifecycleGuard.assertConfigurationAssigned();
            lifecycleGuard.assertSignedIn();

            MyFiziqUserProfileService userProfileService = new MyFiziqUserProfileService();
            userProfileService.getUser(callback);
        });
    }

    /**
     * Updates the user's profile asynchronously.
     *
     * @param userProfile The new user profile.
     * @param callback    A callback that will be made once the operation has succeeded or failed.
     */
    public static void updateUserProfile(ModelUserProfile userProfile, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // We need to be logged in to update a user's profile, but we don't need to have the SDK initialised
            lifecycleGuard.assertConfigurationAssigned();
            lifecycleGuard.assertSignedIn();

            MyFiziqUserProfileService userProfileService = new MyFiziqUserProfileService();
            userProfileService.updateUser(userProfile, callback);
        });
    }

    /**
     * Signs in the user asynchronously.
     *
     * @param username The username to sign in with.
     * @param password The password to sign in with.
     * @param callback A callback that will be made once the operation has succeeded or failed.
     */
    public static void signIn(String username, String password, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can sign in
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserService userService = new MyFiziqUserService();
            userService.signIn(username, password, lifecycleGuard, callback);
        });
    }

    /**
     * Signing in using custom authentication is for integration to environments that don't provide
     * an AWS Cognito compatible idP such as Open ID Connect, SAML, or OAuth.
     *
     * This basically authorises a user using a set of user claims, by either logging in the user (if already exists),
     * or registering the user first and then logging the user in.
     *
     * A user logout call will be done first, so that the intended user will be logged in when this method completes, regardless of current state.
     *
     * Note: This is NOT a recommended solution, but provides an alternative where an integration has some non-standards based user auth implementation.
     *
     * @param partnerUserId The partner User ID (not the same as MyFiziq User ID).
     * @param claims An array of claims unique to the user and will not change (e.g. registration date).
     *               Likewise, the order must not change. Therefore, don’t include the email, as the user could change this.
     * @param salt The initiation vector (or salt) for signing the user. In the hex format "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee".
     * @param callback A callback that will be made once the operation has succeeded or failed.
     */
    public static void userCustomAuthenticateForId(String partnerUserId, List<String> claims, String salt, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can sign in
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserClaimsService userClaimsService = new MyFiziqUserClaimsService();
            userClaimsService.signInWithClaims(partnerUserId, claims, salt, lifecycleGuard, callback);
        });
    }

    /**
     * Determines if the user is currently signed in using claims.
     *
     * @param partnerUserId The partner User ID (not the same as MyFiziq User ID).
     * @param claims An array of claims unique to the user and will not change (e.g. registration date).
     *               Likewise, the order must not change. Therefore, don’t include the email, as the user could change this.
     * @param salt The initiation vector (or salt) for signing the user. In the hex format “aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee”.
     * @param callback A callback that will be made once the operation has succeeded or failed.
     */
    public static void isCurrentlySignedInWithClaims(String partnerUserId, List<String> claims, String salt, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can call API methods on it
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserClaimsService userClaimsService = new MyFiziqUserClaimsService();
            userClaimsService.isSignedInWithClaims(partnerUserId, callback);
        });
    }

    /**
     * Signs out the user synchronously.
     */
    public static void signOut(MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can sign out
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserService userService = new MyFiziqUserService();
            userService.signOut(callback);
        });
    }

    public static void requestPasswordResetWithEmail(String username, MyFiziqApiCallbackPayload<UserCodeDeliveryDetails> callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can request a password reset
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserService userService = new MyFiziqUserService();
            userService.resetPasswordWithEmail(username, callback);
        });
    }


    public static void resetPasswordWithResetCode(String resetCode, String newPassword, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // AWS must be setup before we can request a password reset with a reset code
            lifecycleGuard.assertConfigurationAssigned();

            MyFiziqUserService userService = new MyFiziqUserService();
            userService.resetPasswordWithCode(resetCode, newPassword, callback);
        });
    }

    public static void uploadAvatarZips(ModelAvatar avatar, MyFiziqApiCallback callback)
    {
        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            // We must be logged in to upload avatar ZIP files
            lifecycleGuard.assertReady();

            MyFiziqAvatarUploadService service = new MyFiziqAvatarUploadService();
            service.uploadAvatarZips(avatar, callback);
        });
    }

    /**
     * Downloads an avatar asynchronously from the remote server. This should be called after the avatar has finished
     * processing remotely.
     *
     * @param avatar The avatar to download.
     * @param callback A callback that will be made once the operation has succeeded or failed.
     */
    public static void downloadAvatar(ModelAvatar avatar, MyFiziqApiCallback callback)
    {
        avatargetall(false, callback);
    }

    /**
     * Gets all avatars from the remote server asynchronously.
     *
     * @param bCacheMesh Whether the 3D avatar meshes should be immediately generated and cached or
     *                   if they should only be generated when needed. Note that generating them
     *                   immediately for many avatars may take a long time.
     * @param callback A callback once fetching all avatars from the remote server has been
     *                 completed.
     */
    public static void avatargetall(boolean bCacheMesh, MyFiziqApiCallback callback)
    {
        Timber.d("Getting avatars from remote server...");

        MyFiziqSdkManager mgr = getInstance();

        // The Lambda function invocation results in a network call
        // Make sure it is not called from the main thread
        mgr.mThreadPool.execute(() ->
        {
            // We must be signed in before we can download avatars
            lifecycleGuard.assertReady();

            ModelAppConf conf = ModelAppConf.getInstance();
            if (conf == null)
            {
                mgr.postCallback(callback, SdkResultCode.SDK_EMPTY_CONFIGURATION, null);
                return;
            }
            if (!TextUtils.isEmpty(conf.interface_url))
            {
                MyFiziq.getInstance().pollAvatars();

                // Recalculates the adjusted values for all avatars and saves them into the database
                //TODO: AP: remove: AdjustedValueCalculator.updateAllAvatarAdjustedValues();

                mgr.postCallback(callback, SdkResultCode.SUCCESS, null);
            }
            else
            {
                MyFiziqAvatarMetadataService service = new MyFiziqAvatarMetadataService();
                service.avatargetall(bCacheMesh, callback);
            }
        });
    }

    /**
     * Returns whether this device can run the MyFiziq SDK.
     *
     * This method is synchronous.
     */
    public static boolean isDeviceCompatible()
    {
        // Do not use getInstance() here. It will make JNI calls to initialise the SDK
        // which might not exist if we're running on an unsupported CPU architecture.
        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        return service.isDeviceCompatible();
    }

    public void uploadSupportData(String reason, String message, ModelAvatar avatar, AsyncHelper.Callback<Boolean> onComplete)
    {
        final MyFiziqSdkManager mgr = getInstance();

        mgr.mThreadPool.execute(() ->
        {
            MyFiziqAvatarSupportService service = new MyFiziqAvatarSupportService();
            service.uploadSupportData(reason, message, avatar, onComplete);
        });
    }

    /**
     * Uploads extended avatar support data asynchronously.
     *
     * @param bucketToUploadTo The S3 bucket to upload the information to.
     * @param avatar           The avatar to upload extended avatar support data for.
     * @param callback         The callback to trigger on either success or failure.
     */
    public static void uploadExtendedAvatarSupportData(@NonNull String bucketToUploadTo, @NonNull ModelAvatar avatar, @NonNull AsyncHelper.Callback<Boolean> callback)
    {
        final MyFiziqSdkManager mgr = getInstance();

        mgr.mThreadPool.execute(() ->
        {
            // We must be signed in before we can upload avatar extended support data
            lifecycleGuard.assertReady();

            MyFiziqAvatarSupportService service = new MyFiziqAvatarSupportService();
            service.uploadExtendedAvatarSupportInfo(bucketToUploadTo, avatar, callback);
        });
    }


    /**
     * Delete's an avatar from the remote server and on the user's device.
     *
     * @param modelAvatar The avatar to delete.
     * @param callback    A callback that will be made if the operation has succeeded or failed.
     */
    public static void deleteAvatar(ModelAvatar modelAvatar, MyFiziqApiCallbackPayload<Boolean> callback)
    {
        MyFiziqSdkManager mgr = getInstance();
        mgr.mThreadPool.execute(() ->
        {
            // We must be signed in before we can delete an avatar
            lifecycleGuard.assertReady();

            MyFiziqAvatarDeletionService service = new MyFiziqAvatarDeletionService();
            service.deleteAvatar(modelAvatar, callback);
        });
    }

    public static String computeEvoltPass(String evoltUserId, String evoltDateCreated)
    {
        return MyFiziq.getInstance().computeEvoltPass(evoltUserId, evoltDateCreated);
    }

    /**
     * Gets the Lifecycle Guard that can ensure that the MyFiziqSdkManager is in the right state.
     */
    public MFZLifecycleGuard getLifecycleGuard()
    {
        return lifecycleGuard;
    }

    public static void refreshLogin()
    {
        // Ensure AWS id token is valid before upload.
        // This will refresh the tokens as part of the method call
        UserStateDetails userStateDetails = AWSMobileClient.getInstance().currentUserState();

        if (userStateDetails.getUserState() == UserState.SIGNED_IN)
        {
            String idToken = userStateDetails.getDetails().get("token");
            AwsUtils.putIdToken(idToken);
        }


        // Ensure encrypted database is set-up
        AwsUtils.tryToUnlockDatabase();
    }
}
