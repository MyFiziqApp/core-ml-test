package com.myfiziq.sdk.db;

@Cached
public class ModelLocalUserData extends Model
{
    @Persistent
    protected String value;

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public void setId(String guestName, LocalUserDataKey key)
    {
        this.id = generateIdString(guestName, key);
    }

    public static String generateIdString(String guestName, LocalUserDataKey key)
    {
        return guestName + key.toString();
    }
}
