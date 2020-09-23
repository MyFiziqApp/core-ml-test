package com.myfiziq.sdk.helpers;

import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Feet;
import com.myfiziq.sdk.db.Imperial;
import com.myfiziq.sdk.db.Inches;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.Metric;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Pounds;
import com.myfiziq.sdk.db.SystemOfMeasurement;
import com.myfiziq.sdk.db.Weight;

import java.util.Locale;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class AppWideUnitSystemHelper
{
    @Nullable
    private ModelUserProfile userProfile = null;

    private static AppWideUnitSystemHelper INSTANCE;


    private AppWideUnitSystemHelper()
    {
    }

    public static AppWideUnitSystemHelper getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new AppWideUnitSystemHelper();
            initialiseUserProfileCache();
        }

        return INSTANCE;
    }

    public static Class<? extends SystemOfMeasurement> getAppWideUnitSystemSync()
    {
        AppWideUnitSystemHelper self = getInstance();
        initialiseUserProfileCache();

        if (self.userProfile != null && self.userProfile.getSystemOfMeasurement() != null)
        {
            // Use System of Measurement from Profile if it's been set
            return self.userProfile.getSystemOfMeasurement();
        }
        else
        {
            // Else, use it from the locale
            return self.determineFromLocale();
        }
    }

    /**
     * Gets the desired Weight Units of Measurement that the user has selected.
     *
     * If they have not selected a Units of Measurement, it is determined from the phone's locale.
     */
    public static Class<? extends Weight> getAppWideWeightUnit()
    {
        Class<? extends SystemOfMeasurement> desiredSystem = getAppWideUnitSystemSync();

        if (desiredSystem == Imperial.class)
        {
            return Pounds.class;
        }
        else if (desiredSystem == Metric.class)
        {
            return Kilograms.class;
        }
        else
        {
            Timber.e("Logic error. Cannot determine system of measurement. Defaulting to kilograms");
            return Kilograms.class;
        }
    }

    /**
     * Gets the desired Length Units of Measurement that the user has selected.
     *
     * If they have not selected a Units of Measurement, it is determined from the phone's locale.
     */
    public static Class<? extends Length> getAppWideLengthUnit()
    {
        Class<? extends SystemOfMeasurement> desiredSystem = getAppWideUnitSystemSync();

        if (desiredSystem == Imperial.class)
        {
            return Inches.class;
        }
        else if (desiredSystem == Metric.class)
        {
            return Centimeters.class;
        }
        else
        {
            Timber.e("Logic error. Cannot determine system of measurement. Defaulting to centimeters");
            return Centimeters.class;
        }
    }

    /**
     * Gets the desired Height Units of Measurement that the user has selected.
     *
     * If they have not selected a Units of Measurement, it is determined from the phone's locale.
     */
    public static Class<? extends Length> getAppWideHeightUnit()
    {
        Class<? extends SystemOfMeasurement> desiredSystem = getAppWideUnitSystemSync();

        if (desiredSystem == Imperial.class)
        {
            return Feet.class;
        }
        else if (desiredSystem == Metric.class)
        {
            return Centimeters.class;
        }
        else
        {
            Timber.e("Logic error. Cannot determine system of measurement. Defaulting to centimeters");
            return Centimeters.class;
        }
    }

    public static void invalidateCachedProfile()
    {
        AppWideUnitSystemHelper self = getInstance();

        self.userProfile = null;
        initialiseUserProfileCache();
    }

    private static void initialiseUserProfileCache()
    {
        AppWideUnitSystemHelper self = getInstance();

        if (self.userProfile == null)
        {
            self.userProfile = ORMTable.getModel(ModelUserProfile.class, null);
        }

        // If we already have a user profile cached, do nothing
    }

    private Class<? extends SystemOfMeasurement> determineFromLocale()
    {
        String countryCode = Locale.getDefault().getCountry();

        if (countryCode.equals("US") || countryCode.equals("LR") || countryCode.equals("MM"))
        {
            return Imperial.class;
        }
        else
        {
            return Metric.class;
        }
    }
}
