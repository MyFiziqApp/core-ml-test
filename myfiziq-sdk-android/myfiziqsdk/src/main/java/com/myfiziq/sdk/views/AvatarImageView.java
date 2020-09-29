package com.myfiziq.sdk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.gles.AvatarMesh;
import com.myfiziq.sdk.gles.Camera;
import com.myfiziq.sdk.gles.EglCore;
import com.myfiziq.sdk.gles.OffscreenSurface;
import com.myfiziq.sdk.gles.ShaderPhong;
import com.myfiziq.sdk.gles.Texture;
import com.myfiziq.sdk.util.GlobalContext;

import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class AvatarImageView extends ImageView
{
    //AvatarOffscreen mOffscreen = new AvatarOffscreen();
    int mWidth = 512;
    int mHeight = 512;

    public AvatarImageView(Context context)
    {
        super(context);
    }

    public AvatarImageView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public AvatarImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
//    {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        //mWidth = getMeasuredWidth();
//        //mHeight = getMeasuredHeight();
//    }

    public void render(final ModelAvatar avatar)
    {
        Thread thread = new Thread(() ->
        {
            EglCore eglCore = null;
            OffscreenSurface surface = null;
            try
            {
                Looper.prepare();
                final Handler handler = new Handler(Looper.myLooper());

                final Camera camera = new Camera();
                eglCore = new EglCore(null, 0);
                surface = new OffscreenSurface(eglCore, mWidth, mHeight);
                surface.makeCurrent();

                final OffscreenSurface fsurface = surface;

                AvatarMesh rendmesh = new AvatarMesh(
                        avatar,
                        new ShaderPhong(GlobalContext.getContext(), "Phong.vert", "Phong.frag"),
                        new AvatarMesh.AvatarMeshListener()
                        {
                            @Override
                            public void meshLoading()
                            {
                            }

                            @Override
                            public void meshLoadComplete(final AvatarMesh mesh)
                            {
                                handler.post(() ->
                                {
                                    Texture texture = new Texture(GlobalContext.getContext(), R.drawable.skin_grey);
                                    mesh.setTexture(texture);

                                    //Texture2dProgram texProgram =
                                    //        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
                                    //Drawable2d rectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
                                    //Sprite2d rect = new Sprite2d(rectDrawable);
                                    GLES20.glClearColor(1f, 1f, 1f, 0f);
                                    GLES20.glClearDepthf(1.0f);

                                    GLES20.glDisable(GLES20.GL_DITHER);
                                    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                                    GLES20.glDepthFunc(GLES20.GL_LESS);
                                    GLES20.glDepthMask(true);

                                    GLES20.glEnable(GLES20.GL_TEXTURE_2D);
                                    GLES20.glFrontFace(GL10.GL_CCW);
                                    GLES20.glCullFace(GL10.GL_BACK);
                                    GLES20.glEnable(GL10.GL_CULL_FACE);
                                    GLES20.glEnable(GL10.GL_SMOOTH);

                                    GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); //GL_DST_ALPHA
                                    GLES20.glDisable(GL10.GL_BLEND);

                                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                                    GLES20.glViewport(0, 0, mWidth, mHeight);

                                    camera.perspective(
                                            45f,
                                            mWidth / mHeight,
                                            1f, 1000f);

                                    mesh.draw(camera);

                                    GLES20.glFinish();
                                    fsurface.swapBuffers();

                                    final Bitmap bitmap = fsurface.getFrame();
                                    Handler ui = new Handler(Looper.getMainLooper());
                                    ui.post(() ->setImageBitmap(bitmap));
                                    Looper.myLooper().quitSafely();
                                });
                            }
                        });

                Looper.loop();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
            finally
            {
                if (surface != null)
                {
                    surface.release();
                }
                if (eglCore != null)
                {
                    eglCore.release();
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

//    @Override
//    protected void onDetachedFromWindow()
//    {
//        super.onDetachedFromWindow();
//    }
}
