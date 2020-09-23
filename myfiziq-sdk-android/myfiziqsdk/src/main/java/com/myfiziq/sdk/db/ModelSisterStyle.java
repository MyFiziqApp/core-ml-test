package com.myfiziq.sdk.db;

import android.text.TextUtils;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import java.util.ArrayList;

public class ModelSisterStyle extends Model
{
    @Persistent
    public String toolbarItemsColor;

    @Persistent
    public String captureTopBarColor;

    @Persistent
    public String chartLineColor;

    @Persistent
    public String alertBackgroundColor;

    @Persistent
    public String avatarMeshColor;

    @Persistent
    public String circleCountdownColor;

    @Persistent
    public String textCountdownColor;

    @Persistent
    public String spinnerPopupColor;

    @Persistent
    public String tabIndicatorColor;

    @Persistent
    public String bottomNavLabels;

    @Persistent
    public String bottomNavIconDp;

    @Persistent
    public ArrayList<ModelTextColorMap> textColour;                 // NOSONAR

    @Persistent
    public ArrayList<ModelTextColorStateMap> textColorState;        // NOSONAR

    @Persistent
    public ArrayList<ModelCardColorMap> cardColour;                 // NOSONAR

    @Persistent
    public ArrayList<ModelViewBgColorMap> viewBgColour;             // NOSONAR

    @Persistent
    public ArrayList<ModelViewBgTintColorMap> tintColour;          // NOSONAR

    @Persistent
    public ArrayList<ModelOnDemandFont> viewFonts;                  // NOSONAR

    @Persistent
    public ArrayList<ModelOnDemandImage> images;

    @Persistent
    public ArrayList<ModelOnDemandData> data;

    CachedColor cacheToolbarItemsColor = null;

    CachedColor cacheCaptureTopBarColor = null;

    CachedColor cacheChartLineColor = null;

    CachedColor cacheAlertBackgroundColor = null;

    CachedColor cacheAvatarMeshColor = null;

    CachedColor cacheCircleCountdownColor = null;

    CachedColor cacheTextCountdownColor = null;

    CachedColor cacheSpinnerPopupColor = null;

    CachedColor cacheTabIndicatorColor = null;

    CachedInteger cacheBottomNavLabels = null;

    CachedInteger cacheBottomNavIconDp = null;

    public ModelSisterStyle()
    {

    }

    public CachedColor getToolbarItemsColor()
    {
        if (null == cacheToolbarItemsColor)
            cacheToolbarItemsColor = new CachedColor(toolbarItemsColor);

        return cacheToolbarItemsColor;
    }

    public CachedColor getCaptureTopBarColor()
    {
        if (null == cacheCaptureTopBarColor)
            cacheCaptureTopBarColor = new CachedColor(captureTopBarColor);

        return cacheCaptureTopBarColor;
    }

    public CachedColor getChartLineColor()
    {
        if (null == cacheChartLineColor)
            cacheChartLineColor = new CachedColor(chartLineColor);

        return cacheChartLineColor;
    }

    public CachedColor getAlertBackgroundColor()
    {
        if (null == cacheAlertBackgroundColor)
            cacheAlertBackgroundColor = new CachedColor(alertBackgroundColor);

        return cacheAlertBackgroundColor;
    }

    public CachedColor getAvatarMeshColor()
    {
        if (null == cacheAvatarMeshColor)
            cacheAvatarMeshColor = new CachedColor(avatarMeshColor);

        return cacheAvatarMeshColor;
    }

    public CachedColor getCircleCountdownColor()
    {
        if (null == cacheCircleCountdownColor)
            cacheCircleCountdownColor = new CachedColor(circleCountdownColor);

        return cacheCircleCountdownColor;
    }

    public CachedColor getTextCountdownColor()
    {
        if (null == cacheTextCountdownColor)
            cacheTextCountdownColor = new CachedColor(textCountdownColor);

        return cacheTextCountdownColor;
    }

    public CachedColor getSpinnerPopupColor()
    {
        if (null == cacheSpinnerPopupColor)
            cacheSpinnerPopupColor = new CachedColor(spinnerPopupColor);

        return cacheSpinnerPopupColor;
    }

    public CachedColor getTabIndicatorColor()
    {
        if (null == cacheTabIndicatorColor)
            cacheTabIndicatorColor = new CachedColor(tabIndicatorColor);

        return cacheTabIndicatorColor;
    }

    public CachedInteger getBottomNavLabels()
    {
        if (null == cacheBottomNavLabels)
        {
            if (!TextUtils.isEmpty(bottomNavLabels))
            {
                /**
                 *     int LABEL_VISIBILITY_AUTO = -1;
                 *     int LABEL_VISIBILITY_SELECTED = 0;
                 *     int LABEL_VISIBILITY_LABELED = 1;
                 *     int LABEL_VISIBILITY_UNLABELED = 2;
                 */
                String lower = bottomNavLabels.toLowerCase();
                if (lower.contentEquals("auto"))
                    cacheBottomNavLabels = new CachedInteger(LabelVisibilityMode.LABEL_VISIBILITY_AUTO);
                else if (lower.contentEquals("selected"))
                    cacheBottomNavLabels = new CachedInteger(LabelVisibilityMode.LABEL_VISIBILITY_SELECTED);
                else if (lower.contentEquals("labelled"))
                    cacheBottomNavLabels = new CachedInteger(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
                else if (lower.contentEquals("unlabelled"))
                    cacheBottomNavLabels = new CachedInteger(LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED);
            }

            if (null == cacheBottomNavLabels)
                cacheBottomNavLabels = new CachedInteger();
        }

        return cacheBottomNavLabels;
    }

    public CachedInteger getBottomNavIconDp()
    {
        if (null == cacheBottomNavIconDp)
            cacheBottomNavIconDp = new CachedInteger(bottomNavIconDp);

        return cacheBottomNavIconDp;
    }

    public ModelOnDemandData getData(String id)
    {
        if (null != data)
        {
            for (ModelOnDemandData model : data)
            {
                if (model.id.contentEquals(id))
                {
                    return model;
                }
            }
        }

        return null;
    }
}
