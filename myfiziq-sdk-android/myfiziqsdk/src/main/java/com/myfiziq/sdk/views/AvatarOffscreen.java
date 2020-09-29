package com.myfiziq.sdk.views;

import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
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

/**
 * @hide
 */

public class AvatarOffscreen
{
    int WIDTH = 1024;
    int HEIGHT = 1024;

    public AvatarOffscreen()
    {
    }

    public void render(final ModelAvatar avatar, final ImageView imageView)
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
                surface = new OffscreenSurface(eglCore, WIDTH, HEIGHT);
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
                                    GLES20.glClearColor(1f, 1f, 1f, 1f);
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

                                    GLES20.glViewport(0, 0, WIDTH, HEIGHT);

                                    camera.perspective(
                                            45f,
                                            WIDTH / HEIGHT,
                                            1f, 1000f);

                                    mesh.draw(camera);

                                    GLES20.glFinish();
                                    fsurface.swapBuffers();

                                    if (null != imageView)
                                    {
                                        imageView.setImageBitmap(fsurface.getFrame());
                                    }

                                    //avatar.release();
                                    /*
                                    try
                                    {
                                        fsurface.saveFrame(new File(Environment.getExternalStorageDirectory(),
                                                avatar.attemptId + ".png"));
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    */

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
}
