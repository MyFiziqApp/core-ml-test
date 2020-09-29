package com.myfiziq.sdk.db;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewParent;
import android.widget.ProgressBar;

import com.myfiziq.sdk.views.TintableImageView;

public class ModelViewBgTintColorMap extends ModelSisterColour
{
    @Override
    public void applyStyle(View view, ViewParent parent)
    {
        if (view instanceof ProgressBar)
        {
            getDestinationColour().apply((color)-> ((ProgressBar) view).getIndeterminateDrawable().setTint(color));
        }
        else if (view instanceof TintableImageView)
        {
            Drawable d = ((TintableImageView)view).getDrawable();
            if (null != d)
            {
                getDestinationColour().apply(d::setTint);
            }
        }
        else
        {
            if (getSourceColour().isNull())
            {
                Drawable d = view.getBackground();
                if (d != null)
                {
                    getDestinationColour().apply(d::setTint);
                }
            }
            else
            {
                Drawable drawable = view.getBackground();
                if (drawable instanceof ColorDrawable && (getSourceColour().nullOrMatch(((ColorDrawable) drawable).getColor())))
                {
                    getDestinationColour().apply(drawable::setTint);
                }
            }
        }
    }
}
