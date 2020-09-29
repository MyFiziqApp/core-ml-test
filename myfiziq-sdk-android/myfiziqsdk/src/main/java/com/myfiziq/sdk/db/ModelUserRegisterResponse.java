package com.myfiziq.sdk.db;

import com.myfiziq.sdk.util.JwtUtils;

@Cached
public class ModelUserRegisterResponse extends Model
{
    @Persistent
    public String status = "";

    @Persistent
    public String uid = "";

    @Persistent
    public int statusCode = 0;

    public String getUsername()
    {
        return JwtUtils.getTokenItem(uid, "cognito:username");
    }
}
