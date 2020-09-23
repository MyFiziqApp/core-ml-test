package com.myfiziq.sdk.db;

import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.Base64;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;

import timber.log.Timber;

public class ModelOnDemandImage extends Model
{
    @Persistent
    public ArrayList<String> viewTags;

    @Persistent
    public ArrayList<String> viewTypeClass;

    @Persistent
    public String imageData;

    @Persistent
    public String imageFormat;

    private ArrayList<Class> mInstanceCache = new ArrayList<>();
    private boolean mViewTypeClassProcessed = false;

    @Override
    public void afterDeserialize()
    {
        super.afterDeserialize();
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
        }

        return false;
    }

    /**
     * Check if the view and or view parent matches.
     *
     * @param view
     * @param parent
     * @return
     */
    public boolean matches(View view, ViewParent parent)
    {
        return viewMatches(view);
    }

    public void applyStyle(View view, ViewParent parent)
    {
        if (view instanceof ImageView)
        {
            // TODO: cache values
            if (imageFormat.contentEquals("b64svg"))
            {
                try
                {
                    SVG svg = SVG.getFromString(new String(Base64.decode(imageData, Base64.DEFAULT)));
                    Picture picture = svg.renderToPicture();
                    Drawable drawable = new PictureDrawable(picture);

                    ((ImageView) view).setImageDrawable(drawable);
                }
                catch (SVGParseException e)
                {
                    Timber.e(e);
                }
            }
        }
    }
}
