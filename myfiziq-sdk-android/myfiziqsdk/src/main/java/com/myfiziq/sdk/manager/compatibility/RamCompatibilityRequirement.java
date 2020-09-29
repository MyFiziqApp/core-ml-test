package com.myfiziq.sdk.manager.compatibility;

import android.app.ActivityManager;
import android.content.Context;

import com.myfiziq.sdk.util.GlobalContext;

/**
 * @hide
 */
public class RamCompatibilityRequirement implements CompatibilityRequirement
{
    private long minimumRamInBytes;

    public RamCompatibilityRequirement(long minimumRamInBytes)
    {
        this.minimumRamInBytes = minimumRamInBytes;
    }

    @Override
    public boolean isCompatible()
    {
        long currentDeviceRam = getTotalDeviceRAM();
        return currentDeviceRam >= minimumRamInBytes;
    }

    /**
     * Gets the total amount of RAM the device has in bytes.
     * <p>
     * If the check is unsuccessful, -1 is returned.
     */
    private long getTotalDeviceRAM()
    {
        Context context = GlobalContext.getContext();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager == null)
        {
            return -1;
        }

        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        return memInfo.totalMem;
    }
}
