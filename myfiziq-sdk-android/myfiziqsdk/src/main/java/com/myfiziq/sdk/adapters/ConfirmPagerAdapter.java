package com.myfiziq.sdk.adapters;


import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.fragments.FragmentConfirmPage;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * @hide
 */

public class ConfirmPagerAdapter extends FragmentStatePagerAdapter
{
    private WeakReference<ActivityInterface> mActivity;
    private List<BaseFragment> fragments = new ArrayList<>();
    private ParameterSet mParameterSet;

    public ConfirmPagerAdapter(ActivityInterface activity, ParameterSet parameterSet)
    {
        super(activity.getSupportFragmentManager());
        mActivity = new WeakReference<>(activity);
        mParameterSet = parameterSet;
    }

    @Override
    public Fragment getItem(int position)
    {
        Fragment frag = null;
        ActivityInterface activity = mActivity.get();
        if (null != activity)
        {
            frag = BaseFragment.newInstance(activity, new ParameterSet.Builder(FragmentConfirmPage.class)
                    .addParam(new Parameter(R.id.TAG_ARG_SIDE, PoseSide.fromInt(position).ordinal()))
                    .addParam(mParameterSet.getParam(R.id.TAG_ARG_MODEL_AVATAR))
                    .build());
        }

        return frag;
    }

    @Override
    public int getCount()
    {
        return 2;
    }

    public void destroy()
    {
        for (BaseFragment fragment: fragments)
        {
            fragment.fragmentPopSelf();
        }

        fragments.clear();

        notifyDataSetChanged();
    }

}
