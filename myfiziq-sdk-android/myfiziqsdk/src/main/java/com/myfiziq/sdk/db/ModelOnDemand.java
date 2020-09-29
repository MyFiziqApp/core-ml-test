package com.myfiziq.sdk.db;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.regex.Pattern;

import timber.log.Timber;

public class ModelOnDemand extends Model
{
    @Persistent
    public String parentviewTypeRegex;

    @Persistent
    public String viewTypeRegex;

    @Persistent
    public ArrayList<String> viewTypeClass;

    @Persistent
    public ArrayList<String> viewTags;

    private Pattern parentviewTypePattern = null;
    private Pattern viewTypePattern = null;
    private ArrayList<Class> mParentInstanceCache = new ArrayList<>();
    private ArrayList<Class> mInstanceCache = new ArrayList<>();
    private boolean mViewTypeClassProcessed = false;

    public String getParentviewTypeRegex()
    {
        return parentviewTypeRegex;
    }

    public String getViewTypeRegex()
    {
        return viewTypeRegex;
    }

    @Override
    public void afterDeserialize()
    {
        super.afterDeserialize();

        if (!TextUtils.isEmpty(viewTypeRegex))
        {
            viewTypePattern = Pattern.compile(viewTypeRegex);
        }

        if (!TextUtils.isEmpty(parentviewTypeRegex))
        {
            parentviewTypePattern = Pattern.compile(parentviewTypeRegex);
        }
    }

    /**
     * recurse up the parent tree to see if any parent view matches.
     * @param parent
     * @return
     */
    private boolean parentMatches(ViewParent parent)
    {
        if (null != parent)
        {
            if (mParentInstanceCache.contains(parent.getClass()))
            {
                //Timber.d("Parent Cache hit for:%s", parent.getClass().getSimpleName());
                return true;
            }

            String name = parent.getClass().getSimpleName();
            if (parentviewTypePattern.matcher(name).matches())
            {
                if (!mParentInstanceCache.contains(parent.getClass()))
                {
                    mParentInstanceCache.add(parent.getClass());
                }
                return true;
            }

            return parentMatches(parent.getParent());
        }

        return false;
    }

    private boolean tagMatches(View view)
    {
        if (null != viewTags)
        {
            Object tag = view.getTag();
            if (null != tag)
            {
                String tagStr = (String) tag;
                if (viewTags.contains(tagStr))
                {
                    return true;
                }
            }

            return false;
        }

        // No tags to match -> return true.
        return true;
    }

    private boolean viewMatches(View view)
    {
        if (null != view)
        {
            if (!mViewTypeClassProcessed && null != viewTypeClass)
            {
                for (String type : viewTypeClass)
                {
                    try
                    {
                        Class clazz = Class.forName(type);
                        if (null != clazz)
                        {
                            mInstanceCache.add(clazz);
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        Timber.e(e);
                    }
                }

                mViewTypeClassProcessed = true;
            }

            if (mInstanceCache.contains(view.getClass()))
            {
                return tagMatches(view);
            }

            if (null != viewTypePattern && viewTypePattern.matcher(view.getClass().getSimpleName()).matches())
            {
                mInstanceCache.add(view.getClass());
                return tagMatches(view);
            }
        }

        return false;
    }

    /**
     * Check if the view and or view parent matches.
     * @param view
     * @param parent
     * @return
     */
    public boolean matches(View view, ViewParent parent)
    {
        if (null != parentviewTypePattern)
        {
            return (viewMatches(view) && parentMatches(parent));
        }
        else
        {
            return viewMatches(view);
        }
    }

    public void applyStyle(View view, ViewParent parent)
    {
        // Base style does nothing...
    }
}
