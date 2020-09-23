package com.myfiziq.sdk.util;

import android.content.Context;
import android.util.Size;

import com.google.common.util.concurrent.ListenableFuture;
import com.myfiziq.sdk.views.GraphicOverlay;
import com.myfiziq.sdk.vo.SaveImageRequest;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import timber.log.Timber;

public class CameraXContainer
{
    private Preview preview;
    private ImageAnalysis imageAnalysis;

    // Will be null after being destroyed
    @Nullable
    private CameraXImageAnalyzer cameraXImageAnalyzer;

    private CameraSelector cameraDirection = CameraSelector.DEFAULT_FRONT_CAMERA;

    /**
     * Constructs a new container for CameraX.
     *
     * @param cameraXImageAnalyzer A {@link CameraXImageAnalyzer} object that will analyse images in real time (if configured to do so).
     */
    public CameraXContainer(@NonNull CameraXImageAnalyzer cameraXImageAnalyzer)
    {
        this.cameraXImageAnalyzer = cameraXImageAnalyzer;
    }

    /**
     * Starts CameraX.
     *
     * @param lifecycleOwner An activity/fragment that is hosting CameraX. The lifecycle of CameraX will be bound to this object.
     * @param viewFinder A TextureView to render the live camera preview.
     */
    public void startCameraX(LifecycleOwner lifecycleOwner, PreviewView viewFinder)
    {
        if (cameraXImageAnalyzer == null)
        {
            Timber.e("The CameraX container object has probably been destroyed. Cannot start CameraX again with the same container object.");

            // The container is already destroyed. Don't do anything
            return;
        }

        // If the Camera Provider is not initialised, this WILL initialise it
        // Lets make this call early on to ensure it's initialised before doing other CameraX stuff
        ProcessCameraProvider cameraProvider = getOrCreateCameraProvider();

        if (cameraProvider == null)
        {
            Timber.e("ProcessCameraProvider is null. Not starting CameraX.");
            return;
        }


        Size targetResolution = new Size(720, 1280);

        preview = new Preview.Builder()
                .setTargetResolution(targetResolution)
                .build();

        Preview.PreviewSurfaceProvider surfaceProvider = viewFinder.getPreviewSurfaceProvider();
        preview.setPreviewSurfaceProvider(surfaceProvider);

        if (cameraXImageAnalyzer.getOverlay() != null)
        {
            GraphicOverlay overlay = cameraXImageAnalyzer.getOverlay();
            overlay.setCameraInfo(720, 1280, true);
        }


        // Use a worker thread for image analysis to prevent image analysis from running on the UI
        // thread and freezing it.
        //
        // Use an executor with only 1 thread to ensure that we don't overload the CPU on slower devices.
        //
        // If the CameraX image queue fills up while we're handling the image on a slow device,
        // it will discard the oldest image currently in the queue.
        //
        // This is fine since we don't need to analyse EVERY image, we just need to have a recent
        // image available (e.g. when "capturing" photos or doing real time pose detection).
        Executor executor = Executors.newSingleThreadExecutor();


        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(targetResolution)
                .build();

        imageAnalysis.setAnalyzer(executor, cameraXImageAnalyzer);

        // Attach CameraX to the lifecycle of the activity
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraDirection, preview, imageAnalysis);
    }

    /**
     * Determines if CameraX is currently setup, the listeners have been attached and is currently processing
     * camera input.
     */
    public boolean isBound()
    {
        Context context = GlobalContext.getContext();
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);

        ProcessCameraProvider cameraProvider;

        try
        {
            cameraProvider = future.get();
        }
        catch (Exception e)
        {
            Timber.e(e);
            return false;
        }

        return cameraProvider.isBound(preview) && cameraProvider.isBound(imageAnalysis);
    }

    /**
     * Indicates that we should capture the next image we receive from CameraX and save it to the filesystem.
     * @param request The configuration we should use when saving the image.
     */
    public void captureNextImage(SaveImageRequest request)
    {
        if (cameraXImageAnalyzer != null)
        {
            cameraXImageAnalyzer.captureNextImage(request);
        }
    }

    public void destroy()
    {
        ProcessCameraProvider cameraProvider = getOrCreateCameraProvider();

        if (cameraProvider != null)
        {
            cameraProvider.unbindAll();
        }

        if (cameraXImageAnalyzer != null)
        {
            cameraXImageAnalyzer.destroy();
            cameraXImageAnalyzer = null;
        }
    }

    @Nullable
    private ProcessCameraProvider getOrCreateCameraProvider()
    {
        Context context = GlobalContext.getContext();

        // If the Camera Provider is not initialised, this WILL initialise it
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);

        try
        {
            return future.get();
        }
        catch (Exception e)
        {
            Timber.e(e);
            return null;
        }
    }
}
