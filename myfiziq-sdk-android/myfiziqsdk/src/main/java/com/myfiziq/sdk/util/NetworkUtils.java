package com.myfiziq.sdk.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import java.util.List;
import java.util.Locale;

/**
 * @hide
 */
public class NetworkUtils
{
    public static final String CONNECTION_MOBILE = "MOBILE";
    public static final String CONNECTION_WIFI = "WIFI";

    /*
    public static boolean isSyncAvailable(Context context)
    {
        if (NetworkUtils.isNetworkAvailable(context))
        {
            if (ModelSetting.getSetting(ModelSetting.Setting.SYNC_WIFI_ONLY, false))
            {
                if (!NetworkUtils.isOnWifi(context))
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
    */

    public static boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnected();
    }

    public static boolean isOnWifi(Context context)
    {
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
        return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    /**
     * Get the IP of current Wi-Fi connection
     *
     * @return IP as string
     */
    public static String getIPAddress(Context context)
    {
        try
        {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                    (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        catch (Exception e)
        {
            //Timber.e(e, "");
            return null;
        }
    }


    /*
    Get the Current Connection Type
     */
    public static String getConnectionType(Context context)
    {

        String connectionType = "UNKNOWN";
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo is3gInfo = manager.getNetworkInfo(
                ConnectivityManager.TYPE_MOBILE);
        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        boolean is3g = false;
        if (is3gInfo != null)
        { // Need to do this as straight away checking for isConnected
            // throws exception
            is3g = is3gInfo.isConnected();
        }

        if (isWifi)
        {
            connectionType = CONNECTION_WIFI;
        }
        else if (is3g)
        {
            connectionType = CONNECTION_MOBILE;
        }

        return connectionType;

    }

    /* Get the Mobile Signal Type
     */
    public static String getMobileNetworkConnectionType(Context context)
    {

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int nt = tm.getNetworkType();
        String mobileConnectionType = "UNKNOWN";
        //Timber.d(String.format("getNetworkType is %d", nt));

        switch (nt)
        {
            case 1:
                //Timber.d("GPRS ");
                mobileConnectionType = "GPRS";
                break;
            case 2:
                //Timber.d("EDGE ");
                mobileConnectionType = "EDGE";
                break;
            case 3:
                //Timber.d("UMTS ");
                mobileConnectionType = "UMTS";
                break;
            case 8:
                //Timber.d("HSDPA ");
                mobileConnectionType = "HSDPA";
                break;
            case 9:
                //Timber.d("HSUPA ");
                mobileConnectionType = "HSUPA";
                break;
            case 10:
                //Timber.d("HSPA ");
                mobileConnectionType = "HSPA";
                break;
            case 13:
                //Timber.d("LTE ");
                mobileConnectionType = "LTE";
                break;
            case 14:
                //Timber.d("EHRPD ");
                mobileConnectionType = "EHRPD";
                break;
            case 15:
                //Timber.d("HSPAP ");
                mobileConnectionType = "HSPAP";
                break;
            default:
                //Timber.d("UNKNOWN ");
                mobileConnectionType = "UNKNOWN";
                break;
        }

        return mobileConnectionType;
    }

    /*
    Get the Wifi Signal Strength of the current Connection
     */
    public static String getWifiSignalState(Context ctx)
    {
        int signalStrength = -1;
        try
        {
            final WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            int state = wifi.getWifiState();
            if (state == WifiManager.WIFI_STATE_ENABLED)
            {
                List<ScanResult> results = wifi.getScanResults();
                if (results == null)
                    return String.valueOf(signalStrength);
                for (ScanResult result : results)
                {
                    if (result != null && wifi != null && wifi.getConnectionInfo() != null && result.BSSID != null
                            && result.BSSID.equals(wifi.getConnectionInfo().getBSSID()))
                    {
                        int level = 0;
                        level = wifi.calculateSignalLevel(wifi.getConnectionInfo().getRssi(), result.level);

                        // this is to handle java.lang.ArithmeticException: divide by zero
                        if (level != 0 && result.level != 0)
                        {
                            // convert signal strength into percentage
                            int difference = level * 100 / result.level;
                            if (difference >= 100)
                                signalStrength = 4;
                            else if (difference >= 75)
                                signalStrength = 3;
                            else if (difference >= 50)
                                signalStrength = 2;
                            else if (difference >= 25)
                                signalStrength = 1;
                        }
                    }

                }
            }
        }
        catch (Exception e)
        {
            //Timber.e(e, "");
        }

        return String.valueOf(signalStrength);
    }
}
