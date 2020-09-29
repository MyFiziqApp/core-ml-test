package com.myfiziq.sdk.activities;

import android.os.Bundle;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.fragments.FragmentHomeInterface;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Simple interface to abstract common <code>Activity</code> functionality.
 */
public interface BaseActivityInterface extends ActivityInterface
{
    ActivityManagerInterface getActivityManager();

    default void setFragment(Fragment fragment)
    {
        getActivityManager().setFragment(fragment);
    }

    default Fragment getFragment()
    {
        return getActivityManager().getFragment();
    }

    default void fragmentCommit(ParameterSet params)
    {
        fragmentCommit(BaseFragment.newInstance(this, params), params.toBundle(getContext()), true);
    }

    default void fragmentCommit(ParameterSet params, boolean addToBackStack)
    {
        fragmentCommit(BaseFragment.newInstance(this, params), params.toBundle(getContext()), addToBackStack);
    }

    default int fragmentCommit(Fragment fragment)
    {
        return fragmentCommit(fragment, null, true, false);
    }

    default int fragmentCommit(Fragment fragment, boolean addToBackStack)
    {
        return fragmentCommit(fragment, null, addToBackStack, false);
    }

    default int fragmentCommit(Fragment fragment, Bundle bundle, boolean addToBackStack)
    {
        return fragmentCommit(fragment, bundle, addToBackStack, false);
    }

    default int fragmentCommit(Fragment fragment, Bundle bundle, boolean addToBackStack, boolean isAnimated)
    {
        return fragmentCommit(R.id.container, fragment, bundle, addToBackStack, isAnimated);
    }

    default int fragmentCommit(int containerId, Fragment fragment, Bundle bundle, boolean addToBackStack, boolean isAnimated)
    {
        FragmentManager mgr = getSupportFragmentManager();

        FragmentTransaction fTrans = mgr.beginTransaction();

        if (!isAnimated)
        {
            fTrans.setCustomAnimations(
                    R.animator.fragment_slide_left_enter,
                    R.animator.fragment_slide_left_exit,
                    R.animator.fragment_slide_right_enter,
                    R.animator.fragment_slide_right_exit);
        }

        if (fragment instanceof FragmentHomeInterface)
        {
            BaseFragment.setPoppingBackStack(true);

            // Clear the back stack if the user is viewing a main navigation page.
            mgr.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            BaseFragment.setPoppingBackStack(false);
        }

        if (null != bundle)
        {
            fragment.setArguments(bundle);
        }

        fTrans.replace(containerId, fragment, fragment.getClass().getName());

        if (addToBackStack)
        {
            fTrans.addToBackStack(fragment.getClass().getName());
        }

        setFragment(fragment);

        return commitFragmentTransaction(fTrans);
    }

    default int fragmentAdd(Fragment fragment, Bundle bundle, boolean addToBackStack)
    {
        return fragmentAdd(fragment, bundle, addToBackStack, false);
    }

    default int fragmentAdd(Fragment fragment, Bundle bundle, boolean addToBackStack, boolean isAnimated)
    {
        FragmentManager mgr = getSupportFragmentManager();
        int count = mgr.getBackStackEntryCount();

        FragmentTransaction fTrans = mgr.beginTransaction();

        if (!isAnimated)
        {
            fTrans.setCustomAnimations(R.animator.fragment_slide_left_enter,
                    R.animator.fragment_slide_left_exit,
                    R.animator.fragment_slide_right_enter,
                    R.animator.fragment_slide_right_exit);
        }

        if (null != bundle)
            fragment.setArguments(bundle);

        fTrans.add(R.id.container, fragment, String.valueOf(count));
        if (addToBackStack) fTrans.addToBackStack(fragment.getClass().getName());
        setFragment(fragment);

        return commitFragmentTransaction(fTrans);
    }

    default void fragmentPopAll()
    {
        postDeferredOperation(new DeferredOperation()
        {
            @Override
            public void execute()
            {
                FragmentManager mgr = getSupportFragmentManager();
                mgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    default void fragmentPopAll(int txnId)
    {
        postDeferredOperation(new DeferredOperation()
        {
            @Override
            public void execute()
            {
                FragmentManager mgr = getSupportFragmentManager();
                mgr.popBackStackImmediate(txnId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    /**
     * Commits a fragment transaction if possible. If the transaction could not be committed,
     * it will be queued until it can be.
     *
     * @param transaction the fragment transaction to execute.
     */
    default int commitFragmentTransaction(FragmentTransaction transaction)
    {
        int txnId = -1;

        if (getActivityManager().isActivityIsRunning())
        {
            txnId = transaction.commit();
        }
        else
        {
            DeferredOperation operation = new DeferredOperation()
            {
                @Override
                public void execute()
                {
                    transaction.commit();
                }
            };

            getActivityManager().addDeferredOperation(operation);
        }

        return txnId;
    }

    /**
     * Commits a fragment transaction if possible. If the transaction could not be committed,
     * it will be queued until it can be.
     *
     * @param transaction the fragment transaction to execute.
     */
    default void commitFragmentTransactionAnimated(FragmentTransaction transaction)
    {
        if (getActivityManager().isActivityIsRunning())
        {
            transaction.commit();
        }
        else
        {
            DeferredOperation operation = new DeferredOperation()
            {
                @Override
                public void execute()
                {
                    transaction.commit();
                }
            };

            getActivityManager().addDeferredOperation(operation);
        }
    }

    default void postDeferredOperation(DeferredOperation operation)
    {
        getActivityManager().postDeferredOperation(operation);
    }

    default void runDeferredOperations()
    {
        getActivityManager().runDeferredOperations();
    }
}
