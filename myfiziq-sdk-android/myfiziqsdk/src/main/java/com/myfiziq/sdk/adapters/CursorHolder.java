package com.myfiziq.sdk.adapters;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;

import com.myfiziq.sdk.db.Cached;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMDbCache;
import com.myfiziq.sdk.db.ORMDbHelper;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.util.UiUtils;

import org.sqlite.database.sqlite.SQLiteCursor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

/**
 * A <code>CursorHolder</code> is a container for a <code>Model</code> and <code>View</code> set
 * linked to a <code>Cursor</code>.
 */
public class CursorHolder implements Parcelable, Filterable, CursorFilter.CursorFilterClient
{
    private int mLoaderId;
    private CursorArgs mCursorArgs;

    private Class<? extends Model> mModelClass;
    private Class<? extends ViewGroup> mViewClass;
    private ArrayList<WeakReference<CursorChangedListener>> mCursorChangedListeners = new ArrayList<>();
    private RecyclerView mRecycler;
    private Cursor mCursor;

    // Adapter filtering.
    private CursorFilter mCursorFilter;
    private FilterQueryProvider mFilterQueryProvider;

    private List<HeaderHolder> mHeaders;

    // Selection.
    private ArrayList<String> mSelectedItems = new ArrayList<>();
    private boolean mMultiSelect = false;

    boolean mModelCached = false;
    boolean mVisible = true;

    int mPrimaryKeyIndex = -1;
    int mTimestampIndex = -1;
    int mIdIndex = -1;

    Thread mPreloadThread = null;

    //Handler mHandler = new Handler(Looper.getMainLooper());

    public interface Observer
    {
        void onChange(boolean selfChange);
    }

    /**
     * Creates a <code>CursorHolder</code>.
     * @param id - The cursor loader id.
     * @param uri - The <code>ORMContentProvider</code> uri
     * @param where - The where clause.
     * @param order - The order clause.
     * @param depth - The <code>Model</code> load depth.
     * @param modelClass - The class type for the <code>Model</code>.
     * @param viewClass - The class type for the <code></code>
     * @param cursorChangedListener - A listener to be notified of events.
     */
    public CursorHolder(int id, Uri uri, String where, String order, int depth, Class<? extends Model> modelClass, Class<? extends ViewGroup> viewClass, CursorChangedListener cursorChangedListener)
    {
        mLoaderId = id;
        mCursorArgs = new CursorArgs(uri, where, order, depth);
        mModelClass = modelClass;
        mViewClass = viewClass;
        mCursorChangedListeners.add(new WeakReference<>(cursorChangedListener));
        init();
    }

    /*
    public CursorHolder(int id, CursorArgs args, Class<? extends Model> modelClass, Class<? extends ViewGroup> viewClass, CursorChangedListener cursorChangedListener)
    {
        mLoaderId = id;
        mCursorArgs = new CursorArgs(args.mUri, args.mWhere, args.mOrder, args.mDepth);
        mModelClass = modelClass;
        mViewClass = viewClass;
        mCursorChangedListeners.add(new WeakReference<>(cursorChangedListener));
        init();
    }

    public CursorHolder(Cursor cursor, Class<? extends Model> modelClass, Class<? extends ViewGroup> viewClass, CursorChangedListener cursorChangedListener)
    {
        mLoaderId = -1;
        mCursorArgs = null;
        mCursor = cursor;
        mModelClass = modelClass;
        mViewClass = viewClass;
        mCursorChangedListeners.add(new WeakReference<>(cursorChangedListener));
        init();
    }
    */

    private CursorHolder(Parcel in) throws ClassNotFoundException
    {
        mLoaderId = in.readInt();
        String modelClassName = in.readString();
        if (!TextUtils.isEmpty(modelClassName))
        {
            mModelClass = (Class<? extends Model>) Class.forName(modelClassName);
        }
        else
        {
            mModelClass = null;
        }
        String viewClassName = in.readString();
        if (!TextUtils.isEmpty(viewClassName))
        {
            mViewClass = (Class<? extends ViewGroup>) Class.forName(viewClassName);
        }
        else
        {
            mViewClass = null;
        }
        mCursorArgs = in.readParcelable(CursorArgs.class.getClassLoader());
        init();
    }

    void setRecycler(RecyclerView recycler)
    {
        mRecycler = recycler;
    }

    /**
     * Changes the visibility of the items for this <code>CursorHolder</code>
     * @param bVisible - true if the items are to be visible.
     * @param bNotify - If set to true a <code>notifyDataSetChanged</code> call will be made.
     */
    public void setVisible(boolean bVisible, boolean bNotify)
    {
        boolean bChanged = (mVisible != bVisible);
        mVisible = bVisible;
        if (bChanged && bNotify)
        {
            notifyDataSetChanged();
        }
    }

    private void init()
    {
        if (null != mModelClass)
        {
            Cached cached = mModelClass.getAnnotation(Cached.class);
            if (null != cached && cached.cached())
            {
                mModelCached = true;
            }
        }
    }

    /**
     * Adds a listener for <code>CursorHolder</code> events.
     * @param listener - The listener.
     */
    public void addListener(CursorChangedListener listener)
    {
        mCursorChangedListeners.add(new WeakReference<>(listener));
    }

    /**
     * Removes a listener for <code>CursorHolder</code> events.
     * @param listener - The listener.
     */
    public void remListener(CursorChangedListener listener)
    {
        Iterator<WeakReference<CursorChangedListener>> iter = mCursorChangedListeners.iterator();
        while (iter.hasNext())
        {
            WeakReference<CursorChangedListener> listenerWeakReference = iter.next();
            CursorChangedListener item = listenerWeakReference.get();
            if (item == null || item == listener)
            {
                iter.remove();
            }
        }
    }

    public Class<? extends Model> getModelClass()
    {
        return mModelClass;
    }

    Class<? extends ViewGroup> getViewClass()
    {
        return mViewClass;
    }

    /**
     * Gets the current <code>Cursor</code> for this holder.
     * @return - The current <code>Cursor</code>.
     */
    public Cursor getCursor()
    {
        return mCursor;
    }

    /**
     * Sets the <code>Cursor</code> for this holder.
     * <br>
     * Issues a <code>notifyDataSetChanged</code>.
     * <br>
     * Handles pre-loading.
     */
    public synchronized void setCursor(Cursor cursor)
    {
        boolean cursorChanged = mCursor != cursor;

        // Stop preloading if preloader is running.

        if (mModelCached)
        {
            if (null != mPreloadThread)
            {
                mPreloadThread.interrupt();
                mPreloadThread = null;
            }
        }

        mCursor = cursor;
        ORMDbHelper.updateSQLiteCursor(mCursor, mModelClass);

        if (cursorChanged)
        {
            if (isCursorFilled())
            {
                if (mModelCached)
                {
                    /*
                    mPreloadThread = new Thread(() ->
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

                        int count = 0;
                        if (isCursorFilled())
                        {
                            count = mCursor.getCount();
                        }

                        for (int i = 0; i < count; i++)
                        {
                            if (!isCursorFilled())
                                break;

                            getItem(i);

                            try
                            {
                                Thread.sleep(1);
                            }
                            catch (InterruptedException e)
                            {
                                break;
                            }
                        }
                    });
                    mPreloadThread.setPriority(Thread.MIN_PRIORITY);
                    mPreloadThread.start();
                    */
                }
            }

            notifyListeners();

            RecyclerView.Adapter cursorAdapter = getCursorAdapter();

            if (mRecycler != null && cursorAdapter instanceof RecyclerCursorAdapter)
            {
                RecyclerCursorAdapter recyclerCursorAdapter = (RecyclerCursorAdapter) cursorAdapter;
                LayoutAnimationController animationController = recyclerCursorAdapter.getAnimationController();

                if (animationController != null)
                {
                    mRecycler.setLayoutAnimation(animationController);
                    notifyDataSetChanged();
                    mRecycler.scheduleLayoutAnimation();
                }
                else
                {
                    // Animation controller has been removed. Probably because we don't need to animate
                    // the current view update or that it's already been rendered.
                    notifyDataSetChanged();
                }
            }
            else
            {
                notifyDataSetChanged();
            }
        }
    }
	
	private void notifyListeners()
    {
        for (WeakReference<CursorChangedListener> listenerWeakReference : mCursorChangedListeners)
        {
            CursorChangedListener listener = listenerWeakReference.get();
            if (null != listener)
            {
                UiUtils.getHandler().post(()-> listener.onCursorChanged(this));
            }
        }
    }

    public synchronized Model getItem(int position)
    {
        Model model = null;

        if (isCursorFilled())
        {
            SQLiteCursor cursor = getSQLiteCursor();
            if (null != cursor)
            {
                model = cursor.getModel(mModelClass, position);
                return model;
            }

            // Since the model doesn't exist, create it now.
            if (model == null)
            {
                model = newModelInstance();
                if (model != null)
                {
                    try
                    {
                        mCursor.moveToPosition(position);
                        model.readFromCursor(mCursor, null, mCursorArgs.mDepth, mCursorArgs.mDepth);
                        if (mModelCached)
                        {
                            ORMDbCache.getInstance().putModel(mModelClass, model, position);
                        }
                    }
                    catch (Exception e)
                    {
                        //Timber.e(e, "");
                    }
                }
            }
        }
        return model;
    }

    private synchronized Model getItem(String id)
    {
        Model model = null;

        if (isCursorFilled())
        {
            mCursor.moveToFirst();
            do
            {
                String modelId = getId();

                if (null != modelId && modelId.contentEquals(id))
                {
                    SQLiteCursor cursor = getSQLiteCursor();
                    if (null != cursor)
                    {
                        model = cursor.getModel(mModelClass);
                    }

                    // Since the model doesn't exist, create it now.
                    if (model == null)
                    {
                        model = newModelInstance();
                        if (model != null)
                        {
                            try
                            {
                                model.readFromCursor(mCursor, null, mCursorArgs.mDepth, mCursorArgs.mDepth);
                                if (mModelCached)
                                {
                                    //TODO: handle this...
                                    ORMDbCache.getInstance().updateModel(mModelClass, model);
                                }
                            }
                            catch (Exception e)
                            {
                                //Timber.e(e, "");
                            }
                        }
                    }

                    break;
                }
            }
            while (mCursor.moveToNext());
        }

        return model;
    }

    private synchronized int getItemPosition(String id)
    {
        if (isCursorFilled())
        {
            mCursor.moveToFirst();
            do
            {
                String modelId = getId();

                if (null != modelId && modelId.contentEquals(id))
                {
                    return mCursor.getPosition();
                }
            }
            while (mCursor.moveToNext());
        }

        return -1;
    }

    @Override
    public Filter getFilter()
    {
        if (mCursorFilter == null)
        {
            mCursorFilter = new CursorFilter(this);
        }

        return mCursorFilter;
    }

    /**
     * Changes the filtering for this <code>CursorHolder</code>.
     * @param filter - The new filter text.
     */
    public void changeFilter(String filter)
    {
        getFilter().filter(filter);
    }

    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        return cursor == null ? "" : cursor.toString();
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint)
    {
        Cursor cursor = mCursor;

        if (mFilterQueryProvider != null)
        {
            cursor = mFilterQueryProvider.runQuery(constraint);
            //changeCursor(cursor);
        }

        return cursor;
    }

    /**
     * Sets a <code>FilterQueryProvider</code>.
     * @param filterQueryProvider - The new <code>FilterQueryProvider</code>.
     */
    public void setFilterQueryProvider(FilterQueryProvider filterQueryProvider)
    {
        mFilterQueryProvider = filterQueryProvider;
    }

    @Override
    public void changeCursor(Cursor cursor)
    {
        if (cursor == mCursor)
        {
            //Timber.d("changeCursor. Cursor has not changed.");
            return;
        }

        if (isCursorFilled())
        {
            mPrimaryKeyIndex = cursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            mTimestampIndex = cursor.getColumnIndex(Model.COLUMN_TIMESTAMP);
        }

        mCursor = cursor;

        notifyListeners();
        notifyDataSetChanged();
    }

    /**
     * Checks whether the cursor is open.
     */
    public boolean isCursorOpen()
    {
        return mCursor != null && !mCursor.isClosed();
    }

    /**
     * Checks whether the cursor is open and has at least 1 row.
     */
    public boolean isCursorFilled()
    {
        return isCursorOpen() && mCursor.getCount() > 0;
    }

    /**
     * Gets the model's timestamp (last modified time).
     *
     * @return The model's timestamp or -1 on error
     */
    public synchronized long getTs()
    {
        if (isCursorFilled())
        {
            if (mTimestampIndex < 0)
            {
                mTimestampIndex = mCursor.getColumnIndex(Model.COLUMN_TIMESTAMP);
            }

            if (mTimestampIndex >= 0)
            {
                return mCursor.getLong(mTimestampIndex);
            }
        }
        return -1;
    }

    /**
     * Gets the mode's ID field value.
     *
     * @return ID value or NULL on error.
     */
    public synchronized String getId()
    {
        if (isCursorFilled())
        {
            if (mIdIndex < 0)
            {
                mIdIndex = mCursor.getColumnIndex(Model.COLUMN_ID);
            }

            if (mIdIndex >= 0)
            {
                return mCursor.getString(mIdIndex);
            }
        }
        return null;
    }

    /**
     * Gets the model's timestamp (last modified time) for a specific cursor row.
     *
     * @return The model's timestamp or -1 on error
     */
    public synchronized long getTs(int position)
    {
        if (isCursorFilled())
        {
            if (mTimestampIndex < 0)
            {
                mTimestampIndex = mCursor.getColumnIndex(Model.COLUMN_TIMESTAMP);
            }

            if (mTimestampIndex >= 0)
            {
                if (mCursor.moveToPosition(getCursorPosition(position)))
                    return mCursor.getLong(mTimestampIndex);
            }
        }
        return -1;
    }

    /**
     * Gets the model's pk (primary key).
     *
     * @return The model's pk or -1 on error
     */
    public synchronized long getPk()
    {
        if (isCursorFilled())
        {
            if (mPrimaryKeyIndex < 0)
            {
                mPrimaryKeyIndex = mCursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            }

            if (mPrimaryKeyIndex >= 0)
            {
                return mCursor.getLong(mPrimaryKeyIndex);
            }
        }
        return -1;
    }

    private SQLiteCursor getSQLiteCursor()
    {
        if (mCursor instanceof CursorWrapper)
        {
            Cursor cursor = ((CursorWrapper)mCursor).getWrappedCursor();
            if (cursor instanceof SQLiteCursor)
            {
                return (SQLiteCursor)cursor;
            }
        }

        return null;
    }

    /**
     * Gets the model's 'stable' id.
     *
     * @return The model's 'stable' id or -1 on error
     */
    public synchronized long getStableId(long group, int position)
    {
        if (isHeader(position))
        {
            return group + mHeaders.get(position).mId;
        }
        else
        {
            int offsetPosition = getCursorPosition(position);

            if (isCursorFilled())
            {
                SQLiteCursor cursor = getSQLiteCursor();
                if (null != cursor)
                {
                    Model model = cursor.getModel(mModelClass, offsetPosition);
                    return group + model.pk;
                }

                if (mPrimaryKeyIndex < 0)
                {
                    mPrimaryKeyIndex = mCursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
                }

                if (mPrimaryKeyIndex >= 0)
                {
                    if (mCursor.moveToPosition(offsetPosition))
                        return group + mCursor.getLong(mPrimaryKeyIndex);
                }
            }
        }

        return -1;
    }

    /**
     * Gets the model's pk (primary key) for a specific cursor row.
     *
     * @return The model's pk or -1 on error
     */
    public synchronized long getPk(int position)
    {
        if (isHeader(position))
        {
            return mHeaders.get(position).mId;
        }
        else
        {
            if (isCursorFilled())
            {
                int offsetPosition = getCursorPosition(position);

                SQLiteCursor cursor = getSQLiteCursor();
                if (null != cursor)
                {
                    Model model = cursor.getModel(mModelClass, offsetPosition);
                    return model.pk;
                }

                if (mPrimaryKeyIndex < 0)
                {
                    mPrimaryKeyIndex = mCursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
                }

                if (mPrimaryKeyIndex >= 0)
                {
                    if (mCursor.moveToPosition(offsetPosition))
                        return mCursor.getLong(mPrimaryKeyIndex);
                }
            }
        }
        return -1;
    }

    /**
     * Gets the corrected position used for the cursor which subtracts the header size.
     */
    private int getCursorPosition(int position)
    {
        int offset = 0;
        if (null != mHeaders)
        {
            offset = mHeaders.size();
        }
        return position - offset;
    }

    /**
     * Gets the number or rows (including header rows).
     */
    public int getItemCount()
    {
        int count = 0;

        if (mVisible)
        {
            if (null != mHeaders)
            {
                count += mHeaders.size();
            }

            if (isCursorFilled())
            {
                count += mCursor.getCount();
            }
        }

        return count;
    }

    int getItemViewType(int position)
    {
        if (isHeader(position))
        {
            return mHeaders.get(position).mId;
        }
        else
        {
            return mLoaderId;
        }
    }

    void notifyDataSetChanged()
    {
        try
        {
            RecyclerView.Adapter cursorAdapter = getCursorAdapter();
            if (cursorAdapter == null)
            {
                return;
            }

            if (cursorAdapter instanceof RecyclerCursorAdapter)
            {
                ((RecyclerCursorAdapter) cursorAdapter).notifyContentChanged();
            }
            else
            {
                cursorAdapter.notifyDataSetChanged();
            }
        }
        catch (Exception e)
        {
            //Timber.e(e, "");
        }
    }

    private RecyclerView.Adapter getCursorAdapter()
    {
        if (null != mRecycler)
        {
            return mRecycler.getAdapter();
        }

        return null;
    }

    RecyclerView.ViewHolder getView(int viewType, ViewGroup parent, FragmentInterface fragment, List<View.OnClickListener> listeners)
    {
        if (viewType == mLoaderId && null != mViewClass && null != mModelClass)
        {
            return new ModelViewHolder<>(fragment, newViewInstance(parent.getContext()), listeners);
        }
        else if (null != mHeaders)
        {
            for (HeaderHolder header : mHeaders)
            {
                if (header.mId == viewType)
                {
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
                }
            }
        }

        return null;
    }

    /**
     * Adds a header item.
     * @param id
     * @param binder
     * @return <code>HeaderHolder</code>
     */
    public HeaderHolder addHeader(int id, HolderViewBinder binder)
    {
        if (null == mHeaders)
        {
            mHeaders = new ArrayList<>();
        }

        HeaderHolder holder = new HeaderHolder(id, binder);
        mHeaders.add(holder);

        //TODO:? notifyContentChanged();

        return holder;
    }

    private boolean isHeader(int position)
    {
        return (null != mHeaders) && (position < mHeaders.size());
    }

    void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        Holder baseHolder = (Holder) holder;
        switch (baseHolder.mType)
        {
            case HEADER:
            {
                HeaderHolder header = mHeaders.get(position);
                header.mBinder.bind(mLoaderId, header.mId, position, holder.itemView);
            }
            break;

            case MODEL:
            {
                ModelViewHolder modelViewHolder = (ModelViewHolder) holder;
                //Cursor cursor = getCursor();
                Model model = null;

                if (isCursorFilled())
                {
                    int cursorPosition = getCursorPosition(position);
                    model = getItem(cursorPosition);
                }
                else
                {
                    // If this occurs, ensure cursors are not being closed incorrectly.
                    //Timber.e("Cursor is unexpectedly closed or empty!");
                }

                if (model != null)
                {
                    try
                    {
                        modelViewHolder.bind(model, this);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        //Timber.e(e, "");
                    }
                }
            }
            break;
        }
    }

    int getHeaderCount()
    {
        if (null != mHeaders)
            return mHeaders.size();

        return 0;
    }

    /**
     * Gets the multi-selection enabled state.
     * @return true if multi-select is enabled.
     */
    public boolean isMultiSelect()
    {
        return mMultiSelect;
    }

    /**
     * Sets the multi-selection enabled state.
     */
    public void setMultiSelect(boolean bMulti)
    {
        mMultiSelect = bMulti;
    }

    /**
     * Selects a single item.
     * @param model - The item to select.
     */
    public synchronized <T extends Model> void setSelection(T model)
    {
        if (!mMultiSelect)
        {
            mSelectedItems.clear();
        }

        if (null != model)
        {
            mSelectedItems.add(model.id);
        }

        notifyDataSetChanged();
    }

    /**
     * Selects multiple items.
     * @param list - The items to select.
     */
    public void setSelections(ArrayList<? extends Model> list)
    {
        if (null != list)
        {
            for (Model m : list)
            {
                mSelectedItems.add(m.id);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Sets an items as being selected.
     * @param model - The item to set as selected.
     */
    public synchronized <T extends Model> void addSelection(T model)
    {
        if (!mMultiSelect)
        {
            mSelectedItems.clear();
        }

        if (null != model)
        {
            mSelectedItems.add(model.id);
        }
        notifyDataSetChanged();
    }

    /**
     * Un-select an item.
     * @param model - The item to un-select.
     * @return - true if the selection succeeded.
     */
    public synchronized <T extends Model> boolean remSelection(T model)
    {
        if (null != model)
        {
            Iterator<String> iterator = mSelectedItems.iterator();
            while (iterator.hasNext())
            {
                String id = iterator.next();
                if (model.getId().contentEquals(id))
                {
                    iterator.remove();
                    notifyDataSetChanged();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear all selections.
     */
    public void clearSelection()
    {
        mSelectedItems.clear();
    }

    /**
     * Checks if an item is selected.
     * @param model - The item to check for selection.
     * @return - true if the item is selected.
     */
    public synchronized <T extends Model> boolean isSelected(T model)
    {
        if (null != model)
        {
            for (String id : mSelectedItems)
            {
                if (model.getId().contentEquals(id))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns a list of items that are currently selected.
     * @return - The list of currently selected items.
     */
    public ArrayList<Model> getSelections()
    {
        ArrayList<Model> result = new ArrayList<>();

        for (String id : mSelectedItems)
        {
            Model m = getItem(id);
            if (null != m)
            {
                result.add(m);
            }
        }

        return result;
    }

    /**
     * Returns a list of items that are currently selected (as the passed type).
     * @param clazz - The type to use for the returned list.
     * @return - The list of currently selected items.
     */
    public <T extends Model> ArrayList<T> getTypeSelections(Class<T> clazz)
    {
        ArrayList<T> result = new ArrayList<>();

        for (String id : mSelectedItems)
        {
            T m = (T) getItem(id);
            if (null != m)
            {
                result.add(m);
            }
        }

        return result;
    }

    /**
     * Returns the id of the first item that is selected.
     * @return the id of the first item that is selected or null.
     */
    public String getFirstSelectedId()
    {
        int size = mSelectedItems.size();
        if (size > 0)
        {
            return mSelectedItems.get(0);
        }

        return null;
    }

    /**
     * Returns the first item that is selected.
     * @return the first item that is selected or null.
     */
    public Model getFirstSelected()
    {
        int size = mSelectedItems.size();
        if (size > 0)
        {
            Model m = getItem(mSelectedItems.get(0));

            if (null != m)
            {
                return m;
            }
        }

        return null;
    }

    /**
     * Returns the position (cursor row) of the first item that is selected.
     */
    public int getFirstSelectedPosition()
    {
        int size = mSelectedItems.size();
        if (size > 0)
        {
            return getItemPosition(mSelectedItems.get(0));
        }

        return -1;
    }

    /**
     * Returns a list of items that are currently selected.
     * @param modelList - The list to store the selections in.
     */
    public void getSelected(List<Model> modelList)
    {
        for (String id : mSelectedItems)
        {
            Model m = getItem(id);
            if (null != m)
            {
                modelList.add(m);
            }
        }
    }

    /**
     * Sets the <code>CursorLoader</code> id.
     * @param id - The id to use.
     */
    public void setLoaderId(int id)
    {
        mLoaderId = id;
    }

    /**
     * Gets the <code>CursorLoader</code> id.
     * @return id.
     */
    public int getLoaderId()
    {
        return mLoaderId;
    }

    /**
     * Sets the <code>Model</code> class type.
     * @param clazz - The <code>Model</code> class type.
     */
    public void setModelClass(Class<? extends Model> clazz)
    {
        mModelClass = clazz;
        init();
    }

    CursorArgs getCursorArgs()
    {
        return mCursorArgs;
    }

    /**
     * Sets the <code>View</code> class type.
     * @param clazz - The <code>View</code> class type.
     */
    public void setViewClass(Class<? extends ViewGroup> clazz)
    {
        mViewClass = clazz;
    }

    /**
     * Sets the Uri.
     * @param uri - The Uri.
     */
    public void setUri(Uri uri)
    {
        if (null != mCursorArgs)
        {
            mCursorArgs.mUri = uri;
        }
    }

    /**
     * Gets the Uri.
     * @return The Uri.
     */
    public Uri getUri()
    {
        if (null != mCursorArgs)
        {
            return mCursorArgs.mUri;
        }

        return null;
    }

    public void addWhere(String key, String value)
    {
        if (null != mCursorArgs)
        {
            mCursorArgs.addWhere(key, value);
        }
    }

    public void remWhere(String key)
    {
        if (null != mCursorArgs)
        {
            mCursorArgs.remWhere(key);
        }
    }

    public void setWhere(String where)
    {
        if (null != mCursorArgs)
        {
            mCursorArgs.mWhere = where;
        }
    }

    public String getWhere()
    {
        if (null != mCursorArgs)
        {
            return mCursorArgs.mWhere;
        }

        return null;
    }

    public void setOrder(String order)
    {
        if (null != mCursorArgs)
        {
            mCursorArgs.mOrder = order;
        }
    }

    public String getOrder()
    {
        if (null != mCursorArgs)
        {
            return mCursorArgs.mOrder;
        }

        return null;
    }

    /**
     * Defines an interface for notifying when the cursor is changed.
     */
    public interface CursorChangedListener
    {
        void onCursorChanged(CursorHolder cursorHolder);
    }

    ViewGroup newViewInstance(Context context)
    {
        try
        {
            return mViewClass.getDeclaredConstructor(Context.class).newInstance(context);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return null;
    }

    Model newModelInstance()
    {
        try
        {
            return Orm.newModel(mModelClass);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return null;
    }

    public Cursor runQuery(Observer observer)
    {
        if (null != mCursor)
        {
            mCursor.close();
        }
        Cursor cursor = ORMTable.dbFromModel(mModelClass).getModelCursor(mModelClass, mCursorArgs.mWhere, mCursorArgs.mOrder);
        if (null != cursor)
        {
            cursor.registerContentObserver(new ContentObserver(new Handler(Looper.getMainLooper()))
            {
                @Override
                public boolean deliverSelfNotifications()
                {
                    return false;
                }

                @Override
                public void onChange(boolean selfChange)
                {
                    observer.onChange(selfChange);
                }
            });
        }

        return cursor;
    }

    public static Cursor createCursor(Class modelClazz, List<Model> items)
    {
        if (items.size() > 0)
        {
            String[] columns = Model.getColumnArray(modelClazz, false);
            MatrixCursor matrixCursor = new MatrixCursor(columns);

            //Timber.d("createCursor:" + columns);

            long pk = 0;

            for (Model m : items)
            {
                // mock Pk fields (as we're not using the database).
                m.pk = pk++;
                try
                {
                    matrixCursor.addRow(m.getAsCursorRow(false));
                }
                catch (IllegalArgumentException e)
                {
                    Timber.e(e);
//                    Timber.e(String.format("%s - Columns:%s (Values:%s)",
//                                             Orm.getModelName(modelClazz),
//                                             ArrayUtils.toString(columns),
//                                             ArrayUtils.toString(m.getAsCursorRow(false))));
                }
            }

            return matrixCursor;
        }

        return null;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags)
    {
        dest.writeInt(mLoaderId);
        if (null != mModelClass)
        {
            dest.writeString(Orm.getModelName(mModelClass));
        }
        else
        {
            dest.writeString("");
        }
        if (null != mViewClass)
        {
            dest.writeString(mViewClass.getName());
        }
        else
        {
            dest.writeString("");
        }
        dest.writeParcelable(mCursorArgs, flags);
    }

    public static final Creator<CursorHolder> CREATOR = new Creator<CursorHolder>()
    {
        @Override
        public CursorHolder createFromParcel(Parcel in)
        {
            try
            {
                return new CursorHolder(in);
            }
            catch (ClassNotFoundException e)
            {
                Timber.e(e);
            }

            return null;
        }

        @Override
        public CursorHolder[] newArray(int size)
        {
            return new CursorHolder[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }
}
