package com.myfiziq.sdk.lifecycle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.enums.ParameterSetName;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.views.MYQFrameLayout;
import com.myfiziq.sdk.views.MYQViewInterface;

import java.util.ArrayList;
import java.util.Iterator;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * ParameterSet contains a set of Parameters for an Activity or Fragment.
 * ParameterSets can be nested (chained) to define a flow or sequence of Fragments or Activities.
 * The ParameterSet for an Activity or Fragment is passed as a Bundle (BaseFragment.BUNDLE_PARAMETERS).
 * PerameterSet chaining (flow) is controlled by the set name.
 * ParameterSet is also used to define a set of Parameters for a StyledRecyclerView.
 */
public class ParameterSet implements Parcelable
{
    private enum ClassType
    {
        CLASS_UNKNOWN,
        CLASS_MYFIZIQ_ACTIVITY,
        CLASS_CUSTOMER_ACTIVITY,
        CLASS_FRAGMENT
    }

    private int mRecyclerId = 0;
    private LayoutStyle mLayoutStyle = LayoutStyle.VERTICAL;

    private String mName = "";
    private String mClass = "";

    @Nullable
    private String mNextState = "";

    private ClassType mClassType = ClassType.CLASS_UNKNOWN;
    private ParameterSet mParentSet = null;
    private ArrayList<ParameterSet> mNextSets = new ArrayList<>();
    private ArrayList<Parameter> mParameters = new ArrayList<>();

    private int mNextSetIx = -1;

    private ParameterSet()
    {

    }

    public ParameterSet(int recyclerId, LayoutStyle layoutStyle)
    {
        mRecyclerId = recyclerId;
        mLayoutStyle = layoutStyle;
    }

    ParameterSet(Class<?> clazz)
    {
        setClazz(clazz);
    }

    public void start(ActivityInterface activity)
    {
        if (isMyFiziqActivity() || isCustomerActivity())
        {
            startActivity(activity);
        }
        else if (isFragment() && activity instanceof BaseActivityInterface)
        {
            ((BaseActivityInterface)activity).fragmentCommit(this, true);
        }
    }

    public void start()
    {
        if (isMyFiziqActivity() || isCustomerActivity())
        {
            startActivity();
        }
        else if (isFragment())
        {
            throw new IllegalStateException("Cannot start without a context when the current ParameterSet is a fragment.");
        }
    }

    public void start(ActivityInterface activity, boolean addToBackStack)
    {
        if (isMyFiziqActivity() || isCustomerActivity())
        {
            startActivity(activity);
        }
        else if (isFragment() && activity instanceof BaseActivityInterface)
        {
            ((BaseActivityInterface)activity).fragmentCommit(this, addToBackStack);
        }
    }

    /**
     * Starts an activity contained in the current ParameterSet without passing in the current user and status.
     *
     * @param context The context.
     */
    public void startBlankActivity(Context context)
    {
        Intent intent = toBlankIntent(context);
        context.startActivity(intent);
    }

    /**
     * Generates an intent intent based on the current ParameterSet without passing in the current user and status.
     *
     * @param context The context.
     */
    public Intent toBlankIntent(Context context)
    {
        Intent intent;

        if (isMyFiziqActivity())
        {
            intent = new Intent(context, getMyActivity());
            intent.putExtra(BaseFragment.BUNDLE_PARAMETERS, this);
        }
        else if (isCustomerActivity())
        {
            intent = new Intent(context, getCustomerActivity());
            intent.putExtra(BaseFragment.BUNDLE_PARAMETERS, this);
        }
        else
        {
            throw new IllegalStateException("Cannot generate an intent to start a blank activity when the current ParameterSet is a fragment.");
        }

        return intent;
    }

    /**
     * Starts an activity contained in the current ParameterSet while passing in the user and status
     * from the current activity.
     *
     */
    public void startActivity()
    {
        Intent intent = toIntent(null);
        intent.addFlags(getIntParamValue(R.id.TAG_ARG_ACTIVITY_FLAGS, 0));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        GlobalContext.getContext().startActivity(intent);
    }

    /**
     * Starts an activity contained in the current ParameterSet while passing in the user and status
     * from the current activity.
     *
     * @param currentActivity The current activity.
     */
    public void startActivity(ActivityInterface currentActivity)
    {
        Intent intent = toIntent(currentActivity);
        intent.addFlags(getIntParamValue(R.id.TAG_ARG_ACTIVITY_FLAGS, 0));
        Activity activity = currentActivity.getActivity();
        activity.startActivity(intent);
    }

    /**
     * Generates an intent based on the current ParameterSet while passing in the user and status
     * from the current activity.
     *
     * @param currentActivity The current activity.
     */
    public Intent toIntent(ActivityInterface currentActivity)
    {
        Intent intent;
        Context context = GlobalContext.getContext();

        if (isMyFiziqActivity())
        {
            intent = new Intent(context, getMyActivity());
            intent.putExtra(BaseFragment.BUNDLE_PARAMETERS, this);

            //TODO: Do we actually need this?
            /*
            // instanceof also checks if not null
            if (currentActivity instanceof BaseActivityInterface)
            {
                // TODO Find a way to uncouple this from the ModelAvatarUser
                if (!hasParam(R.id.TAG_ARG_GENDER) && !hasParam(R.id.TAG_ARG_HEIGHT_IN_CM) && !hasParam(R.id.TAG_ARG_WEIGHT_IN_KG))
                {
                    ModelAvatar avatar = ((BaseActivityInterface) currentActivity).getAvatar();

                    if (avatar != null)
                    {
                        setParam(new Parameter(R.id.TAG_ARG_GENDER, String.valueOf(avatar.getGender())));
                        setParam(new Parameter(R.id.TAG_ARG_WEIGHT_IN_KG, String.valueOf(avatar.getWeight().getValueInKg())));
                        setParam(new Parameter(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, String.valueOf(avatar.getWeight().getInternalName())));
                        setParam(new Parameter(R.id.TAG_ARG_HEIGHT_IN_CM, String.valueOf(avatar.getHeight().getValueInCm())));
                        setParam(new Parameter(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, String.valueOf(avatar.getHeight().getInternalName())));

                    if (DateOfBirthCoordinator.getDateOfBirth() != null)
                    {
                        String dobString = DateOfBirthCoordinator.getDateOfBirthAsString();
                        setParam(new Parameter(R.id.TAG_ARG_DOB, dobString));
                    }
                    }
                    else
                    {
                        Timber.w("Avatar is null for a MyFiziq activity");
                    }
                }
            }
            */
        }
        else if (isCustomerActivity())
        {
            intent = new Intent(context, getCustomerActivity());
            intent.putExtra(BaseFragment.BUNDLE_PARAMETERS, this);
        }
        else
        {
            throw new IllegalStateException("Cannot generate an intent to start an activity when the current ParameterSet is a fragment.");
        }

        return intent;
    }

    public Bundle toBundle(Context context)
    {
        Bundle bundle = null;

        //if (isFragment())
        {
            bundle = new Bundle();
            bundle.putParcelable(BaseFragment.BUNDLE_PARAMETERS, this);
        }

        return bundle;
    }

    public String getClazz()
    {
        return mClass;
    }

    public void setClazz(Class clazz)
    {
        mClass = clazz.getName();

        if (ActivityInterface.class.isAssignableFrom(clazz))
        {
            // This is an activity in the MyFiziq SDK
            mClassType = ClassType.CLASS_MYFIZIQ_ACTIVITY;
        }
        else if (Activity.class.isAssignableFrom(clazz))
        {
            // This is an activity in the customer app
            mClassType = ClassType.CLASS_CUSTOMER_ACTIVITY;
        }
        else if (FragmentInterface.class.isAssignableFrom(clazz))
        {
            mClassType = ClassType.CLASS_FRAGMENT;
        }
    }

    public ArrayList<Parameter> getParameters()
    {
        return mParameters;
    }

    public void addNextSet(ParameterSet set)
    {
        set.mParentSet = this;
        mNextSets.add(set);
    }

    public void insertNextSet(int index, ParameterSet set)
    {
        set.mParentSet = this;

        if (index > 0)
        {
            // chain current set to progress to new set...
            mNextSets.get(index - 1).mNextState = set.mName;
        }
        else
        {
            mNextState = set.mName;
        }

        // chain new set to progress to ...
        set.mNextState = findNext(index);

        // insert in to next sets.
        mNextSets.add(index, set);
    }

    public void insertNextSets(int index, ArrayList<ParameterSet> sets)
    {
        for (ParameterSet set : mNextSets)
        {
            set.mParentSet = this;
        }

        if (sets.size() > 0)
        {
            // chain current set to progress to new set...
            if (index > 0)
            {
                mNextSets.get(index - 1).mNextState = sets.get(0).mName;
            }
            else
            {
                mNextState = sets.get(0).mName;
            }

            // chain new set to progress to ...
            sets.get(sets.size() - 1).mNextState = findNext(index);
        }

        mNextSets.addAll(index, sets);
    }

    public void mergeSet(int index, ParameterSet set)
    {
        if (mClass.contentEquals(set.mClass))
        {
            for (ParameterSet s : set.mNextSets)
            {
                s.mParentSet = this;
            }
            mNextSets.addAll(index, set.mNextSets);
        }
    }

    public void mergeSetParameters(ParameterSet set)
    {
        for (Parameter p : set.mParameters)
        {
            addParam(p);
        }
    }

    public boolean hasNextSet()
    {
        return (mNextSets.size() > 0);
    }

    public Parameter addParam(Parameter parameter)
    {
        mParameters.add(parameter);
        return parameter;
    }

    public Parameter setParam(Parameter parameter)
    {
        Iterator<Parameter> iter = mParameters.iterator();
        while (iter.hasNext())
        {
            Parameter param = iter.next();

            if (param.getParamId() == parameter.getParamId())
            {
                iter.remove();
                break;
            }
        }
        mParameters.add(parameter);
        return parameter;
    }

    public boolean hasParam(int paramId)
    {
        boolean bFound = false;
        for (Parameter param : mParameters)
        {
            if (param.getParamId() == paramId)
            {
                bFound = true;
                break;
            }
        }

        return bFound;
    }

    public boolean hasParam(int viewId, int paramId)
    {
        boolean bFound = false;
        for (Parameter param : mParameters)
        {
            if (param.getViewId() == viewId && param.getParamId() == paramId)
            {
                bFound = true;
                break;
            }
        }

        return bFound;
    }

    public Parameter getParam(int paramId)
    {
        for (Parameter param : mParameters)
        {
            if (param.getParamId() == paramId)
            {
                return param;
            }
        }

        return null;
    }

    public String getStringParamValue(int paramId, String defaultValue)
    {
        Parameter param = getParam(paramId);

        if (null != param)
        {
            return param.getValue();
        }
        else
        {
            return defaultValue;
        }
    }

    @Nullable
    public <T extends Enum<T>> T getEnumParamValue(Class<T> enumClass, int paramId)
    {
        String value = getStringParamValue(paramId, "");

        if (!TextUtils.isEmpty(value))
        {
            return T.valueOf(enumClass, value);
        }
        else
        {
            return null;
        }
    }

    public int getIntParamValue(int paramId, int defaultValue)
    {
        Parameter param = getParam(paramId);

        if (null != param)
        {
            try
            {
                return Integer.parseInt(param.getValue());
            }
            catch (NumberFormatException e)
            {
                return defaultValue;
            }
        }
        else
        {
            return defaultValue;
        }
    }

    public double getDoubleParamValue(int paramId, double defaultValue)
    {
        Parameter param = getParam(paramId);

        if (null != param)
        {
            try
            {
                return Double.parseDouble(param.getValue());
            }
            catch (NumberFormatException e)
            {
                return defaultValue;
            }
        }
        else
        {
            return defaultValue;
        }
    }

    public boolean getBooleanParamValue(int paramId, boolean defaultValue)
    {
        Parameter param = getParam(paramId);

        if (null != param)
        {
            return Boolean.parseBoolean(param.getValue());
        }
        else
        {
            return defaultValue;
        }
    }

    public Parameter getParam(int viewId, int paramId)
    {
        for (Parameter param : mParameters)
        {
            if (param.getViewId() == viewId && param.getParamId() == paramId)
            {
                return param;
            }
        }

        return null;
    }

    public Parameter addParam(int viewId, int paramId, int value)
    {
        Parameter param = getParam(viewId, paramId);
        if (null == param)
        {
            param = new Parameter(viewId, paramId, value);
            mParameters.add(param);
        }

        return param;
    }

    public Class<?> getParamSetClass()
    {
        if (!TextUtils.isEmpty(mClass))
        {
            try
            {
                Class<?> c = Class.forName(mClass);

                return c;
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    public boolean isMyFiziqActivity()
    {
        return mClassType == ClassType.CLASS_MYFIZIQ_ACTIVITY;
    }

    public boolean isCustomerActivity()
    {
        return mClassType == ClassType.CLASS_CUSTOMER_ACTIVITY;
    }

    public Class<? extends ActivityInterface> getMyActivity()
    {
        if (!isMyFiziqActivity())
        {
            Timber.e("%s is not a MyFiziq Activity", mClass);
            return null;
        }

        if (!TextUtils.isEmpty(mClass))
        {
            try
            {
                Class<?> c = Class.forName(mClass);

                return c.asSubclass(ActivityInterface.class);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    public Class<? extends Activity> getCustomerActivity()
    {
        if (!isCustomerActivity())
        {
            Timber.e("%s is not a Customer Activity", mClass);
            return null;
        }

        if (!TextUtils.isEmpty(mClass))
        {
            try
            {
                Class<?> c = Class.forName(mClass);

                return c.asSubclass(Activity.class);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    public boolean isFragment()
    {
        return mClassType == ClassType.CLASS_FRAGMENT;
    }

    public Class<? extends FragmentInterface> getFragment()
    {
        if (!TextUtils.isEmpty(mClass))
        {
            try
            {
                Class<?> c = Class.forName(mClass);

                return c.asSubclass(FragmentInterface.class);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Recursively search through all child and parent sets to find a set with 'name'
     * @param name
     * @return
     */
    public ParameterSet findSet(String name)
    {
        // Go up the tree to the top level set..
        ParameterSet set = this;
        while (null != set.mParentSet)
        {
            set = mParentSet;
        }

        return set.getSubSet(name);
    }

    /**
     * Recursively search through all child and parent sets to find a set with 'name'
     * @param name
     * @return
     */
    public ParameterSet findSet(ParameterSetName name)
    {
        // Go up the tree to the top level set..
        ParameterSet set = this;
        while (null != set.mParentSet)
        {
            set = mParentSet;
        }

        return set.getSubSet(name.name());
    }
    /**
     * Recursively search through all child sets to find a set with 'name'
     * @param name
     * @return
     */
    public ParameterSet getSubSet(String name)
    {
        for (ParameterSet set : mNextSets)
        {
            if (set.mName.contentEquals(name))
            {
                return set;
            }
            else
            {
                ParameterSet subset = set.getSubSet(name);
                if (null != subset)
                    return subset;
            }
        }

        return null;
    }

    /**
     * Returns the index of an immediate ParameterSet child node that has a name
     * specified by "next".
     * <p>
     * If there are no immediate children that have the specified name, "-1" is returned.
     *
     * @param next The name of the node to locate.
     */
    public int findNextIx(String next)
    {
        int ix = 0;
        for (ParameterSet set : mNextSets)
        {
            if (set.mName.contentEquals(next))
            {
                return ix;
            }
            ix++;
        }

        return -1;
    }

    /**
     * Returns the index of the next child ParameterSet node.
     * <p>
     * If a "Next State" has been specified, the index location of the immediate child node that
     * has that name will be returned.
     * <p>
     * If there are no more ParameterSet children to visit or if "Next State" cannot be found
     * (if it has been specified), -1 will be returned.
     */
    public int findNext()
    {
        ParameterSet set = null;

        if (mNextSetIx < 0)
        {
            set = this;
        }
        else if (mNextSetIx < mNextSets.size())
        {
            set = mNextSets.get(mNextSetIx);
        }

        if (null != set)
        {
            if (!TextUtils.isEmpty(set.mNextState))
            {
                int ix = 0;
                // TODO Is this a bug? Should be set.mNextSets?
                for (ParameterSet s : mNextSets)
                {
                    if (s.mName.contentEquals(set.mNextState))
                    {
                        return ix;
                    }
                    ix++;
                }
            }
            else if (set.mNextSets.size() > 0)
            {
                // If the next state hasn't been specified, assume we're going to the next immediate one.
                return 0;
            }
        }

        return -1;
    }

    /**
     * Finds the a child ParameterSet node at the specified location.
     * <p>
     * If no ParameterSet exists at that location, null is returned.
     *
     * @param ix The index of the child ParameterSet to locate.
     * @return The name of the ParameterSet at the specified location.
     */
    @Nullable
    public String findNext(int ix)
    {
        if (ix >= 0 && ix < mNextSets.size())
        {
            ParameterSet set = mNextSets.get(ix);
            if (null != set)
            {
                return set.mName;
            }
        }

        return null;
    }

    /**
     * Starts the next fragment or activity based on the {@code next} ParameterSet name that has been specified.
     * <p>
     * The {@code next} ParameterSet must be a child of the current node.
     * <p>
     * If the {@code next} ParameterSet cannot be found, the parent node will be started.
     * <p>
     * If there is no parent node, the activity will finish.
     *
     * @param next           The name of the next ParameterSet to start.
     * @param activity       The current activity in scope.
     * @param clearBackStack Whether we should clear the back button history so that the user cannot go backwards.
     */
    public void setNext(String next, ActivityInterface activity, boolean clearBackStack)
    {
        mNextSetIx = findNextIx(next);
        if (mNextSetIx >= 0)
        {
            ParameterSet set = mNextSets.get(mNextSetIx);

            if (clearBackStack && activity instanceof BaseActivityInterface)
            {
                ((BaseActivityInterface)activity).fragmentPopAll();
            }

            if (set.isMyFiziqActivity() || set.isCustomerActivity())
            {
                set.startActivity(activity);
            }
            else if (set.isFragment() && activity instanceof BaseActivityInterface)
            {
                BaseFragment.fragmentCommit((BaseActivityInterface)activity, set);
            }
        }
        else if (null != mParentSet)
        {
            mParentSet.setNext(next, activity, clearBackStack);
        }
        else
        {
            activity.finish();
        }
    }

    /**
     * Starts the next fragment or activity based on where we are in the current tree of ParameterSets.
     * <p>
     * If there are any child nodes in the ParameterSet tree, we will visit them recursively until we have visited each one.
     * <p>
     * Once we have visited all children, we will then visit the parent.
     * <p>
     * If there are no more nodes to visit, the activity will finish.
     *
     * @param activity       The current activity in scope.
     * @param clearBackStack Whether we should clear the back button history so that the user cannot go backwards.
     */
    public void startNext(ActivityInterface activity, boolean clearBackStack)
    {
        startNext(activity, clearBackStack, true);
    }

    /**
     * Starts the next fragment or activity based on where we are in the current tree of ParameterSets.
     * <p>
     * If there are any child nodes in the ParameterSet tree, we will visit them recursively until we have visited each one.
     * <p>
     * Once we have visited all children, we will then visit the parent.
     * <p>
     * If there are no more nodes to visit, the activity will finish.
     *
     * @param activity       The current activity in scope.
     * @param clearBackStack Whether we should clear the back button history so that the user cannot go backwards.
     * @param addToBackstack Whether the current fragment should be added to the backstack before we start another one
     */
    public void startNext(ActivityInterface activity, boolean clearBackStack, boolean addToBackstack)
    {
        mNextSetIx = findNext();

        if (mNextSetIx >= 0)
        {
            ParameterSet set = mNextSets.get(mNextSetIx);

            if (clearBackStack && activity instanceof BaseActivityInterface)
            {
                ((BaseActivityInterface)activity).fragmentPopAll();
            }

            if (set.isMyFiziqActivity() || set.isCustomerActivity())
            {
                set.startActivity(activity);
            }
            else if (set.isFragment() && activity instanceof BaseActivityInterface)
            {
                BaseFragment.fragmentCommit((BaseActivityInterface)activity, set, addToBackstack);
            }
        }
        else if (null != mParentSet)
        {
            mParentSet.startNext(activity, clearBackStack, addToBackstack);
        }
        else
        {
            if (clearBackStack && activity instanceof BaseActivityInterface)
            {
                // Ensure that the back stack is clear when we finish our activity to prevent memory leaks
                ((BaseActivityInterface)activity).fragmentPopAll();
            }

            activity.finish();
        }
    }

    /**
     * Find and return the next set in the chain.
     * If there are no more sets in the chain null is returned.
     * @return The next ParameterSet or null.
     */
    public ParameterSet getNext()
    {
        mNextSetIx = findNext();

        if (mNextSetIx >= 0)
        {
            return mNextSets.get(mNextSetIx);
        }
        else if (null != mParentSet)
        {
            return mParentSet.getNext();
        }

        return null;
    }

    /**
     * Returns the ParameterSet children of the current node.
     */
    public ArrayList<ParameterSet> getNextSets()
    {
        return mNextSets;
    }

    private View addView(BaseFragment fragment, View rootView, Parameter parameter)
    {
        View view = null;
        if (rootView instanceof ViewGroup)
        {
            try
            {
                Activity activity = fragment.getActivity();
                LayoutInflater inflater = activity.getLayoutInflater();
                view = inflater.inflate(parameter.getViewId(), (ViewGroup) rootView);
            }
            catch (Throwable e)
            {
                Timber.e(e, "Exception encountered when adding a view from a ParameterSet");

                String viewName = fragment.getActivity().getResources().getResourceName(parameter.getViewId());
                Timber.e(e, "Error encountered when inflating: %s", viewName);
            }
        }
        return view;
    }

    public void applyParameters(BaseFragment fragment, View rootView)
    {
        if (null != rootView)
        {
            Resources resources = fragment.getResources();

            for (Parameter parameter : getParameters())
            {
                // Is this a View setting?
                if (0 != parameter.getViewId())
                {
                    View view = rootView.findViewById(parameter.getViewId());
                    if (null == view)
                    {
                        view = addView(fragment, rootView, parameter);
                    }

                    if (null != view)
                    {
                        if (view instanceof MYQViewInterface)
                        {
                            ((MYQViewInterface) view).applyParameter(parameter);
                        }
                        // Can't use a switch statement here as Resource id's aren't constant in lib projects.
                        else if (R.styleable.MyFiziq_MYQ_background == parameter.getParamId())
                        {
                            view.setBackgroundColor(parameter.getColor(resources));
                        }
                        else if (R.styleable.MyFiziq_MYQ_text == parameter.getParamId())
                        {
                            // TODO: handle other View types as needed.
                            if (view instanceof TextView)
                            {
                                ((TextView) view).setText(parameter.getValue());
                            }
                            else if (view instanceof MYQFrameLayout)
                            {
                                ((MYQFrameLayout) view).setText(parameter.getValue());
                            }
                        }
                        else if (R.styleable.MyFiziq_MYQ_textColor == parameter.getParamId())
                        {
                            // TODO: handle other View types as needed.
                            if (view instanceof TextView)
                            {
                                ((TextView) view).setTextColor(parameter.getColor(resources));
                            }
                            else if (view instanceof MYQFrameLayout)
                            {
                                ((MYQFrameLayout) view).setTextColor(parameter.getColor(resources));
                            }
                        }
                    }
                }
                else
                {
                    fragment.getParameterMap().put(parameter.getParamId(), parameter.getValue());
                }
            }
        }
    }

    /**
     * Resets the ParameterSet tree to its original state.
     */
    public void resetState()
    {
        resetState(this, false);
    }

    /**
     * Finds the root node in a ParameterSet tree.
     *
     * @param set The tree set to get a root node in. The set can be pointing to any level in the tree.
     * @return The root node.
     */
    public ParameterSet findRootSet(ParameterSet set)
    {
        if (set.mParentSet == null)
        {
            return set;
        }
        else
        {
            return findRootSet(set.mParentSet);
        }
    }

    /**
     * Resets the ParameterSet tree to its original state.
     */
    private void resetState(ParameterSet set, boolean hasAlreadyVisitedRoot)
    {
        if (!hasAlreadyVisitedRoot)
        {
            // Go to the top of the tree and reset everything below it if we haven't visited the root already
            set = findRootSet(set);

            // We're currently viewing the root set, so set it to 0
            set.mNextSetIx = 0;
        }
        else
        {
            // Reset the current node. We haven't visited it yet.
            set.mNextSetIx = -1;
        }

        for (ParameterSet child : set.mNextSets)
        {
            // Reset all child nodes
            resetState(child, true);
        }
    }

    /**
     * Returns the parent of the current node.
     */
    public ParameterSet getParent()
    {
        return mParentSet;
    }

    public int getNextSetIndex()
    {
        return mNextSetIx;
    }

    public void setNextSetIndex(int index)
    {
        mNextSetIx = index;
    }

    public LayoutStyle getLayoutStyle()
    {
        return mLayoutStyle;
    }

    /**
     * Sets the parent of the current node.
     *
     * @param mParentSet The parent node to set.
     */
    public void setParent(ParameterSet mParentSet)
    {
        this.mParentSet = mParentSet;
    }

    // TODO REMOVE
    public void setNextIndex(int mNextSetIx)
    {
        this.mNextSetIx = mNextSetIx;
    }

    public void setNextState(@Nullable String mNextState)
    {
        this.mNextState = mNextState;
    }

    public void setNextState(ParameterSetName mNextState)
    {
        this.mNextState = mNextState.name();
    }
    public String getName()
    {
        return mName;
    }

    private ParameterSet(Parcel in)
    {
        int listSize = 0;
        mRecyclerId = in.readInt();
        mLayoutStyle = LayoutStyle.values()[in.readInt()];

        mClassType = ClassType.values()[in.readInt()];

        if (0 != in.readByte())
        {
            mName = in.readString();
        }

        if (0 != in.readByte())
        {
            mClass = in.readString();
        }

        if (0 != in.readByte())
        {
            mNextState = in.readString();
        }

        listSize = in.readInt();
        for (int i = 0; i < listSize; i++)
        {
            ParameterSet set = in.readParcelable(ParameterSet.class.getClassLoader());
            if (null != set)
                addNextSet(set);
        }

        listSize = in.readInt();
        for (int i = 0; i < listSize; i++)
        {
            Parameter parameter = in.readParcelable(Parameter.class.getClassLoader());
            if (null != parameter)
                addParam(parameter);
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags)
    {
        dest.writeInt(mRecyclerId);
        dest.writeInt(mLayoutStyle.ordinal());

        dest.writeInt(mClassType.ordinal());

        if (!TextUtils.isEmpty(mName))
        {
            dest.writeByte((byte) 1);
            dest.writeString(mName);
        }
        else
        {
            dest.writeByte((byte) 0);
        }

        if (!TextUtils.isEmpty(mClass))
        {
            dest.writeByte((byte) 1);
            dest.writeString(mClass);
        }
        else
        {
            dest.writeByte((byte) 0);
        }

        if (!TextUtils.isEmpty(mNextState))
        {
            dest.writeByte((byte) 1);
            dest.writeString(mNextState);
        }
        else
        {
            dest.writeByte((byte) 0);
        }

        dest.writeInt(mNextSets.size());
        for (ParameterSet set : mNextSets)
        {
            dest.writeParcelable(set, flags);
        }

        dest.writeInt(mParameters.size());
        for (Parameter set : mParameters)
        {
            dest.writeParcelable(set, flags);
        }
    }

    public static final Creator<ParameterSet> CREATOR = new Creator<ParameterSet>()
    {
        @Override
        public ParameterSet createFromParcel(Parcel in)
        {
            return new ParameterSet(in);
        }

        @Override
        public ParameterSet[] newArray(int size)
        {
            return new ParameterSet[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static class Builder
    {
        ParameterSet mSet = new ParameterSet();

        public Builder()
        {

        }

        /**
         * Create a new ParameterSet Builder for an Activity or Fragment
         * @param clazz - A Fragment or Activity class type.
         */
        public Builder(Class clazz)
        {
            mSet.setClazz(clazz);
        }

        /**
         * Get the resulting ParameterSet
         * @return The built ParameterSet.
         */
        public ParameterSet build()
        {
            return mSet;
        }

        /**
         * Set the name of the ParameterSet for flow control.
         * @param name
         * @return Builder
         */
        public Builder setName(String name)
        {
            mSet.mName = name;
            return this;
        }

        /**
         * Set the name of the ParameterSet for flow control.
         * @param name
         * @return Builder
         */
        public Builder setName(ParameterSetName name)
        {
            mSet.mName = name.name();
            return this;
        }
        /**
         * Set the name of the next ParameterSet to flow to when this one completes.
         * @param next
         * @return Builder
         */
        public Builder setNext(String next)
        {
            mSet.mNextState = next;
            return this;
        }

        /**
         * Set the name of the next ParameterSet to flow to when this one completes.
         * @param next
         * @return Builder
         */
        public Builder setNext(ParameterSetName next)
        {
            mSet.mNextState = next.name();
            return this;
        }

        /**
         * Set the LayoutStyle for the owning RecyclerView
         * @param layoutStyle
         * @return
         */
        public Builder setLayout(LayoutStyle layoutStyle)
        {
            mSet.mLayoutStyle = layoutStyle;
            return this;
        }

        /**
         * Add a new Parameter to the set
         * @param parameter
         * @return
         */
        public Builder addParam(Parameter parameter)
        {
            mSet.addParam(parameter);
            return this;
        }

        /**
         * Add a ParameterSet to the list of nested sets.
         * @param set
         * @return
         */
        public Builder addNextSet(ParameterSet set)
        {
            mSet.addNextSet(set);
            return this;
        }

        /**
         * Add a ParameterSet to the list of nested sets at the specified index.
         * @param index
         * @param set
         * @return
         */
        public Builder insertNextSet(int index, ParameterSet set)
        {
            mSet.insertNextSet(index, set);
            return this;
        }

        /**
         * Set the Fragment or Activity class for this set.
         * @param clazz
         * @return
         */
        public Builder setClass(Class<?> clazz)
        {
            mSet.setClazz(clazz);
            return this;
        }
    }
}
