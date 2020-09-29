package com.myfiziq.sdk.db;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewParent;
import android.widget.RadioButton;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.sdk.views.MYQBottomNavigationView;
import com.myfiziq.sdk.views.TintableButton;

import java.util.ArrayList;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.widget.CompoundButtonCompat;

public class ModelTextColorStateMap extends ModelOnDemand
{
    @Persistent
    public String sourceColour;

    @Persistent
    public ArrayList<String> destinationColours;

    private CachedColor cachedSourceColour = null;

    private CachedColorArray cachedDestinationColours = null;

    @Override
    public void afterDeserialize()
    {
        getSourceColour();
        getDestinationColours();
    }

    public void applyStyle(View view, ViewParent parent)
    {
        if (view instanceof TabLayout)
        {
            getDestinationColours().apply((colors)->((TabLayout)view).setTabTextColors(colors[0], colors[1]));
        }
        else if (view instanceof TintableButton)
        {
            getDestinationColours().apply((colors)->{
                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{android.R.attr.state_enabled}},
                        colors);

                Drawable drawable = view.getBackground();
                drawable.setTintList(colorStateList);
            });
        }
        else if (view instanceof RadioButton)
        {
            getDestinationColours().apply((colors)->{
                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{android.R.attr.state_enabled}},
                        colors);
                ((RadioButton) view).setButtonTintList(colorStateList);
            });
        }
        else if (view instanceof MYQBottomNavigationView)
        {
            getDestinationColours().apply((colors)->{
                ColorStateList colorStateList = new ColorStateList(
                        new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                        colors);

                ((MYQBottomNavigationView) view).setItemIconTintList(colorStateList);
                ((MYQBottomNavigationView) view).setItemTextColor(colorStateList);
            });
        }
        else if (view instanceof AppCompatCheckBox)
        {
            //TODO... do we need more than one color?
            getDestinationColours().apply((colors)->CompoundButtonCompat.setButtonTintList((AppCompatCheckBox) view, ColorStateList.valueOf(colors[0])));
        }
    }

    public CachedColor getSourceColour()
    {
        if (null == cachedSourceColour)
        {
            cachedSourceColour = new CachedColor(sourceColour);
        }

        return cachedSourceColour;
    }

    public CachedColorArray getDestinationColours()
    {
        if (cachedDestinationColours == null)
        {
            cachedDestinationColours = new CachedColorArray(destinationColours);
        }

        return cachedDestinationColours;
    }
}
