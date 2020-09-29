package com.myfiziq.sdk.gles;

import java.nio.FloatBuffer;

/**
 * A basic 3D vecor class.
 */
public class Vector3D
{
	public float mX;
	public float mY;
	public float mZ;
	
	public Vector3D()
	{
		mX = 0f;
		mY = 0f;
		mZ = 0f;
	}

	public Vector3D(float x, float y, float z)
	{
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Vector3D(Vector3D v3d) 
	{
		mX = v3d.mX;
		mY = v3d.mY;
		mZ = v3d.mZ;
	}

	public void set(float x, float y, float z)
	{
		mX = x;
		mY = y;
		mZ = z;
	}

	public void set(Vector3D v3d) 
	{
		mX = v3d.mX;
		mY = v3d.mY;
		mZ = v3d.mZ;
	}

	/**
     * Computes the cross-product of two vectors q and r and places
     * the result in out. 
     */
    public void cross(Vector3D q, Vector3D r) 
    {
        mX = r.mY * q.mZ - r.mZ * q.mY;
        mY = r.mZ * q.mX - r.mX * q.mZ;
        mZ = r.mX * q.mY - r.mY * q.mX;
    }

    public void add(Vector3D q) 
    {
    	mX += q.mX;
    	mY += q.mY;
    	mZ += q.mZ;
    }

	public void add(float x, float y, float z)
	{
		mX += x;
		mY += y;
		mZ += z;
	}

    public void add(Vector3D q, Vector3D r)
    {
    	mX = q.mX + r.mX;
    	mY = q.mY + r.mY;
    	mZ = q.mZ + r.mZ;
    }

    public void subtract(Vector3D q, Vector3D r) 
    {
    	mX = q.mX - r.mX;
    	mY = q.mY - r.mY;
    	mZ = q.mZ - r.mZ;
    }

    public void scale(float v) 
    {
        mX *= v;
        mY *= v;
        mZ *= v;
    }

    /**
     * Returns the length of a vector, given as three floats.
     */
    public float length() 
    {
        return (float) Math.sqrt(mX * mX + mY * mY + mZ * mZ);
    }

    public float lengthSquared() 
    {
        return (float)(mX * mX + mY * mY + mZ * mZ);
    }
    /**
     * Normalizes the given vector.
     * Vectors with length zero are unaffected.
     */
    public void normalize() 
    {
        float length = length();
        if (length != 0.0f) 
        {
            float norm = 1.0f / length;
            mX *= norm;
            mY *= norm;
            mZ *= norm;
        }
    }
    
    public void multiply(MatrixStack matrix)
    {
    	float x = 
    		mX * matrix.mMatrix[0] + 
    		mY * matrix.mMatrix[4] + 
    		mZ * matrix.mMatrix[8] +
    		     matrix.mMatrix[12];
    	
    	float y = 
    		mX * matrix.mMatrix[1] + 
    		mY * matrix.mMatrix[5] + 
    		mZ * matrix.mMatrix[9] +
		         matrix.mMatrix[13];

    	float z = 
    		mX * matrix.mMatrix[2] + 
    		mY * matrix.mMatrix[6] + 
    		mZ * matrix.mMatrix[10] +
		         matrix.mMatrix[14];

    	//float w = 
    	//	mX * matrix.mMatrix[3] + 
    	//	mY * matrix.mMatrix[7] + 
    	//	mZ * matrix.mMatrix[11] +
		//       matrix.mMatrix[15];
    	
    	mX = x;
    	mY = y;
    	mZ = z;
    }
    
    public static float[] getInverseMatrix(float matrix[], Vector3D eye, Vector3D up, Vector3D position)
    {
       Vector3D tempVector1 = new Vector3D();
       Vector3D tempVector2 = new Vector3D();
       Vector3D tempVector3 = new Vector3D();
       
       // Reset X axis
       tempVector1.mX = 0;
       tempVector1.mY = 0;
       tempVector1.mZ = 0;
  
       // Reset Y axis
       tempVector2.mX = up.mX;
       tempVector2.mY = up.mY;
       tempVector2.mZ = up.mZ;
  
       // Set Z axis to position
       tempVector3.mX = position.mX;
       tempVector3.mY = position.mY;
       tempVector3.mZ = position.mZ;
  
       // Sub eye
       tempVector3.mX -= eye.mX;
       tempVector3.mY -= eye.mY;
       tempVector3.mZ -= eye.mZ;
  
       tempVector3.normalize();
  
       tempVector1.cross(tempVector2, tempVector3);
       tempVector1.normalize();
       tempVector2.cross(tempVector1, tempVector3);
       tempVector2.normalize();
  
       tempVector2.scale(-1f);
  
       matrix[0] = tempVector1.mX;
       matrix[1] = tempVector1.mY;
       matrix[2] = tempVector1.mZ;
       matrix[12] = position.mX;
       matrix[4] = tempVector2.mX;
       matrix[5] = tempVector2.mY;
       matrix[6] = tempVector2.mZ;
       matrix[13] = position.mY;
       matrix[8] = tempVector3.mX;
       matrix[9] = tempVector3.mY;
       matrix[10] = tempVector3.mZ;
       matrix[14] = position.mZ;
       matrix[15] = 1.0f;
  
       return matrix;
    }
    
    public String toString()
    {
    	return String.format("X:% 3.3f Y:% 3.3f Z:% 3.3f", mX, mY, mZ);
    }

	public void min(float x, float y, float z)
	{
		mX = Math.min(mX, x);
		mY = Math.min(mY, y);
		mZ = Math.min(mZ, z);
	}

	public void max(float x, float y, float z)
	{
		mX = Math.max(mX, x);
		mY = Math.max(mY, y);
		mZ = Math.max(mZ, z);
	}

	public void write(FloatBuffer fBb)
	{
		assert (fBb.remaining()>=3);
		
		fBb.put(mX);
		fBb.put(mY);
		fBb.put(mZ);
	}

	public void read(FloatBuffer fBb)
	{
		assert (fBb.remaining()>=3);
		
		mX = fBb.get();
		mY = fBb.get();
		mZ = fBb.get();
	}

	public float max()
	{
		return Math.max(mZ, Math.max(mX, mY));
	}

	public float absmax()
	{
		return Math.max(Math.abs(mZ), Math.max(Math.abs(mX), Math.abs(mY)));
	}

	public void put(FloatBuffer fb)
	{
		fb.put(mX);
		fb.put(mY);
		fb.put(mZ);
	}
}
