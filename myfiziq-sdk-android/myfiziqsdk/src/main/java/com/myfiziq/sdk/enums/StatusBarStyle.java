package com.myfiziq.sdk.enums;

import android.view.View;

public enum StatusBarStyle
{
    DEFAULT(View.SYSTEM_UI_FLAG_LAYOUT_STABLE),
    DEFAULT_LIGHT(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR),
    RENDER_BEHIND_STATUS_BAR(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN),
    HIDE_STATUS_BAR(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                      | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                      | View.SYSTEM_UI_FLAG_IMMERSIVE);


    private int systemUiVisibilityStyle;

    StatusBarStyle(int systemUiVisibilityStyle)
    {
        this.systemUiVisibilityStyle = systemUiVisibilityStyle;
    }

    public int getSystemUiVisibilityStyle()
    {
        return systemUiVisibilityStyle;
    }
}
