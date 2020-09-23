package com.myfiziq.sdk.db;

import android.text.TextUtils;

/**
 * @hide
 */

public class ModelAvatarReq extends ModelAvatarAttempt
{
    @Persistent(jsonMap = "AvatarRequest!PercentBodyFat")
    public double PercentBodyFat = 0.0;

    @Persistent(jsonMap = "AvatarRequest!Status")
    public String status = "";

    @Persistent(jsonMap = "AvatarRequest!inseam")
    public double inseam = 0.0;

    @Persistent(jsonMap = "AvatarRequest!weight")
    public double weight = 0.0;

    @Persistent(jsonMap = "AvatarRequest!hip")
    public double hip = 0.0;

    @Persistent(jsonMap = "AvatarRequest!fitness")
    public double fitness = 0.0;

    @Persistent(jsonMap = "AvatarRequest!requestdate")
    public String requestdate = "";

    @Persistent(jsonMap = "AvatarRequest!height")
    public double height = 0.0;

    @Persistent(jsonMap = "AvatarRequest!thigh")
    public double thigh = 0.0;

    @Persistent(jsonMap = "AvatarRequest!completeddate")
    public String completeddate = "";

    @Persistent(jsonMap = "AvatarRequest!chest")
    public double chest = 0.0;

    @Persistent(jsonMap = "AvatarRequest!error_id")
    public String error_id = "";

    @Persistent(jsonMap = "AvatarRequest!gender")
    public Gender gender = Gender.M;

    @Persistent(jsonMap = "AvatarRequest!waist")
    public double waist = 0.0;

    @Persistent(idMap = true, jsonMap = "AvatarRequest!AttemptId")
    public String attemptId = "";

    @Override
    public String getAttemptId()
    {
        return attemptId;
    }

    public boolean isValid()
    {
        if (TextUtils.isEmpty(status))
            return false;

        if (!TextUtils.isEmpty(error_id))
            return false;

        if (weight == 0.0 || height == 0.0)
            return false;

        //TODO: extra validation of other fields...

        return true;
    }

    public boolean isError()
    {
        if (!TextUtils.isEmpty(error_id))
            return true;

        return false;
    }
}
