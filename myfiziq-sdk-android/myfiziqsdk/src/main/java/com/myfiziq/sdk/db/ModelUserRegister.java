package com.myfiziq.sdk.db;

public class ModelUserRegister extends Model
{
    @Persistent
    public String aid = "";

    @Persistent
    public String email = "";

    public ModelUserRegister()
    {

    }

    public ModelUserRegister(String appId, String userEmail)
    {
        aid = appId;
        email = userEmail;
    }

    public void set(String appId, String userEmail)
    {
        aid = appId;
        email = userEmail;
    }
}
