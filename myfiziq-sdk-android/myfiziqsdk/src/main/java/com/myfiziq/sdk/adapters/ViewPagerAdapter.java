package com.myfiziq.sdk.adapters;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class ViewPagerAdapter extends PagerAdapter
{
    private ViewPager mPager;
    private List<View> mViews;

    public ViewPagerAdapter(ViewPager pager, List<View> views)
    {
        mPager = pager;
        mViews = views;
        pager.setAdapter(this);
    }

    public void setViews(List<View> views)
    {
        mViews = views;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position)
    {
        View view = mViews.get(position);
        collection.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view)
    {
        collection.removeView((View) view);
    }

    @Override
    public int getCount()
    {
        return mViews.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
    {
        return view == object;
    }

    public View getCurrentView()
    {
        int item = mPager.getCurrentItem();
        return mViews.get(item);
    }
    public void setViews(ArrayList<View> views)
    {
        mViews = views;
        notifyDataSetChanged();
    }
}
