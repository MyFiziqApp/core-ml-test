package com.myfiziq.sdk.adapters;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MYQFragmentPagerAdapter extends FragmentStatePagerAdapter
{
    private Activity mActivity;
    private ViewPager mViewPager;
    private TabLayout mTablayout;

    private SparseArray<Stack<TabInfo>> mPages = new SparseArray<>();
    private SparseArray<Fragment> mPageFragments = new SparseArray<>();
    private FragmentManager.OnBackStackChangedListener mListener;

    public MYQFragmentPagerAdapter(Activity activity, FragmentManager childFragmentManager, ViewPager pager)
    {
        super(childFragmentManager);
        init(activity, pager, null);
    }

    public MYQFragmentPagerAdapter(Activity activity, FragmentManager childFragmentManager, ViewPager pager, TabLayout tablayout)
    {
        super(childFragmentManager);
        init(activity, pager, tablayout);
    }

    private void init(Activity activity, ViewPager pager, TabLayout tablayout)
    {
        mActivity = activity;
        mViewPager = pager;
        mTablayout = tablayout;
    }

    public void addOnBackStackChangedListener(FragmentManager.OnBackStackChangedListener listener)
    {
        mListener = listener;
    }

    public void fragmentCommit(int page, Class fragmentClass, Bundle args)
    {
        Stack<TabInfo> stack = mPages.get(page);
        if (null == stack)
        {
            stack = new Stack<>();
            mPages.put(page, stack);
        }
        stack.push(new TabInfo(fragmentClass, args));
        notifyDataSetChanged();
    }

    public void clear()
    {
        mPages.clear();
        mPageFragments.clear();
        notifyDataSetChanged();
    }
    public void destroy()
    {
        mActivity = null;
        mViewPager = null;
        mTablayout = null;

        if (mPages != null)
        {
            mPages.clear();
            mPages = null;
        }

        if (mPageFragments != null)
        {
            mPageFragments.clear();
            mPageFragments = null;
        }

        if (mListener != null)
        {
            mListener = null;
        }
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public Fragment getItem(final int position)
    {
        Stack<TabInfo> stack = mPages.get(position);
        if (!stack.empty())
        {
            final TabInfo tabInfo = stack.peek();
            Fragment fragment = tabInfo.mFragment;
            if (null == fragment)
            {
                Bundle bundle = tabInfo.args;
                ParameterSet set;
                String nextClassName;

                if (null != bundle)
                {
                    set = bundle.getParcelable(BaseFragment.BUNDLE_PARAMETERS);
                    nextClassName = set.getClazz();
                }
                else
                {
                    nextClassName = tabInfo.fragmentClass.getName();
                }
                fragment = Fragment.instantiate(mActivity, nextClassName, tabInfo.args);
            }

            return fragment;
        }
        return null;
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        Object object = super.instantiateItem(container, position);
        if (object instanceof Fragment)
        {
            Stack<TabInfo> stack = mPages.get(position);
            if (!stack.empty())
            {
                final TabInfo tabInfo = stack.peek();
                Fragment fragment = (Fragment) object;
                mPageFragments.put(position, fragment);
                tabInfo.setFragment(fragment);

                if (null != mListener)
                {
                    mListener.onBackStackChanged();
                }
            }
        }

        return object;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        Stack<TabInfo> stack = mPages.get(position);
        if (!stack.empty())
        {
            final TabInfo tabInfo = stack.peek();
            return tabInfo.mTitle;
        }
        return null;
    }

    public boolean canGoBack(int page)
    {
        Stack<TabInfo> tabInfoStack = mPages.get(page);
        return (tabInfoStack.size() > 1);
    }

    public Fragment getTopItemForPage(int page)
    {
        Stack<TabInfo> tabInfoStack = mPages.get(page);
        if (tabInfoStack.size() > 0)
        {
            return tabInfoStack.get(tabInfoStack.size() - 1).getFragment();
        }
        else
        {
            return null;
        }
    }

    @Override
    public int getItemPosition(@NonNull final Object object)
    {
        int position = mViewPager.getCurrentItem();

        if (mActivity.isFinishing())
            return POSITION_UNCHANGED;

        if (null == mPages || null == mPages.get(position) || mPages.get(position).isEmpty())
        {
            return POSITION_NONE;
        }

        /* Checks if the object exists in current mPages. */
        for (int i=0; i<mPages.size(); i++)
        {
            Stack<TabInfo> stack = mPages.get(i);

            TabInfo c = stack.peek();

            if (null == c.getFragment())
                return POSITION_NONE;

            if (!c.fragmentClass.getName().contentEquals(object.getClass().getName()))
            {
                return POSITION_NONE;
            }

            if (object instanceof Fragment)
            {
                Bundle args = ((Fragment) object).getArguments();

                if (null == args || null == c.args)
                    return POSITION_NONE;

                if (args.hashCode() != c.args.hashCode())
                    return POSITION_NONE;

                String tag1 = ((Fragment) object).getTag();
                String tag2 = c.getFragment().getTag();
                if (null != tag1 && null != tag2)
                {
                    if (!tag1.contentEquals(tag2))
                        return POSITION_NONE;
                }
                else if (null != tag1 && null == tag2)
                {
                    return POSITION_NONE;
                }
                else if (null == tag1 && null != tag2)
                {
                    return POSITION_NONE;
                }
            }
        }

        return POSITION_UNCHANGED;
    }

    /**
     * Determines if the user is currently viewing the specified page.
     */
    public boolean isPageVisible(BaseFragment page)
    {
        int thisPageNumber = getPageNumberOfFragment(page);
        int currentlyVisiblePageNumber = mViewPager.getCurrentItem();

        return thisPageNumber == currentlyVisiblePageNumber;
    }

    public int getPageNumberOfFragment(Fragment fragment)
    {
        return mPageFragments.indexOfValue(fragment);
    }

    @Override
    public int getCount()
    {
        return mPages.size();
    }

    public boolean onBackPressed()
    {
        int position = mViewPager.getCurrentItem();
        boolean hasBack = true;
        if (!historyIsEmpty(position))
        {
            if (isLastItemInHistory(position))
            {
                mActivity.finish();
                hasBack = false;
            }
            popPage(position);
        }
        notifyDataSetChanged();

        return hasBack;
    }

    private boolean historyIsEmpty(final int position)
    {
        return mPages == null || mPages.size() == 0 || mPages.get(position).isEmpty();
    }

    private boolean isLastItemInHistory(final int position)
    {
        return mPages.get(position).size() == 1;
    }

    private void popPage(final int position)
    {
        TabInfo currentTabInfo = mPages.get(position).pop();
        currentTabInfo.mFragment = null;
    }

    private class TabInfo
    {
        public Class fragmentClass;
        public Bundle args;
        Fragment mFragment;
        String mTitle;

        public TabInfo(Class fragmentClass, Bundle args)
        {
            this.fragmentClass = fragmentClass;
            this.args = args;

            if (null != args && args.containsKey(BaseFragment.BUNDLE_PARAMETERS))
            {
                ParameterSet set = args.getParcelable(BaseFragment.BUNDLE_PARAMETERS);
                if (null != set && set.hasParam(R.id.TAG_TITLE))
                {
                    Parameter param = set.getParam(R.id.TAG_TITLE);
                    if (null != param)
                    {
                        mTitle = param.getValue();
                    }
                }
            }
        }

        @Override
        public boolean equals(final Object o)
        {
            return this.fragmentClass.getName().equals(o.getClass().getName());
        }

        @Override
        public int hashCode()
        {
            return fragmentClass.getName() != null ? fragmentClass.getName().hashCode() : 0;
        }

        @NonNull
        @Override
        public String toString()
        {
            return "TabInfo{" +
                    "fragmentClass=" + fragmentClass +
                    '}';
        }

        public void setFragment(Fragment fragment)
        {
            mFragment = fragment;
        }

        public Fragment getFragment()
        {
            return mFragment;
        }
    }
}

