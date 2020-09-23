package com.myfiziq.sdk.manager;

import com.myfiziq.sdk.enums.SdkResultCode;

import androidx.annotation.NonNull;

public class MyFiziqException extends Exception
{
    private final SdkResultCode code;

    public MyFiziqException(SdkResultCode code)
    {
        super();
        this.code = code;
    }

    public MyFiziqException(SdkResultCode code, @NonNull String message)
    {
        super(message);
        this.code = code;
    }

    public SdkResultCode getCode()
    {
        return code;
    }
}
