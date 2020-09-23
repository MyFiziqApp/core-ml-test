package com.myfiziq.sdk;

import com.myfiziq.sdk.enums.SdkResultCode;

/**
 * Callback interface for asynchronous API calls that may also return a payload under some circumstances (e.g. User Profile details).
 */
public interface MyFiziqApiCallbackPayload<T>
{
    void apiResult(SdkResultCode responseCode, String result, T payload);
}
