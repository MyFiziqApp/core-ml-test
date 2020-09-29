package com.myfiziq.sdk.helpers;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.enums.DisplayOptionsFlag;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;

/**
 * Helper methods to allow us to easily manipulate the ActionBar/Toolbar in an activity.
 */
public class ActionBarHelper
{
    private ActionBarHelper()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Gets the ActionBar/Toolbar from the Activity.
     */
    @Nullable
    public static ActionBar getActionBar(Activity activity)
    {
        if (activity instanceof AppCompatActivity)
        {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            return appCompatActivity.getSupportActionBar();
        }
        else
        {
            Timber.w("An Action Bar does not exist on the activity. Maybe it never existed to begin with or the activity has been closed?");
            return null;
        }
    }

    /**
     * Sets the ActionBar's title and shows if it is currently hidden.
     *
     * @param title The ActionBar title to set.
     */
    public static void setActionBarTitle(Activity activity, String title)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            actionBar.show();
            actionBar.setTitle(title);
        }
    }

    /**
     * Sets the ActionBar's title and shows if it is currently hidden.
     *
     * @param titleResourceId The ActionBar title to set as a string resource.
     */
    public static void setActionBarTitle(Activity activity, @StringRes int titleResourceId)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            showActionBar(activity);
            actionBar.setTitle(titleResourceId);
        }
    }

    /**
     * Shows the ActionBar on the screen.
     */
    public static void showActionBar(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            actionBar.show();
        }
    }

    /**
     * Hides the ActionBar on the screen.
     */
    public static void hideActionBar(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            actionBar.hide();
        }
    }

    public static void enableSwapButton(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(activity);
            intentManagerService.respond(IntentResponses.SHOW_SWAP_MENU_BUTTON, null);
        }
    }

    public static void disableSwapButton(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(activity);
            intentManagerService.respond(IntentResponses.HIDE_SWAP_MENU_BUTTON, null);
        }
    }

    /**
     * Shows the back button in the ActionBar.
     */
    public static void showBackButton(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (SisterColors.getInstance().getToolbarItemsColor() != null)
            {
                String resName = Build.VERSION.SDK_INT >= 23 ? "abc_ic_ab_back_material" : "abc_ic_ab_back_mtrl_am_alpha";

                int res = activity.getResources().getIdentifier(resName, "drawable", activity.getPackageName());
                final Drawable upArrow = activity.getResources().getDrawable(res);

                upArrow.setColorFilter(SisterColors.getInstance().getToolbarItemsColor(), PorterDuff.Mode.SRC_ATOP);
                actionBar.setHomeAsUpIndicator(upArrow);
            }
        }
    }

    /**
     * If sister colors are available, this applies them to the toolbar and menu items
     */
    public static void applyActionBarColors(Menu menu, Toolbar mainToolbar)
    {
        SisterColors sc = SisterColors.getInstance();

        if (sc.getToolbarItemsColor() != null)
        {
            int itemCount = menu.size();
            for (int i = 0; i < itemCount; i++)
            {
                MenuItem item = menu.getItem(i);
                item.getIcon().setTint(sc.getToolbarItemsColor());
            }

            if (sc.getAlertBackgroundColor() != null)
            {
                mainToolbar.setBackgroundColor(sc.getAlertBackgroundColor());
            }

            for (int i = 0; i < mainToolbar.getChildCount(); i++)
            {
                View child = mainToolbar.getChildAt(i);
                if (child instanceof TextView)
                {
                    TextView tv = (TextView) child;
                    tv.setTextColor(sc.getToolbarItemsColor());
                    sc.applyStyle(tv, tv.getParent());
                }

                if (sc.getAlertBackgroundColor() != null)
                {
                    child.setBackgroundColor(sc.getAlertBackgroundColor());
                }
            }
        }
    }

    /**
     * Hides the back button in the ActionBar.
     */
    public static void hideBackButton(Activity activity)
    {
        ActionBar actionBar = getActionBar(activity);

        if (null != actionBar)
        {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    /**
     * Determines if the customer app has specified a style for the back button in the Action Bar.
     *
     * @return Whether the style "displayOptions" has been specified.
     */
    public static boolean isBackButtonStyleSpecified(Activity activity)
    {
        Resources.Theme theme = activity.getTheme();

        TypedValue value = new TypedValue();

        return theme.resolveAttribute(android.R.attr.displayOptions, value, true);
    }

    /**
     * Resets the back button to the style specified by the customer app.
     */
    public static void resetBackButtonToDefaultState(@Nullable Activity activity)
    {
        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        Resources.Theme theme = activity.getTheme();

        TypedValue value = new TypedValue();

        // Make sure we use "android.R.attr.displayOptions" and NOT "R.attr.displayOptions", otherwise we won't find anything!
        boolean found = theme.resolveAttribute(android.R.attr.displayOptions, value, true);

        if (!found)
        {
            // Customer app hasn't specified any back button style. Don't do anything.
            return;
        }

        int bitwiseValue = value.data;


        // If the user has specified "homeAsUp|showTitle" as the value for the "displayOptions" attribute, the below code will check to see if it's enabled
        boolean isHomeAsUpEnabled = DisplayOptionsFlag.HOME_AS_UP.isFlagEnabled(bitwiseValue);


        // If the customer app wants to show the back button by default
        if (isHomeAsUpEnabled)
        {
            // Then show it
            showBackButton(activity);
        }
        else
        {
            // Otherwise hide it
            hideBackButton(activity);
        }
    }
}
