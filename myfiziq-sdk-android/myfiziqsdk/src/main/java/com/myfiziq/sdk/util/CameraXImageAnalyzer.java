package com.myfiziq.sdk.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.manager.MyFiziqRealtimePoseService;
import com.myfiziq.sdk.views.GraphicOverlay;
import com.myfiziq.sdk.vo.SaveImageRequest;

import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import timber.log.Timber;

public class CameraXImageAnalyzer implements ImageAnalysis.Analyzer
{
    /**
     * Tracks a list of requests to capture images.
     *
     * Ensure that we use the {@link Stack} class since this variable can be accessed from
     * multiple threads. {@link Stack} provides synchronisation across threads.
     * whereas {@link java.util.Deque} does not.
     */
    private Stack<SaveImageRequest> saveImageRequests = new Stack<>();                     //NOSONAR

    private YuvToRgb converter;

    @Nullable
    private GraphicOverlay overlay;

    private MyFiziqRealtimePoseService realtimePoseService;
    private String contourId;

    public CameraXImageAnalyzer(boolean isPoseEnabled, @Nullable PoseSide side, @NonNull String contourId)
    {
        realtimePoseService = new MyFiziqRealtimePoseService(side);
        realtimePoseService.setEnabled(isPoseEnabled);

        this.contourId = contourId;
    }

    public CameraXImageAnalyzer(boolean isPoseEnabled, @NonNull GraphicOverlay overlay, @Nullable PoseSide side, @NonNull String contourId)
    {
        realtimePoseService = new MyFiziqRealtimePoseService(overlay, side);
        realtimePoseService.setEnabled(isPoseEnabled);

        this.contourId = contourId;
        this.overlay = overlay;
    }

    // analyze() runs synchronously
    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy image)
    {
        if (!isRealtimePoseRequired() && !isSaveImageRequired())
        {
            // Neither realtime pose nor saving an image is required
            // No work is required so do nothing

            // Release the image back to CameraX. We MUST do this!
            image.close();

            // Finish analysing this image
            return;
        }

        Timber.i("Image received from CameraX. Starting to process image.");

        // We MUST extract all data from the image we need before the analyze() method finishes and asynchronous processing starts.
        // We need to recycle the image as soon as this method completes.
        byte[] nv21ImageData = CameraXImageUtil.yuv_420_888toNv21(image);

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int rotationDegrees = image.getImageInfo().getRotationDegrees();

        // Release the image back to CameraX. We MUST do this!
        image.close();


        if (isRealtimePoseRequired())
        {
            realtimePoseService.detectPose(nv21ImageData, imageWidth, imageHeight, rotationDegrees, contourId);
        }

        if (isSaveImageRequired())
        {
            saveImage(nv21ImageData, imageWidth, imageHeight, rotationDegrees);
        }
    }

    public void setRealtimePoseEnabled(boolean realtimePoseEnabled)
    {
        realtimePoseService.setEnabled(realtimePoseEnabled);
    }

    public void setSide(PoseSide side)
    {
        if (realtimePoseService != null)
        {
            realtimePoseService.setSide(side);
        }
    }

    public void captureNextImage(SaveImageRequest request)
    {
        saveImageRequests.push(request);
    }

    @androidx.camera.core.ExperimentalGetImage
    private void saveImage(byte[] nv21ImageData, int imageWidth, int imageHeight, int rotationDegrees)
    {
        SaveImageRequest request = saveImageRequests.pop();

        if (converter == null)
        {
            converter = new YuvToRgb(request.getContext());
        }

        // Save the photo to the filesystem and do all post-processing on a separate thread
        // Otherwise CameraX will wait until this method has finished and other images will queue up in the background
        new Thread(() -> savePhotoToFilesystem(request, converter, nv21ImageData, imageWidth, imageHeight, rotationDegrees)).start();
    }

    public static void savePhotoToFilesystem(SaveImageRequest request, YuvToRgb converter, byte[] nv21Data, int width, int height, int rotationDegrees)
    {
        String filePath = request.getImageFilePath();
        AsyncHelper.Callback<String> callback = request.getCallback();

        if (nv21Data.length == 0)
        {
            Timber.e("NV21 image byte array is empty");
            callback.execute("");
            return;
        }

        long postProcessingStartTime = System.currentTimeMillis();

        Bitmap rotatedBitmap;

        try
        {
            Bitmap originalBitmap = converter.nv21ToBitmap(nv21Data, nv21Data.length, width, height);

            Matrix matrix = new Matrix();

            // Rotate the bitmap so it's upright
            // Note, CameraX will send us the "rotationDegrees" that the image is rotated by
            // We simply need to rotate it by that amount to get the right orientation
            matrix.postRotate(rotationDegrees);

            // Flip the bitmap so it's not back to front
            matrix.postScale(-1, 1, originalBitmap.getWidth() / 2f, originalBitmap.getHeight() / 2f);

            // Create a new Bitmap with the matrix operations applied to it
            rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

            originalBitmap.recycle();
        }
        catch (Exception e)
        {
            Timber.e(e);
            callback.execute("");
            return;
        }

        long postProcessingEndTime = System.currentTimeMillis();
        Timber.d("Saving image to filesystem");
        long saveFsStartTime = System.currentTimeMillis();

        boolean isSaveSuccessful = BmpUtil.save(rotatedBitmap, filePath);

        if (!isSaveSuccessful)
        {
            Timber.e("Saving the capture image was unsuccessful");

            callback.execute("");
            return;
        }

        /*try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            rotatedBitmap.compress(compressionFormat, 100, fileOutputStream);
        }
        catch (Exception e)
        {
            Timber.e(e);

            callback.execute("");
            return;
        }*/

        rotatedBitmap.recycle();

        long saveFsEndTime = System.currentTimeMillis();

        Timber.d("Saved image filesystem. Took %sms for image post-processing. Took %sms to save the %s image.",
                (postProcessingEndTime - postProcessingStartTime),
                (saveFsEndTime - saveFsStartTime),
                PoseSide.CAPTURE_IMAGE_EXTENSION
        );

        callback.execute(filePath);
    }

    @Nullable
    public GraphicOverlay getOverlay()
    {
        return overlay;
    }

    public void destroy()
    {
        if (overlay != null)
        {
            overlay.clear();
            overlay = null;
        }

        if (realtimePoseService != null)
        {
            realtimePoseService.destroy();
            realtimePoseService = null;
        }

        if (converter != null)
        {
            converter.destroy();
            converter = null;
        }
    }

    private boolean isRealtimePoseRequired()
    {
        return realtimePoseService != null && realtimePoseService.isEnabled() && !realtimePoseService.isRunning();
    }

    private boolean isSaveImageRequired()
    {
        return !saveImageRequests.isEmpty();
    }
}
