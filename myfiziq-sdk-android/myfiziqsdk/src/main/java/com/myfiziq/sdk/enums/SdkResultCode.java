package com.myfiziq.sdk.enums;

import android.util.SparseArray;

/**
 * Represents the result of an operation.
 */
public enum SdkResultCode
{
    /**
     * The operation was successful.
     */
    HTTP_OK(200),

    /**
     * Generic success code.
     */
    SUCCESS(0),

    /**
     * A generic error.
     */
    ERROR(-1),

    NATIVE_ERROR(-2),

    /**
     * The request lacks valid authentication credentials to perform the operation.
     */
    HTTP_UNAUTHORISED(-401),

    /**
     * The resources to perform the desired operation were not found.
     */
    HTTP_NOT_FOUND(-404),

    /**
     * The token has expired.
     */
    HTTP_EXPIRED(-498),

    /**
     * The operation could not be completed due to a resource conflict.
     */
    HTTP_CONFLICT(-409),

    /**
     * An internal error occurred that prevented the operation from being completed.
     */
    HTTP_INTERNAL_ERROR(-500),

    /**
     * There is no internet connection available to fulfil the request.
     */
    NO_INTERNET(-20000),

    /**
     * An unexpected null value was encountered.
     */
    NULL_VALUE(-20001),

    INTERRUPTED_EXCEPTION(-20002),
    EXECUTION_EXCEPTION(-20003),
    HTTP_ERROR(-20004),
    USER_CANCELLED(-20005),

    /**
     * The SDK has not been initialised yet.
     */
    SDK_NOT_INITIALISED(-20100),

    /**
     * The SDK has an empty configuration.
     */
    SDK_EMPTY_CONFIGURATION(-20101),

    SDK_ERROR_UPLOAD_ZIPS(-20103),
    SDK_ERROR_UPLOAD_STATUS_FILE(-20104),
    SDK_ERROR_USER_NOT_EXIST(-20105),
    SDK_ERROR_WRONG_PASSWORD(-20106),
    SDK_NOT_COMPATIBLE(-20107),
    SDK_ERROR_PARCELABLE_SERIALISATION(-20108),



    DB_CANNOT_SAVE(-20200),
    DB_EMPTY_INSTANCE(-20201),

    FILESYSTEM_ERROR(-20300),


    AUTH_EMPTY_SESSION(-20400),
    AUTH_INVALID_SESSION(-20401),
    AUTH_EMPTY_USER(-20402),
    AUTH_EMPTY_USER_POOL(-20403),
    AUTH_EMPTY_USER_DETAILS(-20404),
    AUTH_ERROR_UPDATE_USER_ATTRIBUTES(-20406),
    AUTH_EXPIRED_TOKEN(-20407),
    AUTH_CANNOT_INIT_AWS(-20408),
    AUTH_CANNOT_GET_AWS_CONFIG(-20409),
    AUTH_SIGNED_IN_DIFFERENT_USER(-20409),
    AUTH_SIGNED_OUT(-20410),


    AVATAR_EMPTY_STATUS(-20500),
    AVATAR_FRONT_SIDE_MISMATCH(-20501),
    AVATAR_FRONT_SIDE_EMPTY(-20502),
    AVATAR_FILE_COUNT_MISMATCH(-20503),

    AVATARDOWNLOAD_EXCEPTION(-20600),
    AVATARDOWNLOAD_PARTIAL(-20601),
    AVATARDOWNLOAD_UPDATESTATUSFAIL(-20602),
    AVATARDOWNLOAD_UPLOADSTATUSFILEFAIL(-20603),
    AVATARDOWNLOAD_BADFINALSTATUS(-20604);



    private int code;

    // Key = Code Integer
    // Value = SdkResponseCode object
    private static SparseArray<SdkResultCode> codeMap = new SparseArray<>();


    SdkResultCode(int code)
    {
        this.code = code;
    }

    /*
     * Loads all SDK results when the application starts to ensure that we can easily iterate through them in the future.
     */
    static
    {
        for (SdkResultCode item : SdkResultCode.values())
        {
            codeMap.put(item.code, item);
        }
    }

    /**
     * Converts a response code into an SdkResponseCode.
     *
     * The response code may or may not be a HTTP Status Code.
     */
    public static SdkResultCode valueOfHttpCode(int httpCode)
    {
        SdkResultCode foundCode = null;
        SdkResultCode foundNegativeCode = null;

        // a httpCode of zero does not map correctly - it should be an error.
        if (0 != httpCode)
        {
            foundCode = codeMap.get(httpCode);
            foundNegativeCode = codeMap.get(httpCode);
        }

        if (null != foundCode)
        {
            return foundCode;
        }
        else if (null != foundNegativeCode)
        {
            return foundNegativeCode;
        }
        else if (200 <= httpCode && httpCode < 400)
        {
            return HTTP_OK;
        }
        else
        {
            return HTTP_INTERNAL_ERROR;
        }
    }

    /**
     * Converts a result code from the MyFiziq C++ code into an {@link SdkResultCode} object.
     *
     * @param nativeResultCode The result code from the MyFiziq C++ layer.
     * @return An SdkResultCode representing the nativeResultCode.
     */
    public static SdkResultCode fromNativeResultCode(int nativeResultCode)
    {
        SdkResultCode foundCode = codeMap.get(nativeResultCode);
        SdkResultCode foundNegativeCode = codeMap.get(nativeResultCode);

        if (null != foundCode)
        {
            return foundCode;
        }
        else if (null != foundNegativeCode)
        {
            return foundNegativeCode;
        }
        else
        {
            return NATIVE_ERROR;
        }
    }

    /**
     * Returns a primitive integer representing the underlying result code.
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Whether the {@link SdkResultCode} represents a successful result.
     */
    public boolean isOk()
    {
        return (code >= 0);
    }

    /**
     * Whether the {@link SdkResultCode} indicates that the configuration is empty.
     */
    public boolean isConfigurationEmpty()
    {
        return this == SdkResultCode.SDK_EMPTY_CONFIGURATION;
    }

    /**
     * Whether the {@link SdkResultCode} indicates that there is no internet access.
     */
    public boolean isInternetDown()
    {
        return this == SdkResultCode.NO_INTERNET;
    }
}
