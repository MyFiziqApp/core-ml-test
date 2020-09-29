package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.sdk.adapters.MYQFragmentPagerAdapter;
import com.myfiziq.sdk.fragments.FragmentHomeInterface;
import com.myfiziq.sdk.helpers.SisterColors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;


public class MYQTabLayout extends TabLayout
{
    ViewPager mViewPager;

    public MYQTabLayout(Context context)
    {
        super(context);
    }

    public MYQTabLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MYQTabLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setupWithViewPager(@Nullable ViewPager viewPager)
    {
        super.setupWithViewPager(viewPager);
        mViewPager = viewPager;
    }

    public void applyStyle(Tab tab)
    {
        ViewGroup mainView = (ViewGroup) getChildAt(0);
        ViewGroup tabView = (ViewGroup) mainView.getChildAt(tab.getPosition());

        int tabChildCount = tabView.getChildCount();
        for (int i = 0; i < tabChildCount; i++)
        {
            View tabViewChild = tabView.getChildAt(i);
            if (tabViewChild instanceof TextView)
            {
                SisterColors.getInstance().applyStyle((TextView)tabViewChild, tabViewChild.getParent());
            }
        }
    }

    @Override
    public void addTab(Tab tab)
    {
        super.addTab(tab);
        applyStyle(tab);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected)
    {
        super.addTab(tab, position, setSelected);
        applyStyle(tab);

        if (null != mViewPager && mViewPager.getAdapter() instanceof MYQFragmentPagerAdapter)
        {
            MYQFragmentPagerAdapter adapter = (MYQFragmentPagerAdapter) mViewPager.getAdapter();
            Fragment fragment = adapter.getItem(position);
            if (fragment instanceof FragmentHomeInterface)
            {
                int icon = ((FragmentHomeInterface) fragment).getIcon();
                if (0 != icon)
                {
                    tab.setIcon(icon);
                }
            }
        }
    }
}
