package com.myfiziq.sdk.db;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.util.GlobalContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * A factory for managing multiple database instances based on the database 'type'.
 */
public class ORMDbFactory
{
    private static final String DATABASE_BASE_NAME = "MYQ%s.%s.db";

    private static ORMDbFactory mThis;

    private HashMap<String, ORMDbHelper> mDatabases = new HashMap<>();

    private static HashMap<String, String> mPathCache = new HashMap<>();

    private String mUser = "";
    private String mPassword = null;

    /**
     * Returns a singleton instance.
     * @return ORMDbFactory
     */
    public synchronized static ORMDbFactory getInstance()
    {
        if (null == mThis)
        {
            mThis = new ORMDbFactory();
        }

        return mThis;
    }

    private ORMDbFactory()
    {

    }

    public void setPassword(String user, String password)
    {
        mUser = user;
        mPassword = password;
    }

    public String getUser()
    {
        return mUser;
    }

    /**
     * Opens (if not already open) all databases.
     * @param context - For loading from Resources.
     */
    public void openAllDatabases(Context context)
    {
        HashSet<ORMTableType> usedTableTypes = new HashSet<>();

        for (ORMTable table : ORMTable.values())
        {
            usedTableTypes.add(table.getTableType());
        }

        for (ORMTableType tableType : usedTableTypes)
        {
            switch (tableType)
            {
                case NOT_PERSISTED:
                case NOT_PERSISTED_GLOBAL:
                    break;

                case USER:
                    if (!TextUtils.isEmpty(getUser()))
                    {
                        getDb(context, tableType, getUser(), null);
                    }
                    break;

				case USER_SECURE:
				    if (!TextUtils.isEmpty(getUser()) && !TextUtils.isEmpty(mPassword))
                    {
                        getDb(context, tableType, getUser(), mPassword);
                    }
                    break;

                default:
                {
                    getDb(context, tableType, "", null);
                }
                break;
            }
        }
    }

    public void signOut(Context context)
    {
        dropDatabases(context);
        MyFiziq.getInstance().setGuestUser("");
        mUser = "";
        mPassword = "";
        closeAll();
    }

    /**
     * Drops all tables in all databases... excluding the types specified.
     * @param context -
     * @param excluded - Array of <code>ORMTableType</code> to exclude from processing.
     */
    public void dropDatabases(Context context, ORMTableType... excluded)
    {
        HashSet<ORMTableType> usedTableTypes = new HashSet<>();

        for (ORMTable table : ORMTable.values())
        {
            usedTableTypes.add(table.getTableType());
        }

        for (ORMTableType tableType : usedTableTypes)
        {
            boolean bExcluded = false;

            for (ORMTableType type : excluded)
            {
                if (type.name().contentEquals(tableType.name()))
                {
                    bExcluded = true;
                    break;
                }
            }

            if (!bExcluded)
            {
                switch (tableType)
                {
                    case NOT_PERSISTED:
                    case NOT_PERSISTED_GLOBAL:
                        break;

                    case USER:
                        if (!TextUtils.isEmpty(getUser()))
                        {
                            ORMDbHelper db = getDb(context, tableType, getUser(), null);
                            db.wipe();
                        }
                        break;

                    case USER_SECURE:
                        if (!TextUtils.isEmpty(getUser()) && !TextUtils.isEmpty(mPassword))
                        {
                            ORMDbHelper db = getDb(context, tableType, getUser(), mPassword);
                            db.wipe();
                        }
                        break;

                    default:
                        ORMDbHelper db = getDb(context, tableType, "", null);
                        db.wipe();
                        break;
                }
            }
        }
    }

    public void deleteDatabases(Context context, DeleteFilesListener listener, ORMTableType... excluded)
    {
        File dbPathFile = context.getDatabasePath("-");
        File dbPath = dbPathFile.getParentFile();

        closeAll();

        ArrayList<String> excluding = new ArrayList<>();
        for (ORMTableType type : excluded)
        {
            switch (type)
            {
                case NOT_PERSISTED:
                case NOT_PERSISTED_GLOBAL:
                {

                }
                break;

                default:
                {
                    String filename = getDatabaseName(context, type, null);
                    File f = new File(filename);
                    excluding.add(f.getName());
                }
                break;
            }
        }
        new DeleteDbFilesAsyncTask(dbPath, false, excluding, listener)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
    * Remove the database from the map and close it.
     */
    private void removeDatabase(String filename)
    {
        Iterator<Map.Entry<String, ORMDbHelper>> iter = mDatabases.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, ORMDbHelper> entry = iter.next();
            if (entry.getKey().equalsIgnoreCase(filename))
            {
                entry.getValue().close();
                iter.remove();
                break;
            }
        }
    }

    public ORMDbHelper getDb(Context context, ORMTableType type)
    {
        switch (type)
        {
            case NOT_PERSISTED:
            {
                return null;
            }

            case USER:
            {
                if (!TextUtils.isEmpty(getUser()))
                {
                    return getDb(context, type, getUser(), mPassword);
                }
            }
            break;

            case USER_SECURE:
            {
                if (!TextUtils.isEmpty(getUser()) && !TextUtils.isEmpty(mPassword))
                {
                    return getDb(context, type, getUser(), mPassword);
                }
            }
            break;

            default:
            {
                return getDb(context, type, "", null);
            }
        }

        return null;
    }

    public ORMDbHelper getDb(Context context, ORMTableType type, String user, String password)
    {
        String dbName = getDatabaseName(context, type, user);

        ORMDbHelper db = mDatabases.get(dbName);
        if (null == db)
        {
            ORMDbHelper tempDb = null;

            switch (type)
            {
                case NOT_PERSISTED:
                case NOT_PERSISTED_GLOBAL:
                {
                    return null;
                }

                case USER:
                {
                    tempDb = ORMDbHelper.getInstance(context, type, dbName, user, null);
                }
                break;

                case USER_SECURE:
                {
                    tempDb = ORMDbHelper.getInstance(context, type, dbName, user, password);
                }
                break;

                default:
                {
                    // Open database... & create if not created already.
                    tempDb = ORMDbHelper.getInstance(context, type, dbName, null, null);
                }
                break;
            }

            if (null != tempDb)
            {
                mDatabases.put(dbName, tempDb);

                if (tempDb.checkTables())
                {
                    db = tempDb;
                    db.createIndexTables(db.getWritableDatabase());
                }
            }
        }
        return db;
    }

    public String getDatabaseName(Context context, ORMTableType type, String name)
    {
        if (!TextUtils.isEmpty(name))
        {
            name = name.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        }
        else
        {
            // ensure name is not null.
            name = "";
        }

        switch (type)
        {
            case USER:
            case USER_SECURE:
                break;

            default:
                // force empty name.
                name = "";
                break;
        }

        return getDbPath(context, type, String.format(DATABASE_BASE_NAME, name, type.getName()));
    }

    public static String getDbPath(Context context, ORMTableType type, String name)
    {
        String path = null;

//                File dbFile = context.getDatabasePath(name);
//                File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                dbFile = new File(file+"/myq", name);
//                dbFile.getParentFile().mkdirs();
//                path = String.format("file:%s?cache=shared", dbFile.getAbsolutePath());

        switch (type)
        {
            default:
            {
                if (mPathCache.containsKey(name))
                {
                    path = mPathCache.get(name);
                }
                else
                {
                    File dbFile = context.getDatabasePath(name);

                    if (!dbFile.exists())
                    {
                        dbFile.getParentFile().mkdirs();
                    }

                    path = String.format("file:%s?cache=shared", dbFile.getAbsolutePath());
                    mPathCache.put(name, path);
                }
            }
            break;

            case MEMORY_GLOBAL:
            {
                // create an in memory database.
                path = String.format("file:%s?mode=memory&cache=shared", name);
            }
            break;
        }

        return path;
    }


    public void closeAll()
    {
        for (ORMDbHelper db : mDatabases.values())
        {
            if (null != db)
            {
                String path = db.getDatabaseName();
                db.close();
                switch (db.getTableType())
                {
                    default:
                        break;

                    case USER:
                    case USER_SECURE:
                        MyFiziq.getInstance().closeDatabase(path);
                        break;
                }
            }
        }

        mDatabases.clear();
    }

    //TODO: remove
    public void setGuestUser(String selectedGuest)
    {
        MyFiziq.getInstance().setGuestUser(selectedGuest);
    }

    //TODO: remove
    public String getGuestUser()
    {
        return MyFiziq.getInstance().getGuestUser();
    }

    public interface DeleteFilesListener
    {
        void onDeleteComplete(boolean deleteSuccess);
    }

    private static class DeleteDbFilesAsyncTask extends AsyncTask
    {
        File mFileDir;
        boolean mSubdirs;
        ArrayList<String> mExcluding;
        DeleteFilesListener mClearListener;
        boolean mDeleteSuccess;

        public DeleteDbFilesAsyncTask(File fileDir, boolean bSubdirs, ArrayList<String> excluding, DeleteFilesListener clearListener)
        {
            mFileDir = fileDir;
            mSubdirs = bSubdirs;
            mExcluding = excluding;
            mClearListener = clearListener;
            mDeleteSuccess = true;
        }

        private void deleteFiles(File folder)
        {
            File[] fileList = folder.listFiles();
            if (null != fileList)
            {
                for (File file : fileList)
                {
                    if (file.isDirectory())
                    {
                        // recurse in to subdirs?
                        if (mSubdirs)
                        {
                            deleteFiles(file);
                        }
                    }
                    else
                    {
                        boolean bExcluded = false;
                        for (String name : mExcluding)
                        {
                            if (name.contentEquals(file.getName()))
                            {
                                bExcluded = true;
                                break;
                            }
                        }
                        if (!bExcluded)
                        {
                            if (!GlobalContext.getContext().deleteDatabase(file.getAbsolutePath()))
                            {
                                //Timber.e("Failed to delete: " + file.getAbsolutePath());
                                mDeleteSuccess = false;
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected Object doInBackground(Object... params)
        {
            deleteFiles(mFileDir);
            return null;
        }

        @Override
        protected void onPostExecute(Object o)
        {
            if (null != mClearListener)
                // always return true for now - otherwise it blocks the user.
                mClearListener.onDeleteComplete(true);
        }
    }
}
