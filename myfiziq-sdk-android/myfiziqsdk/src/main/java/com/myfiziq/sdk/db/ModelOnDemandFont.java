package com.myfiziq.sdk.db;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.myfiziq.sdk.manager.MyFiziqFontManager;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.UiUtils;

public class ModelOnDemandFont extends ModelOnDemand
{
    @Persistent
    protected String fontName;

    @Persistent
    protected String sourceSp;

    @Persistent
    protected String destSp;

    private char destSpOp = '\0';
    private CachedFloat cacheSourceSp;
    private CachedFloat cacheDestSp;

    public String getFontName()
    {
        return fontName;
    }

    @Override
    public void afterDeserialize()
    {
        super.afterDeserialize();
        cacheSourceSp = new CachedFloat(sourceSp);
        if (!TextUtils.isEmpty(destSp))
        {
            // no source.... dest format must be + or - or = <sp> e.g: "+2" or "=12"
            if (cacheSourceSp.isNull())
            {
                String destVal = destSp.substring(1);
                cacheDestSp = new CachedFloat(destVal);
                destSpOp = destSp.charAt(0);
            }
            // from source to dest in SP.
            else
            {
                cacheDestSp = new CachedFloat(destSp);
            }
        }
    }

    @Override
    public void applyStyle(View view, ViewParent parent)
    {
        if (view instanceof TextView && matches(view, parent))
        {
            TextView tv = (TextView) view;
            if (!cacheDestSp.isNull())
            {
                // no source.... dest format must be + or - or = <sp> e.g: "+2" or "=12"
                if (cacheSourceSp.isNull())
                {
                    switch (destSpOp)
                    {
                        case '+':
                        {
                            float sp = UiUtils.convertPixelToSp(GlobalContext.getContext(), tv.getTextSize());
                            sp += cacheDestSp.get();
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, UiUtils.convertSpToPixel(GlobalContext.getContext(), sp));
                        }
                        break;

                        case '-':
                        {
                            float sp = UiUtils.convertPixelToSp(GlobalContext.getContext(), tv.getTextSize());
                            sp -= cacheDestSp.get();
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, UiUtils.convertSpToPixel(GlobalContext.getContext(), sp));
                        }
                        break;

                        case '=':
                        {
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, UiUtils.convertSpToPixel(GlobalContext.getContext(), cacheDestSp.get()));
                        }
                        break;
                    }
                }
                // from source to dest in SP.
                else
                {
                    int sp = Math.round(UiUtils.convertPixelToSp(GlobalContext.getContext(), tv.getTextSize()));
                    if (sp == cacheSourceSp.getAsInteger())
                    {
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, UiUtils.convertSpToPixel(GlobalContext.getContext(), cacheDestSp.get()));
                    }
                }
            }

            // Set the new font after adjusting the size.
            if (!TextUtils.isEmpty(fontName))
                tv.setTypeface(MyFiziqFontManager.getInstance().getFont(fontName));
        }
    }
}
