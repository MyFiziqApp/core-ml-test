package com.myfiziq.sdk.models;

import com.myfiziq.sdk.db.Measurement;

public class MeasurementFormat
{
    private String pattern;
    private Class<? extends Measurement> measurement;
    private boolean hasDeltaMeasurement;
    private boolean forDeltaMeasurement;

    public MeasurementFormat(String pattern, Class<? extends Measurement> measurement, boolean hasDeltaMeasurement, boolean forDeltaMeasurement)
    {
        this.pattern = pattern;
        this.measurement = measurement;
        this.hasDeltaMeasurement = hasDeltaMeasurement;
        this.forDeltaMeasurement = forDeltaMeasurement;
    }

    public Class<? extends Measurement> getMeasurementClass()
    {
        return measurement;
    }

    public String getPattern()
    {
        return pattern;
    }

    public boolean hasDeltaMeasurement()
    {
        return hasDeltaMeasurement;
    }

    public boolean isForDeltaMeasurement()
    {
        return forDeltaMeasurement;
    }
}
