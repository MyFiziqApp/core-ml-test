package com.myfiziq.myfiziq_android.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.util.AttributeSet;

import com.alphamovie.lib.AlphaMovieView;

/**
 * Assists in rendering a video on the splash screen that gradually fades in.
 */
public class SplashScreenVideo extends AlphaMovieView
{
    private int renderedFrames = 0;

    public SplashScreenVideo(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void surfaceCreated(SurfaceTexture texture)
    {
        super.surfaceCreated(texture);

        // Set the video to be almost invisible at start to hide the black flicker from the TextureView
        // Don't make it completely invisible since it is ignored and then the black flicker will happen regardless
        setAlpha(0.01f);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        super.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        super.onSurfaceTextureSizeChanged(surface, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        return super.onSurfaceTextureDestroyed(surface);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        super.onSurfaceTextureUpdated(surface);

        renderedFrames++;

        // Once we have rendered a few frames
        if (renderedFrames == 2)
        {
            // Run after a 500 milliseconds
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                // An animation to fade in the video
                ObjectAnimator anim = ObjectAnimator.ofFloat(this,"alpha",1f);
                anim.setDuration(500);
                anim.start();

            }, 500);
        }
    }
}
