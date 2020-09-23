package com.myfiziq.sdk.lifecycle;

import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.sdk.db.ModelCardColorMap;
import com.myfiziq.sdk.db.ModelOnDemandData;
import com.myfiziq.sdk.db.ModelOnDemandFont;
import com.myfiziq.sdk.db.ModelOnDemandImage;
import com.myfiziq.sdk.db.ModelSisterStyle;
import com.myfiziq.sdk.db.ModelTextColorMap;
import com.myfiziq.sdk.db.ModelTextColorStateMap;
import com.myfiziq.sdk.db.ModelViewBgColorMap;
import com.myfiziq.sdk.db.ModelViewBgTintColorMap;
import com.myfiziq.sdk.manager.MyFiziqFontManager;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.CircleCountdown;
import com.myfiziq.sdk.views.CircleTextView;
import com.myfiziq.sdk.views.MYQBottomNavigationView;

import org.jetbrains.annotations.NotNull;

import androidx.cardview.widget.CardView;
import io.github.inflationx.viewpump.InflateResult;
import io.github.inflationx.viewpump.Interceptor;

public class StyleInterceptor implements Interceptor
{
    final static String TAG = StyleInterceptor.class.getName();

    private ModelSisterStyle mStyle = null;

    public StyleInterceptor(ModelSisterStyle style)
    {
        mStyle = style;
    }

    public void init()
    {
        for (ModelOnDemandFont fontMap : mStyle.viewFonts)
        {
            MyFiziqFontManager.getInstance().loadFont(fontMap.getFontName());
        }

        // Run through the styling to init what class caches we can.
        applyStyle(new View(GlobalContext.getContext()), null);
    }

    @NotNull
    @Override
    public InflateResult intercept(@NotNull Chain chain)
    {
        InflateResult result = chain.proceed(chain.request());
        View view = result.view();
        if (view == null) return result;

        ViewParent parent = null;
        View vp = chain.request().parent();
        if (vp instanceof ViewParent)
        {
            parent = (ViewParent)vp;
        }

        applyStyle(view, parent);

        return result;
    }

    public void applyStyle(View view, ViewParent parent)
    {
        //long startTime = System.nanoTime();

        // Apply font(s).
        if (view instanceof TextView)
        {
            if (null != mStyle.viewFonts)
            {
                for (ModelOnDemandFont fontMap : mStyle.viewFonts)
                {
                    fontMap.applyStyle(view, parent);
                }
            }
        }

        // Apply text color(s)
        if (null != mStyle.textColour)
        {
            for (ModelTextColorMap color : mStyle.textColour)
            {
                if (color.matches(view, parent))
                {
                    color.applyStyle(view, parent);
                }
            }
        }

        // Apply text color state(s) - items with stateful colours.
        if (null != mStyle.textColorState)
        {
            for (ModelTextColorStateMap color : mStyle.textColorState)
            {
                if (color.matches(view, parent))
                {
                    color.applyStyle(view, parent);
                }
            }
        }

        // Apply background color(s)
        if (null != mStyle.viewBgColour)
        {
            for (ModelViewBgColorMap color : mStyle.viewBgColour)
            {
                if (color.matches(view, parent))
                {
                    color.applyStyle(view, parent);
                }
            }
        }

        // Apply Tint background color
        if (null != mStyle.tintColour)
        {
            for (ModelViewBgTintColorMap row : mStyle.tintColour)
            {
                if (row.matches(view, parent))
                {
                    row.applyStyle(view, parent);
                }
            }
        }

        if (view instanceof ImageView && null != mStyle.images)
        {
            for (ModelOnDemandImage image : mStyle.images)
            {
                if (image.matches(view, parent))
                {
                    image.applyStyle(view, parent);
                }
            }
        }

        if (view instanceof CircleCountdown)
        {
            mStyle.getCircleCountdownColor().apply((color)->((CircleCountdown) view).mColor = color);
        }
        else if (view instanceof CircleTextView)
        {
            mStyle.getTextCountdownColor().apply((color)-> ((CircleTextView) view).strokePaint.setColor(color));
        }
        else if (view instanceof Spinner)
        {
            mStyle.getSpinnerPopupColor().apply((color)-> ((Spinner) view).getPopupBackground().setTint(color));
        }
        else if (view instanceof TabLayout)
        {
            mStyle.getTabIndicatorColor().apply(((TabLayout) view)::setSelectedTabIndicatorColor);
        }
        else if (view instanceof CardView)
        {
            // Apply card color
            if (null != mStyle.cardColour)
            {
                for (ModelCardColorMap card : mStyle.cardColour)
                {
                    if (card.matches(view, parent))
                    {
                        card.applyStyle(view, parent);
                    }
                }
            }
        }
        else if (view instanceof MYQBottomNavigationView)
        {
            mStyle.getBottomNavLabels().apply(((MYQBottomNavigationView) view)::setLabelVisibilityMode);

            mStyle.getBottomNavIconDp().apply((iconSize)->{
                float dp = UiUtils.convertDpToPixel(view.getContext(), iconSize);
                ((MYQBottomNavigationView) view).setItemIconSize(Math.round(dp));
            });
        }

        //long endTime = System.nanoTime();
        //Timber.i("Time taken to evaluate: %sms", (endTime - startTime) / 1000000);
    }

    public ModelOnDemandData getData(String id)
    {
        if (null != mStyle)
        {
            return mStyle.getData(id);
        }

        return null;
    }

    /*
    public void applyStyleOrig(View view)
    {

    }
    */
}
