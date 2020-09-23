package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.gles.AvatarMesh;
import com.myfiziq.sdk.gles.ShaderPhong;
import com.myfiziq.sdk.util.UiUtils;

/**
 * @hide
 */
public class AvatarViewSpinner extends FrameLayout implements AvatarMesh.AvatarMeshListener
{
    AvatarView avatarView;
    View progressBar;
    AvatarMesh avatarMesh;
    boolean scaleToFit = false;
    float heightCm;


    // This relates screen width to the height of an avatar that would fit the view perfectly.
    static final float IDEAL_HEIGHT_FACTOR = 0.4f;

    // This relates a change in Z position to the resulting change in perceived avatar height.
    // ie. moving an avatar towards or away from camera by this amount changes perceived height by 1 centimeter
    static final float SCALE_FACTOR = 0.0097f;

    // This relates the change in an avatars perceived vertical position per centimeter change in Z position
    static final float VERTICAL_FACTOR = 0.0037f;

    // This constant Y translation vertically centers any avatar after height-dependant scaling has occured.
    static final float VERTICAL_OFFSET = 0.04f;

    public AvatarViewSpinner(Context context)
    {
        super(context);
        init(context);
    }

    public AvatarViewSpinner(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public AvatarViewSpinner(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        avatarView = findViewById(R.id.avatarView);
        progressBar = findViewById(R.id.progress);
    }

    public int getLayout()
    {
        return R.layout.view_avatar_spinner;
    }

    public AvatarView getAvatarView()
    {
        return avatarView;
    }

    public AvatarMesh getAvatarMesh() { return avatarMesh; }

    @Override
    public void meshLoading()
    {
        UiUtils.setViewVisibility(progressBar, VISIBLE);
    }

    @Override
    public void meshLoadComplete(AvatarMesh mesh)
    {
        UiUtils.setViewVisibility(progressBar, INVISIBLE);
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null)
        {
            Display display = windowManager.getDefaultDisplay();
            if (display != null)
            {
                int rotation = display.getRotation();
                boolean landscape = (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);
                if (scaleToFit && !landscape)
                {
                    DisplayMetrics metrics = new DisplayMetrics();
                    windowManager.getDefaultDisplay().getMetrics(metrics);
                    float idealHeight = (metrics.widthPixels / metrics.density) * IDEAL_HEIGHT_FACTOR;
                    float heightDelta = idealHeight - heightCm;
                    mesh.translate(
                            0,
                            (heightDelta * VERTICAL_FACTOR) + VERTICAL_OFFSET,
                            heightDelta * SCALE_FACTOR
                    );
                }
            }
        }
        avatarView.setMesh(mesh);
    }

    public void setScaleToFit(boolean scaleToFit)
    {
        this.scaleToFit = scaleToFit;
    }

    public void setModel(ModelAvatar model)
    {
            avatarView.setMesh(null);
            heightCm = (float) model.getHeight().getValueInCm();

            avatarMesh = new AvatarMesh(
                    model,
                    new ShaderPhong(getContext(), "Phong.vert", "Phong.frag"),
                    this);
        }
    }
