package com.myfiziq.sdk.helpers;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.vo.BodyFatCategory;

import androidx.annotation.ColorInt;

public class InsightsFormulas
{
    public enum InsightsOutcome
    {
        LOW,
        MEDIUM,
        HIGH,
        VERYHIGH,
        UNKNOWN;

        public String displayName()
        {
            if (this == VERYHIGH)
            {
                return "VERY HIGH";
            }
            else return this.name();
        }
    }

    public static String getEthnicityDisplayFromNumber(int ethnumber, Resources res)
    {
        switch (ethnumber)
        {
            case 1:
                return res.getString(R.string.ethnicity1);
            case 2:
                return res.getString(R.string.ethnicity2);
            case 3:
                return res.getString(R.string.ethnicity3);
        }
        return "unknown";
    }

    public static String getEthnicityAbbreviation(int ethnumber, Resources res)
    {
        switch (ethnumber)
        {
            case 1:
                return res.getString(R.string.eth1abbr);
            case 2:
                return res.getString(R.string.eth2abbr);
            case 3:
                return res.getString(R.string.eth3abbr);
        }
        return "unknown";
    }

    public static InsightsOutcome WaistCircumference(double measurecm, Gender gender, int ethnicity)
    {
        if (ethnicity == 1)
        {
            if (gender == Gender.M)
            {
                if (measurecm < 94) return InsightsOutcome.LOW;
                if (94 <= measurecm && measurecm < 102) return InsightsOutcome.MEDIUM;
                if (measurecm >= 102) return InsightsOutcome.HIGH;
            }
            if (gender == Gender.F)
            {
                if (measurecm < 80) return InsightsOutcome.LOW;
                if (80 <= measurecm && measurecm < 88) return InsightsOutcome.MEDIUM;
                if (measurecm >= 88) return InsightsOutcome.HIGH;
            }
        }
        if (ethnicity == 2 || ethnicity == 3)
        {
            if (gender == Gender.M)
            {
                if (measurecm < 90) return InsightsOutcome.LOW;
                if (measurecm >= 90) return InsightsOutcome.HIGH;
            }
            if (gender == Gender.F)
            {
                if (measurecm < 80) return InsightsOutcome.LOW;
                if (measurecm >= 80) return InsightsOutcome.HIGH;
            }
        }
        return InsightsOutcome.UNKNOWN;
    }

    public static InsightsOutcome WaistHipRatio(double measurecm, Gender gender, int ethnicity)
    {
        if (ethnicity == 1 || ethnicity == 3)
        {
            if (gender == Gender.M)
            {
                if (measurecm < 0.90) return InsightsOutcome.LOW;
                if (measurecm >= 0.90) return InsightsOutcome.HIGH;
            }
            if (gender == Gender.F)
            {
                if (measurecm < 0.85) return InsightsOutcome.LOW;
                if (measurecm >= 0.85) return InsightsOutcome.HIGH;
            }
        }
        if (ethnicity == 2)
        {
            if (gender == Gender.M)
            {
                if (measurecm < 0.90) return InsightsOutcome.LOW;
                if (measurecm >= 0.90) return InsightsOutcome.HIGH;
            }
            if (gender == Gender.F)
            {
                if (measurecm < 0.80) return InsightsOutcome.LOW;
                if (measurecm >= 0.80) return InsightsOutcome.HIGH;
            }
        }
        return InsightsOutcome.UNKNOWN;
    }

    public static InsightsOutcome WaistHeightRatio(double measurecm, Gender gender, int ethnicity)
    {
        if (ethnicity == 1 || ethnicity == 3 || ethnicity == 2)
        {
            if (gender == Gender.M || gender == Gender.F)
            {
                if (measurecm < 0.50) return InsightsOutcome.LOW;
                if (measurecm >= 0.50) return InsightsOutcome.HIGH;
            }
        }
        return InsightsOutcome.UNKNOWN;
    }

    public static InsightsOutcome OverallRisk(double waistCircumferencecm, Gender gender, int ethnicity, BodyFatCategory bodyFatCategory)
    {
        if (gender == Gender.M)
        {
            if (ethnicity == 1)
            {
                if (bodyFatCategory == BodyFatCategory.UNDERWEIGHT || bodyFatCategory == BodyFatCategory.HEALTHY)
                {
                    if (waistCircumferencecm < 94) return InsightsOutcome.LOW;
                    if (waistCircumferencecm >= 94 && waistCircumferencecm < 102)
                        return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.HIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OVERWEIGHT)
                {
                    if (waistCircumferencecm < 94) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 94 && waistCircumferencecm < 102)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.VERYHIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OBESE)
                {
                    if (waistCircumferencecm < 94) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 94 && waistCircumferencecm < 102)
                        return InsightsOutcome.VERYHIGH;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.VERYHIGH;
                }
            }
            if (ethnicity == 2 || ethnicity == 3)
            {
                if (bodyFatCategory == BodyFatCategory.UNDERWEIGHT || bodyFatCategory == BodyFatCategory.HEALTHY)
                {
                    if (waistCircumferencecm < 90) return InsightsOutcome.LOW;
                    if (waistCircumferencecm >= 90 && waistCircumferencecm < 102)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.VERYHIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OVERWEIGHT)
                {
                    if (waistCircumferencecm < 90) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 90 && waistCircumferencecm < 102)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.VERYHIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OBESE)
                {
                    if (waistCircumferencecm < 90) return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 90 && waistCircumferencecm < 102)
                        return InsightsOutcome.VERYHIGH;
                    if (waistCircumferencecm >= 102) return InsightsOutcome.VERYHIGH;
                }
            }
        }

        if (gender == Gender.F)
        {
            if (ethnicity == 1)
            {
                if (bodyFatCategory == BodyFatCategory.UNDERWEIGHT || bodyFatCategory == BodyFatCategory.HEALTHY)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.LOW;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.HIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OVERWEIGHT)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.LOW;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.HIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OBESE)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.VERYHIGH;
                }
            }
            if (ethnicity == 2 || ethnicity == 3)
            {
                if (bodyFatCategory == BodyFatCategory.UNDERWEIGHT || bodyFatCategory == BodyFatCategory.HEALTHY)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.LOW;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.VERYHIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OVERWEIGHT)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.VERYHIGH;
                }
                if (bodyFatCategory == BodyFatCategory.OBESE)
                {
                    if (waistCircumferencecm < 80) return InsightsOutcome.MEDIUM;
                    if (waistCircumferencecm >= 80 && waistCircumferencecm < 88)
                        return InsightsOutcome.HIGH;
                    if (waistCircumferencecm >= 88) return InsightsOutcome.VERYHIGH;
                }
            }
        }
        return InsightsOutcome.UNKNOWN;
    }

    public static InsightsOutcome convertBFCtoBctOutcome(BodyFatCategory bfc)
    {
        if (bfc == BodyFatCategory.UNDERWEIGHT) return InsightsOutcome.LOW;
        if (bfc == BodyFatCategory.HEALTHY) return InsightsOutcome.MEDIUM;
        if (bfc == BodyFatCategory.OVERWEIGHT) return InsightsOutcome.HIGH;
        if (bfc == BodyFatCategory.OBESE) return InsightsOutcome.VERYHIGH;
        else return InsightsOutcome.VERYHIGH;
    }

    public static Drawable getPillDrawable(InsightsOutcome outcome, Resources resources)
    {
        int indicatorColour = getPillColour(outcome, resources);
        Drawable image = resources.getDrawable(R.drawable.bct_outcome_pill);
        image.setColorFilter(new PorterDuffColorFilter(indicatorColour, PorterDuff.Mode.SRC));
        return image;
    }

    @ColorInt
    public static int getPillColour(InsightsOutcome outcome, Resources resources)
    {
        switch (outcome)
        {
            case LOW:
                return resources.getColor(R.color.indicator_low);
            case MEDIUM:
                return resources.getColor(R.color.indicator_medium);
            case HIGH:
                return resources.getColor(R.color.indicator_high);
            case VERYHIGH:
                return resources.getColor(R.color.indicator_very_high);
            default:
                return 0;
        }
    }
}
