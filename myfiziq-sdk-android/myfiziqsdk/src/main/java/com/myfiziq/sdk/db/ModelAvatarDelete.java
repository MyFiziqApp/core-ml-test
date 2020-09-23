package com.myfiziq.sdk.db;

import java.util.ArrayList;

public class ModelAvatarDelete extends Model
{
    @Persistent
    public ModelLambdaHeaders headers;

    @Persistent(escaped = true)
    public ArrayList<ModelAvatarDeleteAttempt> body = new ArrayList<ModelAvatarDeleteAttempt>();

    public ModelAvatarDelete()
    {
    }

    public ModelAvatarDelete(ModelLambdaHeaders headers, String[] attemptIds)
    {
        this.headers = headers;

        for (String attemptId : attemptIds)
        {
            ModelAvatarDeleteAttempt model = new ModelAvatarDeleteAttempt();
            model.setAttemptId(attemptId);
            body.add(model);
        }
    }
}
