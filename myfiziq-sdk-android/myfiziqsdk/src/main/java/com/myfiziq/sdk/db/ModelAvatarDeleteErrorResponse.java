package com.myfiziq.sdk.db;

import com.google.gson.annotations.SerializedName;

public class ModelAvatarDeleteErrorResponse
{
    @SerializedName("errorMessage")
    private String errorMessage;

    @SerializedName("errorType")
    private String errorType;

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public String getErrorType()
    {
        return errorType;
    }
}
