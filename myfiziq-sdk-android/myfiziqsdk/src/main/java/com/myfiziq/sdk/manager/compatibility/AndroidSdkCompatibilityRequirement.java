package com.myfiziq.sdk.manager.compatibility;

import android.os.Build;

/**
 * @hide
 */
public class AndroidSdkCompatibilityRequirement implements CompatibilityRequirement
{
    private int minimumSupportedSdkVersion;

    public AndroidSdkCompatibilityRequirement(int minimumSupportedSdkVersion)
    {
        this.minimumSupportedSdkVersion = minimumSupportedSdkVersion;
    }

    @Override
    public boolean isCompatible()
    {
        int currentSdkVersion = getSdkVersion();
        return currentSdkVersion >= minimumSupportedSdkVersion;
    }

    /**
     * Returns the SDK version.
     */
    private int getSdkVersion()
    {
        return Build.VERSION.SDK_INT;
    }

}
