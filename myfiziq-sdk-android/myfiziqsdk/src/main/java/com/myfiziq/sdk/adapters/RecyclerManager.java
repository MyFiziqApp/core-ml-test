package com.myfiziq.sdk.adapters;

import android.content.Context;
import android.view.View;
import android.view.animation.LayoutAnimationController;

import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Keep;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A manager for handling <code>RecyclerView</code> setup based on a <code>ParameterSet</code>
 */
@Keep
public class RecyclerManager extends MyFiziqLoaderManager implements HolderViewBinder
{
    //ArrayList<CursorHolder> mHolders = new ArrayList<>();
    private WeakReference<RecyclerManagerInterface> mManagerInterface;

    /**
     * Create a new manager for an Activity/Fragment instance.
     * @param context
     * @param loader - The MyFiziqLoaderManager from the parent Activity or Fragment.
     * @param managerInterface
     */
    public RecyclerManager(Context context, LoaderManager loader, RecyclerManagerInterface managerInterface)
    {
        super(context, loader);
        mManagerInterface = new WeakReference<>(managerInterface);
    }

    private void updateRecyclerView(
            FragmentInterface fragmentInterface,
            RecyclerView recyclerView,
            LayoutStyle layoutStyle,
            ArrayList<CursorHolder> holders,
            List<View.OnClickListener> listeners)
    {
        if (recyclerView.getAdapter() == null)
        {
            RecyclerCursorAdapter adapter = new RecyclerCursorAdapter(fragmentInterface, holders, listeners);
            if (this instanceof RecyclerCursorAdapter.AdapterChangedListener)
            {
                adapter.setListener((RecyclerCursorAdapter.AdapterChangedListener) this);
            }
            recyclerView.setAdapter(adapter);
        }

        // Only create a layout manager if we need to.
        if (recyclerView.getLayoutManager() == null)
        {
            setRecyclerLayout(recyclerView, layoutStyle);
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void setRecyclerLayout(
            RecyclerView recyclerView,
            LayoutStyle layout)
    {
        Context context = recyclerView.getContext();

        switch (layout)
        {
            case HORIZONTAL:
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(context,
                        LinearLayoutManager.HORIZONTAL, false));
                break;

            case VERTICAL:
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(context,
                        RecyclerView.VERTICAL, false));
                break;

            case GRID_2:
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setLayoutManager(new GridLayoutManager(context,
                        layout.mPhoneColumns));
                break;

            case GRID_3:
                recyclerView.setNestedScrollingEnabled(true);
                GridLayoutManager glm = new GridLayoutManager(context,
                        layout.mPhoneColumns);
                glm.setInitialPrefetchItemCount(3);
                glm.setItemPrefetchEnabled(true);
                recyclerView.setLayoutManager(glm);
                break;

            default:
                break;
        }
    }

    /**
     * Sets up the <code>RecyclerView</code> based on the <code>ParameterSet</code>
     * Once it has been set up - The model load(s) and view rendering will begin.
     * @param recyclerView - The <code>RecyclerView</code>
     * @param set - The parameters to use for setting up the recycler...
     *            The <code>CursorArgs</code>, cursor loader id & layout style.
     * @see ParameterSet
     * @see Parameter
     */
    public void setupRecycler(RecyclerView recyclerView, ParameterSet set)
    {
        setupRecycler(null, recyclerView, set);
    }

    public void setWhere(int loaderId, String where)
    {
        CursorHolder holder = mLoaderMap.get(Integer.valueOf(loaderId));
        if (null != holder)
        {
            holder.setWhere(where);
            if (null != mLoader.get())
            {
                mLoader.get().restartLoader(loaderId, null, this);
            }
        }
    }

    public void setupRecycler(FragmentInterface fragmentInterface, RecyclerView recyclerView, ParameterSet set)
    {
        ArrayList<CursorHolder> holders = new ArrayList<>();
        for (Parameter parameter : set.getParameters())
        {
            if (this instanceof HolderViewBinder)
            {
                parameter.initHolder((HolderViewBinder) this);
            }

            //parameter.mHolder.setListener(mCursorHolderListener);
            parameter.getHolder().setRecycler(recyclerView);
            holders.add(parameter.getHolder());

            if (null != parameter.getHolder().getUri())
            {
                loadCursor(parameter.getHolder());
            }
        }

        // Assigns the adapter and layout manager.
        List<View.OnClickListener> onClickListeners = null;
        RecyclerManagerInterface managerInterface = mManagerInterface.get();
        if (null != managerInterface)
        {
            onClickListeners = managerInterface.getItemSelectListeners();
        }
        updateRecyclerView(fragmentInterface, recyclerView, set.getLayoutStyle(), holders, onClickListeners);
    }

    @Override
    public void bind(int holderId, int id, int position, View view)
    {

    }
}
