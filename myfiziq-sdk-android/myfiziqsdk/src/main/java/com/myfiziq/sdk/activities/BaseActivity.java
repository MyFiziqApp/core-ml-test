package com.myfiziq.sdk.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

/**
 * Support methods commonly used in Activities.
 *
 * @hide
 */
public class BaseActivity extends AppCompatActivity implements BaseActivityInterface
{
    String logTag;
    ParameterSet mParameterSet;

    ActivityManager activityManager = new ActivityManager(this);

    @Override
    public Activity getActivity()
    {
        return this;
    }

    @Override
    public Context getContext()
    {
        return this;
    }

    @Override
    public ActivityManagerInterface getActivityManager()
    {
        return activityManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        activityManager.onCreate(savedInstanceState);
        logTag = getClass().getSimpleName();
        mParameterSet = getIntent().getParcelableExtra(BaseFragment.BUNDLE_PARAMETERS);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        activityManager.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        activityManager.onResume();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        activityManager.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        activityManager.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed()
    {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
        {
            Fragment fragment = activityManager.getFragment();
            if (!(fragment instanceof BaseFragment)  || !((BaseFragment)fragment).onBackPressed())
            {
                super.onBackPressed();
            }
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void fragmentPopAll()
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

    @Override
    public void fragmentPopAll(final int txnId)
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


    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
        /*
        if (SisterColors.getInstance().isSisterMode())
        {
            super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
        }
        else
        {
            super.attachBaseContext(newBase);
        }
        */
    }

    public boolean hasParam(int paramId)
    {
        return (null != mParameterSet && mParameterSet.hasParam(paramId));
    }

    public Parameter getParam(int paramId)
    {
        if (null != mParameterSet)
        {
            return mParameterSet.getParam(paramId);
        }

        return null;
    }

    public String getStringParamValue(int paramId, String defaultValue)
    {
        if (null != mParameterSet)
        {
            return mParameterSet.getStringParamValue(paramId, defaultValue);
        }

        return null;
    }
}
