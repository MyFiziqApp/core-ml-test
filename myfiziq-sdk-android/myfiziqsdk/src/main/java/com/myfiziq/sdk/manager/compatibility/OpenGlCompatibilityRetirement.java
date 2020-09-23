package com.myfiziq.sdk.manager.compatibility;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;

import com.myfiziq.sdk.util.GlobalContext;

/**
 * @hide
 */
public class OpenGlCompatibilityRetirement implements CompatibilityRequirement
{
    private double minimumOpenGlVersion;

    public OpenGlCompatibilityRetirement(double minimumOpenGlVersion)
    {
        this.minimumOpenGlVersion = minimumOpenGlVersion;
    }

    @Override
    public boolean isCompatible()
    {
        double openGlVersion = getOpenGLVersion();
        return openGlVersion >= minimumOpenGlVersion;
    }

    /**
     * Returns the version of OpenGL that the device supports.
     */
    private double getOpenGLVersion()
    {
        int openGlVersion = getOpenGlVersionFromPackageManager();

        int majorVersion = getGlEsMajorVersion(openGlVersion);
        int minorVersion = getGlEsMinorVersion(openGlVersion);

        return majorVersion + (minorVersion * 0.1);
    }

    /**
     * Taken from OpenGlEsVersionTest.java#getVersionFromPackageManager(Context context)
     * <p>
     * https://android.googlesource.com/platform/cts/+/2b87267/tests/tests/graphics/src/android/opengl/cts/OpenGlEsVersionTest.java
     */
    private int getOpenGlVersionFromPackageManager()
    {
        Context context = GlobalContext.getContext();

        PackageManager packageManager = context.getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();

        if (featureInfos.length > 0)
        {
            for (FeatureInfo featureInfo : featureInfos)
            {
                // Null feature name means this feature is the open gl es version feature.
                if (featureInfo.name == null)
                {
                    if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED)
                    {
                        return featureInfo.reqGlEsVersion;
                    }
                    else
                    {
                        return 1 << 16; // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }

        return 1;
    }

    private int getGlEsMajorVersion(int glEsVersion)
    {
        return ((glEsVersion & 0xffff0000) >> 16);
    }

    private int getGlEsMinorVersion(int glEsVersion)
    {
        return glEsVersion & 0xffff;
    }
}
