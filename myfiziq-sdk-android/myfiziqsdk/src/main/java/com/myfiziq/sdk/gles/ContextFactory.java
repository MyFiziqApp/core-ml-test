package com.myfiziq.sdk.gles;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * @hide
 */
public class ContextFactory implements GLTextureView.EGLContextFactory
{
    private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig)
    {
        int[] attrib_list =
                {
                        EGL_CONTEXT_CLIENT_VERSION, 2,
                        EGL10.EGL_NONE
                };

        if (null == eglConfig)
            throw new IllegalArgumentException("Invalid EGLConfig");

        EGLContext context = egl.eglCreateContext(display, eglConfig,
                EGL10.EGL_NO_CONTEXT, attrib_list);

        return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext config)
    {
        egl.eglDestroyContext(display, config);
    }
}
