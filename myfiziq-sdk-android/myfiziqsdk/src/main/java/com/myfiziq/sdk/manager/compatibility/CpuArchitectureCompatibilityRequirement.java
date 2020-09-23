package com.myfiziq.sdk.manager.compatibility;

import android.os.Build;

/**
 * @hide
 */
public class CpuArchitectureCompatibilityRequirement implements CompatibilityRequirement
{
    @Override
    public boolean isCompatible()
    {
        String[] supportedABIs = Build.SUPPORTED_ABIS;

        for (String abi: supportedABIs)
        {
            if (abi.equals("arm64-v8a"))
            {
                return true;
            }
        }

        return false;

        // Don't use the below since it doesn't work well with the remote asset download and can be buggy

        // If the SDK libraries (i.e. MFZJni) is loaded, we're probably running a compatible CPU architecture
        // e.g. If the MFZJni ABI for x86 isn't in the APK, the ABI won't load and we're probably not running on a compatible CPU architecture
        //return MyFiziqSdkManager.isLoaded();
    }
}
