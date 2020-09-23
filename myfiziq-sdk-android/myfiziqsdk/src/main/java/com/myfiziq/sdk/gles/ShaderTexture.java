package com.myfiziq.sdk.gles;

import android.content.Context;
import android.opengl.GLES20;

/**
 * A <code>Shader</code> class for handling shaders with textures.
 */
public class ShaderTexture extends Shader
{
    private int mTexture0;
    private int mTexCoordId;

    /**
     * Creates a new <code>ShaderTexture</code> instance.
     * @param context - Context for accessing resources.
     * @param vert - Assets filename of the Vertices shader.
     * @param frag - Assets filename of the Fragment shader.
     */
    public ShaderTexture(Context context, String vert, String frag)
    {
        super(context, vert, frag);
    }

    @Override
    void bindProgDefault()
    {
        super.bindProgDefault();

        mTexture0 = GLES20.glGetUniformLocation(mProgId, "texture0");
        mTexCoordId = GLES20.glGetAttribLocation(mProgId, "aTexCoord");
    }

    @Override
    public void setTexture0(int i)
    {
        GLES20.glUniform1i(mTexture0, i);
    }

    public int getTexture()
    {
        return mTexture0;
    }

    public int getTextureCoordinateId()
    {
        return mTexCoordId;
    }
}
