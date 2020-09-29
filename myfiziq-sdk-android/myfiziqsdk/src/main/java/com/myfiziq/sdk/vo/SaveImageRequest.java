package com.myfiziq.sdk.vo;

import android.content.Context;

import com.myfiziq.sdk.helpers.AsyncHelper;

public class SaveImageRequest
{
    private Context context;
    private String imageFilePath;
    private AsyncHelper.Callback<String> callback;

    public SaveImageRequest(Context context, String imageFilePath, AsyncHelper.Callback<String> callback)
    {
        this.context = context;
        this.imageFilePath = imageFilePath;
        this.callback = callback;
    }

    public Context getContext()
    {
        return context;
    }

    public String getImageFilePath()
    {
        return imageFilePath;
    }

    public AsyncHelper.Callback<String> getCallback()
    {
        return callback;
    }
}
