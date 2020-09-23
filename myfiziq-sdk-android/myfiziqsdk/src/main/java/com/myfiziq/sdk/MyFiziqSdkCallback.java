package com.myfiziq.sdk;

/**
 * Callback interface for asynchronous API calls.
 */
public interface MyFiziqSdkCallback
{
    void onError(String result);
    void onSuccess(String result);
}
