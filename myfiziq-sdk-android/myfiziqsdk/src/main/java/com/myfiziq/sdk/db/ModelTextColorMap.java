package com.myfiziq.sdk.db;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

public class ModelTextColorMap extends ModelSisterColour
{
    public void applyStyle(View view, ViewParent parent)
    {
        if (view instanceof TextView)
        {
            TextView tv = (TextView)view;
            if (getSourceColour().nullOrMatch(tv.getCurrentTextColor()))
            {
                getDestinationColour().apply(tv::setTextColor);
            }
        }
        else if (view instanceof TabLayout)
        {
            getDestinationColour().apply((color)->
            {
                TabLayout tl = (TabLayout) view;
                int[] selected_state = new int[]{android.R.attr.state_selected};
                ColorStateList cs = tl.getTabTextColors();
                tl.setTabTextColors(color, cs.getColorForState(selected_state, color));
            });
        }
    }
    /*
    public void apply(TextView view)
    {
        if (getSourceColour() < 0 || view.getCurrentTextColor() == getSourceColour())
        {
            view.setTextColor(getDestinationColour());
        }
    }
    */
}
