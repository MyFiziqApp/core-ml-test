package com.myfiziq.sdk.db;

/**
 * @hide
 */

import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.helpers.AsyncHelper;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

@Cached
public class ModelSetting
{
    public enum Setting
    {
        MODEL,
        USERNAME,
        UNITTYPE,
        NUMBER_OF_TIMES_SEEN_ONBOARDING,
        AGREED_TO_TOS,
        AGREED_TO_IMAGE_CONSENT,
        ID_TOKEN,
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        FEATURE_VIDEO_ONBOARDING,
        FEATURE_GUEST_USERS,
        FEATURE_PRACTISE_MODE,
        FEATURE_INSIGHTS_VIEW_AVATAR,
        FEATURE_INSIGHTS_INPUT,
        DEBUG_DISABLE_ALIGNMENT,
        DEBUG_INSPECT_PASS,
        DEBUG_INSPECT_FAIL,
        DEBUG_VISUALIZE,
        DEBUG_VISUALIZE_POSE,
        DEBUG_INDEVICE,
        DEBUG_RUNJOINTS,
        DEBUG_PAYLOAD,
        DEBUG_UPLOAD_RESULTS,
        DEBUG_POSEFRAMES,
        DEBUG_RESOURCE_SVR,
        DEBUG_HARNESS,
        DEBUG_STYLING,
        STYLE,
        STYLE_LAST_UPDATED,
        FRONT_IMAGE_NAME,
        SIDE_IMAGE_NAME
    }

    public ModelSetting()
    {

    }

    public static boolean isConfigured(@NonNull Setting setting)
    {
        String value = MyFiziq.getInstance().getKey(setting);
        return !TextUtils.isEmpty(value);
    }

    /**
     * Gets a setting value from the database.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     */
    @NonNull
    public static String getSetting(@NonNull Setting setting, @NonNull String defaultValue)
    {
        return getSettingInternal(setting, defaultValue);
    }

    /**
     * Gets a setting value from the database asynchronously. Once retrieved, the value is passed into the callback
     * that is executed ON THE UI THREAD.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     * @param callback The callback to execute with the retireved setting. This is executed ON THE UI THREAD.
     */
    public static void getSettingAsync(@NonNull Setting setting, @NonNull String defaultValue, AsyncHelper.Callback<String> callback)
    {
        AsyncHelper.run(
                () -> getSetting(setting, defaultValue),
                callback,
                true
        );
    }

    /**
     * Gets a setting value from the database.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     */
    public static boolean getSetting(@NonNull Setting setting, boolean defaultValue)
    {
        return getSettingInternal(setting, defaultValue);
    }

    /**
     * Gets a setting value from the database asynchronously. Once retrieved, the value is passed into the callback
     * that is executed ON THE UI THREAD.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     * @param callback The callback to execute with the retireved setting. This is executed ON THE UI THREAD.
     */
    public static void getSettingAsync(@NonNull Setting setting, boolean defaultValue, AsyncHelper.Callback<Boolean> callback)
    {
        AsyncHelper.run(
                () -> getSetting(setting, defaultValue),
                callback,
                true
        );
    }

    /**
     * Gets a setting value from the database.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     */
    public static int getSetting(@NonNull Setting setting, int defaultValue)
    {
        return getSettingInternal(setting, defaultValue);
    }

    /**
     * Gets a setting value from the database asynchronously. Once retrieved, the value is passed into the callback
     * that is executed ON THE UI THREAD.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     * @param callback The callback to execute with the retireved setting. This is executed ON THE UI THREAD.
     */
    public static void getSettingAsync(@NonNull Setting setting, int defaultValue, AsyncHelper.Callback<Integer> callback)
    {
        AsyncHelper.run(
                () -> getSetting(setting, defaultValue),
                callback,
                true
        );
    }

    /**
     * Gets a setting value from the database.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     */
    public static double getSetting(@NonNull Setting setting, double defaultValue)
    {
        return getSettingInternal(setting, defaultValue);
    }

    /**
     * Gets a setting value from the database asynchronously. Once retrieved, the value is passed into the callback
     * that is executed ON THE UI THREAD.
     *
     * @param setting The setting to retrieve.
     * @param defaultValue The value to return if the setting has not been set.
     * @param callback The callback to execute with the retrieved setting. This is executed ON THE UI THREAD.
     */
    public static void getSettingAsync(@NonNull Setting setting, double defaultValue, AsyncHelper.Callback<Double> callback)
    {
        AsyncHelper.run(
                () -> getSetting(setting, defaultValue),
                callback,
                true
        );
    }

    public static void putSetting(Setting setting, String val)
    {
        MyFiziq.getInstance().setKey(setting, val);
    }

    public static void putSetting(Setting setting, boolean val)
    {
        MyFiziq.getInstance().setKey(setting, String.valueOf(val));
    }

    public static void putSetting(Setting setting, int val)
    {
        MyFiziq.getInstance().setKey(setting, String.valueOf(val));
    }

    public static void putSetting(Setting setting, double val)
    {
        MyFiziq.getInstance().setKey(setting, String.valueOf(val));
    }

    /**
     * Gets a setting from the database.
     * @param setting The setting to get.
     * @param defaultValue The default value if the setting doesn't exist or hasn't been specified yet.
     * @param <T> The data type of the setting. This MUST be the same as the defaultV
     * @return The setting with a data type that is the same as the default value.
     */
    @NonNull
    private static <T> T getSettingInternal(Setting setting, @NonNull T defaultValue)
    {
        // Make sure no silly programmers pass null to this method even when the IDE warns them not to
        Objects.requireNonNull(defaultValue);

        String value = MyFiziq.getInstance().getKey(setting);

        if (!TextUtils.isEmpty(value))
        {
            return convertStringToReturnType(value, defaultValue);
        }

        //Timber.v("%s does not exist. Using default settings value of %s", setting.ordinal(), defaultValue);
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private static <T> T convertStringToReturnType(@Nullable String returnValue, @NonNull T defaultValue)
    {
        if (returnValue == null)
        {
            // Just in case something terrible happens and we receive null for some reason
            return defaultValue;
        }

        // Cast the return value to the same class as the defaultValue
        if (defaultValue instanceof String)
        {
            return (T) returnValue;
        }
        else if (defaultValue instanceof Boolean)
        {
            return (T) Boolean.valueOf(returnValue);
        }
        else if (defaultValue instanceof Double)
        {
            return (T) Double.valueOf(returnValue);
        }
        else if (defaultValue instanceof Integer)
        {
            return (T) Integer.valueOf(returnValue);
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported ModelSetting data type: " + defaultValue.getClass().getSimpleName());
        }
    }
}
