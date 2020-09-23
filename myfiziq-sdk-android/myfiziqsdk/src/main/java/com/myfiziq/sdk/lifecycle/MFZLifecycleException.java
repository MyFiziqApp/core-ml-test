package com.myfiziq.sdk.lifecycle;

/**
 * This exception will be thrown whenever the MyFiziq SDK is currently not in a valid state to perform
 * the requested operation.
 */
public class MFZLifecycleException extends RuntimeException
{
    public MFZLifecycleException()
    {
        super();
    }

    public MFZLifecycleException(String message)
    {
        super(message);
    }

    public MFZLifecycleException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MFZLifecycleException(Throwable cause)
    {
        super(cause);
    }
}
