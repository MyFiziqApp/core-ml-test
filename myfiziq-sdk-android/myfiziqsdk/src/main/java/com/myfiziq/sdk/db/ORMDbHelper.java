package com.myfiziq.sdk.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.manager.MyFiziqCompatibilityService;

import org.sqlite.database.ExtraUtils;
import org.sqlite.database.sqlite.SQLiteCursor;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;
import org.sqlite.database.sqlite.SQLiteStatement;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * SQLite database wrapper for handling CRUD operations on <code>Model</code> types.
 */
public class ORMDbHelper extends SQLiteOpenHelper
{
    private ContentResolver mResolver;
    private ORMTableType mTableType;
    private String mUser;
    private String mPassword;

    private ORMDbHelper(Context context, ORMTableType type, String name, String user, String password)
    {
        super(context, name, null, ORMTable.DATABASE_VERSION);
        mUser = user;
        mPassword = password;
        mResolver = context.getContentResolver();
        mTableType = type;

        if (!TextUtils.isEmpty(mPassword))
        {
            MyFiziq.getInstance().setPassword(name, mPassword);
        }
    }

    public static ORMDbHelper getInstance(Context context, ORMTableType type, String name, String user, String password)
    {
        return new ORMDbHelper(context, type, name, user, password);
    }

    public Uri uri(Class model)
    {
        return ORMContentProvider.uri(mUser, model);
    }

    public static boolean isDeviceCompatible()
    {
        return MyFiziqCompatibilityService.isCpuCompatible();
    }

    public ORMTableType getTableType()
    {
        return mTableType;
    }

    @Override
    public void onConfigure(SQLiteDatabase db)
    {
        super.onConfigure(db);
        db.execSQL("PRAGMA read_uncommitted=true;PRAGMA journal_mode=WAL;PRAGMA busy_timeout=5000;PRAGMA synchronous=OFF;");
    }

    public void reConfigure(SQLiteDatabase db, String password)
    {
        if (!TextUtils.isEmpty(password))
        {
            mPassword = password;
            MyFiziq.getInstance().changePassword(password);
        }
        db.execSQL("PRAGMA read_uncommitted=true;PRAGMA journal_mode=WAL;PRAGMA busy_timeout=5000;PRAGMA synchronous=OFF;");
    }

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        super.onOpen(db);
        if (!TextUtils.isEmpty(mPassword))
        {
            MyFiziq.getInstance().checkPassword();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // create all tables if they don't already exist.
        for (ORMTable table : ORMTable.values())
        {
            // only create tables that match the database type.
            if (table.isPersisted() && table.getTableType().matches(mTableType))
            {
                db.execSQL(table.getCreate());
            }
        }
        if (!TextUtils.isEmpty(mPassword))
        {
            MyFiziq.getInstance().execPending();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion < ORMTable.DATABASE_DROP_VERSIONS)
        {
            // drop all tables and start from scratch...
            for (ORMTable table : ORMTable.values())
            {
                db.execSQL("DROP TABLE IF EXISTS " + table.name());
            }
        }
        upgradeCreateAndAlter(db);
    }

    private void upgradeCreateAndAlter(SQLiteDatabase db)
    {
        // Create new tables if they are missing.
        for (ORMTable table : ORMTable.values())
        {
            // only create tables that match the database type.
            if (table.getTableType().matches(mTableType))
            {
                table.doCreate(db);
            }
        }

        // alter - add missing items
        for (ORMTable table : ORMTable.values())
        {
            // only alter tables that match the database type.
            if (table.getTableType().matches(mTableType))
            {
                table.doAlter(db);
            }
        }
    }

    public void createIndexTables(SQLiteDatabase db)
    {
        for (ORMTable table : ORMTable.values())
        {
            // only index tables that match the database type.
            if (table.getTableType().matches(mTableType))
            {
                String[] createIndexes = table.getCreateIndex();
                if (null != createIndexes && createIndexes.length > 0)
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = db.rawQuery(String.format("SELECT COUNT(*) FROM %s", table.name()), null);
                        if (null != cursor)
                        {
                            cursor.moveToFirst();
                            if (0 != cursor.getInt(0))
                            {
                                for (String index : createIndexes)
                                    db.execSQL(index);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        logException("createIndexTables", e);
                    }
                    finally
                    {
                        closeCursor(cursor);
                    }
                }
            }
        }
    }

    public void wipe()
    {
        wipe(getWritableDatabase());
    }

    public void wipe(SQLiteDatabase db)
    {
        for (ORMTable table : ORMTable.values())
        {
            // only create tables that match the database type.
            if (table.getTableType().matches(mTableType))
            {
                db.execSQL("DROP TABLE IF EXISTS " + table.name());
                db.execSQL(table.getCreate());
            }
        }
    }

    public boolean checkTables()
    {
        /*
        boolean result = false;
        Cursor check = null;
        try
        {
            SQLiteDatabase db = getWritableDatabase();
            check = db.query("sqlite_master",
                    null,
                    "type='table'",
                    null,
                    null,
                    null,
                    null);

            check.getCount();
            if (check.moveToFirst())
            {
                result = true;
            }
        }
        catch (SQLiteDatabaseCorruptException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != check)
            {
                check.close();
            }
        }
         */

        return true;
    }

    Cursor safeQuery(String queryName, @NonNull Uri uri, @Nullable String[] projection,
                             @Nullable String selection, @Nullable String[] selectionArgs,
                             @Nullable String sortOrder)
    {
        warnIfOnUiThread();

        try
        {
            return mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        }
        catch (Exception e)
        {
            logException(queryName, e);
        }
        return null;
    }

    int safeDelete(String queryName, @NonNull Uri url, @Nullable String where, @Nullable String[] selectionArgs)
    {
        warnIfOnUiThread();

        try
        {
            return mResolver.delete(url, where, selectionArgs);
        }
        catch (Exception e)
        {
            logException(queryName, e);
        }
        return -1;
    }

    private int safeUpdate(String queryName, @NonNull Uri uri, @Nullable ContentValues values,
                           @Nullable String where, @Nullable String[] selectionArgs)
    {
        if (values.size() > 0)
        {
            warnIfOnUiThread();

            try
            {
                return mResolver.update(uri, values, where, selectionArgs);
            }
            catch (Exception e)
            {
                logException(queryName, e);
            }
        }
        return -1;
    }

    private Uri safeInsert(String queryName, @NonNull Uri uri, @Nullable ContentValues values)
    {
        if (values.size() > 0)
        {
            warnIfOnUiThread();

            try
            {
                return mResolver.insert(uri, values);
            }
            catch (Exception e)
            {
                logException(queryName, e);
            }
        }
        return null;
    }

    public <T extends Model> T getModel(Class<T> clazz, String where)
    {
        return getModel(clazz, null, where, null, Model.DEFAULT_DEPTH, Model.DEFAULT_DEPTH);
    }

    public <T extends Model> T getModel(Class<T> clazz, String where, String order)
    {
        return getModel(clazz, null, where, order, Model.DEFAULT_DEPTH, Model.DEFAULT_DEPTH);
    }

    public <T extends Model> T getModel(Class<T> clazz, String where, int childDepth, int childArrayDepth)
    {
        return getModel(clazz, null, where, null, childDepth, childArrayDepth);
    }

    public <T extends Model> T getModel(Class<T> clazz, Model parent, String where, String order, int childDepth, int childArrayDepth)
    {
        T model = null;

        Cursor cursor = safeQuery(
                "getModel",
                ORMContentProvider.uri(mUser, clazz),  // table
                null,//Model.getColumnArray(clazz),    // projection
                where,                          // sel
                null,                           // sel args
                order);                          // sort order

        if ((null != cursor) && (cursor.moveToFirst()))
        {
            updateSQLiteCursor(cursor, clazz);
            model = createModelFromCursor(clazz, parent, cursor, childDepth, childArrayDepth);
        }
        closeCursor(cursor);

        return model;
    }

    public <T extends Model> int getModelListSize(Class<T> clazz, String where)
    {
        //int size = 0;
        String s = (!TextUtils.isEmpty(where)) ? " where " + where : "";
        long size = ExtraUtils.longForQuery(getReadableDatabase(), "select count(*) from " + ORMContentProvider.table(clazz) + s, null);

        return (int)size;
    }

    public <T extends Model> ArrayList<T> getModelList(
            Class<T> clazz, Model parent, String where, String orderBy, String... fieldsToRead)
    {
        ArrayList<T> models = new ArrayList<>();

        Cursor cursor = safeQuery(
                "getModelList",
                ORMContentProvider.uri(mUser, clazz),  // table
                null,//Model.getColumnArray(clazz),    // projection
                where,                          // sel
                null,                           // sel args
                orderBy);                       // sort order

        if ((null != cursor) && (cursor.moveToFirst()))
        {
            updateSQLiteCursor(cursor, clazz);

            do
            {
                T model = createModelFromCursor(clazz, parent, cursor, Model.DEFAULT_DEPTH, Model.DEFAULT_DEPTH, fieldsToRead);
                if (model != null)
                {
                    models.add(model);
                }
            }
            while (cursor.moveToNext());
        }
        closeCursor(cursor);

        return models;
    }

    public <T extends Model> Cursor getModelCursor(Class<T> clazz, String where, String orderBy)
    {
        return getModelCursor(clazz, null, where, orderBy);
    }

    public <T extends Model> Cursor getModelCursor(Class<T> clazz, String[] projection, String where, String orderBy)
    {
        return updateSQLiteCursor(safeQuery(
                "getModelCursor",
                ORMContentProvider.uri(mUser, clazz),  // table
                projection,                     // projection
                where,                          // sel
                null,                           // sel args
                orderBy), clazz);                       // sort order
    }

    public synchronized void saveModel(Model model)
    {
        saveModel(model, null, true);
    }

    public synchronized void saveModel(Model model, @Nullable String where)
    {
        saveModel(model, where, true);
    }

    public synchronized void saveModel(Model model, @Nullable String where, boolean bQuery)
    {
        final String queryName = "saveModel";
        Class clazz = model.getClass();
        Cursor cursor = null;
        String query = where;

        // Zero out the model's timestamp (in memory) for cache handling.
        model.ts.mValue = 0;

        if (bQuery)
        {
            if (TextUtils.isEmpty(where))
                query = String.format("%s='%s'", model.getIdFieldName(), model.getId());

            if (model.deleted)
            {
                safeDelete(
                        queryName,
                    ORMContentProvider.uri(mUser, clazz),  // table
                        query,                          // where
                        null                            // whereArgs
                );
                return;
            }

            cursor = safeQuery(
                    queryName,
                ORMContentProvider.uri(mUser, clazz),  // table
                    null,//Model.getColumnArray(clazz),    // projection
                    query,                          // sel
                    null,                           // sel args
                    null);                          // sort order
        }

        if ((null == cursor) || (cursor.getCount() == 0))
        {
            Uri uri = safeInsert(
                    queryName,
                ORMContentProvider.uri(mUser, clazz),  // table
                    model.getContentValues(cursor)  // values
            );

            // get the pk for the new item...
            if (null != uri)
            {
                String pkStr = uri.getLastPathSegment();
                if (null != pkStr)
                {
                    model.setPk(Long.valueOf(pkStr));
                }
            }
        }
        else
        {
            cursor.moveToFirst();

            // read back the pk from the cursor.
            int primaryKeyIndex = cursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            if (primaryKeyIndex >= 0)
            {
                model.setPk(cursor.getLong(primaryKeyIndex));
            }

            safeUpdate(
                    queryName,
                ORMContentProvider.uri(mUser, clazz),          // table
                    model.getContentValues(cursor),         // values
                    query,                                  // where
                    null                                    // sel args
            );
        }

        closeCursor(cursor);
    }

    public synchronized void saveModel(Model model, @Nullable String where, boolean bQuery, String... fieldsToSave)
    {
        final String queryName = "saveModel";
        Class clazz = model.getClass();
        Cursor cursor = null;
        String query = where;

        // Zero out the model's timestamp (in memory) for cache handling.
        model.ts.mValue = 0;

        if (bQuery)
        {
            if (TextUtils.isEmpty(where))
                query = String.format("%s='%s'", model.getIdFieldName(), model.getId());

            if (model.deleted)
            {
                safeDelete(
                        queryName,
                        ORMContentProvider.uri(mUser, clazz),  // table
                        query,                          // where
                        null                            // whereArgs
                );
                return;
            }

            cursor = safeQuery(
                    queryName,
                    ORMContentProvider.uri(mUser, clazz),  // table
                    null,//Model.getColumnArray(clazz),    // projection
                    query,                          // sel
                    null,                           // sel args
                    null);                          // sort order
        }

        if ((null == cursor) || (cursor.getCount() == 0))
        {
            Uri uri = safeInsert(
                    queryName,
                    ORMContentProvider.uri(mUser, clazz),  // table
                    model.getContentValues(cursor, fieldsToSave)  // values
            );

            // get the pk for the new item...
            if (null != uri)
            {
                String pkStr = uri.getLastPathSegment();
                if (null != pkStr)
                {
                    model.setPk(Long.valueOf(pkStr));
                }
            }
        }
        else
        {
            cursor.moveToFirst();

            // read back the pk from the cursor.
            int primaryKeyIndex = cursor.getColumnIndex(ORMContentProvider.PRIMARY_TABLE_KEY_ALIAS);
            if (primaryKeyIndex >= 0)
            {
                model.setPk(cursor.getLong(primaryKeyIndex));
            }

            safeUpdate(
                    queryName,
                    ORMContentProvider.uri(mUser, clazz),          // table
                    model.getContentValues(cursor, fieldsToSave),         // values
                    query,                                  // where
                    null                                    // sel args
            );
        }

        closeCursor(cursor);
    }

    public synchronized <T extends Model> void deleteModel(Class<T> clazz, String where)
    {
        safeDelete(
                "deleteModel",
                ORMContentProvider.uri(mUser, clazz),  // table
                where,                          // where
                null                            // whereArgs
        );
    }

    public synchronized <T extends Model> void deleteModel(Class<T> clazz, Model model)
    {
        deleteModel(
                clazz,
                String.format("%s = '%s'", model.getIdFieldName(), model.getId())  // where
        );
    }

    @Nullable
    public static <T extends Model> T createModelFromCursor(Class<T> clazz, Model parent, Cursor cursor, int childDepth, int childArrayDepth, String... fieldsToRead)
    {
        try
        {
            T model = null;

            SQLiteCursor c = getSQLiteCursor(cursor);
            if (null != c)
            {

                model = c.getModelByPk(clazz);
            }

            if (null == model)
            {
                model = Orm.newModel(clazz);
                model.readFromCursor(cursor, parent, childDepth, childArrayDepth, fieldsToRead);
            }

            return model;
        }
        catch (Exception e)
        {
            logException("createModelFromCursor", e);
        }
        return null;
    }

    static void closeCursor(Cursor cursor)
    {
        if (cursor != null)
        {
            cursor.close();
        }
    }

    private static SQLiteCursor getSQLiteCursor(Cursor cursor)
    {
        if (cursor instanceof CursorWrapper)
        {
            Cursor c = ((CursorWrapper)cursor).getWrappedCursor();
            if (c instanceof SQLiteCursor)
            {
                return (SQLiteCursor)c;
            }
        }

        return null;
    }

    public static Cursor updateSQLiteCursor(Cursor cursor, Class clazz)
    {
        if (cursor instanceof CursorWrapper)
        {
            Cursor c = ((CursorWrapper)cursor).getWrappedCursor();
            if (c instanceof SQLiteCursor)
            {
                ((SQLiteCursor)c).setModelClass(clazz);
            }
        }

        return cursor;
    }

    public static boolean isOnUiThread()
    {
        return (Looper.getMainLooper().getThread() == Thread.currentThread());
    }

    private void warnIfOnUiThread()
    {
        if (isOnUiThread())
        {
            //Timber.w(new Throwable(), "Database operation being made on the UI thread.");
        }
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    static long longForQuery(
            SQLiteDatabase db, String query, String[] selectionArgs
    )
    {
        SQLiteStatement prog = db.compileStatement(query);
        try
        {
            return longForQuery(prog, selectionArgs);
        }
        finally
        {
            prog.close();
        }
    }
	
    /**
     * Utility method to run the pre-compiled query and return the value in the
     * first column of the first row.
     */
    static long longForQuery(
            SQLiteStatement prog, String[] selectionArgs
    )
    {
        prog.bindAllArgsAsStrings(selectionArgs);
        return prog.simpleQueryForLong();
    }
	
    private static void logException(String method, Throwable e)
    {
        Timber.e(e, "Error in %s", method);
    }
}
