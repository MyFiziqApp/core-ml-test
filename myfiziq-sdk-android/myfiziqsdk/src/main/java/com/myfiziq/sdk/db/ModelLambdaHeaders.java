package com.myfiziq.sdk.db;

/**
 * @hide
 */
public class ModelLambdaHeaders extends Model {
    @Persistent(idMap = true, jsonMap = "MFZ-AppID")
    public String mfzAppId = "";

    @Persistent(jsonMap = "MFZ-Token")
    public String mfzToken = "";

    public ModelLambdaHeaders()
    {

    }

    public ModelLambdaHeaders(String appId, String token)
    {
        mfzToken = token;
        mfzAppId = appId;
    }

    public void set(String appId, String token)
    {
        mfzToken = token;
        mfzAppId = appId;
    }
}
