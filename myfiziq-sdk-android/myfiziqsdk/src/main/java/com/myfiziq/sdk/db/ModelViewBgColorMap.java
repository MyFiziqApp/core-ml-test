package com.myfiziq.sdk.db;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewParent;

public class ModelViewBgColorMap extends ModelSisterColour
{
    public void applyStyle(View view, ViewParent parent)
    {
        if (getSourceColour().isNull())
        {
            getDestinationColour().apply(view::setBackgroundColor);
        }
        else
        {
            Drawable drawable = view.getBackground();
            if (drawable instanceof ColorDrawable && getSourceColour().nullOrMatch(((ColorDrawable)drawable).getColor()))
            {
                getDestinationColour().apply(view::setBackgroundColor);
            }
        }
    }
}
