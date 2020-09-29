package com.myfiziq.sdk.db;

import com.google.gson.annotations.Expose;

/**
 * @hide
 */
public class ModelLambdaHeader extends Model {
    @Persistent(childIdMap = "MFZ-AppID")
    @Expose
    public ModelLambdaHeaders headers = new ModelLambdaHeaders();

    public ModelLambdaHeader()
    {

    }

    public ModelLambdaHeader(String appId, String token)
    {
        headers.set(appId, token);
    }
}
