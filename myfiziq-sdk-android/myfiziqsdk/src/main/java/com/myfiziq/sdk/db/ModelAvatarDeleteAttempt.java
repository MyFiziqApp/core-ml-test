package com.myfiziq.sdk.db;

public class ModelAvatarDeleteAttempt extends Model
{
    @Persistent
    public String attemptId = "";

    public ModelAvatarDeleteAttempt()
    {
    }

    public void setAttemptId(String attemptId)
    {
        this.attemptId = attemptId;
    }
}
