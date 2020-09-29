package com.myfiziq.sdk.db;

public class ModelSupportVersions extends Model
{
    @Persistent
    public String app;

    @Persistent
    public String myfiziq;

    public ModelSupportVersions()
    {

    }

    public ModelSupportVersions(String app, String myfiziq)
    {
        this.app = app;
        this.myfiziq = myfiziq;
    }
}
