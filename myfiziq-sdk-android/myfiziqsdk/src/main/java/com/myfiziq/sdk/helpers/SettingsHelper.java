package com.myfiziq.sdk.helpers;

import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Weight;

public class SettingsHelper
{
    private SettingsHelper()
    {
        // Empty hidden constructor for the utility class
    }

    public static Class<? extends Length> getPreferredChestUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getAdjustedChest().getClass();
        }
        else
        {
            return avatar2.getAdjustedChest().getClass();
        }
    }

    public static Class<? extends Length> getPreferredWaistUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getAdjustedWaist().getClass();
        }
        else
        {
            return avatar2.getAdjustedWaist().getClass();
        }
    }

    public static Class<? extends Length> getPreferredHipsUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getAdjustedHip().getClass();
        }
        else
        {
            return avatar2.getAdjustedHip().getClass();
        }
    }

    public static Class<? extends Length> getPreferredThighUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getAdjustedThigh().getClass();
        }
        else
        {
            return avatar2.getAdjustedThigh().getClass();
        }
    }

    public static Class<? extends Length> getPreferredHeightUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getHeight().getClass();
        }
        else
        {
            return avatar2.getHeight().getClass();
        }
    }

    public static Class<? extends Weight> getPreferredWeightUnitOfMeasurement(ModelAvatar avatar1, ModelAvatar avatar2)
    {
        if (avatar1.getRequestTime() > avatar2.getRequestTime())
        {
            return avatar1.getWeight().getClass();
        }
        else
        {
            return avatar2.getWeight().getClass();
        }
    }
}
