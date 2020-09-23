package com.myfiziq.myfiziq_android;

public class Credentials
{
    private Credentials()
    {
        // Empty hidden constructor for the utility class
    }


    // Don't forget to change "avatar_extended_support_s3_bucket" to point where we'll upload the crash data to.
    // This can't be stored in the appconf for security reasons - Phil
    //public static final String KEY = BuildConfig.KEY;
    //public static final String SECRET = BuildConfig.SECRET;
    //public static final String ENV = BuildConfig.ENV;
    public static final String TOKEN = BuildConfig.TOKEN;
}
