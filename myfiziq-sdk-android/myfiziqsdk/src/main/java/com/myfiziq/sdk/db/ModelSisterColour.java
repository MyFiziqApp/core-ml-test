package com.myfiziq.sdk.db;

import android.graphics.Color;
import android.text.TextUtils;

public class ModelSisterColour extends ModelOnDemand
{
    @Persistent
    protected String sourceColour;

    @Persistent
    protected String destinationColour;

    private CachedColor cachedSourceColour = null;

    private CachedColor cachedDestinationColour = null;

    @Override
    public void afterDeserialize()
    {
        super.afterDeserialize();
        getSourceColour();
        getDestinationColour();
    }

    public CachedColor getSourceColour()
    {
        if (null == cachedSourceColour)
        {
            cachedSourceColour = new CachedColor(sourceColour);
        }

        return cachedSourceColour;
    }

    public CachedColor getDestinationColour()
    {
        if (null == cachedDestinationColour)
        {
            cachedDestinationColour = new CachedColor(destinationColour);
        }

        return cachedDestinationColour;
    }
}
