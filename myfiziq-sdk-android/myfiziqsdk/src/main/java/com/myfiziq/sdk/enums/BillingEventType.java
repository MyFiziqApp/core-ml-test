package com.myfiziq.sdk.enums;

/**
 * Contains the type of billing events.
 */
public enum BillingEventType
{
    // Source: https://internal.myfiziq.io:8090/books/billing-ids/page/billing-registry
    SDK_INITIALIZED(100, BillingSource.SDK, "SDK initialized"),
    USER_LOGIN(101, BillingSource.SDK, "User login"),
    USER_LOGOUT(102, BillingSource.SDK, "User logout"),
    USER_REGISTERED(103, BillingSource.SDK, "User registered"),
    NEW_MEASUREMENT_REQUESTED(104, BillingSource.SDK, "New Measurement requested"),
    NEW_MEASUREMENT_RESOLVED(105, BillingSource.SDK, "New Measurement resolved"),
    UNIT_TEST_BILLING_EVENT(106, BillingSource.SDK, "Unit Test billing event"),
    RANGE_NON_BASE_RESULTS(200, BillingSource.SDK, "Range for non-base results (like body fat)"),
    MAYWEATHER_USER_LOGIN(2000, BillingSource.APP, "Mayweather User login"),
    MAYWEATHER_USER_LOGIN_GOLD(2001, BillingSource.APP, "Mayweather Gold Level User Login");


    private int id;
    private BillingSource source;
    private String description;

    BillingEventType(int id, BillingSource source, String description)
    {
        this.id = id;
        this.source = source;
        this.description = description;
    }

    public int getEventId()
    {
        return id;
    }

    public BillingSource getEventSource()
    {
        return source;
    }

    public String getEventDescription()
    {
        return description;
    }}
