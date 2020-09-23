package com.myfiziq.sdk.db;

import android.database.DatabaseUtils;

import java.util.ArrayList;

/**
 * A common set of queries for performing operations against the database.
 */
public class ORMDbQueries
{
    private ORMDbQueries()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Gets a list of the last completed avatars.
     * @param numberOfAvatars The number of most recent avatars to retrieve.
     */
    public static ArrayList<ModelAvatar> getLastXCompletedAvatars(int numberOfAvatars)
    {
        ArrayList<ModelAvatar> avatars = ORMTable.getModelList(
                ModelAvatar.class,
                ModelAvatar.getWhere(
                        String.format("Status='%s'", Status.Completed)
                ),
                ModelAvatar.getOrderBy(0) + " LIMIT " + numberOfAvatars
        );

        if (null == avatars)
        {
            return new ArrayList<>();
        }

        return avatars;
    }

    /**
     * Count the number of avatars created by a guest.
     */
    public static int countAvatarsCreatedByGuest(String guestName)
    {
        StringBuilder escapedStringBuilder = new StringBuilder("miscGuest = ");

        // Escape the guest name to handle any quotation marks that may have been entered
        DatabaseUtils.appendEscapedSQLString(escapedStringBuilder, guestName);

        return ORMTable.getModelCount(
                ModelAvatar.class,
                ModelAvatar.getWhereWithGuestAvatars(escapedStringBuilder.toString())
        );
    }
}
