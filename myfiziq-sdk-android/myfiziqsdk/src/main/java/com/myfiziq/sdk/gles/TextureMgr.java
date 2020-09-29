package com.myfiziq.sdk.gles;

import android.content.Context;

import java.util.Iterator;
import java.util.Vector;

/**
 * Manages <code>Texture</code> instances.
 * <br>
 * Textures need to be re-uploaded when the GL context is recreated - This manager handles
 * that task among others.
 */
public class TextureMgr
{
	static TextureMgr mThis;
	private Vector<Texture> mTextures = new Vector<>();
	
	public static synchronized TextureMgr getInstance()
	{
		if (null == mThis)
		{
			mThis = new TextureMgr();
		}

		return mThis;
	}
	
	private TextureMgr()
	{
	}

	public void forceReload()
	{
		synchronized (mTextures)
		{
			Iterator<Texture> i = mTextures.iterator();
			while (i.hasNext())
			{
				Texture t = i.next();
				t.mUploaded = false;
			}
		}
	}
	
	public void reloadTextures()
	{
		synchronized (mTextures)
		{
			Iterator<Texture> i = mTextures.iterator();
			while (i.hasNext())
			{
				Texture t = i.next();
				t.uploadGLTexture();
			}
		}
	}

	public void unloadTextures()
	{
		synchronized (mTextures)
		{
			Iterator<Texture> i = mTextures.iterator();
			while (i.hasNext())
			{
				Texture t = i.next();
				if (t.isPendingRemove())
				{
					t.doUnload();
					i.remove();
				}
			}
		}
	}

	public void addTexture(Texture texture)
	{
		synchronized (mTextures)
		{
			mTextures.add(texture);
		}
	}
	
	public Texture getTexture(Context context, int textureId)
	{
		Texture t = findTextureById(textureId);
		
		if (null == t)
		{
			t = new Texture(context, textureId);
			synchronized (mTextures)
			{
				mTextures.add(t);
			}
		}
		
		return t;
	}
	
	public Texture findTextureById(int textureId)
	{
		synchronized (mTextures)
		{
			Iterator<Texture> i = mTextures.iterator();
			while (i.hasNext())
			{
				Texture t = i.next();
				if (t.mTextureId == textureId)
				{
					return t;
				}
			}
		}
		// not found
		return null;
	}

	public int findGLById(int textureId)
	{
		synchronized (mTextures)
		{
			Iterator<Texture> i = mTextures.iterator();
			while (i.hasNext())
			{
				Texture t = i.next();
				if (t.mTextureId == textureId)
				{
					return t.mGlTextureId;
				}
			}
		}
		// not found
		return -1;
	}

	public void unload(Texture texture)
	{
		if (null != texture)
			texture.unload();
	}

	public void remove(Texture texture)
	{
		synchronized (mTextures)
		{
			mTextures.remove(texture);
		}
	}
}
