package com.myfiziq.sdk.adapters;


import com.myfiziq.sdk.fragments.FragmentTrackChart;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

public class ChartTabAdapter extends FragmentPagerAdapter
{
    private final List<FragmentTrackChart> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();
    private FragmentManager mFragmentManager;

    public ChartTabAdapter(FragmentManager fragmentManager, int behaviour)
    {
        super(fragmentManager, behaviour);
        this.mFragmentManager = fragmentManager;
    }

    @Override
    public Fragment getItem(int position)
    {
        return mFragmentList.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object)
    {
        for (FragmentTrackChart fragment : mFragmentList)
        {
            if (fragment.equals(object))
            {
                // getItemPosition() expects the NEW item's position to be returned
                // However, we don't reorder items in this adapter, so we return POSITION_UNCHANGED
                return POSITION_UNCHANGED;
            }
        }

        // If Android is trying to find an item that has been removed, return POSITION_NONE
        return POSITION_NONE;
    }

    public void addFragment(FragmentTrackChart fragment, String title)
    {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    public void renderChanges()
    {
        notifyDataSetChanged();
    }

    public void clearFragments()
    {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();

        for (Fragment fragment : mFragmentList)
        {
            transaction.remove(fragment);
        }

        transaction.commit();

        mFragmentList.clear();
        mFragmentTitleList.clear();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position)
    {
        return mFragmentTitleList.get(position);
    }

    @Override
    public int getCount()
    {
        return mFragmentList.size();
    }

    public void handlePageStateChange(int position)
    {
        mFragmentList.get(position).handlePageStateChange();
    }
}
