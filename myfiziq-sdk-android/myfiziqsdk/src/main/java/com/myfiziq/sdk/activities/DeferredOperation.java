package com.myfiziq.sdk.activities;

/**
 * Represents an operation that is to be executed at some future point in time.
 */
public abstract class DeferredOperation
{
    /**
     * The method to execute in the future.
     */
    public abstract void execute();
}
