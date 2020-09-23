package com.myfiziq.sdk.db;

import android.text.TextUtils;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziqSdk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @hide
 */

@Cached
public class ModelRemoteAssets extends Model
{
    @Persistent
    public ArrayList<ModelRemoteAsset> files = new ArrayList<>();

    /**
     * Find the first asset that matches.
     *
     * @param file
     * @return
     */
    public ModelRemoteAsset findAsset(String file)
    {
        long latest = -1;
        ModelRemoteAsset result = null;
        String assetPathRegex;
        String sdkVersion = BuildConfig.SDK_VERSION;
        boolean bByVendor = true;
        String vid = MyFiziq.getInstance().getTokenVid();
        String aid = MyFiziq.getInstance().getTokenAid();

        do
        {
            if (bByVendor)
            {
                assetPathRegex = String.format("android/%s/BASE/%s_%s/(.*)/%s", sdkVersion, vid, aid, file);
            }
            else
            {
                assetPathRegex = String.format("android/%s/BASE/BASE/(.*)/%s", sdkVersion, file);
            }

            if (files.size() > 0)
            {
                for (ModelRemoteAsset asset : files)
                {
                    if (asset.id.matches(assetPathRegex))
                    {
                        return asset;
                    }
                }
            }

            int ixVerMin = sdkVersion.lastIndexOf('.');
            if (ixVerMin > 0)
            {
                sdkVersion = sdkVersion.substring(0, ixVerMin-1);
            }
            else if (bByVendor)
            {
                // restart search without vendor and with full sdk version.
                bByVendor = false;
                sdkVersion = BuildConfig.SDK_VERSION;
            }
            else
            {
                // nothing left to search.
                sdkVersion = "";
            }
        }
        while (sdkVersion.length() > 0);

        return null;
    }

    /**
     * Find the newest version that matches.
     * Search for SDK full version, the SDK major.minor then SDK major.
     * i.e. 19.1.13 then 19.1 then 19
     * First try to find latest version by VID&AID...
     * Then try to find latest version by BASE/BASE
     * @param file
     * @return
     */
    public ModelRemoteAsset findLatestVersion(String file)
    {
        long latest = -1;
        ModelRemoteAsset result = null;
        String assetPathRegex;
        String sdkVersion = BuildConfig.SDK_VERSION;
        boolean bByVendor = true;
        String vid = MyFiziq.getInstance().getTokenVid();
        String aid = MyFiziq.getInstance().getTokenAid();

        do
        {
            if (bByVendor)
            {
                assetPathRegex = String.format("android/%s/BASE/%s_%s/(.*)/%s", sdkVersion, vid, aid, file);
            }
            else
            {
                assetPathRegex = String.format("android/%s/BASE/BASE/(.*)/%s", sdkVersion, file);
            }

            if (files.size() > 0)
            {
                for (ModelRemoteAsset asset : files)
                {
                    if (asset.id.matches(assetPathRegex))
                    {
                        long version = asset.getVersion();
                        if (version > latest)
                        {
                            latest = version;
                            result = asset;
                        }
                    }
                }
            }

            // Found a result?
            if (latest > 0)
                break;

            int ixVerMin = sdkVersion.lastIndexOf('.');
            if (ixVerMin > 0)
            {
                sdkVersion = sdkVersion.substring(0, ixVerMin);
            }
            else if (bByVendor)
            {
                // restart search without vendor and with full sdk version.
                bByVendor = false;
                sdkVersion = BuildConfig.SDK_VERSION;
            }
            else
            {
                // nothing left to search.
                sdkVersion = "";
            }
        }
        while (sdkVersion.length() > 0);

        return result;
    }

    public synchronized void replaceAsset(ModelRemoteAsset asset)
    {
        if (null != asset)
        {
            String file = asset.id.replaceAll(".*/(.*)", "$1");
            Iterator<ModelRemoteAsset> assetIterator = files.iterator();
            while (assetIterator.hasNext())
            {
                ModelRemoteAsset a = assetIterator.next();
                if (a.id.endsWith(file))
                {
                    assetIterator.remove();
                }
            }

            files.add(asset);
            save();
        }
    }
}
