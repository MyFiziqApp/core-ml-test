package com.myfiziq.sdk.helpers;

import android.app.Activity;
import android.os.Build;
import android.view.Window;

import com.myfiziq.sdk.enums.StatusBarStyle;

import androidx.annotation.ColorInt;

/**
 * Proves helper methods for manipulating the StatusBar in Android.
 */
public class StatusBarHelper
{
    /**
     * Sets the style of the StatusBar.
     * @param activity The current activity.
     * @param style The style to set.
     * @param colour The colour of the StatusBar to use. Note, the colour will only change in Android Marshmallow and onwards.
     *
     *
     * Note:
     *      Setting the status bar colour is supported from Lollipop onwards.
     *      However, only in Android M did dark icons in the StatusBar become available.
     *      If we set a white StatusBar with white icons in Lollipop, you won't see anything in the status bar.
     */
    public static void setStatusBarStyle(Activity activity, StatusBarStyle style, @ColorInt int colour)
    {
        int styleInt = style.getSystemUiVisibilityStyle();

        Window window = activity.getWindow();
        window.getDecorView().setSystemUiVisibility(styleInt);

        // Setting the status bar colour is supported from Lollipop onwards.
        // However, only in Android M did dark icons in the StatusBar become available
        // If we set a white StatusBar with white icons in Lollipop, you won't see anything in the status bar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            window.setStatusBarColor(colour);
        }
    }
}
