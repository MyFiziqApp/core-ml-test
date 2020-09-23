package com.myfiziq.sdk.billing;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.ModelAWSFirehose;
import com.myfiziq.sdk.db.ModelAWSFirehosePayload;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelBillingEvent;
import com.myfiziq.sdk.db.ModelBillingEventContainer;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.TimeFormatUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Contains methods to log billing events.
 * <p>
 * This class should only be called from {@link BillingManager}.
 *
 * @hide
 */
class BillingFactory
{
    private static final String LOCAL_TIME_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String UTC_TIME_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZZ";

    private BillingFactory()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Log a billing event.
     *
     * @param eventId The ID of the event.
     * @param source The source of the event.
     * @param eventMisc Additional information that can be sent with the billing event.
     */
    static void logEvent(int eventId, @NonNull String source, Map<String, String> eventMisc)
    {
        ModelBillingEvent billingData = buildModelBillingEvent(eventId, source, eventMisc);
        ArrayList<ModelBillingEvent> billingDataArray = new ArrayList<>(Arrays.asList(billingData));

        ModelBillingEventContainer billingEventContainer = new ModelBillingEventContainer(billingDataArray);

        ModelAWSFirehosePayload payload = new ModelAWSFirehosePayload(billingEventContainer);
        ModelAWSFirehose firehosePayload = new ModelAWSFirehose("myfiziq-prod-billing-stream", payload);

        sendBillingEventToServer(firehosePayload);
    }

    /**
     * Generates a {@link ModelBillingEvent} object.
     *
     * @param eventId The ID of the event.
     * @param source The source of the event.
     * @param eventMisc Additional information that can be sent with the billing event.
     * @return The {@link ModelBillingEvent} object that was generated.
     */
    private static ModelBillingEvent buildModelBillingEvent(int eventId, @NonNull String source, Map<String, String> eventMisc)
    {
        ModelBillingEvent data = new ModelBillingEvent();
        populateFromModelAppConf(data);

        data.c_misc = UUID.randomUUID().toString();
        data.c_time = BillingFactory.getUtcDateString();
        data.c_timeiso = BillingFactory.getLocalDateString();
        data.c_uid = hashUserId();

        data.e_id = Integer.toString(eventId);
        data.e_misc = generateJsonFromHashmap(eventMisc);
        data.e_src = source;

        data.c_sig = generateProofOfWork(data);

        return data;
    }

    private static String getUtcDateString()
    {
        return TimeFormatUtils.formatDate(new Date(), TimeZone.getDefault(), LOCAL_TIME_DATE_PATTERN, TimeZone.getTimeZone("UTC"));
    }

    private static String getLocalDateString()
    {
        return TimeFormatUtils.formatDate(new Date(), TimeZone.getDefault(), UTC_TIME_DATE_PATTERN);
    }

    /**
     * Sends a billing event to the server.
     *
     * @param data The data to send.
     */
    private static void sendBillingEventToServer(ModelAWSFirehose data)
    {
        HttpsURLConnection connection = null;

        try
        {
            String json = data.serialize();

            URL url = new URL(BuildConfig.BILLING_BASE_URL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-api-key", BuildConfig.BILLING_KEY);

            OutputStreamWriter outputStream = new OutputStreamWriter(connection.getOutputStream());
            outputStream.write(json);
            outputStream.close();

            int responseCode = connection.getResponseCode();
            String responseBody;

            if (responseCode < 300)
            {
                responseBody = IOUtils.toString(connection.getInputStream());
            }
            else
            {
                responseBody = IOUtils.toString(connection.getErrorStream());
            }

            Timber.i("Billing Response Code: %s. Response Body: %s", responseCode, responseBody);
        }
        catch (Exception e)
        {
            // As per the iOS app, transmitting billing events is done on a best-effort basis.
            // If it fails, it will not try to send it at a later date.
            // There is no check on the server side for duplicate billing events (i.e. multiple c_sig's).
            Timber.e(e, "Failed to log billing event");
        }
        finally
        {
            try
            {
                if (connection != null)
                {
                    connection.disconnect();
                }
            }
            catch (Exception e)
            {
                Timber.e(e, "Error when disconnecting HttpURLConnection");
            }
        }
    }

    /**
     * Populates the {@link ModelBillingEvent} with data from {@link ModelAppConf}.
     *
     * @param data The {@link ModelAppConf} data.
     */
    private static void populateFromModelAppConf(ModelBillingEvent data)
    {
        if (MyFiziq.getInstance().hasTokens())
        {
            data.vid = MyFiziq.getInstance().getTokenVid();
            data.aid = MyFiziq.getInstance().getTokenAid();
        }
        else
        {
            // Crash if we can't report to the server that a billing event is occurring (i.e. no free rides)
            throw new IllegalStateException("ModelAppConf is null when trying to report a billing event");
        }
    }

    /**
     * Generates a proof of work signature based on the data contained in {@link ModelBillingEvent}.
     * <p>
     * This is a hash of the data contained in {@link ModelBillingEvent} and should only be called after all other data has been populated.
     *
     * @param data The data to generate a proof of work signature for.
     * @return The signature generated.
     */
    private static String generateProofOfWork(ModelBillingEvent data)
    {
        MyFiziq myfiziq = MyFiziq.getInstance();

        return myfiziq.computeBillingSignature(
                "",
                data.vid,
                data.aid,
                data.e_id,
                data.e_src,
                data.e_misc,
                data.c_uid,
                data.c_misc,
                data.c_timeiso
        );
    }

    /**
     * Hashes a user ID based on the currently logged in user.
     *
     * @return A hashed user ID using SHA-256 or a blank string if we cannot hash the User ID.
     */
    private static String hashUserId()
    {
        try
        {
            String userId = AwsUtils.getCognitoUsernameNumber();

            if (StringUtils.isEmpty(userId))
            {
                return "";
            }
            else
            {
                byte[] rawHash = hashSha256(userId);

                return Base64.encodeToString(rawHash, Base64.NO_WRAP);
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot generate User ID hash");
            // All versions of Android since API Level 1 should support SHA-256
            // See: https://developer.android.com/reference/java/security/MessageDigest
            return "";
        }
    }

    /**
     * Generates a JSON string from a map.
     * <p>
     * The map's keys will be represented as JSON keys and the map's values will be represented as JSON values.
     */
    @NonNull
    private static String generateJsonFromHashmap(@Nullable Map<String, String> map)
    {
        if (map == null)
        {
            map = new HashMap<>();
        }

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(map);

        return Base64.encodeToString(json.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Hashes a string using the SHA-256 algorithm and returns it as a hexadecimal byte array.
     * <p>
     * For example, if the SHA-256 string is:
     * 5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9
     * <p>
     * <p>
     * Then the output by this functional will be:
     * 5f ec eb 66 ff c8 6f 38 d9 52 78 6c 6d 69 6c 79
     * c2 db c2 39 dd 4e 91 b4 67 29 d7 3a 27 fb 57 e9
     * <p>
     * ... as a raw byte array.
     */
    @NonNull
    private static byte[] hashSha256(@NonNull String input) throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

}
