package com.myfiziq.sdk.helpers;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.LocalUserDataKey;
import com.myfiziq.sdk.db.ModelLocalUserData;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;

import org.jetbrains.annotations.Contract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provides helper utilities for storing user data locally on the device.
 *
 * The local user data is associated with a particular user and guest user.
 *
 * @since 19.5, this class will also take into account guest users and save locally stored data
 * against a particular guest and username combination.
 */
public class MyFiziqLocalUserDataHelper
{
    private MyFiziqLocalUserDataHelper()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Gets data stored locally on the device associated with the currently signed in user and
     * chosen guest (if any).
     * @param key The key of the data to retrieve.
     */
    @Nullable
    public static String getValue(LocalUserDataKey key)
    {
        String guestName = getGuestName();
        ModelLocalUserData localUserData = getLocalUserData(guestName, key);

        if (localUserData == null)
        {
            return null;
        }
        else
        {
            return localUserData.getValue();
        }
    }

    /**
     * Gets data stored locally on the device associated with the currently signed in user and
     * chosen guest (if any).
     *
     * If the data does not exist, the default value will be returned. The returned value WILL be
     * null if the {@code defaultValue} is null.
     *
     * @param key The key of the data to retrieve.
     * @param defaultValue The default value to return if no data exists.
     */
    // The method returns null if its second argument is null, and not-null otherwise.
    @Contract("_, null -> null; _, !null -> !null")
    public static String getValue(LocalUserDataKey key, String defaultValue)
    {
        String value = getValue(key);

        if (value == null)
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    /**
     * Stores the desired key and value locally on the device for the currently signed in user and
     * chosen guest (if any).
     */
    public static void setValue(LocalUserDataKey key, String value)
    {
        String guestName = getGuestName();

        ModelLocalUserData localUserData = getLocalUserData(guestName, key);

        if (localUserData == null)
        {
            localUserData = Orm.newModel(ModelLocalUserData.class);
            localUserData.setId(guestName, key);
        }

        localUserData.setValue(value);
        localUserData.save();
    }

    /**
     * Retrieves the currently signed in guest name.
     */
    @NonNull
    private static String getGuestName()
    {
        String guestName = MyFiziq.getInstance().getGuestUser();

        if (guestName == null)
        {
            // Just in case...
            guestName = "";
        }

        return guestName;
    }

    /**
     * Attempts to retrieve the local user data from the in-memory cache (if available).
     *
     * Otherwise, retrieve it directly from the database.
     */
    @Nullable
    private static ModelLocalUserData getLocalUserData(String guestName, LocalUserDataKey key)
    {
        String id = ModelLocalUserData.generateIdString(guestName, key);

        // Get the model directly from the database
        return ORMTable.getModel(ModelLocalUserData.class, id);
    }
}
