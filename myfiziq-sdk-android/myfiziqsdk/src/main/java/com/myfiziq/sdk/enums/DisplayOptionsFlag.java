package com.myfiziq.sdk.enums;

/**
 * Represents the various bitwise flags available when setting the "displayOptions" style for an Activity's theme.
 */
public enum DisplayOptionsFlag
{
    /*
        From: https://android.googlesource.com/platform/frameworks/support/+/20ac724/appcompat/res/values/attrs.xml#154
     */

    NONE(0),
    USE_LOGO(1),
    SHOW_HOME(2),
    HOME_AS_UP(4),
    SHOW_TITLE(8),
    SHOW_CUSTOM(10),
    DISABLE_HOME(20);


    private int flag;

    DisplayOptionsFlag(int flag)
    {
        this.flag = flag;
    }

    public int getFlag()
    {
        return flag;
    }

    /**
     * Determines if this flag is enabled in a given bitwise value.
     */
    public boolean isFlagEnabled(int bitwiseValue)
    {
        return (bitwiseValue & getFlag()) == getFlag();
    }
}
