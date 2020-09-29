package com.myfiziq.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.JwtIdToken;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbFactory;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Handles basic helper functions used throughout the app.
 *
 * @hide
 */
public class AwsUtils
{
    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client sS3Client;
    private static TransferUtility sTransferUtility;
    private static AWSConfiguration sAWSConfiguration;

    /**
     * The number of milliseconds to wait for data to be transferred to S3 before giving up.
     */
    private static final int SOCKET_TIMEOUT_IN_MS = 300000;


    private AwsUtils()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Gets the username that's currently signed in.
     */
    @NonNull
    public static String getUsername()
    {
        // WARNING! WARNING! WARNING!
        // Don't use "AWSMobileClient.getInstance().getUsername();"
        // We no longer use it since it's really really slow which makes it bad when starting the app
        // Always assume the username comes from "ModelSetting.Setting.USERNAME" and make sure to update it
        //
        // If "AWSMobileClient.getInstance().getUsername();" and "ModelSetting.Setting.USERNAME"
        // becomes mismatched, it should fail when we do the background sign in using "SignInHelper"
        // and the user will be forced to login again
        return ModelSetting.getSetting(ModelSetting.Setting.USERNAME, "");
    }

    /**
     * Sets the username that the user is currently signed in as and persists it to the database.
     */
    public static void setUsername(@NonNull String username)
    {
        // WARNING! WARNING! WARNING!
        // Don't use "AWSMobileClient.getInstance().getUsername();"
        // We no longer use it since it's really really slow which makes it bad when starting the app
        // Always assume the username comes from "ModelSetting.Setting.USERNAME" and make sure to update it
        //
        // If "AWSMobileClient.getInstance().getUsername();" and "ModelSetting.Setting.USERNAME"
        // becomes mismatched, it should fail when we do the background sign in using "SignInHelper"
        // and the user will be forced to login again
        ModelSetting.putSetting(ModelSetting.Setting.USERNAME, username);
    }

    /**
     * Returns the Cognito username number for the current user.
     *
     * This is often referred to as the "cognito:username" field and is a numeric representation
     * for the currently signed in user.
     */
    // This returns a numeric representation of the username (I think this is an auto-incrementing primary key in AWS?)
    @Nullable
    public static String getCognitoUsernameNumber()
    {
        String idToken = getRawIdToken();
        return JwtUtils.getTokenItem(idToken, "cognito:username");
    }

    @NonNull
    public static String getProviderID()
    {
        ModelAppConf conf = ModelAppConf.getInstance();
        String providerId = "";

        if (conf != null)
        {
            providerId = "cognito-idp." + conf.awsregion + ".amazonaws.com/" + conf.userpool;
        }

        return providerId;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @return A default S3 client.
     */
    public static AmazonS3Client getS3Client()
    {
        if (sS3Client == null)
        {
            ModelAppConf conf = ModelAppConf.getInstance();

            if (null != conf)
            {
                ClientConfiguration clientConfiguration = new ClientConfiguration();
                clientConfiguration.setSocketTimeout(SOCKET_TIMEOUT_IN_MS);

                Region region = Region.getRegion(Regions.fromName(conf.awsregion));
                sS3Client = new AmazonS3Client(AWSMobileClient.getInstance(), region, clientConfiguration);
            }
        }

        return sS3Client;
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @return a TransferUtility instance
     */
    public static TransferUtility getTransferUtility()
    {
        Context context = GlobalContext.getContext();

        // Initialise TransferNetworkLossHandler to make the AWS SDK happy
        TransferNetworkLossHandler.getInstance(context);

        if (sTransferUtility == null)
        {
            sTransferUtility = TransferUtility
                    .builder()
                    .s3Client(getS3Client())
                    .context(context)
                    .build();
        }

        return sTransferUtility;
    }

    /**
     * Gets a JWT ID Token stored in the cache. This avoids making a call to AWS.
     *
     * Note, this method does not determine if the token is expired, nor does it try to refresh it.
     */
    @Nullable
    public static JwtIdToken getIdToken()
    {
        String rawIdToken = getRawIdToken();

        if (TextUtils.isEmpty(rawIdToken))
        {
            return null;
        }
        else
        {
            return JwtUtils.getJwtToken(rawIdToken);
        }
    }

    /**
     * Gets a raw JWT ID Token stored in the cache. This avoids making a call to AWS.
     *
     * Note, this method does not determine if the token is expired, nor does it try to refresh it.
     */
    public static String getRawIdToken()
    {
        return ModelSetting.getSetting(ModelSetting.Setting.ID_TOKEN, "");
    }

    /**
     * Stores an ID Token in the cache that can be quickly retrieved without having to invoke AWS Cognito or make any network calls.
     *
     * @param idToken The ID token to store.
     */
    public static void putIdToken(@Nullable String idToken)
    {
        if (idToken != null)
        {
            ModelSetting.putSetting(ModelSetting.Setting.ID_TOKEN, idToken);
        }
    }

    public static void clearIdToken()
    {
        ModelSetting.putSetting(ModelSetting.Setting.ID_TOKEN, "");
    }

    public static void tryToUnlockDatabase()
    {
        JwtIdToken idToken = AwsUtils.getIdToken();

        if (idToken == null)
        {
            Timber.i("Not ready to unlock database yet");
            return;
        }

        String email = idToken.getEmail();
        String sub = idToken.getSub();

        ORMDbFactory.getInstance().setPassword(email, sub);
    }

    @Nullable
    public static AWSConfiguration getAWSConfiguration()
    {
        if (sAWSConfiguration != null)
        {
            return sAWSConfiguration;
        }

        if (!MyFiziq.getInstance().hasTokens())
        {
            Timber.e("App tokens are empty");
            return null;
        }

        ModelAppConf conf = ModelAppConf.getInstance();

        if (conf == null)
        {
            Timber.e("Cannot deserialise ModelAppToken");
            return null;
        }

        JSONObject awsConfigJsonObject;
        String cognitoIdentityPoolId = conf.idpool;
        String cognitoIdentityRegion = conf.awsregion;

        String appClientId = MyFiziq.getInstance().getTokenCid();
        String appClientSecret = "";
        String cognitoUserPoolId = conf.userpool;
        String cognitoUserRegion = conf.awsregion;

        awsConfigJsonObject = getAWSConfigJSON(cognitoIdentityPoolId, cognitoIdentityRegion,
                cognitoUserPoolId, appClientId, appClientSecret, cognitoUserRegion);

        if (awsConfigJsonObject == null)
        {
            Timber.e("AWS Config is null");
            return null;
        }

        sAWSConfiguration = new AWSConfiguration(awsConfigJsonObject);
        return sAWSConfiguration;
    }

    @Nullable
    private static JSONObject getAWSConfigJSON(@Nullable String cognitoIdentityPoolId, @Nullable String cognitoIdentityRegion,
                                               @Nullable String cognitoUserPoolId,@Nullable String appClientId,
                                               @Nullable String appClientSecret, @Nullable String cognitoUserRegion)
    {
        try
        {
            //Identity Manager
            JSONObject identityManager = new JSONObject();
            identityManager.put("Default", null);

            JSONObject cognitoIdentityDefault = new JSONObject();
            cognitoIdentityDefault.put("PoolId", cognitoIdentityPoolId);
            cognitoIdentityDefault.put("Region", cognitoIdentityRegion);

            //Cognito Identity
            JSONObject cognitoIdentity = new JSONObject();
            cognitoIdentity.put("Default", cognitoIdentityDefault);

            JSONObject credentialsProvider = new JSONObject();
            credentialsProvider.put("CognitoIdentity", cognitoIdentity);

            //Cognito User Pool
            JSONObject cognitoUserDefault = new JSONObject();
            cognitoUserDefault.put("PoolId", cognitoUserPoolId);
            cognitoUserDefault.put("AppClientId", appClientId);
            cognitoUserDefault.put("AppClientSecret", appClientSecret);
            cognitoUserDefault.put("Region", cognitoUserRegion);

            JSONObject cognitoUserPool = new JSONObject();
            cognitoUserPool.put("Default", cognitoUserDefault);

            //AWS Configuration JSON
            JSONObject rootJSON = new JSONObject();
            rootJSON.put("IdentityManager", identityManager);
            rootJSON.put("CredentialsProvider", credentialsProvider);
            rootJSON.put("CognitoUserPool", cognitoUserPool);

            return rootJSON;

        }
        catch (JSONException e)
        {
            Timber.e("Unable to build AWSConfigJson");
            return null;
        }
    }
}