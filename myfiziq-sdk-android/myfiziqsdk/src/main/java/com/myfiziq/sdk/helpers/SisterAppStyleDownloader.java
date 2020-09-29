package com.myfiziq.sdk.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqApiCallbackPayload;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAppConf;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ModelSisterStyle;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.manager.FLAG;
import com.myfiziq.sdk.manager.MyFiziqException;
import com.myfiziq.sdk.util.GlobalContext;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class SisterAppStyleDownloader
{
    /**
     * The number of seconds to cache the styling for.
     */
    private static final long CACHE_MAX_AGE = 3600L;


    private SisterAppStyleDownloader()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * ASYNCHRONOUSLY Gets styles for a given App ID, Vendor ID and environment. The styles may be either downloaded remotely, or may be cached.
     *
     * This method is not guaranteed to execute within a certain period of time.
     *
     * @param callback The callback with the identified styles. The callback will be executed on the UI thread.
     */
    public static void getStyling(@NonNull MyFiziqApiCallbackPayload<ModelSisterStyle> callback)
    {
        AsyncHelper.run(
                () -> getStylingSynchronously(callback)
        );
    }

    /**
     * SYNCHRONOUSLY Gets styles for a given App ID, Vendor ID and environment. The styles may be either downloaded remotely, or may be cached.
     *
     * This method is not guaranteed to execute within a certain period of time.
     *
     * @param callback The callback with the identified styles. The callback will be executed on the UI thread.
     */
    private static void getStylingSynchronously(@NonNull MyFiziqApiCallbackPayload<ModelSisterStyle> callback)
    {
        String json = MyFiziq.getInstance().getKey(ModelSetting.Setting.STYLE);

        if (TextUtils.isEmpty(json) || hasStyleCacheExpired())
        {
            Timber.i("No cached styles found. Downloading remotely.");

            try
            {
                // No cached styles found. Try to download one from the remote server.
                json = downloadStyles();

                MyFiziq.getInstance().setKey(ModelSetting.Setting.STYLE, json);
                MyFiziq.getInstance().setKey(ModelSetting.Setting.STYLE_LAST_UPDATED, String.valueOf(getCurrentTimestamp()));
            }
            catch (MyFiziqException exception)
            {
                executeOnUiThread(callback, exception.getCode(), exception.getMessage(), null);
                return;
            }
        }
        else
        {
            Timber.i("Using cached style.");
        }


        try
        {
            ModelSisterStyle model = Orm.newModel(ModelSisterStyle.class);
            model.deserialize(json);

            executeOnUiThread(callback, SdkResultCode.SUCCESS, "", model);
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot parse received styling");
            executeOnUiThread(callback, SdkResultCode.ERROR, "Cannot parse received styling", null);
        }
    }

    /**
     * Downloads styles from the remote server, saves it to the database to be cached and returns the colours in the JSON.
     * @return The JSON downloaded from the server.
     */
    private static String downloadStyles() throws MyFiziqException
    {
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


        MyFiziq mfz = MyFiziq.getInstance();
        String url = buildDownloadUrl();

        String responseBody = mfz.apiGet(
                "",
                url,
                responseCode,
                0,
                0,
                FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS, FLAG.FLAG_NOBASE, FLAG.FLAG_RESPONSE)
        );


        SdkResultCode resultCode = SdkResultCode.valueOfHttpCode(responseCode);

        if (resultCode.isInternetDown())
        {
            Timber.e("Cannot get styling. Internet down.");
            throw new MyFiziqException(SdkResultCode.NO_INTERNET, "Internet down");
        }
        else if (!resultCode.isOk())
        {
            Timber.e("Cannot get styling. Received HTTP Code: %s. Message: %s", resultCode, responseBody);
            throw new MyFiziqException(SdkResultCode.ERROR, "");
        }
        else
        {
            return responseBody;
        }
    }

    /**
     * Determines if the style cache has expired and we need to download fresh styling.
     */
    private static boolean hasStyleCacheExpired()
    {
        String styleLastUpdatedString = MyFiziq.getInstance().getKey(ModelSetting.Setting.STYLE_LAST_UPDATED);

        if (TextUtils.isEmpty(styleLastUpdatedString) || !TextUtils.isDigitsOnly(styleLastUpdatedString))
        {
            return true;
        }

        long currentUnixTimestamp = getCurrentTimestamp();
        long earliestCacheTimestamp = currentUnixTimestamp - CACHE_MAX_AGE;

        try
        {
            long styleLastUpdated = Long.parseLong(styleLastUpdatedString);

            return earliestCacheTimestamp > styleLastUpdated;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * Builds the URL where the styles JSON file may be downloaded from.
     *
     * @return The URL where the styles JSON file may be downloaded from.
     */
    private static String buildDownloadUrl() throws MyFiziqException
    {
        ModelAppConf appConf = ModelAppConf.getInstance();

        if (appConf == null)
        {
            throw new MyFiziqException(SdkResultCode.SDK_EMPTY_CONFIGURATION, "");
        }

        String environment = appConf.env;
        String vendorId = appConf.vid;
        String appId = appConf.aid;

        Context context = GlobalContext.getContext();
        Resources resources = context.getResources();

        if (environment.equals("dev"))
        {
            return resources.getString(R.string.styles_url_dev, environment, vendorId, appId);
        }
        else
        {
            return resources.getString(R.string.styles_url, environment, vendorId, appId);
        }
    }

    /**
     * Executes the callback on the UI thread.
     */
    private static <T> void executeOnUiThread(MyFiziqApiCallbackPayload<T> callback, SdkResultCode responseCode, String result, T payload)
    {
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(() -> callback.apiResult(responseCode, result, payload));
    }

    private static long getCurrentTimestamp()
    {
        return System.currentTimeMillis() / 1000L;
    }
}
