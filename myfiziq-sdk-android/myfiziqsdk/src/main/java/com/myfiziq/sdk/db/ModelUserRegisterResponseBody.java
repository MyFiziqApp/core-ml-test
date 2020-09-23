package com.myfiziq.sdk.db;

public class ModelUserRegisterResponseBody extends Model
{
    @Persistent(escaped = true)
    public ModelUserRegisterResponse body = new ModelUserRegisterResponse();

    @Persistent
    public int statusCode;
}