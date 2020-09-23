package com.myfiziq.sdk.db;

public class ModelUserRegisterBody extends Model
{
    @Persistent(escaped = true)
    public ModelUserRegister body = new ModelUserRegister();

    public ModelUserRegisterBody()
    {

    }

    public ModelUserRegisterBody(String appId, String email)
    {
        body.set(appId, email);
    }
}
