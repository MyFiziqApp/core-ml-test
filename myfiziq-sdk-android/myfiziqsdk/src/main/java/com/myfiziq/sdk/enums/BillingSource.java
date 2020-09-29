package com.myfiziq.sdk.enums;

public enum BillingSource
{
    UNKNOWN("unknown"),
    SDK("sdk"),
    APP("app");



    private String id;

    BillingSource(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
}
