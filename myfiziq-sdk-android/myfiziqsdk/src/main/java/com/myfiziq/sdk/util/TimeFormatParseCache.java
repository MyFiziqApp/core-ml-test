package com.myfiziq.sdk.util;

import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import androidx.annotation.Nullable;

/**
 * Provides a cache for accelerating date parsing.
 */
class TimeFormatParseCache
{
    private static final HashMap<String, Date> parsedDateCache = new HashMap<>();

    private TimeFormatParseCache()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Tries to get a date that has already been parsed from the cache.
     *
     * @param dateAsString   The date represented a string.
     * @param inputTimeZone  The timezone of the date in the string.
     * @param outputTimeZone The timezone of the desired date object.
     * @return A date object if it exists in the cache for the desired parameters. Otherwise null if no date exists in the cache.
     */
    @Nullable
    static Date getDateFromCache(String dateAsString, TimeZone inputTimeZone, TimeZone outputTimeZone)
    {
        String key = generateCacheKey(dateAsString, inputTimeZone, outputTimeZone);
        return parsedDateCache.get(key);
    }

    /**
     * Puts a date that has been parsed into the cache for its associated string representation.
     *
     * @param dateAsObject   The date object that has been parsed with the date string and timezone
     *                       combination.
     * @param dateAsString   The date represented a string.
     * @param inputTimeZone  The timezone of the date in the string.
     * @param outputTimeZone The timezone of the desired date object.
     */
    static void putDateInCache(Date dateAsObject, String dateAsString, TimeZone inputTimeZone, TimeZone outputTimeZone)
    {
        String key = generateCacheKey(dateAsString, inputTimeZone, outputTimeZone);
        parsedDateCache.put(key, dateAsObject);
    }

    /**
     * Generates a key for the HashMap cache that represents the parsed date.
     */
    private static String generateCacheKey(String dateAsString, TimeZone inputTimeZone, TimeZone outputTimeZone)
    {
        return dateAsString + inputTimeZone.getID() + outputTimeZone.getID();
    }
}
