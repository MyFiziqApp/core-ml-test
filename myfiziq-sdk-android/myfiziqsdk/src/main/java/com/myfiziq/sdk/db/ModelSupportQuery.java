package com.myfiziq.sdk.db;

public class ModelSupportQuery extends Model
{
    @Persistent
    public String queryType;
    @Persistent
    public String message;
    @Persistent
    public Boolean agreedToTerms;

    public ModelSupportQuery()
    {

    }

    public ModelSupportQuery(String reason, String message, Boolean agreedToTerms)
    {
        this.queryType = reason;
        this.message = message;
        this.agreedToTerms = agreedToTerms;
    }
}
