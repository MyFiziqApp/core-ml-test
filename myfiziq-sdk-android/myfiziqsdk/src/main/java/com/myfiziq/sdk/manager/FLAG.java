package com.myfiziq.sdk.manager;

/**
 * @hide
 */
public enum FLAG
{
    // These flags need to match the NDK layer flags.
    FLAG_NONE(0),
    // Use ETAG headers.
    FLAG_ETAG(1),
    // post as application/x-www-form-urlencoded.
    FLAG_FORM(2),
    // read token from response.
    FLAG_TOKEN(4),
    // use refresh token.
    FLAG_PROGRESS(8),
    // token not required
    FLAG_FILE(16),
    // first optional arg is a post data string (e.g. JSON)
    // remaining args are header pairs (key: value).
    FLAG_DATA(32),
    // return response data.
    FLAG_RESPONSE(64),
    // response is JTW data (decrypt and verify).
    FLAG_JWT(128),
    // sign request with an HMAC.
    FLAG_HMAC(256),
    FLAG_SIGN_URL(512),
    // Pagination
    FLAG_PAGING(1024),
    // Data is extra query data
    FLAG_QUERY(2048),
    // HTTP basic authentication
    FLAG_BASIC_AUTH(4096),
    // No base url.
    FLAG_NOBASE(8192),
    // No additional headers
    FLAG_NO_EXTRA_HEADERS(16384);

    int mValue;

    FLAG(int value)
    {
        mValue = value;
    }

    public static int getFlags(FLAG... flags)
    {
        int result = 0;

        for (FLAG f : flags)
        {
            result |= f.mValue;
        }

        return result;
    }

    public boolean hasFlag(int value, FLAG flag)
    {
        return (0 != (value & flag.ordinal()));
    }
}