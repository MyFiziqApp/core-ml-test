/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 ** Modified to support SQLite extensions by the SQLite developers:
 ** sqlite-dev@sqlite.org.
 */

package org.sqlite.database.sqlite;

import android.content.ContentResolver;
import android.database.AbstractWindowedCursor;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;

import android.util.SparseArray;

import com.myfiziq.sdk.db.Cached;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMDbCache;
import com.myfiziq.sdk.db.Orm;

import org.sqlite.database.ExtraUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A Cursor implementation that exposes results from a query on a
 * {@link SQLiteDatabase}.
 * <p>
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 */
public class SQLiteCursor extends AbstractWindowedCursor
{
    static final int NO_COUNT = -1;

    /**
     * The name of the table to edit
     */
    private final String mEditTable;

    /**
     * The names of the columns in the rows
     */
    private final String[] mColumns;

    /**
     * The query object for the cursor
     */
    private final SQLiteQuery mQuery;

    /**
     * The compiled query this cursor came from
     */
    private final SQLiteCursorDriver mDriver;

    /**
     * The number of rows in the cursor
     */
    private int mRowIdColumnIndex = 0;

    /**
     * The number of rows that can fit in the cursor window, 0 if unknown
     */
    private int mCursorWindowCapacity;

    /**
     * A mapping of column names to column indices, to speed up lookups
     */
    private Map<String, Integer> mColumnNameMap;

    /**
     * Used to find out where a cursor was allocated in case it never got released.
     */
    private final Throwable mStackTrace;

    private int mCount = NO_COUNT;

    private ORMDbCache mCache = ORMDbCache.getInstance();
    private SparseArray<Long> mPkCache = new SparseArray<>();
    private int mPkIndex = -1;

    private Class<? extends Model> mModelClass;

    private boolean mModelCached = false;

    private Thread mPreloadThread = null;

    /**
     * Execute a query and provide access to its result set through a Cursor
     * interface. For a query such as: {@code SELECT name, birth, phone FROM
     * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
     * phone) would be in the projection argument and everything from
     * {@code FROM} onward would be in the params argument.
     *
     * @param db        a reference to a Database object that is already constructed
     *                  and opened. This param is not used any longer
     * @param editTable the name of the table used for this query
     * @param query     the rest of the query terms
     *                  cursor is finalized
     * @deprecated use {@link #SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)} instead
     */
    @Deprecated
    public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                        String editTable, SQLiteQuery query)
    {
        this(driver, editTable, query);
    }

    /**
     * Execute a query and provide access to its result set through a Cursor
     * interface. For a query such as: {@code SELECT name, birth, phone FROM
     * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
     * phone) would be in the projection argument and everything from
     * {@code FROM} onward would be in the params argument.
     *
     * @param editTable the name of the table used for this query
     * @param query     the {@link SQLiteQuery} object associated with this cursor object.
     */
    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
    {
        if (query == null)
        {
            throw new IllegalArgumentException("query object cannot be null");
        }
        if (/* StrictMode.vmSqliteObjectLeaksEnabled() */ false)
        {
            mStackTrace = new DatabaseObjectNotClosedException().fillInStackTrace();
        }
        else
        {
            mStackTrace = null;
        }
        mDriver = driver;
        mEditTable = editTable;
        mColumnNameMap = null;
        mQuery = query;

        mColumns = query.getColumnNames();
        mRowIdColumnIndex = ExtraUtils.findRowIdColumnIndex(mColumns);
    }

    /**
     * Get the database that this cursor is associated with.
     *
     * @return the SQLiteDatabase that this cursor is associated with.
     */
    public SQLiteDatabase getDatabase()
    {
        return mQuery.getDatabase();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition)
    {
        // Make sure the row at newPosition is present in the window
        if (mWindow == null || newPosition < mWindow.getStartPosition() ||
                newPosition >= (mWindow.getStartPosition() + mWindow.getNumRows()))
        {
            fillWindow(newPosition);
        }

        return true;
    }

    @Override
    public int getCount()
    {
        if (mCount == NO_COUNT)
        {
            fillWindow(0);
        }
        return mCount;
    }

    /*
     ** The AbstractWindowClass contains protected methods clearOrCreateWindow() and
     ** closeWindow(), which are used by the android.database.sqlite.* version of this
     ** class. But, since they are marked with "@hide", the following replacement
     ** versions are required.
     */
    private void awc_clearOrCreateWindow(String name)
    {
        CursorWindow win = getWindow();
        if (win == null)
        {
            win = new CursorWindow(name);
            setWindow(win);
        }
        else
        {
            win.clear();
        }
    }

    private void awc_closeWindow()
    {
        setWindow(null);
    }

    private void fillWindow(int requiredPos)
    {
        awc_clearOrCreateWindow(getDatabase().getPath());

        try
        {
            if (mCount == NO_COUNT)
            {
                int startPos = ExtraUtils.cursorPickFillWindowStartPosition(requiredPos, 0);
                mCount = mQuery.fillWindow(mWindow, startPos, requiredPos, true);
                mCursorWindowCapacity = mWindow.getNumRows();

                //Timber.d("received count(*) from native_fill_window: " + mCount);
            }
            else
            {
                int startPos = ExtraUtils.cursorPickFillWindowStartPosition(requiredPos,
                        mCursorWindowCapacity);
                mQuery.fillWindow(mWindow, startPos, requiredPos, false);
            }

            preloadModels();
        }
        catch (RuntimeException ex)
        {
            // Close the cursor window if the query failed and therefore will
            // not produce any results.  This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            awc_closeWindow();
            throw ex;
        }
    }

    @Override
    public int getColumnIndex(String columnName)
    {
        // Create mColumnNameMap on demand
        if (mColumnNameMap == null)
        {
            String[] columns = mColumns;
            int columnCount = columns.length;
            HashMap<String, Integer> map = new HashMap<String, Integer>(columnCount, 1);
            for (int i = 0; i < columnCount; i++)
            {
                map.put(columns[i], i);
            }
            mColumnNameMap = map;
        }

        // Hack according to bug 903852
        final int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1)
        {
            Exception e = new Exception();
            //Timber.e(e, "requesting column name with table name -- " + columnName);
            columnName = columnName.substring(periodIndex + 1);
        }

        Integer i = mColumnNameMap.get(columnName);
        if (i != null)
        {
            return i.intValue();
        }
        else
        {
            return -1;
        }
    }

    @Override
    public String[] getColumnNames()
    {
        return mColumns;
    }

    @Override
    public void deactivate()
    {
        super.deactivate();
        mDriver.cursorDeactivated();
    }

    @Override
    public void close()
    {
        super.close();
        synchronized (this)
        {
            mQuery.close();
            mDriver.cursorClosed();
            if (null != mPreloadThread)
            {
                mPreloadThread.interrupt();
                mPreloadThread = null;
            }
        }
    }

    @Override
    public boolean requery()
    {
        if (isClosed())
        {
            return false;
        }

        synchronized (this)
        {
            if (!mQuery.getDatabase().isOpen())
            {
                return false;
            }

            if (null != mPreloadThread)
            {
                mPreloadThread.interrupt();
                mPreloadThread = null;
            }

            if (mWindow != null)
            {
                mWindow.clear();
            }
            mPos = -1;
            mCount = NO_COUNT;

            mDriver.cursorRequeried(this);
        }

        try
        {
            return super.requery();
        }
        catch (IllegalStateException e)
        {
            // for backwards compatibility, just return false
            //Timber.w(e, "requery() failed " + e.getMessage());
            return false;
        }
    }

    @Override
    public void setWindow(CursorWindow window)
    {
        super.setWindow(window);
        mCount = NO_COUNT;
    }

    /**
     * Changes the selection arguments. The new values take effect after a call to requery().
     */
    public void setSelectionArguments(String[] selectionArgs)
    {
        mDriver.setBindArguments(selectionArgs);
    }

    /**
     * Release the native resources, if they haven't been released yet.
     */
    @Override
    protected void finalize()
    {
        try
        {
            // if the cursor hasn't been closed yet, close it first
            if (mWindow != null)
            {
                    /*
                if (mStackTrace != null) {
                    String sql = mQuery.getSql();
                    int len = sql.length();
                    StrictMode.onSqliteObjectLeaked(
                        "Finalizing a Cursor that has not been deactivated or closed. " +
                        "database = " + mQuery.getDatabase().getLabel() +
                        ", table = " + mEditTable +
                        ", query = " + sql.substring(0, (len > 1000) ? 1000 : len),
                        mStackTrace);
                }
                */
                close();
            }
        }
        finally
        {
            super.finalize();
        }
    }

    public void setModelClass(Class clazz)
    {
        mModelClass = clazz;
        if (null != mModelClass)
        {
            Cached cached = mModelClass.getAnnotation(Cached.class);
            if (null != cached && cached.cached())
            {
                mModelCached = true;

                preloadModels();
            }
        }
    }

    private void preloadModels()
    {
        if (mModelCached)
        {
            if (mPkIndex < 0)
            {
                mPkIndex = getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            }

            if (null != mPreloadThread)
            {
                mPreloadThread.interrupt();
                mPreloadThread = null;
            }

            mPreloadThread = new Thread(() ->
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

                if (isClosed())
                {
                    return;
                }

                try
                {
                    int startPosition = mWindow.getStartPosition();
                    int endPosition = startPosition + mWindow.getNumRows();
                    for (int position = startPosition; position < endPosition; position++)
                    {
                        if (isClosed())
                            break;

                        try
                        {
                            PreloadCursor preloadCursor = new PreloadCursor(this, mPkIndex, position);
                            preloadCursor.getModel(mModelClass, position);
                        }
                        catch (Throwable t)
                        {
                            // ignored.
                        }

                        try
                        {
                            Thread.sleep(1);
                        }
                        catch (InterruptedException e)
                        {
                            break;
                        }
                    }
                }
                catch (Throwable t)
                {
                    // The cursor may get closed while we're processing - no need to report it.
                }
            });
            mPreloadThread.setPriority(Thread.MIN_PRIORITY);
            mPreloadThread.start();
        }
    }

    public <T extends Model> T getModel(Class<T> clazz)
    {
        return getModel(clazz, getPosition());
    }

    public <T extends Model> T getModel(Class<T> clazz, int position)
    {
        Model model = null;

        if (mModelCached)
        {
            //model = mCache.getModel(mModelClass, position);
            model = getModelByPk(clazz, position);
        }

        if (null == model)
        {
            model = Orm.newModel(clazz);
            moveToPosition(position);
            model.readFromCursor(this);
            if (mModelCached)
            {
                mCache.putModel(mModelClass, model, position);
            }
        }

        return (T) model;
    }

    public <T extends Model> T getModelByPk(Class<T> clazz)
    {
        return getModelByPk(clazz, getPosition());
    }

    public <T extends Model> T getModelByPk(Class<T> clazz, int position)
    {
        Model model = null;

        if (mModelCached)
        {
            model = mCache.getModelByPk(mModelClass, getModelPk(position));
        }

        if (null == model)
        {
            model = Orm.newModel(clazz);
            moveToPosition(position);
            model.readFromCursor(this);
            if (mModelCached)
            {
                mCache.putModel(mModelClass, model, position);
            }
        }

        return (T) model;
    }

    public long getModelPk()
    {
        return getModelPk(getPosition());
    }

    public long getModelPk(int position)
    {
        Long pk = mPkCache.get(position);
        if (null == pk)
        {
            if (mPkIndex < 0)
            {
                mPkIndex = getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            }

            if (getPosition() != position)
            {
                moveToPosition(position);
            }
            pk = getLong(mPkIndex);
            mPkCache.put(position, pk);
        }

        return pk;
    }

    public byte[] getBlob(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getBlob(position, columnIndex);
    }

    public String getString(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getString(position, columnIndex);
    }

    public void copyStringToBuffer(int position, int columnIndex, CharArrayBuffer buffer) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        mWindow.copyStringToBuffer(position, columnIndex, buffer);
    }

    public short getShort(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getShort(position, columnIndex);
    }

    public int getInt(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getInt(position, columnIndex);
    }

    public long getLong(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getLong(position, columnIndex);
    }

    public float getFloat(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getFloat(position, columnIndex);
    }

    public double getDouble(int position, int columnIndex) throws IllegalStateException
    {
        if (null == mWindow || isClosed())
            throw new IllegalStateException();
        return mWindow.getDouble(position, columnIndex);
    }

    static class PreloadCursor implements Cursor
    {
        private ORMDbCache mCache = ORMDbCache.getInstance();
        SQLiteCursor mCursor;
        private int mPkIndex;
        int mPos;

        public PreloadCursor(SQLiteCursor cursor, int pkIndex, int position)
        {
            mCursor = cursor;
            mPkIndex = pkIndex;
            mPos = position;
        }

        @Override
        public int getCount()
        {
            return mCursor.getCount();
        }

        @Override
        public int getPosition()
        {
            return 0;
        }

        @Override
        public boolean move(int offset)
        {
            return false;
        }

        @Override
        public boolean moveToPosition(int position)
        {
            return false;
        }

        @Override
        public boolean moveToFirst()
        {
            return false;
        }

        @Override
        public boolean moveToLast()
        {
            return false;
        }

        @Override
        public boolean moveToNext()
        {
            return false;
        }

        @Override
        public boolean moveToPrevious()
        {
            return false;
        }

        @Override
        public boolean isFirst()
        {
            return false;
        }

        @Override
        public boolean isLast()
        {
            return false;
        }

        @Override
        public boolean isBeforeFirst()
        {
            return false;
        }

        @Override
        public boolean isAfterLast()
        {
            return false;
        }

        @Override
        public int getColumnIndex(String columnName)
        {
            return mCursor.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException
        {
            return mCursor.getColumnIndexOrThrow(columnName);
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return mCursor.getColumnName(columnIndex);
        }

        @Override
        public String[] getColumnNames()
        {
            return mCursor.getColumnNames();
        }

        @Override
        public int getColumnCount()
        {
            return mCursor.getColumnCount();
        }

        @Override
        public byte[] getBlob(int columnIndex)
        {
            return mCursor.getBlob(mPos, columnIndex);
        }

        @Override
        public String getString(int columnIndex)
        {
            return mCursor.getString(mPos, columnIndex);
        }

        @Override
        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer)
        {
            mCursor.copyStringToBuffer(mPos, columnIndex, buffer);
        }

        @Override
        public short getShort(int columnIndex)
        {
            return mCursor.getShort(mPos, columnIndex);
        }

        @Override
        public int getInt(int columnIndex)
        {
            return mCursor.getInt(mPos, columnIndex);
        }

        @Override
        public long getLong(int columnIndex)
        {
            return mCursor.getLong(mPos, columnIndex);
        }

        @Override
        public float getFloat(int columnIndex)
        {
            return mCursor.getFloat(mPos, columnIndex);
        }

        @Override
        public double getDouble(int columnIndex)
        {
            return mCursor.getDouble(mPos, columnIndex);
        }

        @Override
        public int getType(int columnIndex)
        {
            return mCursor.getType(columnIndex);
        }

        @Override
        public boolean isNull(int columnIndex)
        {
            return mCursor.isNull(columnIndex);
        }

        @Override
        public void deactivate()
        {

        }

        @Override
        public boolean requery()
        {
            return false;
        }

        @Override
        public void close()
        {

        }

        @Override
        public boolean isClosed()
        {
            return false;
        }

        @Override
        public void registerContentObserver(ContentObserver observer)
        {

        }

        @Override
        public void unregisterContentObserver(ContentObserver observer)
        {

        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer)
        {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer)
        {

        }

        @Override
        public void setNotificationUri(ContentResolver cr, Uri uri)
        {

        }

        @Override
        public Uri getNotificationUri()
        {
            return null;
        }

        @Override
        public boolean getWantsAllOnMoveCalls()
        {
            return false;
        }

        @Override
        public void setExtras(Bundle extras)
        {

        }

        @Override
        public Bundle getExtras()
        {
            return null;
        }

        @Override
        public Bundle respond(Bundle extras)
        {
            return null;
        }

        public <T extends Model> T getModel(Class<T> clazz)
        {
            return getModel(clazz, getPosition());
        }

        public <T extends Model> T getModel(Class<T> clazz, int position)
        {
            Model model = null;

            //model = mCache.getModel(mModelClass, position);
            model = getModelByPk(clazz, position);

            if (null == model)
            {
                model = Orm.newModel(clazz);
                moveToPosition(position);
                model.readFromCursor(this);
                mCache.putModel(mCursor.mModelClass, model, position);
            }

            return (T) model;
        }

        public <T extends Model> T getModelByPk(Class<T> clazz)
        {
            return getModelByPk(clazz, getPosition());
        }

        public <T extends Model> T getModelByPk(Class<T> clazz, int position)
        {
            Model model = null;

            model = mCache.getModelByPk(mCursor.mModelClass, getModelPk(position));

            if (null == model)
            {
                model = Orm.newModel(clazz);
                moveToPosition(position);
                model.readFromCursor(this);
                mCache.putModel(mCursor.mModelClass, model, position);
            }

            return (T) model;
        }

        public long getModelPk()
        {
            return getModelPk(getPosition());
        }

        public long getModelPk(int position)
        {
            Long pk = mCursor.mPkCache.get(position);
            if (null == pk)
            {
                if (mPkIndex < 0)
                {
                    mPkIndex = getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
                }

                if (getPosition() != position)
                {
                    moveToPosition(position);
                }
                pk = getLong(mPkIndex);
                mCursor.mPkCache.put(position, pk);
            }

            return pk;
        }
    }
}
