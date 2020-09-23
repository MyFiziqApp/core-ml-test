package com.myfiziq.sdk.helpers;

import android.content.res.Resources;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.vo.BodyFatCategory;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class BodyFatCategoryFormatter
{
    /**
     * Returns text to indicate which body fat category a measurement belong to.
     * @return A category or null if the category is unknown (e.g. empty).
     */
    @Nullable
    public static String getIndicatorText(Resources resources, BodyFatCategory bodyFatCategory)
    {
        switch (bodyFatCategory)
        {
            case UNDERWEIGHT:
                return resources.getString(R.string.body_fat_indicator_underweight);
            case HEALTHY:
                return resources.getString(R.string.body_fat_indicator_normal);
            case OVERWEIGHT:
                return resources.getString(R.string.body_fat_indicator_overweight);
            case OBESE:
                return resources.getString(R.string.body_fat_indicator_obese);
            default:
                return null;
        }
    }

    /**
     * Returns a colour to indicate which body fat category a measurement belong to.
     * @return A colour or 0 if the category is unknown (e.g. empty).
     */
    @ColorInt
    public static int getIndicatorColour(Resources resources, BodyFatCategory bodyFatCategory)
    {
        switch (bodyFatCategory)
        {
            case UNDERWEIGHT:
                return resources.getColor(R.color.myfiziqsdk_body_fat_indicator_underweight);
            case HEALTHY:
                return resources.getColor(R.color.myfiziqsdk_body_fat_indicator_normal);
            case OVERWEIGHT:
                return resources.getColor(R.color.myfiziqsdk_body_fat_indicator_overweight);
            case OBESE:
                return resources.getColor(R.color.myfiziqsdk_body_fat_indicator_obese);
            default:
                return 0;
        }
    }
}
