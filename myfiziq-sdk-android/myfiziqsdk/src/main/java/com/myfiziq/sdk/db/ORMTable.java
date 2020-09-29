package com.myfiziq.sdk.db;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.myfiziq.sdk.util.GlobalContext;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Defines the table/Model types that are stored in the database(s).
 */
public enum ORMTable
{
    ModelAppConf(ModelAppConf.class, ORMTableType.GLOBAL),
    ModelRemoteAsset(ModelRemoteAsset.class, ORMTableType.GLOBAL),
    ModelRemoteAssets(ModelRemoteAssets.class, ORMTableType.GLOBAL),
    ModelAvatar(ModelAvatar.class, ORMTableType.USER_SECURE),
    ModelAvatarSource(ModelAvatarSource.class, ORMTableType.USER_SECURE),
    ModelUserProfile(ModelUserProfile.class, ORMTableType.USER_SECURE),
    ModelAvatarReqList(ModelAvatarReqList.class, ORMTableType.NOT_PERSISTED),
    ModelAvatarReq(ModelAvatarReq.class, ORMTableType.USER_SECURE),
    ModelAvatarUpload(ModelAvatar.class, ORMTableType.USER_SECURE),
    ModelLog(ModelLog.class, ORMTableType.GLOBAL, "timestamp"),
    ModelLocalUserData(ModelLocalUserData.class, ORMTableType.USER_SECURE);

    /**
     * If the DATABASE_VERSION on the device is LESS than this version, all tables will be dropped.
     *
     * (i.e. increment the DATABASE_VERSION by 1 and make the DATABASE_DROP_VERSIONS value the same to force
     * all tables to be dropped when the user updates the app(
     */
    static final int DATABASE_DROP_VERSIONS = 20;

    /**
     * Bump this whenever a new table is added or a table is altered.
     *
     * ORMTable will then TRY to reconcile the two (no guarantees though!).
     */
    static final int DATABASE_VERSION = 24;


    @NonNull private String mCreate = "";
    @Nullable private String[] mCreateIndex;
    @NonNull private Class mClass;
    @NonNull private ORMTableType mTableType;


    ORMTable(@NonNull Class clazz, @Nullable String... indexes)
    {
        this(clazz, ORMTableType.NOT_PERSISTED, 0, indexes);
    }

    ORMTable(@NonNull Class clazz, @NonNull ORMTableType tableType, @Nullable String... indexes)
    {
        this(clazz, tableType, 0, indexes);
    }

    ORMTable(@NonNull Class clazz, @NonNull ORMTableType tableType, int fts, @Nullable String... indexes)
    {
        mClass = clazz;
        mTableType = tableType;

        switch (mTableType)
        {
            case NOT_PERSISTED:
            case NOT_PERSISTED_GLOBAL:
                break;

            default:
            {
                switch (fts)
                {
                    default:
                    case 0:
                    {
                        mCreate = "CREATE TABLE IF NOT EXISTS "
                                + name()
                                + " ("
                                + Model.getTable(clazz)
                                + ");";
                    }
                    break;

                    case 3:
                    case 4:
                    case 5:
                    {
                        mCreate = "CREATE VIRTUAL TABLE IF NOT EXISTS "
                                + name()
                                + String.format(" USING fts%d(", fts)
                                + Model.getTable(clazz)
                                + ");";
                    }
                    break;
                }

                if ((null != indexes) && (indexes.length > 0))
                {
                    mCreateIndex = new String[indexes.length];
                    for (int i = 0; i < indexes.length; i++)
                    {
                        mCreateIndex[i] = String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s);",
                                                        getIndexName(indexes[i]), name(), indexes[i]);
                    }
                }
            }
            break;
        }
    }

    @NonNull
    private String getIndexName(String cols)
    {
        return name() + "_" + cols.replaceAll(",", "_");
    }

    @NonNull
    public Class getTableClass()
    {
        return mClass;
    }

    @NonNull
    public String getCreate()
    {
        return mCreate;
    }

    @NonNull
    public ORMTableType getTableType()
    {
        return mTableType;
    }

    @Nullable
    public String[] getCreateIndex()
    {
        return mCreateIndex;
    }

    public void doCreate(SQLiteDatabase db)
    {
        if (!TextUtils.isEmpty(mCreate))
        {
            db.execSQL(mCreate);
        }
    }

    public void doAlter(SQLiteDatabase db)
    {
        if (!TextUtils.isEmpty(mCreate))
        {
            try (Cursor check = db.query(name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
            {

                ArrayList<String> alters = Model.getAlter(mClass, check);
                for (String alter : alters)
                {
                    try
                    {
                        db.execSQL(String.format("ALTER TABLE %s %s",
                                name(), alter));
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Cannot alter table %s", name());
                    }
                }
            }
            catch (Throwable e)
            {
                Timber.e(e, "Cannot execute query");
            }
        }
    }

    @NonNull
    public String R(@NonNull String field)
    {
        return name() + field;
    }

    public ORMDbHelper getDb(Uri uri)
    {
        return ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), getTableType());
        /*
        switch (mTableType)
        {
            case USER:
                List<String> segments = uri.getPathSegments();
                if (segments.size() > 1)
                {
                    return ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), getTableType(), segments.get(2), null);
                }
                else
                {
                    return null;
                }
            default:
                return ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), getTableType());
        }
        */
    }

    public ORMDbHelper getDb()
    {
        return ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), getTableType());
    }

    public static Uri uri(Class model)
    {
        ORMDbHelper db = dbFromModel(model);
        if (null != db)
        {
            return db.uri(model);
        }
        return null;
    }

    /**
     * Gets the <code>ORMTable</code> type for the <code>Model</code> class.
     * @param clazz - the <code>Model</code> class.
     * @return The <code>ORMTable</code> type or null if not found.
     */
    @Nullable
    public static ORMTable fromModel(Class<? extends Model> clazz)
    {
        String name = Orm.getModelName(clazz);

        if (name == null)
        {
            return null;
        }

        for (ORMTable table : ORMTable.values())
        {
            if (table.name().contentEquals(name))
            {
                return table;
            }
        }

        return null;
    }

    /**
     * Gets the database class associated with the specified <code>Model</code> class.
     * @param clazz - The <code>Model</code> class.
     * @return - The database instance or null on error.
     */
    @Nullable
    public static ORMDbHelper dbFromModel(Class<? extends Model> clazz)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            return table.getDb();
        }

        return null;
    }

    /**
     * Gets the number of rows in the database for the specified <code>Model</code> class using the
     * specified 'where' clause.
     * @param clazz - The Model class.
     * @param where - A where clause or null.
     * @return The number of rows or -1 on error.
     */
    public static <T extends Model> int getModelCount(Class<T> clazz, String where)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            return table.getModelListSize(clazz, where);
        }

        return -1;
    }

    /**
     * Gets a list of <code>Model</code> objects with optional 'where' and 'order' clauses.
     * @param clazz - The <code>Model</code> class.
     * @param where - An optional 'where' clause.
     * @param order - An optional 'order by' clause.
     * @return - The resulting list or null on error.
     */
    @Nullable
    public static <T extends Model> ArrayList<T> getModelList(Class<T> clazz, String where, String order)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            return table.getModelList(clazz, null, where, order);
        }

        return null;
    }

    @Nullable
    public static <T extends Model> T getModel(Class<T> clazz, int pk)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), table.getTableType());
            if (null != db)
            {
                return db.getModel(clazz, String.format("pk=%d", pk));
            }
        }

        return null;
    }


    @Nullable
    public static <T extends Model> T getModelWhere(Class<T> clazz, String where)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                return table.getModel(clazz, where);
            }
        }

        return null;
    }
	
    public static <T extends Model> T getModelWhere(Class<T> clazz, String where, String order)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                return db.getModel(clazz, where, order);
            }
        }

        return null;
    }
		
    @Nullable
    public static <T extends Model> T getModel(Class<T> clazz, String id)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = ORMDbFactory.getInstance().getDb(GlobalContext.getContext(), table.getTableType());
            if (null != db)
            {
                String where = null;
                if (!TextUtils.isEmpty(id))
                {
                    where = String.format("%s='%s'", Model.getIdFieldName(clazz), id);
                }
                return db.getModel(clazz, where);
            }
        }

        return null;
    }

    @Nullable
    public static Model getModel(String name)
    {
        for (ORMTable table : ORMTable.values())
        {
            if (table.name().contentEquals(name))
            {
                try
                {
                    return (Model) table.mClass.newInstance();
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot create new instance of table %s", table.getTableClass().getSimpleName());
                }
            }
        }

        return null;
    }

    @Nullable
    public static <T extends Model> T getModel(Class<T> clazz, Bundle args, int idTagId)
    {
        if (null != args)
        {
            String id = args.getString(String.valueOf(idTagId));
            if (!TextUtils.isEmpty(id))
            {
                return getModel(clazz, id);
            }
        }

        return null;
    }

    public boolean isPersisted()
    {
        switch (mTableType)
        {
            case NOT_PERSISTED:
            case NOT_PERSISTED_GLOBAL:
                return false;
        }

        return true;
    }

    /*
    public static <T extends Model> T getModel(Class<T> clazz, String where)
    {
        return getModel(clazz, null, where, null, Model.DEFAULT_DEPTH, Model.DEFAULT_DEPTH);
    }

    public static <T extends Model> T getModel(Class<T> clazz, String where, String order)
    {
        return getModel(clazz, null, where, order, Model.DEFAULT_DEPTH, Model.DEFAULT_DEPTH);
    }

    public static <T extends Model> T getModel(Class<T> clazz, String where, int childDepth, int childArrayDepth)
    {
        return getModel(clazz, null, where, null, childDepth, childArrayDepth);
    }
    */

    public static <T extends Model> T getModel(Class<T> clazz, Model parent, String where, String order, int childDepth, int childArrayDepth)
    {
        T model = null;
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                model = db.getModel(clazz, parent, where, order, childDepth, childArrayDepth);
            }
        }
        return model;
    }

    public static <T extends Model> int getModelListSize(Class<T> clazz, String where)
    {
        long size = 0;
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                size = db.getModelListSize(clazz, where);
            }
        }
        return (int) size;
    }

    public static <T extends Model> ArrayList<T> getModelList(
            Class<T> clazz, Model parent, String where, String orderBy, String... fieldsToRead)
    {
        ArrayList<T> models = new ArrayList<>();
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                models = db.getModelList(clazz, parent, where, orderBy, fieldsToRead);
            }
        }
        return models;
    }

    public static synchronized <T extends Model> Cursor getModelCursor(Class<T> clazz, String where, String orderBy)
    {
        return getModelCursor(clazz, null, where, orderBy);
    }

    public static synchronized <T extends Model> Cursor getModelCursor(Class<T> clazz, String[] projection, String where, String orderBy)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                return db.getModelCursor(clazz, projection, where, orderBy);
            }
        }
        return null;
    }

    public static synchronized void saveModel(Model model)
    {
        saveModel(model, null, true);
    }

    public static synchronized void saveModel(Model model, @Nullable String where)
    {
        saveModel(model, where, true);
    }

    public static synchronized void saveModel(Model model, @Nullable String where, boolean bQuery)
    {
        ORMTable table = fromModel(model.getClass());
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                db.saveModel(model, where, bQuery);
            }
        }
    }

    public static synchronized void saveModel(Model model, @Nullable String where, boolean bQuery, String... fieldsToSave)
    {
        ORMTable table = fromModel(model.getClass());
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                db.saveModel(model, where, bQuery, fieldsToSave);
            }
        }
    }

    public static synchronized <T extends Model> void deleteModel(Class<T> clazz, String where)
    {
        ORMTable table = fromModel(clazz);
        if (null != table)
        {
            ORMDbHelper db = table.getDb();
            if (null != db)
            {
                db.deleteModel(clazz, where);
            }
        }
    }

    public static synchronized <T extends Model> void deleteModel(Class<T> clazz, Model model)
    {
        deleteModel(
                clazz,
                String.format("%s = '%s'", model.getIdFieldName(), model.getId())  // where
        );
    }
}
