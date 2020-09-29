package com.myfiziq.sdk.helpers;

import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.myfiziq.sdk.db.ModelOnDemandData;
import com.myfiziq.sdk.db.ModelSisterStyle;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.lifecycle.StyleInterceptor;
import com.myfiziq.sdk.views.CaptureCountdown;

import io.github.inflationx.viewpump.ViewPump;

public class SisterColors
{
    private static SisterColors INSTANCE;

    public static SisterColors getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new SisterColors();
        }

        return INSTANCE;
    }

    private boolean sisterMode = false;
    private Integer chartLineColor = null;
    private Integer alertBackgroundColor = null;
    private Integer toolbarItemsColor = null;
    private Integer captureTopBarColor = null;
    private Integer avatarMeshColor = null;
    private StyleInterceptor mStyleInterceptor;

    public void init(String json)
    {
        ModelSisterStyle model = Orm.newModel(ModelSisterStyle.class);
        model.deserialize(json);
        init(model);

        StyleInterceptor mStyleInterceptor = new StyleInterceptor(model);
        mStyleInterceptor.init();

        setStyle(mStyleInterceptor);

        setSisterMode(true);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(mStyleInterceptor)
                .build());
    }

    public boolean isSisterMode()
    {
        return sisterMode;
    }

    public void setSisterMode(boolean sisterMode)
    {
        this.sisterMode = sisterMode;
    }

    public ModelOnDemandData getData(String id)
    {
        if (null != mStyleInterceptor)
        {
            return mStyleInterceptor.getData(id);
        }

        return null;
    }

    public Integer getAvatarMeshColor()
    {
        return avatarMeshColor;
    }

    public void setAvatarMeshColor(Integer meshColor)
    {
        avatarMeshColor = meshColor;
    }

    public Integer getChartLineColor()
    {
        return chartLineColor;
    }

    public void setChartLineColor(Integer chartLineColor)
    {
        this.chartLineColor = chartLineColor;
    }

    public Integer getAlertBackgroundColor()
    {
        return alertBackgroundColor;
    }

    public void setAlertBackgroundColor(Integer alertBackgroundColor)
    {
        this.alertBackgroundColor = alertBackgroundColor;
    }

    public Integer getToolbarItemsColor()
    {
        return toolbarItemsColor;
    }

    public void setToolbarItemsColor(Integer toolbarItemsColor)
    {
        this.toolbarItemsColor = toolbarItemsColor;
    }

    public Integer getCaptureTopBarColor()
    {
        return captureTopBarColor;
    }

    public void setCaptureTopBarColor(Integer captureTopBarColor)
    {
        this.captureTopBarColor = captureTopBarColor;
    }

    public void setStyle(StyleInterceptor styleInterceptor)
    {
        mStyleInterceptor = styleInterceptor;
    }

    public void applyStyle(TextView textView, ViewParent parent)
    {
        if (null != mStyleInterceptor)
        {
            mStyleInterceptor.applyStyle(textView, parent);
        }
    }

    public void init(ModelSisterStyle model)
    {
        model.getToolbarItemsColor().apply(this::setToolbarItemsColor);

        model.getCaptureTopBarColor().apply(this::setCaptureTopBarColor);

        model.getChartLineColor().apply(this::setChartLineColor);

        model.getAlertBackgroundColor().apply(this::setAlertBackgroundColor);

        model.getAvatarMeshColor().apply(this::setAvatarMeshColor);
    }
    public void applyStyle(CaptureCountdown countdown, int defaultColor)
    {
        if (isSisterMode() && null != countdown && null != getChartLineColor())
        {
            countdown.setTextColor(getChartLineColor());
        }
        else
        {
            countdown.setTextColor(defaultColor);
        }
    }
    public int getContourColor(int defaultColor)
    {
        if (isSisterMode() && null != getChartLineColor())
            return getChartLineColor();
        return defaultColor;
    }
}
