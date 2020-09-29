package com.myfiziq.sdk.gles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.FactoryAvatar;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.util.GlobalContext;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * <code>Mesh</code> handling for an Avatar Model.
 */
public class AvatarMesh extends Mesh implements ModelAvatar.MeshReadyListener
{
    private WeakReference<AvatarMeshListener> mListener;

    /**
     * Callback methods for notification of mesh load events.
     */
    public interface AvatarMeshListener
    {
        /**
         * Called if a background load occurs.
         */
        void meshLoading();

        /**
         * Called when mesh loading is complete.
         *
         * @param mesh
         */
        void meshLoadComplete(AvatarMesh mesh);
    }

    /**
     * Constructs a new avatar mesh for the given avatar.
     *
     * @param avatar   - The avatar.
     * @param shader   - The <code>Shader</code> to use for rendering.
     * @param listener - The listener for mesh load events.
     */
    public AvatarMesh(final ModelAvatar avatar, Shader shader, AvatarMeshListener listener)
    {
        super();

        super.setShader(shader);

        mListener = new WeakReference<>(listener);

        translate(0, -0.75f, 0);
        scale(0.80f);

        int avatarColor = GlobalContext
                .getContext()
                .getResources()
                .getColor(R.color.avatarMeshColor);

        if (SisterColors.getInstance().getAvatarMeshColor() == null)
        {
            setAvatarColor(avatarColor);
        }
        else
        {
            setAvatarColor(SisterColors.getInstance().getAvatarMeshColor());
        }

        if (FactoryAvatar.getInstance().queueAvatarMesh(avatar, this, this))
        {
            if (null != listener)
            {
                listener.meshLoading();
            }
        }
    }

    public void setAvatarColor(int avatarColor)
    {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(
                255,
                Color.red(avatarColor),
                Color.green(avatarColor),
                Color.blue(avatarColor)
        );

        setTexture(new Texture(bitmap));

    }

    public Shader getShader()
    {
        return super.getShader();
    }

    @Override
    public void onMeshReady()
    {
        if (null != mListener)
        {
            AvatarMeshListener l = mListener.get();
            if (null != l)
            {
                Timber.i("Mesh is ready!");
                l.meshLoadComplete(AvatarMesh.this);
            }
        }
    }

    @Override
    public void draw(Camera camera)
    {
        // Create a rotation transformation for the mesh
        //long time = SystemClock.uptimeMillis() % 4000L;
        //float angle = 0.090f * ((int) time);
        //Matrix.setRotateM(mModelMatrix, 0, angle, 0, 1, 0);

        super.draw(camera);
    }
}
