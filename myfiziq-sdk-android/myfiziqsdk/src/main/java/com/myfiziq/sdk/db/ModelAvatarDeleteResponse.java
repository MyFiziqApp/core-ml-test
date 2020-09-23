package com.myfiziq.sdk.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class ModelAvatarDeleteResponse
{
    @SerializedName("statusCode")
    private Integer statusCode;

    @SerializedName("body")
    private String body;

    public Integer getStatusCode() {
        return statusCode;
    }

    public ModelAvatarDeleteResponseBody getBody()
    {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.fromJson(body, ModelAvatarDeleteResponseBody.class);
    }
}
