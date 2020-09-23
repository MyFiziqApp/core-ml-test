package com.myfiziq.sdk.db;

/**
 * @hide
 */

public class ModelAppToken extends Model
{
    @Persistent(idMap = true)
    public String vid = "";

    @Persistent
    public String url = "";

    @Persistent
    public String aid = "";

    @Persistent
    public String cid = "";
}
