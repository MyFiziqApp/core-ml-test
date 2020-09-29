package com.myfiziq.sdk.db;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.util.GlobalContext;

import org.apache.commons.lang3.ArrayUtils;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * @hide
 */
public class ORMContentProvider extends ContentProvider
{
    public static final String AUTHORITY = GlobalContext.getContext().getPackageName() + ".myq_provider";
    public static final String URL = "content://" + AUTHORITY + "/";
    public static final String PRIMARY_TABLE_KEY_ALIAS = "primary_table_pk";

    private static final String TAG = ORMContentProvider.class.getSimpleName();

    public static class UriItem
    {
        String mName;
        int mValue;
        boolean mById;
        ORMTable mTable;

        public UriItem(String name, int value, ORMTable table)
        {
            mName = name;
            mValue = value;
            mById = false;
            mTable = table;
        }

        public UriItem(String name, int value, ORMTable table, boolean byId)
        {
            mName = name;
            mValue = value;
            mById = byId;
            mTable = table;
        }

        public String name()
        {
            return mName;
        }

        public int value()
        {
            return mValue;
        }

        public boolean byId()
        {
            return mById;
        }

        public ORMTable getTable() { return mTable; }
    }

    // Extra custom URI items...
    public enum ExtraUriItems
    {
        ModelWorkEvents,
        WIPE
    }

    ORMDbHelper mGlobalDb;

    private static UriMatcher mUriMatcher;
    private static ArrayList<UriItem> mUriItems = new ArrayList<>();

    static
    {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        int ix = 0;
        for (ORMTable table : ORMTable.values())
        {
            mUriItems.add(new UriItem(table.name(), ix++, table));
            mUriItems.add(new UriItem("_count_" + table.name(), ix++, table));
            mUriItems.add(new UriItem(table.name(), ix++, table, true));
        }

        //for (ExtraUriItems uri : ExtraUriItems.values())
        //{
        //    mUriItems.add(new UriItem(uri.name(), ix++, ORMTableType.GLOBAL));
        //}

        for (UriItem item : mUriItems)
        {
            addUri(item);
        }
    }


    // Suppress Timber lint errors since Timber probably hasn't been initialised at this point
    @SuppressLint("LogNotTimber")
    @Override
    public boolean onCreate()
    {
        // DO NOT call "ensureInitialised()" here
        // Android executes this method as soon as the app starts
        // (i.e. before we've had a chance to download any assets remotely if we're using a mini release build)

        return true;
    }

    /**
     * Initialises the content provider if we aren't initialised yet.
     */
    private void ensureInitialised()
    {
        if (mGlobalDb != null)
        {
            // Already initialised
            return;
        }

        if (!ORMDbHelper.isDeviceCompatible())
        {
            Log.e(TAG, "Device is not compatible");
        }

        try
        {
            mGlobalDb = ORMDbFactory.getInstance().getDb(getContext(), ORMTableType.GLOBAL);
        }
        catch (Exception e)
        {
            Log.e(TAG, "ORMContentProvider didn't load", e);
        }
    }

    private static String getAuthority(final Context appContext) throws PackageManager.NameNotFoundException {
        final ComponentName componentName = new ComponentName(appContext, ORMContentProvider.class.getName());
        final ProviderInfo providerInfo = appContext.getPackageManager().getProviderInfo(componentName, 0);
        return providerInfo.authority;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        ensureInitialised();
        
        int uriType = mUriMatcher.match(uri);

        if ((uriType < 0) || (uriType >= mUriItems.size()))
            throw new IllegalArgumentException("Unknown URI: " + uri);

        UriItem item = mUriItems.get(uriType);

        ORMDbHelper dbHelper = item.getTable().getDb();

        if (null == dbHelper)
            return null;

        synchronized (dbHelper)
        {
            Cursor cursor = null;

            if (item.name().contentEquals(ExtraUriItems.WIPE.name()))
            {
                throw new IllegalArgumentException("Cannot wipe from query: " + uri);
            }
            else
            {
                String table = item.name();

                // Set the table
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables(table);

                if (item.byId())
                {
                    String id = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(selection))
                    {
                        // adding the ID to the original query
                        queryBuilder.appendWhere("pk=" + id);
                    }
                    else
                    {
                        // adding the ID to the original query
                        queryBuilder.appendWhere("pk=" + id + " and " + selection);
                    }

                    try
                    {
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        cursor = queryBuilder.query(db, updateProjection(table, projection), selection,
                                selectionArgs, null, null, sortOrder);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try
                    {
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        cursor = queryBuilder.query(db, updateProjection(table, projection), selection,
                                selectionArgs, null, null, sortOrder);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                if (null != cursor)
                {
                    // make sure that potential listeners are getting notified
                    cursor.setNotificationUri(getContext().getContentResolver(), uri(table));
                    //Timber.e("query:"+uri);
                }
            }

            return cursor;
        }
    }

    private String[] updateProjectionJoined(String primaryTable, String[] existingProjection, String... extraProjections)
    {
        String primaryKeyAlias = primaryTable + "." + Model.COLUMN_PRIMARY_KEY + " as " + PRIMARY_TABLE_KEY_ALIAS;
        String[] projection;

        if (existingProjection != null)
        {
            // Add the projection to the end of an existing projection.
            projection = new String[]{primaryKeyAlias, primaryTable + ".*"};
            projection = ArrayUtils.addAll(projection, existingProjection);
            projection = ArrayUtils.addAll(projection, extraProjections);
        }
        else
        {
            // Ensure that all columns are returned as well as the alias.
            projection = new String[]{primaryKeyAlias, primaryTable + ".*"};
            projection = ArrayUtils.addAll(projection, existingProjection);
            projection = ArrayUtils.addAll(projection, extraProjections);
        }

        return projection;
    }

    private String[] updateProjection(String primaryTable, String[] existingProjection)
    {
        String primaryKeyAlias = primaryTable + "." + Model.COLUMN_PRIMARY_KEY + " as " + PRIMARY_TABLE_KEY_ALIAS;
        if (existingProjection != null)
        {
            // Add the projection to the end of an existing projection.
            int newLength = existingProjection.length + 1;
            existingProjection = Arrays.copyOf(existingProjection, newLength);
            existingProjection[newLength - 1] = primaryKeyAlias;
        }
        else
        {
            // Ensure that all columns are returned as well as the alias.
            existingProjection = new String[]{"*", primaryKeyAlias};
        }

        return existingProjection;
    }

    @Override
    public String getType(@NonNull Uri uri)
    {
        // Always ensure we're initialised before doing anything
        ensureInitialised();
        
        return "";
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values)
    {
        // Always ensure we're initialised before doing anything
        ensureInitialised();
        
        int uriType = mUriMatcher.match(uri);

        if ((uriType < 0) || (uriType >= mUriItems.size()))
            throw new IllegalArgumentException("Unknown URI: " + uri);

        UriItem item = mUriItems.get(uriType);

        ORMDbHelper dbHelper = item.getTable().getDb();

        if (null == dbHelper)
            return null;

        synchronized (dbHelper)
        {
            if (item.name().startsWith("WIPE"))
                throw new IllegalArgumentException("Cannot wipe from query: " + uri);

            String table = item.name();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert(table, null, values);

            MyFiziq.getInstance().notifyChange(table, String.valueOf(id));
            Uri notifyUri = Uri.parse(URL + table + "/" + id);
            //getContext().getContentResolver().notifyChange(notifyUri, null, false);
            //Timber.e("insert:"+notifyUri);
            return notifyUri;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        // Always ensure we're initialised before doing anything
        ensureInitialised();
        
        int uriType = mUriMatcher.match(uri);

        if ((uriType < 0) || (uriType >= mUriItems.size()))
            throw new IllegalArgumentException("Unknown URI: " + uri);

        UriItem item = mUriItems.get(uriType);

        ORMDbHelper dbHelper = item.getTable().getDb();

        if (null == dbHelper)
            return 0;

        synchronized (dbHelper)
        {
            String table = item.name();

            int rowsDeleted = 0;

            if (item.name().contentEquals(ExtraUriItems.WIPE.name()))
            {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                dbHelper.wipe(db);
            }
            else if (item.byId())
            {
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection))
                {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    rowsDeleted = db.delete(table,
                            "pk=" + id,
                            null);
                }
                else
                {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    rowsDeleted = db.delete(table,
                            "pk=" + id + " and " + selection,
                            selectionArgs);
                }

                MyFiziq.getInstance().notifyChange(table, String.valueOf(id));
                //Uri notifyUri = Uri.parse(URL + table + "/" + id);
                //getContext().getContentResolver().notifyChange(notifyUri, null, false);
                //Timber.e("delete:"+notifyUri);
            }
            else
            {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                rowsDeleted = db.delete(table, selection,
                        selectionArgs);

                MyFiziq.getInstance().notifyChange(table, null);
                //getContext().getContentResolver().notifyChange(uri, null, false);
                //Timber.e("delete:"+uri);
            }

            return rowsDeleted;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        // Always ensure we're initialised before doing anything
        ensureInitialised();
        
        int uriType = mUriMatcher.match(uri);

        if ((uriType < 0) || (uriType >= mUriItems.size()))
            throw new IllegalArgumentException("Unknown URI: " + uri);

        UriItem item = mUriItems.get(uriType);

        ORMDbHelper dbHelper = item.getTable().getDb();

        if (null == dbHelper)
            return 0;

        Uri notifyUri = uri;
        String table = item.name();
        String id = null;

        synchronized (dbHelper)
        {
            int rowsUpdated = 0;

            if (item.byId())
            {
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection))
                {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    rowsUpdated = db.update(table,
                            values,
                            "pk=" + id,
                            null);
                }
                else
                {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    rowsUpdated = db.update(table,
                            values,
                            "pk=" + id + " and " + selection,
                            selectionArgs);
                }

                notifyUri = Uri.parse(URL + table + "/" + id);
            }
            else
            {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                rowsUpdated = db.update(item.name(),
                        values,
                        selection,
                        selectionArgs);
            }

            MyFiziq.getInstance().notifyChange(table, id);
            //getContext().getContentResolver().notifyChange(notifyUri, null, false);
            //Timber.e("update:"+notifyUri);
            return rowsUpdated;
        }
    }

    private static void addUri(UriItem uri)
    {
        switch (uri.mTable.getTableType())
        {
            case USER:
            case USER_SECURE:
            {
                if (uri.byId())
                {
                    mUriMatcher.addURI(AUTHORITY,
                            uri.name() + "/#/user/*",
                            uri.value());
                }
                else
                {
                    mUriMatcher.addURI(AUTHORITY,
                            uri.name() + "/user/*",
                            uri.value());
                }
            }
            break;

            default:
            {
                if (uri.byId())
                {
                    mUriMatcher.addURI(AUTHORITY,
                            uri.name()+"/#",
                            uri.value());
                }
                else
                {
                    mUriMatcher.addURI(AUTHORITY,
                            uri.name(),
                            uri.value());
                }
            }
            break;
        }
    }

    public static Uri uri(Uri uri)
    {
        return Uri.parse(URL + uri);
    }

    public static Uri uri(UriItem item)
    {
        return Uri.parse(URL + item.name());
    }

    public static Uri uri(ExtraUriItems item)
    {
        return Uri.parse(URL + item.name());
    }

    public static Uri uri(String table)
    {
        return Uri.parse(URL + table);
    }

    public static Uri uri(ORMTable table)
    {
        return Uri.parse(URL + table.name());
    }

    public static Uri uri(Class model)
    {
        Uri uri = ORMTable.uri(model);
        if (null == uri)
        {
            uri = Uri.parse(URL + table(model));
        }
        return uri;
    }

    public static Uri uri(String name, Class model)
    {
        if (!TextUtils.isEmpty(name))
            return Uri.parse(URL + table(model) + "/user/" + name);
        else
        return Uri.parse(URL + table(model));
    }

//    public static Uri uri(UriItem item, String id)
//    {
//        return Uri.parse(URL + item.name() + "/" + id);
//    }

//    public static Uri uri(UriItem item, String id1, String id2)
//    {
//        return Uri.parse(URL + item.name() + "/" + id1 + "/" + id2);
//    }

    public static String table(Class model)
    {
        return Orm.getModelName(model);
    }
}
