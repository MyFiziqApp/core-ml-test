package com.myfiziq.sdk.db;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ModelAvatarDeleteResponseBody
{
    @SerializedName("success")
    @Expose
    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }
}
