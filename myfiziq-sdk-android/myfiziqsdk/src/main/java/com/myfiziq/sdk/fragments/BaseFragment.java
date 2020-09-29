package com.myfiziq.sdk.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.activities.DeferredOperation;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import timber.log.Timber;

/**
 * @hide
 */

abstract public class BaseFragment extends Fragment implements FragmentInterface
{
    public static final String BUNDLE_PARAMETERS = "PARAM";

    private static boolean poppingBackStack = false;

    private SparseArray<String> mParameterMap = new SparseArray<>();

    protected ParameterSet mParameterSet;

    protected abstract int getFragmentLayout();

    private boolean isRunning = true;


    /**
     * We can only perform operations against fragments BEFORE onSaveInstanceState is called.
     * However, in some cases we may want to do this after it has been called.
     *
     * For example, we may want to start a new activity and then pop the back stack.
     *
     * However, this is not possible since fragment operations must happen before
     * onSaveInstanceState is called (i.e. before the fragment is hidden).
     *
     * What would instead happen is that we would need to pop the back stack
     * (i.e. the current fragment), render the last fragment (which may take time and be costly)
     * and then we can open the new activity.
     *
     * What we can instead do is store fragment operations that happen after onSaveInstanceState
     * is called in a queue, to be performed later once the original fragment/activity is running.
     */
    private Queue<DeferredOperation> deferredOperations = new ArrayDeque<>();


    @Override
    public boolean onBackPressed()
    {
        if (null != mParameterSet)
        {
            if (mParameterSet.getNextSetIndex() >= 0)
            {
                // If the next screen in the parameter set tree data structure is NOT a child edge node,
                // go back to the previous child node.
                mParameterSet.setNextSetIndex(-1);
            }
            else if(null != mParameterSet.getParent())
            {
                ParameterSet parent = mParameterSet.getParent();

                // If the next screen IS the child edge node in the tree data structure,
                // go to the parent node and visit the previous node in the current tree level.
                parent.setNextSetIndex(-1);

                if (parent.isMyFiziqActivity())
                {
                    // We've visited all nodes in the ParameterSet, and the parent is an activity, so finish the activity.
                    getActivity().finish();
                }
            }
        }

        // Override and return 'true' to change default activity behaviour.
        return false;
    }

    @Nullable
    @Override
    public ActivityInterface getMyActivity()
    {
        return (ActivityInterface) getActivity();
    }

    /**
     * Gets the current locale that the user is using.
     */
    public Locale getLocale()
    {
        return getResources().getConfiguration().locale;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();

        if (null != bundle)
        {
            mParameterSet = bundle.getParcelable(BaseFragment.BUNDLE_PARAMETERS);
        }

        if (savedInstanceState != null)
        {
            ParameterSet potentialParameterSet = savedInstanceState.getParcelable(BUNDLE_PARAMETERS);

            if (null != potentialParameterSet)
            {
                mParameterSet = potentialParameterSet;
            }
        }

        if (0 != getFragmentLayout())
        {
            if (null != inflater)
            {
                View view = inflater.inflate(getFragmentLayout(), container, false);
                applyParameters(view);

                return view;
            }
            else
            {
                LayoutInflater.from(getContext()).inflate(getFragmentLayout(), container, false);
                View view = inflater.inflate(getFragmentLayout(), container, false);
                applyParameters(view);

                return view;
            }
        }

        return null;
    }

    @Override
    public boolean checkPermissions(String... permissions)
    {
        boolean bHasPermissions = true;
        for (String permission : permissions)
        {
            bHasPermissions &= ActivityCompat.checkSelfPermission(getActivity(), permission) == PackageManager.PERMISSION_GRANTED;
        }

        return bHasPermissions;
    }

    @Override
    public boolean allPermissionsGranted(int[] grantResults)
    {
        if (null != grantResults && grantResults.length > 0)
        {
            for (int result : grantResults)
            {
                if (result != PackageManager.PERMISSION_GRANTED)
                    return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public void askPermissions(final int requestCode, String reason, final String... permissions)
    {
        boolean bShowReason = false;

        for (String permission : permissions)
        {
            bShowReason |= shouldShowRequestPermissionRationale(permission);
        }

        if (bShowReason && !TextUtils.isEmpty(reason))
        {
            requestPermissions(permissions, requestCode);
        }
        else
        {
            requestPermissions(permissions, requestCode);
        }
    }

    @Override
    public void fragmentPopSelf()
    {
        postDeferredOperation(new DeferredOperation()
        {
            @Override
            public void execute()
            {
                getActivity().getSupportFragmentManager().popBackStackImmediate(
                        getClass().getName(),
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    @Override
    public void fragmentPopAll()
    {
        postDeferredOperation(new DeferredOperation()
        {
            @Override
            public void execute()
            {
                FragmentManager mgr = getFragmentManager();
                mgr.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        isRunning = true;

        Activity activity = getActivity();

        if (activity != null)
        {
            ActionBarHelper.resetBackButtonToDefaultState(activity);
        }

        runDeferredOperations();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        isRunning = true;

        runDeferredOperations();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        isRunning = false;

        outState.putParcelable(BUNDLE_PARAMETERS, mParameterSet);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        isRunning = false;
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Activity activity = getActivity();

        if (activity != null)
        {
            ActionBarHelper.resetBackButtonToDefaultState(activity);
        }
    }
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim)
    {
        if (nextAnim == 0)
        {
            return null;
        }
        BaseFragment self = this;
        Animator anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                if (enter)
                {
                    //Timber.v("Starting enter animation for %s", BaseFragment.this.getClass().getSimpleName());
                    onEnterAnimationStart();
                }
                else
                {
                    //Timber.v("Starting exit animation for %s", BaseFragment.this.getClass().getSimpleName());
                    onExitAnimationStart();
                }
                super.onAnimationStart(animation);
            }
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (enter)
                {
                    //Timber.v("Ending enter animation for %s", BaseFragment.this.getClass().getSimpleName());
                    self.onEnterAnimationEnd();
                }
                else
                {
                    //Timber.v("Ending exit animation for %s", BaseFragment.this.getClass().getSimpleName());
                    onExitAnimationEnd();
                }
                super.onAnimationEnd(animation);
            }
        });
        return anim;
    }
    /**
     * Executed when the fragment is beginning its enter animation/transition.
     *
     * Override to listen for when the fragment is beginning its enter animation/transition.
     */
    public void onEnterAnimationStart()
    {
        // Override me in the fragment
    }

    /**
     * Executed when the fragment has finished its enter animation/transition.
     *
     * Override to listen for when the fragment has finished its enter animation/transition.
     */
    public void onEnterAnimationEnd()
    {
        // Override me in the fragment
    }

    /**
     * Executed when the fragment is beginning its exit animation/transition.
     *
     * Override to listen for when the fragment is beginning its exit animation/transition.
     */
    public void onExitAnimationStart()
    {
        // Override me in the fragment
    }

    /**
     * Executed when the fragment has finished its exit animation/transition.
     *
     * Override to listen for when the fragment has finished its exit animation/transition.
     */
    public void onExitAnimationEnd()
    {
        // Override me in the fragment
    }

    int getRelativeLeft(View root, View myView) {
        if (myView.getParent() == root || myView.getParent() == null)
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft(root, (View) myView.getParent());
    }

    int getRelativeTop(View root, View myView) {
        if (myView.getParent() == root || myView.getParent() == null)
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop(root, (View) myView.getParent());
    }

    public static void fragmentCommit(BaseActivityInterface activity, ParameterSet params)
    {
        activity.fragmentCommit(newInstance(activity, params));
    }

    public static void fragmentCommit(BaseActivityInterface activity, ParameterSet params, boolean addToBackStack)
    {
        activity.fragmentCommit(newInstance(activity, params), addToBackStack);
    }

    public static <T extends BaseFragment> T newInstance(ActivityInterface activity, ParameterSet params)
    {
        Bundle args = new Bundle();
        setParameterSet(args, params);
        T frag = (T) T.instantiate(activity.getContext(), params.getFragment().getName());
        frag.setArguments(args);
        return frag;
    }

    public static void setParameterSet(Bundle bundle, ParameterSet parameterSet)
    {
        bundle.putParcelable(BUNDLE_PARAMETERS, parameterSet);
    }

    public SparseArray<String> getParameterMap()
    {
        return mParameterMap;
    }

    public ParameterSet getParameterSet()
    {
        Bundle args = getArguments();
        if (null != args)
        {
            return args.getParcelable(BUNDLE_PARAMETERS);
        }

        return null;
    }

    @Override
    public void applyParameters(View rootView)
    {
        if (null != rootView)
        {
            Bundle args = getArguments();
            if (null != args)
            {
                ParameterSet parameterSet = getParameterSet();
                if (null != parameterSet)
                {
                    parameterSet.applyParameters(this, rootView);
                }
            }
        }
    }

    @Override
    public int getParamMapAsInt(int paramId)
    {
        return Integer.valueOf(mParameterMap.get(paramId));
    }

    @Override
    public String getParamMapAsString(int paramId)
    {
        return mParameterMap.get(paramId);
    }

    /**
     * Determines if the fragment will soon be closed (i.e. is about to be removed from the back stack).
     */
    public static boolean willSoonClose()
    {
        return BaseFragment.poppingBackStack;
    }

    public static void setPoppingBackStack(boolean poppingBackStack)
    {
        BaseFragment.poppingBackStack = poppingBackStack;
    }

    private void postDeferredOperation(DeferredOperation operation)
    {
        if (isRunning)
        {
            operation.execute();
        }
        else
        {
            deferredOperations.add(operation);
        }
    }

    private void runDeferredOperations()
    {
        while (!deferredOperations.isEmpty())
        {
            final DeferredOperation operation = deferredOperations.remove();

            // Add the Deferred Operation to the Android event loop to be executed once
            // other internal fragment transactions have finished
            new Handler(Looper.getMainLooper()).post(operation::execute);
        }
    }
}
