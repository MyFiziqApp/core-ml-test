package com.myfiziq.sdk.db;

public class ModelSupportData extends Model
{
    @Persistent
    public ModelSupportQuery query;
    @Persistent
    public ModelSupportVersions sdk;
    @Persistent
    public ModelDeviceData device;
    @Persistent
    public String Email;
    @Persistent
    public String UserID;
    @Persistent
    public String Title;
    @Persistent
    public String Type;
    @Persistent
    public String Comments;
    @Persistent
    public ModelSupportUser user;
    @Persistent
    public ModelSupportImages images;

    public ModelSupportData()
    {

    }

    public ModelSupportData(ModelSupportQuery query, ModelSupportVersions sdk, ModelDeviceData device, String email, String userID, String title, String type, String comments, ModelSupportUser user, ModelSupportImages images)
    {
        this.query = query;
        this.sdk = sdk;
        this.device = device;
        this.Email = email;
        this.UserID = userID;
        this.Title = title;
        this.Type = type;
        this.Comments = comments;
        this.user = user;
        this.images = images;
    }
}
