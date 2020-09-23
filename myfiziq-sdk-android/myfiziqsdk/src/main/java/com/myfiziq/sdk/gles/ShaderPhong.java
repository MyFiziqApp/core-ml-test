package com.myfiziq.sdk.gles;

import android.content.Context;
import android.opengl.GLES20;

/**
 * A <code>Shader</code> class for handling shaders with textures.
 */
public class ShaderPhong extends ShaderTexture
{
    public int uLightPos;
    public int uShininess;
    public int uAmbient;
    public int uDiffuse;
    public int uSpecular;

    public Vector3D mLightPos = new Vector3D(0.0f, 3.3f, -6.0f);

    // This needs to be a very very low value. 0 will make it black on the "ZTE Blade Q Lux"
    public float mShininess = 0.01f;

    public float mAmbient = 0.6f;
    public float mDiffuse = 0.86f;
    public float mSpecular = 0.01f;

    /**
     * Creates a new <code>ShaderTexture</code> instance.
     * @param context - Context for accessing resources.
     * @param vert - Assets filename of the Vertices shader.
     * @param frag - Assets filename of the Fragment shader.
     */
    public ShaderPhong(Context context, String vert, String frag)
    {
        super(context, vert, frag);
    }

    @Override
    void bindProgDefault()
    {
        super.bindProgDefault();

        uLightPos = GLES20.glGetUniformLocation(mProgId, "uLightPos");
        uShininess = GLES20.glGetUniformLocation(mProgId, "uShininess");
        uAmbient = GLES20.glGetUniformLocation(mProgId, "uAmbient");
        uDiffuse = GLES20.glGetUniformLocation(mProgId, "uDiffuse");
        uSpecular = GLES20.glGetUniformLocation(mProgId, "uSpecular");
    }

    @Override
    public void updateShader(Camera camera, float[] mMatrix, float[] mvpMatrix)
    {
        super.updateShader(camera, mMatrix, mvpMatrix);
        GLES20.glUniform3f(uLightPos, mLightPos.mX, mLightPos.mY, mLightPos.mZ);
        GLES20.glUniform1f(uShininess, mShininess);
        GLES20.glUniform1f(uAmbient, mAmbient);
        GLES20.glUniform1f(uDiffuse, mDiffuse);
        GLES20.glUniform1f(uSpecular, mSpecular);
    }
}
