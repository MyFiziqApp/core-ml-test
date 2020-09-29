package com.myfiziq.sdk.db;

import android.util.Base64;

import com.myfiziq.sdk.util.BaseUtils;

public class ModelOnDemandData extends Model
{
    @Persistent
    public String version = "";

    @Persistent
    public String format = "";

    @Persistent
    public String title = "";

    @Persistent
    public String payload = "";

    @Persistent
    public boolean enabled = false;

    public ModelOnDemandData()
    {

    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public String getVersion()
    {
        return version;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDecoded()
    {
        // Base91 format.
        if (format.contentEquals("b91"))
        {
            return new String(BaseUtils.decode(payload, BaseUtils.Format.string));
        }
        else if (format.contentEquals("b64"))
        {
            return new String(Base64.decode(payload, Base64.DEFAULT));
        }

        // fallback to plain payload.
        return payload;
    }
}
