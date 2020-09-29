package com.myfiziq.sdk.db;

public class ModelSupportUser extends Model
{
    @Persistent
    public Double height;
    @Persistent
    public String gender;
    @Persistent
    public Double weight;
    @Persistent
    public String email;
    @Persistent
    public String userID;
    @Persistent
    public ModelSupportMeasurements measurements;

    public ModelSupportUser()
    {

    }

    public ModelSupportUser(ModelAvatar avatar, String email, String userID, ModelSupportMeasurements measurements)
    {
        this.height = avatar.getHeight().valueInCm;
        this.gender = avatar.getGender().name();
        this.weight = avatar.getWeight().valueInKg;
        this.email = email;
        this.userID = userID;
        this.measurements = measurements;
    }
}
