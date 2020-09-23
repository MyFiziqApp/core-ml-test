package com.myfiziq.sdk.adapters;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ORMTable;

import java.lang.ref.WeakReference;


/**
 * This is a legacy version of the {@link MyFiziqLoaderManager} that has NO AndroidX support.
 *
 * It is here to support older third party applications (e.g. older versions of React Native that have no AndroidX support).
 */
@Deprecated
public class MyFiziqLegacyLoaderManager implements android.app.LoaderManager.LoaderCallbacks<Cursor>
{
    SparseArray<CursorHolder> mLoaderMap = new SparseArray<>();
    WeakReference<LoaderManager> mLoader;
    WeakReference<Context> mContext;

    public MyFiziqLegacyLoaderManager(Context context, LoaderManager loader)
    {
        mContext = new WeakReference<>(context);
        mLoader = new WeakReference<>(loader);
    }

    public void loadCursor(int id, Class<? extends Model> modelClass, String where, String order, CursorHolder.CursorChangedListener listener)
    {
        loadCursor(id, ORMTable.uri(modelClass), where, order, modelClass, null, listener);
    }

    public void loadCursor(int id, Uri uri, String where, String order, Class<? extends Model> modelClass, Class<? extends ViewGroup> viewClass, CursorHolder.CursorChangedListener listener)
    {
        mLoaderMap.put(id, new CursorHolder(id, uri, where, order, Model.DEFAULT_DEPTH, modelClass, viewClass, listener));
        mLoader.get().initLoader(id, null, this);
    }

    public void loadCursor(CursorHolder holder)
    {
        mLoaderMap.put(holder.getLoaderId(), holder);
        mLoader.get().initLoader(holder.getLoaderId(), null, this);
    }

    public void reloadCursor(CursorHolder holder)
    {
        if (null == mLoaderMap.get(holder.getLoaderId()))
        {
            mLoaderMap.put(holder.getLoaderId(), holder);
        }

        if (mLoader != null && mLoader.get() != null)
        {
            mLoader.get().restartLoader(holder.getLoaderId(), null, this);
        }
    }

    public CursorHolder getHolder(int id)
    {
        return mLoaderMap.get(id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
    {
        CursorHolder holder = mLoaderMap.get(Integer.valueOf(id));
        if (null != holder)
        {
            CursorArgs args = holder.getCursorArgs();

            return new CursorLoader(mContext.get(),
                    args.mUri,
                    null,
                    args.mWhere,
                    null,
                    args.mOrder);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        CursorHolder loaderHolder = mLoaderMap.get(loader.getId());
        if (null != loaderHolder)
        {
            loaderHolder.setCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        CursorHolder loaderHolder = mLoaderMap.get(loader.getId());
        if (null != loaderHolder)
            loaderHolder.setCursor(null);
    }
}
