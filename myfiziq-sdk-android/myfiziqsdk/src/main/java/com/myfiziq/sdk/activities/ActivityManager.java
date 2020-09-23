package com.myfiziq.sdk.activities;

import android.app.Activity;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import timber.log.Timber;

class ActivityManager implements FragmentManager.OnBackStackChangedListener, ActivityManagerInterface
{
    /**
     * We can only perform operations against fragments BEFORE onSaveInstanceState is called.
     * However, in some cases we may want to do this after it has been called.
     * <p>
     * For example, we may want to start a new activity and then pop the back stack.
     * <p>
     * However, this is not possible since fragment operations must happen before
     * onSaveInstanceState is called (i.e. before the fragment is hidden).
     * <p>
     * What would instead happen is that we would need to pop the back stack
     * (i.e. the current fragment), render the last fragment (which may take time and be costly)
     * and then we can open the new activity.
     * <p>
     * What we can instead do is store fragment operations that happen after onSaveInstanceState
     * is called in a queue, to be performed later once the original fragment/activity is running.
     */
    private Queue<DeferredOperation> deferredOperations = new ArrayDeque<>();

    private boolean mActivityIsRunning = false;

    private WeakReference<Activity> mActivity;
    private WeakReference<Fragment> mFragment;

    public ActivityManager(Activity activity)
    {
        mActivity = new WeakReference<>(activity);
        addOnBackStackChangedListener();
    }

    @Override
    public void setFragment(Fragment fragment)
    {
        mFragment = new WeakReference<>(fragment);
    }

    @Override
    public Fragment getFragment()
    {
        if (null != mFragment)
        {
            return mFragment.get();
        }

        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

    }

    @Override
    public void onStart()
    {
        mActivityIsRunning = true;

        runDeferredOperations();
    }

    protected void onResume()
    {
        mActivityIsRunning = true;

        runDeferredOperations();
    }

    protected void onStop()
    {
        mActivityIsRunning = false;

        // Remove the reference to the fragment to prevent a leak
        mFragment = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        mActivityIsRunning = false;
    }

    @Override
    public boolean isActivityIsRunning()
    {
        return mActivityIsRunning;
    }

    @Override
    public void addDeferredOperation(DeferredOperation operation)
    {
        deferredOperations.add(operation);
    }

    /**
     * Adds an operation to be executed once the activity is running again.
     * @param operation The operation to execute.
     */
    @Override
    public void postDeferredOperation(DeferredOperation operation)
    {
        if (mActivityIsRunning)
        {
            operation.execute();
        }
        else
        {
            deferredOperations.add(operation);
        }
    }

    @Override
    public void runDeferredOperations()
    {
        while (!deferredOperations.isEmpty())
        {
            final DeferredOperation operation = deferredOperations.remove();

            // Add the Deferred Operation to the Android event loop to be executed once
            // other internal fragment transactions have finished
            operation.execute();
        }
    }

    private void addOnBackStackChangedListener()
    {
        Activity activity = mActivity.get();
        if (null != activity)
        {
            // First try to use getSupportFragmentManager...
            if (activity instanceof AppCompatActivity)
            {
                ((AppCompatActivity) activity).getSupportFragmentManager().addOnBackStackChangedListener(this);
            }
            else
            {
                throw new RuntimeException("Activity must be an instance of AppCompatActivity");
            }
        }
    }

    @Override
    public void onBackStackChanged()
    {
        Activity activity = mActivity.get();
        FragmentManager mgr = null;
        // Should always be an instanceof AppCompatActivity unless null.
        if (activity instanceof AppCompatActivity)
        {
            mgr = ((AppCompatActivity) activity).getSupportFragmentManager();
        }

        if (null != mgr)
        {
            int count = mgr.getBackStackEntryCount();

            if (count > 0)
            {
                FragmentManager.BackStackEntry backStackEntry = mgr.getBackStackEntryAt(count - 1);

                String fragmentName = backStackEntry.getName();
                setFragment(mgr.findFragmentByTag(fragmentName));
            }
            else
            {
                List<Fragment> availableFragments = mgr.getFragments();

                if (!availableFragments.isEmpty())
                {
                    Fragment fragment = availableFragments.get(0);
                    setFragment(fragment);
                }
                else
                {
                    Timber.w(BaseActivity.class.getName(), "Back stack is empty. Unexpected behaviour may occur.");
                }
            }
        }
    }
}
