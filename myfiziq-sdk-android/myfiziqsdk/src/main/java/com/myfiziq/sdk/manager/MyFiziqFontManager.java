package com.myfiziq.sdk.manager;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.GlobalContext;

import java.io.IOException;
import java.util.HashMap;

import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat;

public class MyFiziqFontManager
{
    static MyFiziqFontManager mThis = null;

    HashMap<String, Typeface> mFonts = new HashMap<>();
    Handler mHandler = null;

    private MyFiziqFontManager()
    {
        // Preload all fonts in assets font folder...
        Resources res = GlobalContext.getContext().getResources();
        AssetManager ass = res.getAssets();
        try
        {
            String[] fonts = ass.list("font");
            for (String font : fonts)
            {
                String name = font.toLowerCase();
                // only load actual ".ttf" files...
                if (name.endsWith(".ttf")||name.endsWith(".otf"))
                {
                    Typeface fontTypeFace = Typeface.createFromAsset(ass,"font/"+font);
                    if (null != fontTypeFace)
                    {
                        String key = name.replaceFirst("(.*?)\\.(ttf|otf)", "$1");
                        mFonts.put(key, fontTypeFace);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static synchronized MyFiziqFontManager getInstance()
    {
        if (null == mThis)
        {
            mThis = new MyFiziqFontManager();
        }

        return mThis;
    }

    public void loadFonts(String... fonts)
    {
        for (String fontName : fonts)
        {
            loadFont(fontName);
        }
    }

    public void loadFont(String name)
    {
        if (null == mFonts.get(name))
        {
            FontRequest fontRequest = new FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "name="+name,
                    R.array.com_google_android_gms_fonts_certs);

            FontRequestCb callback = new FontRequestCb(fontRequest, null);

            getHandlerThreadHandler().post(()-> FontsContractCompat.requestFont(
                    GlobalContext.getContext(),
                    fontRequest,
                    callback,
                    getHandlerThreadHandler()));

            callback.waitForCallback();

            putFont(name, callback.mTypeface);
        }
    }

    public Typeface getFont(String name)
    {
        return mFonts.get(name);
    }

    public void putFont(String name, Typeface font)
    {
        mFonts.put(name, font);
    }

    private Handler getHandlerThreadHandler()
    {
        if (mHandler == null)
        {
            HandlerThread handlerThread = new HandlerThread("fonts");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }
        return mHandler;
    }

    private static class FontRequestCb extends FontsContractCompat.FontRequestCallback
    {
        AsyncHelper.Callback<FontRequestCb> mCallback;
        Object mNotify = new Object();
        FontRequest mFontRequest;
        Typeface mTypeface = null;
        int mResult = 0;

        public FontRequestCb(FontRequest fontRequest, AsyncHelper.Callback<FontRequestCb> callback)
        {
            mFontRequest = fontRequest;
            mCallback = callback;
        }

        @Override
        public void onTypefaceRetrieved(Typeface typeface)
        {
            mTypeface = typeface;

            if (null != mCallback)
            {
                mCallback.execute(this);
            }

            synchronized (mNotify)
            {
                mNotify.notifyAll();
            }
        }

        @Override
        public void onTypefaceRequestFailed(int reason)
        {
            mResult = reason;

            if (null != mCallback)
            {
                mCallback.execute(this);
            }

            synchronized (mNotify)
            {
                mNotify.notifyAll();
            }
        }

        public void waitForCallback()
        {
            synchronized (mNotify)
            {
                try
                {
                    mNotify.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
