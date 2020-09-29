package com.myfiziq.sdk.gles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * A class for loading and binding a GL texture.
 */
public class Texture
{
    final static boolean mDoMipMap = false;

    public Bitmap mBitmap;
    public int mTextureId = -1;
    public int mGlTextureId = -1;
    public int mWidth = 0;
    public int mHeight = 0;
    public boolean mLoaded = false;
    boolean mUploaded = false;
    boolean mPendingRemove = false;
    TextureListener mListener;
    WeakReference<View> mView;
    Handler mHandler;

    /**
     * A callback for when the Texture has uploaded to GL.
     */
    public interface TextureListener
    {
        void onUpload();
    }

    final Runnable mRenderViewRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                if (null != mView)
                {
                    View v = mView.get();
                    if (null != v)
                    {
                        v.clearFocus();
                        v.setPressed(false);
                        mBitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
                        Canvas canvas = new Canvas(mBitmap);
                        v.draw(canvas);

                        mLoaded = true;
                    }
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    };

    public Texture()
    {

    }

    /**
     * Creates a new <code>Texture</code> based on the <code>Bitmap</code> resource id.
     * @param context - The <code>Context</code> to use for <code>Resource</code> loading.
     * @param textureId - The id of the <code>Bitmap</code> resource to load.
     */
    public Texture(Context context, int textureId)
    {
        mTextureId = textureId;
        loadTexture(context);
    }

    /**
     * Renders the view to a <code>Bitmap</code> and uploads to GL.
     * @param v - The view to render.
     */
    public Texture(View v)
    {
        getViewBitmap();
    }

    /**
     * Creates a new <code>Texture</code> based on the <code>Bitmap</code>.
     * @param bitmap - The bitmap.
     */
    public Texture(Bitmap bitmap)
    {
        createBitmap(bitmap);
    }

    /**
     * Sets the <code>Bitmap</code> to use for the <code>Texture</code>.
     * @param bitmap - The bitmap.
     */
    public void setBitmap(Bitmap bitmap)
    {
        createBitmap(bitmap);
    }

    /**
     * Sets the <code>Bitmap</code> to use for the <code>Texture</code> scaled to the specified
     * width and height.
     * @param bitmap - The bitmap.
     * @param w - The width.
     * @param h - The height.
     */
    public void setBitmap(Bitmap bitmap, float w, float h)
    {
        mBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (int)w, (int)h,
                true);
        mLoaded = true;
        mUploaded = false;
    }

    private void createBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
        mLoaded = true;
        mUploaded = false;
    }

    /**
     * Sets the listener for <code>Texture</code> events.
     * @param listener - The listener.
     */
    public void setListener(TextureListener listener)
    {
        mListener = listener;
    }

    /**
     * Checks if the <code>Texture</code> has been uploaded to GL.
     * @return - True if the <code>Texture</code> has been uploaded.
     */
    public boolean isUploaded()
    {
        return mUploaded;
    }

    /**
     * Checks if the <code>Texture</code> is slated for removal.
     * @return - True if the <code>Texture</code> is slated for removal.
     */
    boolean isPendingRemove()
    {
        return mPendingRemove;
    }

    /**
     * Sets the <code>Texture</code> to be removed (unloaded from GL).
     */
    public void unload()
    {
        mPendingRemove = true;
    }

    /**
     * Renders the view to a <code>Bitmap</code> and uploads to GL.
     * @param view - The view.
     */
    public void setView(View view)
    {
        mView = new WeakReference<>(view);
        if (null != view) {
            getViewBitmap();
        }
        mUploaded = false;
    }

    private void getViewBitmap()
    {
        if (null == mHandler)
        {
            mHandler = new Handler(Looper.getMainLooper());
        }

        mHandler.removeCallbacks(mRenderViewRunnable);
        mHandler.postDelayed(mRenderViewRunnable, 10);
    }

    private void loadTexture(Context context)
    {
        //Get the texture from the Android resource directory
        if ((!mLoaded) || (TextureMgr.getInstance().findGLById(mTextureId) < 0))
        {
            if (null == mBitmap)
                loadBitmapTexture(context, mTextureId);
        }
    }

    void uploadGLTexture()
    {
        if ((mLoaded) && (null != mBitmap) && (!mUploaded))
        {
            loadGLTexture();
        }
    }

    private void loadBitmapTexture(Context context, final int textureId)
    {
        try
        {
            // We need to flip the textures vertically:
            Matrix flip = new Matrix();
            //flip.postScale(1f, -1f);

            // This will tell the BitmapFactory to not scale based on the device's pixel
            // density:
            // (Thanks to Matthew Marshall for this bit)
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // Load up, and flip the texture:
            Bitmap temp = BitmapFactory.decodeResource(context.getResources(), textureId, opts);
            mBitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), flip, true);
            if (temp != mBitmap)
                temp.recycle();

            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();

            mLoaded = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void loadGLTexture()
    {
        doLoadGlTexture();
    }

    private void doLoadGlTexture()
    {
        if (null != mBitmap && !mBitmap.isRecycled())
        {
            int[] textures = new int[1];
            //Generate one texture pointer...
            GLES20.glGenTextures(1, textures, 0);
            //...and bind it to our array
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            if (mDoMipMap)
            {
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
            }
            else
            {
                //Create Nearest Filtered Texture
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            }

            //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_ALPHA, GLES20.GL_ALPHA_BITS);

            Bitmap bmp = mBitmap;

            if (mDoMipMap)
            {
                for (
                        int level = 0,
                        height = mBitmap.getHeight(),
                        width = mBitmap.getWidth(); true; level++)
                {
                    // Push the bitmap onto the GPU:
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, mBitmap, 0);

                    // We need to stop when the texture is 1x1:
                    if (height == 1 && width == 1) break;

                    // Resize, and let's go again:
                    width >>= 1;
                    height >>= 1;
                    if (width < 1) width = 1;
                    if (height < 1) height = 1;

                    Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, true);
                    if (bmp != mBitmap)
                        bmp.recycle();

                    bmp = bmp2;
                }

                if (bmp != mBitmap)
                    bmp.recycle();
            }
            else
            {
                // Use the Android GLUtils to specify a two-dimensional texture
                // image from our bitmap
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            }

            mGlTextureId = textures[0];

            mUploaded = true;

            MYQGLRenderer.checkGlError("doLoadGlTexture");

            if (null != mListener)
            {
                mListener.onUpload();
            }
        }
        else
        {
            //Timber.e("loadGLTexture NULL bitmap" + " Res:" + String.format("%08x", mTextureId));
        }
    }

    void doUnload()
    {
        int[] textures = new int[] {mGlTextureId};
        GLES20.glDeleteTextures(1, textures, 0);
        if (null != mBitmap)
        {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
