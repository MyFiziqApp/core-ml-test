package com.myfiziq.sdk.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @hide
 */

@Cached
public class ModelRemoteAsset extends Model
{
    @Persistent
    public String etag = "";

    @Persistent
    public long size = 0;

    @Persistent
    public boolean force = false;

    private static Pattern mVerPattern = Pattern.compile("[^/]+/[^/]+/[^/]+/[^/]+/([^/]+)/.*");

    public long getVersion()
    {
        Matcher matcher = mVerPattern.matcher(id);
        if (matcher.matches())
        {
            return Long.parseLong(matcher.group(1));
        }

        return 0;
    }
}
