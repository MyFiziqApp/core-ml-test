package com.myfiziq.sdk.gles;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * A Class for loading and handling a vertex and fragment shader for 3D rendering.
 */
public class Shader
{
    int mVertId = 0;
    int mFragId = 0;
    int mProgId = 0;

    public int mMVPMatrix;
    public int mVPMatrix;
    public int mVIMatrix;
    public int mMMatrix;
    public int mMVPNormalMatrix;

    public int mPositionId;
    public int mNormalId;
    public int mColorId;

    private String mVertResName;
    private String mFragResName;
    private boolean mShaderCompiled = false;

    private WeakReference<Context> mContext;

    /**
     * Creates a new <code>Shader</code> instance.
     * @param context - Context for accessing resources.
     * @param vert - Assets filename of the Vertices shader.
     * @param frag - Assets filename of the Fragment shader.
     */
    public Shader(Context context, String vert, String frag)
    {
        mContext = new WeakReference<>(context);

        mVertResName = vert;
        mFragResName = frag;

        //initShader();
    }

    private void initShader()
    {
        if (!mShaderCompiled)
        {
            mVertId = loadAndCompileShaderCode(GLES20.GL_VERTEX_SHADER, getShaderCode(mVertResName));
            mFragId = loadAndCompileShaderCode(GLES20.GL_FRAGMENT_SHADER, getShaderCode(mFragResName));
            mProgId = linkShaderProgram(mVertId, mFragId);

            bindProgDefault();

            mShaderCompiled = true;
        }
    }

    void resetShader()
    {
        mShaderCompiled = false;
    }

    void setShader(Camera camera, float[] mMatrix, float[] mvpMatrix)
    {
        if (!mShaderCompiled)
        {
            initShader();
        }

        GLES20.glUseProgram(mProgId);
        updateShader(camera, mMatrix, mvpMatrix);
    }

    public void updateShader(Camera camera, float[] mMatrix, float[] mvpMatrix)
    {
        GLES20.glUniformMatrix4fv(mVPMatrix, 1, false, camera.getVPMatrix(), 0);
        GLES20.glUniformMatrix4fv(mVIMatrix, 1, false, camera.getVIMatrix(), 0);
        GLES20.glUniformMatrix4fv(mMVPNormalMatrix, 1, false, camera.getMVPNormalMatrix(), 0);
        GLES20.glUniformMatrix4fv(mMMatrix, 1, false, mMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrix, 1, false, mvpMatrix, 0);
    }

    void bindProgDefault()
    {
        mVIMatrix = GLES20.glGetUniformLocation(mProgId, "uVIMatrix");
        mVPMatrix = GLES20.glGetUniformLocation(mProgId, "uVPMatrix");
        mMVPMatrix = GLES20.glGetUniformLocation(mProgId, "uMVPMatrix");
        mMMatrix = GLES20.glGetUniformLocation(mProgId, "uMMatrix");
        mMVPNormalMatrix = GLES20.glGetUniformLocation(mProgId, "uMVPNormalMatrix");
        mPositionId = GLES20.glGetAttribLocation(mProgId, "aPosition");
        mNormalId = GLES20.glGetAttribLocation(mProgId, "aNormal");
        mColorId = GLES20.glGetUniformLocation(mProgId, "uColor");
    }

    /**
     * Sets the GL Texture id to use for texture mapping.
     * @param i
     */
    public void setTexture0(int i)
    {
    }

    private int linkShaderProgram(int vertex, int fragment)
    {
        int shaderBinding = 0;
        int shaderProgram = GLES20.glCreateProgram();

        if (shaderProgram == 0)
        {
            return 0;
        }
        GLES20.glAttachShader(shaderProgram, vertex);
        GLES20.glAttachShader(shaderProgram, fragment);

        GLES20.glBindAttribLocation(shaderProgram, shaderBinding++, "aPosition");
        GLES20.glBindAttribLocation(shaderProgram, shaderBinding++, "aNormal");
        GLES20.glBindAttribLocation(shaderProgram, shaderBinding++, "aTexCoord");
        GLES20.glBindAttribLocation(shaderProgram, shaderBinding++, "aColor");

        GLES20.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE)
        {
            Timber.e(" Shader link failed: %s", GLES20.glGetShaderInfoLog(shaderProgram));
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }

        return shaderProgram;
    }

    private int loadAndCompileShaderCode(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0)
        {
            //Timber.e(" Shader create failed!");
            return 0;
        }

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0)
        {
            //Timber.e(" Shader compile failed: " +
            //        GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
        }

        return shader;
    }

    private String getShaderCode(String name)
    {
        Context context = mContext.get();
        InputStream is;
        StringBuilder stringBuilder = new StringBuilder();

        try
        {
            is = context.getAssets().open(name);
            BufferedReader bufferedBuilder = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = bufferedBuilder.readLine()) != null)
            {
                stringBuilder.append(line + '\n');
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
