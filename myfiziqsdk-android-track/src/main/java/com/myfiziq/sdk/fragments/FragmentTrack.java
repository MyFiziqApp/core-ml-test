package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.adapters.MYQFragmentPagerAdapter;
import com.myfiziq.sdk.adapters.TrackFragmentPager;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.ParameterSetName;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.PendingMessageRepository;
import com.myfiziq.sdk.lifecycle.StateTrack;
import com.myfiziq.sdk.views.NonSwipeableViewPager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import timber.log.Timber;

public class FragmentTrack extends BaseFragment implements FragmentInterface, FragmentHomeInterface
{
    private TabLayout mTabs;
    private NonSwipeableViewPager mViewPager;
    private TrackFragmentPager mPagerAdapter;
    private ParameterSet mViewAvatarSet = null;
    private ParameterSet mSelAvatarSet = null;

    private static ModelAvatar selectedLeftAvatar = null;
    private static ModelAvatar selectedRightAvatar = null;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_title_track));

        Bundle bundle = getArguments();

        mTabs = view.findViewById(R.id.tablayout);
        mViewPager = view.findViewById(R.id.viewPager);
        mPagerAdapter = new TrackFragmentPager(getActivity(), getChildFragmentManager(), mViewPager);

        if (null != bundle)
        {
            mViewAvatarSet = bundle.getParcelable(StateTrack.BUNDLE_VIEWAVATAR);
            mSelAvatarSet = bundle.getParcelable(StateTrack.BUNDLE_SELAVATAR);
        }

        if (null != mParameterSet && null == mViewAvatarSet)
        {
            mViewAvatarSet = mParameterSet.getSubSet(StateTrack.BUNDLE_VIEWAVATAR);
        }

        if (null != mParameterSet && null == mSelAvatarSet)
        {
            mSelAvatarSet = mParameterSet.getSubSet(StateTrack.BUNDLE_SELAVATAR);
        }


        int page = 0;

        Bundle bndCompare = buildCompareBundle();
        mPagerAdapter.fragmentCommit(page++, FragmentCompare.class, bndCompare);


        Bundle bndProgress = buildProgressBundle();
        mPagerAdapter.fragmentCommit(page++, FragmentProgress.class, bndProgress);


        mTabs.setupWithViewPager(mViewPager);

        mViewPager.setAdapter(mPagerAdapter);

        if (null != mParameterSet)
        {
            applyParameters(view);
        }

        return view;
    }


    @Override
    public void onResume()
    {
        super.onResume();

        if (selectedLeftAvatar == null || selectedRightAvatar == null)
        {
            resetAvatarSelection();
        }
        else
        {
            ensureThatAvatarsStillExist();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Activity activity = getActivity();

        if (null != activity)
        {
            ActionBarHelper.disableSwapButton(getActivity());
        }
    }

    @Override
    public void onDestroy()
    {
        // Make sure this gets called in "onDestroy()"
        // Calling it in "onDestroyView()" will have it execute before the fragment transition animation
        // starts which will cause some items to disappear to the user before the transition starts.
        mViewPager.setAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onEnterAnimationStart()
    {
        super.onEnterAnimationStart();
        int count = mPagerAdapter.getCount();

        // Propagate onEnterAnimationStart to all the child tab fragments
        for (int i = 0; i < count; i++)
        {
            Fragment item = mPagerAdapter.getItem(i);

            if (item instanceof BaseFragment)
            {
                // Propagate onEnterAnimationStart to all the child tab fragments
                BaseFragment baseFragment = (BaseFragment) item;
                baseFragment.onEnterAnimationStart();
            }
        }
    }

    @Override
    public void onEnterAnimationEnd()
    {
        super.onEnterAnimationEnd();

        int count = mPagerAdapter.getCount();

        // Propagate onEnterAnimationEnd to all the child tab fragments
        for (int i = 0; i < count; i++)
        {
            Fragment item = mPagerAdapter.getItem(i);

            if (item instanceof BaseFragment)
            {
                // Propagate onEnterAnimationEnd to all the child tab fragments
                BaseFragment baseFragment = (BaseFragment) item;
                baseFragment.onEnterAnimationEnd();
            }
        }
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_track;
    }

    @Override
    public int getIcon()
    {
        return R.drawable.ic_track_circle;
    }

    /**
     * Resets the avatar selection to the ones that should be displayed by default.
     */
    private void resetAvatarSelection()
    {
        AsyncHelper.run(
                () -> ORMDbQueries.getLastXCompletedAvatars(2),
                this::setDefaultAvatars, true
        );
    }

    /**
     * Ensures that the selected avatars still exist in the database (i.e. we haven't logged out or deleted them).
     * <p>
     * If they still exist, announce the selected avatars to whoever is listening.
     * Else, reset to the default avatar selection and then announce to whoever is listening.
     */
    private void ensureThatAvatarsStillExist()
    {
        AsyncHelper.run(
                () -> ORMTable.getModelCount(
                        ModelAvatar.class,
                        ModelAvatar.getWhere(
                                String.format(
                                        "(attemptId='%s' OR attemptId='%s') AND Status='%s'",
                                        selectedLeftAvatar.getAttemptId(),
                                        selectedRightAvatar.getAttemptId(),
                                        Status.Completed
                                )
                        )
                ),
                countOfVisible ->
                {
                    // If the avatars that were previously selected still exist in the database and are completed...
                    if (countOfVisible == 2)
                    {
                        // Announce after other operations in the Android event loop have completed (i.e. creating the child tabs in this fragment)
                        new Handler().post(this::announceSelectedAvatars);
                    }
                    else
                    {
                        Timber.w("Avatars no longer exist in the database. Resetting track to its default state");

                        // Else, avatars have been deleted or no longer exist for this account, reset to the default ones
                        resetAvatarSelection();
                    }
                }, true
        );
    }

    private Bundle buildCompareBundle()
    {
        String compareTitle = getString(R.string.compare_caps);

        Bundle bundle = new ParameterSet.Builder(FragmentCompare.class)
                .addParam(new Parameter(R.id.TAG_TITLE, compareTitle))
                .build()
                .toBundle(getActivity());

        if (null != mViewAvatarSet)
        {
            bundle.putParcelable(StateTrack.BUNDLE_VIEWAVATAR, mViewAvatarSet);
        }
        if (null != mSelAvatarSet)
        {
            bundle.putParcelable(StateTrack.BUNDLE_SELAVATAR, mSelAvatarSet);
        }

        return bundle;
    }

    private Bundle buildProgressBundle()
    {
        String progressTitle = getString(R.string.progress_caps);

        Bundle bundle = new ParameterSet.Builder(FragmentProgress.class)
                .addParam(new Parameter(R.id.TAG_TITLE, progressTitle))
                .build()
                .toBundle(getActivity());

        if (null != mViewAvatarSet)
        {
            bundle.putParcelable(StateTrack.BUNDLE_VIEWAVATAR, mViewAvatarSet);
        }
        if (null != mSelAvatarSet)
        {
            bundle.putParcelable(StateTrack.BUNDLE_SELAVATAR, mSelAvatarSet);
        }

        return bundle;
    }

    private void setDefaultAvatars(List<ModelAvatar> avatarList)
    {
        if (null == getActivity())
        {
            // Fragment has detached from activity
            return;
        }

        if (avatarList.size() >= 2)
        {
            Timber.i("Setting default avatars");

            selectedLeftAvatar = avatarList.get(0);
            selectedRightAvatar = avatarList.get(avatarList.size() - 1);

            announceSelectedAvatars();
        }
        else
        {
            Timber.i("Not enough avatars have been generated or captured yet to render them on the screen.");
            mParameterSet.setNextState(ParameterSetName.NO_AVATARS);
            mParameterSet.startNext(getMyActivity(), true, false);
        }
    }

    private void announceSelectedAvatars()
    {
        ActivityInterface activityInterface = getMyActivity();
        if (activityInterface instanceof BaseActivityInterface)
        {
            // Determine if avatars have been selected for us (i.e. we've come back from the avatar selector screen)
            if (PendingMessageRepository.hasPendingMessage(IntentResponses.AVATAR_ONE_SELECTED))
            {
                // If an avatar has been pre-selected for us, get them and notify the child fragments
                selectedLeftAvatar = (ModelAvatar) PendingMessageRepository.getPendingMessage(IntentResponses.AVATAR_ONE_SELECTED);
            }

            if (PendingMessageRepository.hasPendingMessage(IntentResponses.AVATAR_TWO_SELECTED))
            {
                selectedRightAvatar = (ModelAvatar) PendingMessageRepository.getPendingMessage(IntentResponses.AVATAR_TWO_SELECTED);
            }
        }
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        ParameterSet ps = new ParameterSet.Builder()
                .addParam(new Parameter(R.id.TAG_ARG_MODEL_AVATAR,selectedLeftAvatar))
                .addParam(new Parameter(R.id.TAG_MODEL,selectedRightAvatar))
                .build();
        intentManagerService.respond(IntentResponses.AVATAR_ONE_SELECTED, ps);
//        IntentManagerService<ModelAvatar> intentManagerService = new IntentManagerService<>(getActivity());
//        intentManagerService.respond(IntentResponses.AVATAR_ONE_SELECTED, selectedLeftAvatar);
//        intentManagerService.respond(IntentResponses.AVATAR_TWO_SELECTED, selectedRightAvatar);
    }
}
