package com.myfiziq.sdk.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.gles.Shader;
import com.myfiziq.sdk.gles.ShaderPhong;
import com.myfiziq.sdk.gles.Vector3D;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.DrawableView;

import java.util.Date;

import androidx.annotation.Nullable;

/**
 * @hide
 */
public class DebugAvatarActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener
{
    private AvatarViewSpinner mAvatar;
    private AvatarLayout mAvatarLayout;
    private TextView mColor;
    private SeekBar mShiny;
    private TextView mShinyText;
    private SeekBar mAmbient;
    private TextView mAmbientText;
    private SeekBar mDiffuse;
    private TextView mDiffuseText;
    private SeekBar mSpecular;
    private TextView mSpecularText;
    private DrawableView mLight;
    private TextView mLightText;
    private SeekBar mLightZ;

    Shader mShader = null;

    public float mFShininess = 2.0f;
    public float mFAmbient = 0.5f;
    public float mFDiffuse = 0.8f;
    public float mFSpecular = 1.0f;
    Vector3D mLightPos = new Vector3D();
    int mAvatarColor = 0;
    Paint mPaint;

    View.OnTouchListener mLightTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_avatar);
        initView();
        AsyncHelper.run(
                this::retrieveModel,
                avatar ->
                {
                    if (avatar != null)
                    {
                        renderAvatar(avatar);
                    }
                },
                true);
    }

    private ModelAvatar retrieveModel()
    {
        // Just get the first avatar.
        return ORMTable.getModel(ModelAvatar.class, "");
    }

    /**
     * Renders the avatar on the screen.
     *
     * @param model The model to render.
     */
    protected void renderAvatar(@Nullable ModelAvatar model)
    {
        if (model != null)
        {
            mAvatar.setModel(model);
            mShader = mAvatar.getAvatarMesh().getShader();

            if (mShader instanceof ShaderPhong)
            {
                ShaderPhong phong = (ShaderPhong)mShader;
                mFAmbient = phong.mAmbient;
                mFDiffuse = phong.mDiffuse;
                mFSpecular = phong.mSpecular;
                mFShininess = phong.mShininess;
                mAmbient.setProgress((int)(mFAmbient*100));
                mDiffuse.setProgress((int)(mFDiffuse*100));
                mSpecular.setProgress((int)(mFSpecular*100));
                mShiny.setProgress((int)(mFShininess*100));
            }
        }
    }

    private void updateLight()
    {
        mLight.invalidate();
        mLightText.setText(String.format("Light:%.1f,%.1f,%.1f", mLightPos.mX, mLightPos.mY, mLightPos.mZ));
        if (mShader instanceof ShaderPhong)
        {
            ShaderPhong phong = (ShaderPhong)mShader;
            phong.mLightPos.mX = mLightPos.mX;
            phong.mLightPos.mY = mLightPos.mY;
            phong.mLightPos.mZ = mLightPos.mZ;
        }
        if (null != mAvatar.getAvatarView())
        {
            mAvatar.getAvatarView().requestRender();
        }
    }

    private float getCenterXOffset(View view, float x)
    {
        return (x - (view.getWidth()/2f))/10f;
    }

    private float getCenterYOffset(View view, float y)
    {
        return (y - (view.getHeight()/2f))/10f;
    }

    private float getXCenterOffset(View view, float x)
    {
        return ((view.getWidth()/2f))+(x*10);
    }

    private float getYCenterOffset(View view, float y)
    {
        return (view.getHeight()/2f)+(y*10);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView()
    {
        mAvatar = findViewById(R.id.avatar);
        mAvatarLayout = findViewById(R.id.avatarLayout);
        mAvatarLayout.setScaleEnabled(true);
        mAvatarLayout.setTranslateEnabled(true);

        mColor = findViewById(R.id.color);
        mColor.setOnClickListener(this);

        mShiny = findViewById(R.id.shiny);
        mShiny.setOnSeekBarChangeListener(this);
        mShinyText = findViewById(R.id.shinyText);

        mAmbient = findViewById(R.id.ambient);
        mAmbient.setOnSeekBarChangeListener(this);
        mAmbientText = findViewById(R.id.ambientText);

        mDiffuse = findViewById(R.id.diffuse);
        mDiffuse.setOnSeekBarChangeListener(this);
        mDiffuseText = findViewById(R.id.diffuseText);

        mSpecular = findViewById(R.id.specular);
        mSpecular.setOnSeekBarChangeListener(this);
        mSpecularText = findViewById(R.id.specularText);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);

        mLight = findViewById(R.id.light);
        mLightText = findViewById(R.id.lightText);
        mLightZ = findViewById(R.id.lightZ);
        mLightZ.setOnSeekBarChangeListener(this);

        mLight.setDraw(canvas -> {
            float x = getXCenterOffset(mLight, mLightPos.mX);
            float y = getYCenterOffset(mLight, mLightPos.mY);
            float cx = canvas.getWidth()/2f;
            float cy = canvas.getHeight()/2f;
            canvas.drawCircle(x, y, 15, mPaint);
            canvas.drawLine(cx, 0, cx, canvas.getHeight(), mPaint);
            canvas.drawLine(0, cy, canvas.getWidth(), cy, mPaint);
        });

        mLightTouchListener = (v, e) ->
        {
            float x = getCenterXOffset(mLight, e.getX());
            float y = getCenterYOffset(mLight, e.getY());

            switch (e.getAction() & e.getActionMasked())
            {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    updateLight();
                    break;

                case MotionEvent.ACTION_MOVE:
                    mLightPos.mX = x;
                    mLightPos.mY = y;
                    updateLight();
                    break;
            }
            return true;
        };
        mLight.setOnTouchListener(mLightTouchListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        int id = seekBar.getId();
        float fProgress = progress/100.0f;

        if (R.id.shiny == id)
        {
            mShinyText.setText(String.valueOf(fProgress));
            mFShininess = fProgress;
        }
        else if (R.id.ambient == id)
        {
            mAmbientText.setText(String.valueOf(fProgress));
            mFAmbient = fProgress;
        }
        else if (R.id.diffuse == id)
        {
            mDiffuseText.setText(String.valueOf(fProgress));
            mFDiffuse = fProgress;
        }
        else if (R.id.specular == id)
        {
            mSpecularText.setText(String.valueOf(fProgress));
            mFSpecular = fProgress;
        }
        else if (R.id.lightZ == id)
        {
            float mid = mLightZ.getMax()/2f;
            mLightPos.mZ = (progress-mid)/10f;
            updateLight();
        }

        if (mShader instanceof ShaderPhong)
        {
            ShaderPhong phong = (ShaderPhong)mShader;
            phong.mAmbient = mFAmbient;
            phong.mDiffuse = mFDiffuse;
            phong.mSpecular = mFSpecular;
            phong.mShininess = mFShininess;
        }

        if (null != mAvatar.getAvatarView())
        {
            mAvatar.getAvatarView().requestRender();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {

    }

    public static int getRedInt(int color)
    {
        return ((color & 0x00FF0000) >> 16);
    }

    public static int getGreenInt(int color)
    {
        return ((color & 0x0000FF00) >> 8);
    }

    public static int getBlueInt(int color)
    {
        return ((color & 0x000000FF));
    }

    public static int getAlphaInt(int color)
    {
        return ((color & 0xFF000000) >> 24);
    }

    public static int getInverseColor(int color)
    {
        return Color.argb(255, 255-getRedInt(color), 255-getGreenInt(color), 255-getBlueInt(color));
    }

    public void setAvatarColor(int color)
    {
        mAvatarColor = color;
        mColor.setBackgroundColor(mAvatarColor);
        mColor.setTextColor(getInverseColor(color));
        if (null != mAvatar.getAvatarView())
        {
            mAvatar.getAvatarMesh().setAvatarColor(mAvatarColor);
            mAvatar.getAvatarView().requestRender();
        }
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();

        if (R.id.color == id)
        {
            final UiUtils.PickerColor color = new UiUtils.PickerColor(mAvatarColor);
            UiUtils.showColorDialog(getActivity(), "", color, (dialog, which) ->
            {
                setAvatarColor(color.color);
            });
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mAvatarLayout != null)
        {
            mAvatarLayout.destroy();
            mAvatarLayout = null;
        }

        super.onDestroy();
    }
}
