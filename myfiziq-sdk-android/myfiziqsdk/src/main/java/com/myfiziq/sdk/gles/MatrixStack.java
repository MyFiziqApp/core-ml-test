package com.myfiziq.sdk.gles;

import android.annotation.SuppressLint;

/**
 * Basic matrix operations and state handling.
 */
public class MatrixStack
{
	public float[] mMatrix = new float[16];
	private float[][] mSaveStack; 
	
	private int mSaveSize = 0;
	private int mSaveIx = 0;

	public MatrixStack(int saveSize)
	{
		mSaveSize = saveSize;
		mSaveStack = new float[mSaveSize][16];
	}

	public void scale(float sx, float sy, float sz)
	{
		android.opengl.Matrix.scaleM(mMatrix, 0, sx, sy, sz);
	}

	public void translate(float tx, float ty, float tz)
	{
		android.opengl.Matrix.translateM(mMatrix, 0, tx, ty, tz);
	}

	public void rotate(float angle, float x, float y, float z)
	{
		android.opengl.Matrix.rotateM(mMatrix, 0, angle, x, y, z);
	}

	public void frustum(float left, float right, float bottom, float top,
			float nearZ, float farZ)
	{
		android.opengl.Matrix.frustumM(mMatrix, 0, left, right, bottom, top,
				nearZ, farZ);
	}

	public void perspective(float fovy, float aspect, float nearZ, float farZ)
	{
		float frustumW, frustumH;

		frustumH = (float) Math.tan(fovy / 360.0 * Math.PI) * nearZ;
		frustumW = frustumH * aspect;

		frustum(-frustumW, frustumW, -frustumH, frustumH, nearZ, farZ);
	}

	public void ortho(float left, float right, float bottom, float top,
			float nearZ, float farZ)
	{
		android.opengl.Matrix.orthoM(mMatrix, 0, left, right, bottom, top, nearZ, farZ);
	}

	public void matrixMultiply(float[] srcA, float[] srcB) 
	{
		android.opengl.Matrix.multiplyMM(mMatrix, 0, srcA, 0, srcB, 0);
	}

	public void matrixMultiply(float[] srcA, MatrixStack srcB)
	{
		android.opengl.Matrix.multiplyMM(mMatrix, 0, srcA, 0, srcB.get(), 0);
	}

	public void matrixMultiply(MatrixStack srcA, MatrixStack srcB)
	{
		android.opengl.Matrix.multiplyMM(mMatrix, 0, srcA.get(), 0, srcB.get(), 0);
	}

	public void matrixLoadIdentity()
	{
		android.opengl.Matrix.setIdentityM(mMatrix, 0);
	}

	public float[] get()
	{
		return mMatrix;
	}
	
	public void getPosition(Vector3D dest)
	{
		dest.mX = mMatrix[12];
		dest.mY = mMatrix[13];
		dest.mZ = mMatrix[14];
	}

	public void push() throws Exception 
	{
		if (mSaveIx < mSaveSize)
		{
			System.arraycopy(mMatrix, 0, mSaveStack[mSaveIx], 0, 16);
			mSaveIx++;
		}
		else
			throw new Exception("Failed to Push to Maxtrix Stack - Overflow");
	}

	public void pop() 
	{
		if (mSaveIx > 0)
		{
			mSaveIx--;
			System.arraycopy(mSaveStack[mSaveIx], 0, mMatrix, 0, 16);
		}
	}

	public void copy(MatrixStack src)
	{
		System.arraycopy(src.mMatrix, 0, mMatrix, 0, 16);
	}
	
	@SuppressLint("DefaultLocale")
	public String toString()
	{
		return 
		
		String.format("[Matrix] % 3.3f % 3.3f % 3.3f % 3.3f\n",		
				mMatrix[0],
				mMatrix[4],
				mMatrix[8],
				mMatrix[12]
		) +
		
		String.format("[Matrix] % 3.3f % 3.3f % 3.3f % 3.3f\n",		
				mMatrix[1],
				mMatrix[5],
				mMatrix[9],
				mMatrix[13]
		) +

		String.format("[Matrix] % 3.3f % 3.3f % 3.3f % 3.3f\n",		
				mMatrix[2],
				mMatrix[6],
				mMatrix[10],
				mMatrix[14]
		) +

		String.format("[Matrix] % 3.3f % 3.3f % 3.3f % 3.3f\n",		
				mMatrix[3],
				mMatrix[7],
				mMatrix[11],
				mMatrix[15]
		);
	}
}
