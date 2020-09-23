package com.myfiziq.sdk.gles;

import android.opengl.Matrix;

/**
 * A basic Camera class for handling 3D projection.
 */
public class Camera
{
    private final MatrixStack mProjectionMatrix = new MatrixStack(0);
    private final float[] mViewMatrix = new float[16];
    private final float[] mVPMatrix = new float[16];
    private final float[] mVIMatrix = new float[16];
    private final float[] mMVPNormalMatrix = new float[16];

    /**
     * Sets up the view as a frustum projection.
     * @param width
     * @param height
     */
    public void frustum(int width, int height)
    {
        float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix.get(), 0, -ratio, ratio, -1, 1, 1, 1000);
        //Matrix.orthoM(mProjectionMatrix, 0, 0f, width, height, 0.0f, 0, 50);
    }

    /**
     * Sets up the view as a perspective projection.
     * @param angle
     * @param aspect
     * @param near
     * @param far
     */
    public void perspective(float angle, float aspect, float near, float far)
    {
        mProjectionMatrix.matrixLoadIdentity();
        mProjectionMatrix.perspective(angle, aspect, near, far);
        setLookAt(0f, 0f, 2f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    /**
     * Sets up the view to look at the specified location in 3D space.
     * @param eyeX
     * @param eyeY
     * @param eyeZ
     * @param centerX
     * @param centerY
     * @param centerZ
     * @param upX
     * @param upY
     * @param upZ
     */
    public void setLookAt(float eyeX, float eyeY, float eyeZ,
                          float centerX, float centerY, float centerZ,
                          float upX, float upY, float upZ)
    {
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);

        Matrix.invertM(mVIMatrix, 0, mViewMatrix, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix.get(), 0, mViewMatrix, 0);
    }

    /**
     * Gets the current view matrix.
     * @return float matrix.
     */
    public float[] getViewMatrix()
    {
        return mViewMatrix;
    }

    /**
     * Gets the current inverted view matrix.
     * @return float matrix.
     */
    public float[] getVIMatrix()
    {
        return mVIMatrix;
    }

    /**
     * Gets the current projection matrix.
     * @return float matrix.
     */
    public float[] getProjectionMatrix()
    {
        return mProjectionMatrix.get();
    }

    /**
     * Gets the current View * Projection matrix.
     * @return float matrix.
     */
    public float[] getVPMatrix()
    {
        return mVPMatrix;
    }

    /**
     *
     * @return float matrix.
     */
    public float[] getMVPNormalMatrix()
    {
        return mMVPNormalMatrix;
    }

    /**
     *
     * @param mvpMatrix
     */
    public void setMVPNormal(float[] mvpMatrix)
    {
        Matrix.invertM(mMVPNormalMatrix, 0, mvpMatrix, 0);
        Matrix.transposeM(mMVPNormalMatrix, 0, mMVPNormalMatrix, 0);
    }
}
