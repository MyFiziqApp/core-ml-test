package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.internal.BaselineLayout;
import com.myfiziq.sdk.helpers.SisterColors;

import androidx.annotation.IdRes;

public class MYQBottomNavigationView extends BottomNavigationView
{
    public MYQBottomNavigationView(Context context)
    {
        super(context);
    }

    public MYQBottomNavigationView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQBottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void setCheckedItemId(@IdRes int itemId)
    {
        MenuItem item = getMenu().findItem(itemId);
        if (item != null)
        {
            item.setChecked(true);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && SisterColors.getInstance().isSisterMode())
        {
            final ViewGroup bottomMenu = (ViewGroup) getChildAt(0);
            final int bottomMenuChildCount = bottomMenu.getChildCount();
            for (int i = 0; i < bottomMenuChildCount; i++)
            {
                BottomNavigationItemView item = (BottomNavigationItemView) bottomMenu.getChildAt(i);
                //every BottomNavigationItemView has two children, first is an itemIcon and second is an itemTitle
                View itemTitle = item.getChildAt(1);
                //every itemTitle has two children, first is a smallLabel and second is a largeLabel. these two are type of AppCompatTextView
                TextView tv0 = (TextView) ((BaselineLayout) itemTitle).getChildAt(0);
                TextView tv1 = (TextView) ((BaselineLayout) itemTitle).getChildAt(1);
                SisterColors.getInstance().applyStyle(tv0, tv0.getParent());
                SisterColors.getInstance().applyStyle(tv1, tv1.getParent());
            }
        }
    }
}
