package com.myfiziq.sdk.gles;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * A basic mesh class for rendering a set of vertices.
 * <br>
 * Handles Position, Scale & Rotation.
 * <br>
 * Draws with the given <code>Shader</code>.
 */
public class Mesh
{
    private Vector3D mPosition;
    private Vector3D mScale;
    private Vector3D mAngle;

    private volatile MeshCache mMeshCache = null;
    private MatrixStack mModelMatrix = new MatrixStack(0);
    private MatrixStack mMVPMatrix = new MatrixStack(0);
    private float mColor[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };
    private Texture mTexture = null;
    private Shader mShader;
    private boolean mMatrixDirty = true;

    /**
     * Constructs a new <code>Mesh</code> instance.
     */
    public Mesh()
    {
        reset();
    }

    /**
     * Draws the <code>Mesh</code> using the current view parameters.
     * @param camera - The perspective to use for drawing.
     */
    public synchronized void draw(Camera camera)
    {
        if (mMatrixDirty)
        {
            applyToMatrix();
            mMatrixDirty = false;
        }

        if (null != mShader && null != mMeshCache && mMeshCache.isValid())
        {
            mMVPMatrix.matrixMultiply(camera.getVPMatrix(), mModelMatrix);
            camera.setMVPNormal(mMVPMatrix.get());
            mShader.setShader(camera, mModelMatrix.get(), mMVPMatrix.get());


            ByteBuffer verticesBuffer = mMeshCache.getVerticesBuffer();

            // Verts
            verticesBuffer.position(0);
            GLES20.glVertexAttribPointer(mShader.mPositionId, 3, GLES20.GL_FLOAT, false,
                    8*4, verticesBuffer);
            GLES20.glEnableVertexAttribArray(mShader.mPositionId);

            // Normals
            verticesBuffer.position(3*4);
            GLES20.glVertexAttribPointer(mShader.mNormalId, 3, GLES20.GL_FLOAT, false,
                    8*4, verticesBuffer);
            GLES20.glEnableVertexAttribArray(mShader.mNormalId);

            if (mShader instanceof ShaderTexture)
            {
                ShaderTexture shaderTexture = (ShaderTexture) mShader;

                // Text Cord
                verticesBuffer.position(6*4);
                GLES20.glVertexAttribPointer(shaderTexture.getTextureCoordinateId(), 2, GLES20.GL_FLOAT, false,
                        8 * 4, verticesBuffer);
                GLES20.glEnableVertexAttribArray(shaderTexture.getTextureCoordinateId());

                if (null != mTexture)
                {
                    if (!mTexture.isUploaded())
                    {
                        mTexture.loadGLTexture();
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture.mGlTextureId);
                    mShader.setTexture0(0);
                }
            }

            // Set color for drawing the triangle
            GLES20.glUniform4fv(mShader.mColorId, 1, mColor, 0);


            ByteBuffer indicesBuffer = mMeshCache.getIndicesBuffer();

            // Draw the triangle
            indicesBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBuffer.capacity()/4, GLES20.GL_UNSIGNED_INT, indicesBuffer);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mShader.mPositionId);
        }
    }

    /**
     * Sets the passed texture as the texture to use for texture mapping.
     * @param texture
     */
    public void setTexture(Texture texture) {
        mTexture = texture;
    }

    /**
     * Resets the Position, Scale and Angle back to defaults.
     */
    public void reset()
    {
        mPosition = new Vector3D(0, 0, 0);
        mScale = new Vector3D(1, 1, 1);
        mAngle = new Vector3D(0f, 0f, 0f);
        mModelMatrix.matrixLoadIdentity();
    }

    private void applyToMatrix()
    {
        mModelMatrix.matrixLoadIdentity();
        mModelMatrix.translate(mPosition.mX, mPosition.mY, mPosition.mZ);
        mModelMatrix.rotate(mAngle.mX, 1f, 0f, 0f);
        mModelMatrix.rotate(mAngle.mY, 0f, 1f, 0f);
        mModelMatrix.rotate(mAngle.mZ, 0f, 0f, 1f);
        mModelMatrix.scale(mScale.mX, mScale.mY, mScale.mZ);
    }

    /**
     * Scale the mesh by the passed value.
     * @param val
     */
    public void scale(float val)
    {
        mScale.scale(val);
        mMatrixDirty = true;
    }

    /**
     * Translate the mesh by the passed values.
     * @param x
     * @param y
     * @param z
     */
    public void translate(float x, float y, float z)
    {
        mPosition.add(x, y, z);
        mMatrixDirty = true;
    }

    /**
     * Rotate the mesh by the passed values.
     * @param x
     * @param y
     * @param z
     */
    public void rotate(float x, float y, float z)
    {
        mAngle.add(x, y, z);
        mMatrixDirty = true;
    }

    public void setRotate(float x, float y, float z)
    {
        mAngle.set(x, y, z);
        mMatrixDirty = true;
    }

    public Vector3D getAngle()
    {
        return mAngle;
    }

    public synchronized void setMeshData(MeshCache meshCache)
    {
        if (null != mMeshCache)
        {
            mMeshCache.release();
        }
        mMeshCache = meshCache;
    }

    public synchronized void release()
    {
        Timber.d("[Mesh::release]");
        if (null != mMeshCache)
        {
            mMeshCache.release();
        }
    }

    public Shader getShader()
    {
        return mShader;
    }

    public void setShader(Shader shader)
    {
        this.mShader = shader;
    }
}
