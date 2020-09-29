package com.myfiziq.sdk.db;

import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */
@Cached
public class ModelAppConf extends Model
{
    /**
     * The Vendor ID.
     */
    @Persistent(idMap = true)
    public String vid = "";

    @Persistent
    public String confbucket = "";

    @Persistent
    public String accountid = "";

    @Persistent
    public String userpool = "";

    @Persistent
    public String status = "";

    @Persistent
    public String trace = "";

    @Persistent
    public String awsregion = "";

    @Persistent
    public String url = "";

    @Persistent
    public String interface_url = "";

    /**
     * The environment we're currently running in.
     */
    @Persistent
    public String env = "";

    @Persistent
    public String vendor = "";

    @Persistent
    public String useridcounter = "";

    @Persistent
    public String idpool = "";

    @Persistent
    public String accstatus = "";

    @Persistent
    public String ingress = "";

    @Persistent
    public String requests = "";

    @Persistent
    public String egress = "";

    @Persistent
    public String results = "";

    @Persistent
    public String socket = "";

    /**
     * The App ID.
     */
    @Persistent
    public String aid = "";

    @Persistent
    public String region = "";

    @Persistent
    public String feedback = "";

    @Persistent
    public String submitbatch = "";

    @Nullable
    public static ModelAppConf getInstance()
    {
        String vid = null;

        if (MyFiziq.getInstance().hasTokens())
        {
            vid = MyFiziq.getInstance().getTokenVid();
        }

        if (!TextUtils.isEmpty(vid))
        {
            ModelAppConf modelAppConf = ORMTable.getModel(ModelAppConf.class, vid);
            if (null != modelAppConf)
                return modelAppConf;
        }

        // Don't throw an exception -> handle null grace fully.
        // This will allow a check for app conf without a try/catch block.
        //throw new IllegalStateException("Unable to obtain application configuration.");

        Timber.e("ModelAppConf is null. This should never happen");

        return null;
    }

    public String getModelKeyId()
    {
        return String.format("%s/%s.key", vid, aid);
    }
}
