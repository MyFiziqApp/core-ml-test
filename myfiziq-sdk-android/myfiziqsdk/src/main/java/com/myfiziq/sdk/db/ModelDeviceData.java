package com.myfiziq.sdk.db;

public class ModelDeviceData extends Model
{
    @Persistent
    public String model;
    @Persistent
    public String manufacturer;
    @Persistent
    public String os;

    public ModelDeviceData()
    {

    }

    public ModelDeviceData(String model, String manufacturer, String os)
    {
        this.model = model;
        this.manufacturer = manufacturer;
        this.os = os;
    }
}
