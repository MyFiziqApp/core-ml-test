package com.myfiziq.sdk.db;

/**
 * Represents a billing event JSON object.
 */
public class ModelBillingEvent extends Model
{
    @Persistent
    public String aid;

    @Persistent
    public String vid;

    @Persistent
    public String c_misc;

    @Persistent
    public String c_sig;

    @Persistent
    public String c_time;

    @Persistent
    public String c_timeiso;

    @Persistent
    public String c_uid;

    @Persistent
    public String e_id;

    @Persistent
    public String e_misc;

    @Persistent
    public String e_src;

    public ModelBillingEvent()
    {}
}
