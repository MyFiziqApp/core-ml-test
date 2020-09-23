package com.myfiziq.sdk;

import com.myfiziq.sdk.enums.SdkResultCode;

/**
 * Callback interface for asynchronous API calls.
 */
public interface MyFiziqApiCallback
{
    // Please don't execute me directly. Please use MyFiziqSdkManager.postCallback();
    //
    // This is necessary to prevent the callback from being made on the {@link MyFiziqSdkManager}
    // executor pool. If lots of callbacks are made on the executor pool and don't finish processing
    // in a timely manner, it could starve the executor pool of available threads.
    void apiResult(SdkResultCode responseCode, String result);
}
