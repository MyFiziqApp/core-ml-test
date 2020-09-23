package com.myfiziq.sdk.db;

import android.text.TextUtils;

import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;

import java.lang.reflect.Field;

import androidx.annotation.Nullable;
import timber.log.Timber;


public class ModelUserProfile extends Model
{
    @Persistent(idMap = true)
    protected Integer userid;

    @Persistent
    protected Gender gender = Gender.M;

    @Persistent
    // This is a boolean represented as a string. iOS sets this as either "TRUE" or "FALSE" (All in uppercase. Must be uppercase.)
    // Variable name here must match JSON property name for compatibility with iOS
    protected String metric_preferred;                                      // NOSONAR

    @Persistent
    protected Double height;

    @Persistent
    protected Double weight;


    @Nullable
    public static ModelUserProfile getInstance()
    {
        return ORMTable.getModel(ModelUserProfile.class, null);
    }

    @Nullable
    public Integer getUserId()
    {
        return userid;
    }

    public void setUserId(Integer userId)
    {
        this.userid = userId;
    }

    @Nullable
    public Gender getGender()
    {
        return gender;
    }

    public void setGender(Gender gender)
    {
        this.gender = gender;
    }

    @Nullable
    public Class<? extends SystemOfMeasurement> getSystemOfMeasurement()
    {
        if (TextUtils.isEmpty(metric_preferred))
        {
            return null;
        }
        else if (metric_preferred.startsWith("T") || metric_preferred.startsWith("t"))
        {
            return Metric.class;
        }
        else if (metric_preferred.startsWith("F") || metric_preferred.startsWith("f"))
        {
            return Imperial.class;
        }
        else
        {
            Timber.w("Unrecognised boolean value: %s", metric_preferred);
            return null;
        }
    }

    public void setSystemOfMeasurement(@Nullable Class<? extends SystemOfMeasurement> systemOfMeasurement)
    {
        if (systemOfMeasurement == null)
        {
            metric_preferred = null;
        }
        else if (systemOfMeasurement == Metric.class)
        {
            metric_preferred = "TRUE";
        }
        else if (systemOfMeasurement == Imperial.class)
        {
            metric_preferred = "FALSE";
        }
        else
        {
            Timber.w("Unrecognised System of Measurement: %s", systemOfMeasurement);
            metric_preferred = null;
        }
    }

    public Double getHeight()
    {
        return height;
    }

    public void setHeight(double height)
    {
        this.height = height;
    }

    public Double getWeight()
    {
        return weight;
    }

    public void setWeight(double weight)
    {
        this.weight = weight;
    }

    /**
     * Determines if an instance variable exists in this object by its name.
     *
     * @param attributeName The name of the instance variable.
     * @return Whether it exists.
     */
    public boolean has(String attributeName)
    {
        try
        {
            getClass().getDeclaredField(attributeName);
            return true;
        } catch (NoSuchFieldException e)
        {
            Timber.w(e, "Attribute does not exist: %s", attributeName);
            return false;
        }
    }

    /**
     * Gets an instance variable in this object by its name.
     *
     * @param attributeName The name of the instance variable.
     * @param output        The object to populate the variable into (pass by reference).
     */
    public <T> T get(String attributeName, T output)
    {
        try
        {
            Field field = getClass().getDeclaredField(attributeName);
            field.get(output);
        }
        catch (NoSuchFieldException e)
        {
            Timber.w(e, "Attribute does not exist: %s", attributeName);
        }
        catch (IllegalAccessException e)
        {
            Timber.w(e, "Illegal access on field: %s", attributeName);
        }

        return output;
    }

    /**
     * Sets an instance variable in this object by its name.
     *
     * @param attributeName The name of the instance variable.
     * @param newValue      The new value to set for that variable.
     */
    public void set(String attributeName, Object newValue)
    {
        try
        {
            Field field = getClass().getDeclaredField(attributeName);
            field.set(this, newValue);
        }
        catch (NoSuchFieldException e)
        {
            Timber.w(e, "Attribute does not exist: %s", attributeName);
        }
        catch (IllegalAccessException e)
        {
            Timber.w(e, "Illegal access on field: %s", attributeName);
        }
    }

    @Override
    public void save()
    {
        super.save();

        AppWideUnitSystemHelper.invalidateCachedProfile();
    }

    public boolean updateWeight(Weight setWeight)
    {
        if (null == weight)
        {
            weight = setWeight.getValueInKg();
            return true;
        }
        else if (Math.abs(setWeight.getValueInKg() -getWeight()) > 0.01)
        {
            weight = setWeight.getValueInKg();
            return true;
        }

        return false;
    }

    public boolean updateHeight(Length setHeight)
    {
        if (null == height)
        {
            height = setHeight.getValueInCm();
            return true;
        }
        else if (Math.abs(setHeight.getValueInCm() - getHeight()) > 0.01)
        {
            height = setHeight.getValueInCm();
            return true;
        }

        return false;
    }
}
