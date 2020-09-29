package com.myfiziq.sdk.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.BaseActivity;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.adapters.RecyclerManagerInterface;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class StyledRecyclerView extends RecyclerView implements RecyclerManagerInterface
{
    RecyclerManager mManager;
    Class<? extends Model> mModelClass;
    Class<? extends ViewGroup> mViewClass;
    String mSqlQuery;
    String mSqlWhere;
    String mSqlOrder;
    LayoutStyle mLayout = LayoutStyle.VERTICAL;
    List<OnClickListener> mListeners;
    WeakReference<Fragment> mFragment;

    public StyledRecyclerView(@NonNull Context context)
    {
        super(context);
        init(context, null);
    }

    public StyledRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public StyledRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void init(Context context, @Nullable AttributeSet attrs)
    {
        TypedArray a = null;

        if (null != attrs)
        {
            a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.StyledRecyclerView, 0, 0);

            try
            {
                String modelClass = a.getString(R.styleable.StyledRecyclerView_modelClass);
                String viewClass = a.getString(R.styleable.StyledRecyclerView_viewClass);
                mSqlQuery = a.getString(R.styleable.StyledRecyclerView_sql_query);
                mSqlWhere = a.getString(R.styleable.StyledRecyclerView_sql_where);
                mSqlOrder = a.getString(R.styleable.StyledRecyclerView_sql_order);
                mLayout = LayoutStyle.values()[a.getInt(R.styleable.StyledRecyclerView_itemlayout, LayoutStyle.VERTICAL.ordinal())];

                mModelClass = (Class<? extends Model>) Class.forName(modelClass);
                mViewClass = (Class<? extends ViewGroup>) Class.forName(viewClass);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                a.recycle();
            }
        }
    }

    private Activity getActivity()
    {
        Context context = getContext();
        while (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public void setActivity(BaseActivity activity)
    {
        if (activity instanceof RecyclerManagerInterface)
        {
            setItemSelectListeners(((RecyclerManagerInterface) activity).getItemSelectListeners());
        }
        setup(activity, LoaderManager.getInstance(activity));
    }

    public void setFragment(Fragment fragment)
    {
        mFragment = new WeakReference<>(fragment);
        if (fragment instanceof RecyclerManagerInterface)
        {
            setItemSelectListeners(((RecyclerManagerInterface) fragment).getItemSelectListeners());
        }
        setup(fragment.getActivity(), LoaderManager.getInstance(fragment));
    }

    private void setup(Activity activity, LoaderManager loaderManager)
    {
        if (null != mModelClass && null != mViewClass)
        {
            try
            {
                FragmentInterface fragmentInterface = null;

                mManager = new RecyclerManager(activity, loaderManager, this);

                if (null != mFragment && mFragment.get() instanceof FragmentInterface)
                {
                    fragmentInterface = (FragmentInterface) mFragment.get();
                }

                if (TextUtils.isEmpty(mSqlOrder))
                {
                    Method getOrderBy = mModelClass.getMethod("getOrderBy", int.class);
                    mSqlOrder = (String) getOrderBy.invoke(null, 0);
                }

                mManager.setupRecycler(
                        fragmentInterface,
                        this,
                        new ParameterSet.Builder()
                                .setLayout(mLayout)
                                .addParam(new Parameter.Builder()
                                        .setModel(mModelClass)
                                        .setView(mViewClass)
                                        .setWhere(mSqlWhere)
                                        .setOrder(mSqlOrder)
                                        .build())
                                .build()
                );
            }
            catch (Throwable t)
            {
                Timber.e(t);
            }
        }
    }

    @Override
    public List<OnClickListener> getItemSelectListeners()
    {
        return mListeners;
    }

    public void setItemSelectListeners(List<OnClickListener> listeners)
    {
        mListeners = listeners;
    }
}
