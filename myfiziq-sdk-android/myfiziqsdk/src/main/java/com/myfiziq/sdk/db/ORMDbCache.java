package com.myfiziq.sdk.db;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.myfiziq.sdk.util.GlobalContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * <code>Model</code> memory cache.
 * <br>
 * As <code>Cached</code> models are loaded from the database they are stored here.
 * If they are modified in the database the cache is cleared/updated as needed.
 */
public class ORMDbCache extends ContentObserver
{
    private static ORMDbCache mThis = null;
    private HashMap<String, CacheItem> mCache = new HashMap<>();

    private ORMDbCache()
    {
        super(new Handler(Looper.getMainLooper()));
    }

    /**
     * Gets a singleton instance.
     * @return ORMDbCache
     */
    public static synchronized ORMDbCache getInstance()
    {
        if (null == mThis)
        {
            mThis = new ORMDbCache();
        }

        return mThis;
    }

    /**
     * Stores a <code>Model</code> in the cache.
     * @param clazz - The class type.
     * @param model - The Model instance.
     * @param position
     */
    public synchronized void putModel(Class clazz, Model model, int position)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));
        if (null == list)
        {
            list = new CacheItem();
            mCache.put(clazz.getSimpleName(), list);
            ContentResolver r = GlobalContext.getContext().getContentResolver();
            r.registerContentObserver(ORMContentProvider.uri(null, clazz), true, this);
        }

        if (null != model)
        {
            Model m = list.get(position);
            if (null != m)
            {
                if (m != model)
                {
                    list.put(position, model);
                }
            }
            else
            {
                list.put(position, model);
            }
        }
    }

    public synchronized void updateModel(Class clazz, Model model)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));

        if (null == list)
        {
            return;
        }


        int size = list.size();

        for (int i=0; i<size; i++)
        {
            Model m = list.valueAt(i);

            if (null != m && model.id.contentEquals(m.id))
            {
                if (m != model)
                {
                    m.copy(model);
                }

                break;
            }
        }
    }

    public synchronized <T extends Model> T getLatestModel(Class<T> clazz)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));

        if (null == list || list.size() == 0)
        {
            return null;
        }


        T latestModel = (T) list.valueAt(0);
        int size = list.size();

        for (int i=0; i<size; i++)
        {
            T model = (T) list.valueAt(i);

            if (null != model && model.pk > latestModel.pk)
            {
                latestModel = model;
            }
        }

        return latestModel;
    }

    public synchronized <T extends Model> T getModel(Class<T> clazz, int position)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));
        if (null != list)
        {
            return (T)list.get(position);
        }

        return null;
    }

    @Nullable
    public synchronized <T extends Model> T getModelByPk(Class<T> clazz, long pk)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));
        if (null != list)
        {
            return (T)list.getByPk(pk);
        }

        return null;
    }

    @Nullable
    public synchronized <T extends Model> T getModel(Class<T> clazz, Model parent, String id, int childDepth, int childArrayDepth)
    {
        CacheItem list = mCache.get(Orm.getModelName(clazz));

        if (null == list)
        {
            return null;
        }


        int size = list.size();

        for (int i=0; i<size; i++)
        {
            T model = (T)list.valueAt(i);

            if (null != model && model.id.contentEquals(id))
            {
                // if the memory copy has been zeroed out update the cache.
                if (!model.ts.isZero())
                {
                    return model;
                }
                else
                {
                    return null;//readModel(clazz, parent, id, childDepth, childArrayDepth);
                }
            }
        }

        return null;//readModel(clazz, parent, id, childDepth, childArrayDepth);
    }

    private synchronized <T extends Model> T readModel(Class<T> clazz, Model parent, String id, int childDepth, int childArrayDepth)
    {
        return ORMTable.dbFromModel(clazz).getModel(
                clazz,
                parent,
                String.format("%s='%s'", Model.getIdFieldName(clazz), id),
                null,
                childDepth,
                childArrayDepth);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri)
    {
        // Find the class from the URI...

        List<String> paths = uri.getPathSegments();

        if (paths.size() > 0)
        {
            String table = paths.get(0);

            if (paths.size() > 1)
            {
                String id = paths.get(1).replaceAll("^[\"\']|[\"\']$", "");

                CacheItem list = mCache.get(table);


                if (null != list && list.size() > 0)
                {
                    ArrayList<Integer> remList = new ArrayList<>();

                    int size = list.size();

                    for (int i = 0; i < size; i++)
                    {
                        Model model = list.valueAt(i);
                        if (null != model && model.id.contentEquals(id))
                        {
                            remList.add(list.keyAt(i));
                        }
                    }

                    for (Integer rem : remList)
                    {
                        list.remove(rem);
                    }

                    //HashSet<String> treeNames = list.valueAt(0).getTreeClassNames();

                    // Clear tree members too...
                    //for (String member : treeNames)
                    //{
                    //    clearPath(member);
                    //}
                }
            }
            else
            {
                // Something changed... safest to clear the cache.
                clearPath(table);
            }
        }
    }

    private void clearPath(String path)
    {
        CacheItem list = mCache.get(path);
        if (null != list && list.size() > 0)
        {
            // Something changed... safest to clear the cache.
            list.clear();
        }
    }

    public void clearCache()
    {
        for (CacheItem list : mCache.values())
        {
            list.clear();
        }
    }

    static class CacheItem
    {
        SparseArray<Model> mModelPos = new SparseArray<>();
        LongSparseArray<Model> mModelPk = new LongSparseArray<>();

        public Model get(int position)
        {
            return mModelPos.get(position);
        }

        public Model getByPk(long pk)
        {
            Model model = mModelPk.get(pk);

            //if (null == model)
            //{
            //
            //}

            return model;
        }

        public int size()
        {
            return mModelPos.size();
        }

        public void put(int position, Model model)
        {
            mModelPos.put(position, model);
            mModelPk.put(model.pk, model);
        }

        public Model valueAt(int i)
        {
            if (i < mModelPos.size())
                return mModelPos.valueAt(i);

            return null;
        }

        public void clear()
        {
            mModelPos.clear();
            mModelPk.clear();
        }

        public void remove(int rem)
        {
            Model m = mModelPos.get(rem);
            if (null != m)
            {
                mModelPos.remove(rem);
                mModelPk.remove(m.pk);
            }
        }

        public int keyAt(int i)
        {
            return mModelPos.keyAt(i);
        }
    }
}
