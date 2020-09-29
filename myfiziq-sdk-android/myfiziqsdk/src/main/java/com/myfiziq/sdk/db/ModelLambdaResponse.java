package com.myfiziq.sdk.db;

/**
 * @hide
 */
public class ModelLambdaResponse extends Model
{
    @Persistent
    public String body = "";

    @Persistent
    public int statusCode = 0;
}
