package com.myfiziq.sdk.manager;

import com.myfiziq.sdk.manager.compatibility.AndroidSdkCompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.CompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.CpuArchitectureCompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.OpenGlCompatibilityRetirement;
import com.myfiziq.sdk.manager.compatibility.RamCompatibilityRequirement;


// Package private. Should not be exposed to the customer app.
public class MyFiziqCompatibilityService
{
    private static final int MINIMUM_SDK = 24;
    private static final String[] COMPATIBLE_CPU_ARCHITECTURES = new String[] {"arm64-v8a"};
    private static final long MINIMUM_RAM_BYTES = 2147483648L;
    private static final double MINIMUM_OPENGL_VERSION = 3.1;

    /**
     * Evaluates strategies to determine if the current device is supported by MyFiziq.
     */
    public boolean isDeviceCompatible()
    {
        CompatibilityRequirement[] compatibilityRequirements =
                {
                        new AndroidSdkCompatibilityRequirement(MINIMUM_SDK),                        // Android 7.0
                        new CpuArchitectureCompatibilityRequirement(),                              // If MFZJni is inside the APK for the current platform
                        new RamCompatibilityRequirement(MINIMUM_RAM_BYTES),                         // 2GB RAM
                        new OpenGlCompatibilityRetirement(MINIMUM_OPENGL_VERSION)                   // OpenGL 3.1
                };

        return evaluateCompatibility(compatibilityRequirements);
    }

    public static boolean isCpuCompatible()
    {
        CpuArchitectureCompatibilityRequirement compatibilityRequirement = new CpuArchitectureCompatibilityRequirement();
        return compatibilityRequirement.isCompatible();
    }

    private boolean evaluateCompatibility(CompatibilityRequirement[] compatibilityRequirements)
    {
        for (CompatibilityRequirement compatibilityRequirement : compatibilityRequirements)
        {
            if (!compatibilityRequirement.isCompatible())
            {
                return false;
            }
        }

        return true;
    }
}
