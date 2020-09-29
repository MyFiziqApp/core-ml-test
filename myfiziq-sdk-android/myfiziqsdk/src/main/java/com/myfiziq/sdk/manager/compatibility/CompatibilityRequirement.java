package com.myfiziq.sdk.manager.compatibility;

/**
 * Represents a compatibility requirement for running the MyFiziq SDK.
 *
 * @hide
 */
public interface CompatibilityRequirement
{
    /**
     * Determines if the current device is compatible for running the MyFiziq SDK.
     *
     * @return Whether the application is compatible with the MyFiziq SDK.
     */
    boolean isCompatible();
}
