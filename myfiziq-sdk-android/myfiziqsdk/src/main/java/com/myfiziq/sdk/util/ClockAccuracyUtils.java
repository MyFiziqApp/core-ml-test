package com.myfiziq.sdk.util;



import com.myfiziq.sdk.helpers.AsyncHelper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

import timber.log.Timber;

public class ClockAccuracyUtils
{
    /**
     * Determines how many <strong>minutes</strong> the device's clock is off by.
     *
     * Note that the number of minutes we estimate the clock is off by is returned as opposed to seconds.
     *
     * This is due to the round trip latency between the user's device and the server, along with
     * the imprecise timekeeping nature of both servers and mobile devices.
     *
     *
     * @param callback A callback for the method. The number of seconds the device's clock is off by will be returned.
     */
    public static void getClockAccuracy(AsyncHelper.Callback<Long> callback)
    {
        AsyncHelper.run(() -> getClockAccuracyFromServer(callback));
    }

    private static void getClockAccuracyFromServer(AsyncHelper.Callback<Long> callback)
    {
        long timeOffByInMinutes = 0;

        HttpURLConnection urlConnection = null;

        try
        {
            // Recommended solution by Phillip Cooper.
            // Visit a publically available URL instead of my the MyFiziq AWS instance since our
            // AWS Dynamo is capacity limited.
            URL url = new URL("https://aws.amazon.com");

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("HEAD");


            long serverDateInGmt = urlConnection.getDate();

            Calendar cal1 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            long localDateInGmt = cal1.getTimeInMillis();

            timeOffByInMinutes = (localDateInGmt - serverDateInGmt) / 1000 / 60;


            Timber.i("Server Unix time is: "
                    + serverDateInGmt
                    + ". Local device Unix time is: "
                    + localDateInGmt
                    + ". Difference is: "
                    + Math.abs((serverDateInGmt - localDateInGmt) / 1000) + " seconds"
            );
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot get server time");
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }

        callback.execute(timeOffByInMinutes);
    }
}
