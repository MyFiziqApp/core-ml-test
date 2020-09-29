package com.myfiziq.sdk.adapters;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;

import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A <code>RecyclerView.Adapter</code> that handles <code>RecyclerView</code> items from a
 * <code>Cursor</code>.
 * <br>
 *
 */
public class RecyclerCursorAdapter extends RecyclerView.Adapter
{
    // Allow for maximum groups.
    // Keeping in mind that groups may be include R ids
    // which have a max range of Integer.MAX_VALUE
    // Also model PKs which should be in the lower range.
    private static final long GROUP_SIZE = Long.MAX_VALUE/ Integer.MAX_VALUE;

    private final List<CursorHolder> mHolders;
    private FragmentInterface mFragment;

    // Adapter listeners.
    private final List<View.OnClickListener> mClickListeners;
    private AdapterChangedListener mListener = null;
    private LayoutAnimationController mAnimationController;

    public RecyclerCursorAdapter(FragmentInterface fragment,
                                 List<View.OnClickListener> listeners)
    {
        this(fragment, null, listeners);
    }

    public RecyclerCursorAdapter(FragmentInterface fragment,
                                 List<CursorHolder> holders,
                                 List<View.OnClickListener> listeners)
    {
        mFragment = fragment;
        mHolders = holders;
        mClickListeners = listeners;

        // Performance optimisation. Requires that getItemId is overridden.
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        RecyclerView.ViewHolder viewHolder = null;

        for (CursorHolder holder : mHolders)
        {
            viewHolder = holder.getView(viewType, parent, mFragment, mClickListeners);
            if (null != viewHolder)
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        CursorHolder cursorHolder = getHolderForPosition(position);
        if (null != cursorHolder)
        {
            cursorHolder.onBindViewHolder(holder, getHolderPosition(position));
        }
    }

    public <T extends Model> boolean isSelected(T model)
    {
        for (CursorHolder holder : mHolders)
        {
            if (holder.getModelClass().equals(model.getClass()))
            {
                if (holder.isSelected(model))
                    return true;
            }
        }

        return false;
    }

    public <T extends Model> void addSelection(T model)
    {
        for (CursorHolder holder : mHolders)
        {
            if (model!=null && Orm.isInstance(holder.getModelClass(), model.getClass()))
            {
                holder.addSelection(model);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void clearSelection()
    {
        for (CursorHolder holder : mHolders)
        {
            holder.clearSelection();
        }
    }

    public int getFirstSelectedPosition()
    {
        for (CursorHolder holder : mHolders)
        {
            int position = holder.getFirstSelectedPosition();
            if (position >= 0)
                return position;
        }

        return -1;
    }

    public Model getFirstSelected()
    {
        for (CursorHolder holder : mHolders)
        {
            Model m = holder.getFirstSelected();
            if (null != m)
                return m;
        }

        return null;
    }

    public List<Model> getSelected()
    {
        ArrayList<Model> modelList = new ArrayList<>();

        for (CursorHolder holder : mHolders)
        {
            holder.getSelected(modelList);
        }
        return modelList;
    }

    @Override
    public long getItemId(int position)
    {
        return getHolderStableId(position);
    }

    private CursorHolder getHolderForPosition(int position)
    {
        int offset = 0;

        for (CursorHolder holder : mHolders)
        {
            offset += holder.getItemCount();
            if (offset > position)
            {
                return holder;
            }
        }

        return null;
    }

    /**
     * Gets the corrected position used for the cursor which subtracts the header size.
     */
    private int getHolderPosition(int position)
    {
        int offset = 0;

        for (CursorHolder holder : mHolders)
        {
            int count = holder.getItemCount();

            if (count+offset > position)
            {
                return position - offset;
            }

            offset += count;
        }

        return 0;
    }

    private long getHolderPk(int position)
    {
        int offset = 0;

        for (CursorHolder holder : mHolders)
        {
            int count = holder.getItemCount();

            if (count+offset > position)
            {
                return holder.getPk(position - offset);
            }

            offset += count;
        }

        return position;
    }

    private long getHolderStableId(int position)
    {
        int offset = 0;
        long ix = 0;

        for (CursorHolder holder : mHolders)
        {
            int count = holder.getItemCount();

            if (count+offset > position)
            {
                return holder.getStableId(ix*GROUP_SIZE, position - offset);
            }

            offset += count;
            ix++;
        }

        return position;
    }

    public void setListener(AdapterChangedListener listener)
    {
        mListener = listener;
    }

    public LayoutAnimationController getAnimationController()
    {
        return mAnimationController;
    }

    public void setAnimationController(LayoutAnimationController animationController)
    {
        mAnimationController = animationController;
    }

    @Override
    public int getItemCount()
    {
        int count = 0;

        for (CursorHolder holder : mHolders)
        {
            count += holder.getItemCount();
        }

        return count;
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder)
    {
        return true;
    }

    @Override
    public int getItemViewType(int position)
    {
        int offset = 0;

        for (CursorHolder holder : mHolders)
        {
            int count = holder.getItemCount();

            if (count+offset > position)
            {
                return holder.getItemViewType(position - offset);
            }

            offset += count;
        }

        return -1;
    }

    /**
     * Calls #notifyDataSetChanged and notifies any listeners that the adapter content has changed.
     */
    public void notifyContentChanged()
    {
        notifyDataSetChanged();

        if (null != mListener)
        {
            //TODO: mListener.onAdapterUpdated(mCursor);
        }
    }

    public interface AdapterChangedListener
    {
        void onAdapterUpdated(Cursor mCursor);
    }
}


