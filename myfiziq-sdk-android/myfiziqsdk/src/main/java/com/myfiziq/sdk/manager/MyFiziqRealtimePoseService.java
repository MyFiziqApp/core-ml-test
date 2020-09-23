package com.myfiziq.sdk.manager;

import android.content.Context;
import android.text.TextUtils;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.ModelInspect;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.YuvToRgb;
import com.myfiziq.sdk.views.GraphicOverlay;
import com.myfiziq.sdk.views.PoseGraphic;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class MyFiziqRealtimePoseService
{
    @Nullable
    private PoseSide side;

    @Nullable
    private GraphicOverlay overlay;

    private boolean enabled = false;
    private boolean running = false;
    private YuvToRgb converter;


    public MyFiziqRealtimePoseService(@Nullable PoseSide side)
    {
        this.side = side;
    }

    public MyFiziqRealtimePoseService(@NonNull GraphicOverlay overlay, @Nullable PoseSide side)
    {
        this.overlay = overlay;
        this.side = side;
    }

    @androidx.camera.core.ExperimentalGetImage
    public void detectPose(byte[] nv21ImageData, int imageWidth, int imageHeight, int rotationDegrees, String contourId)
    {
        if (!enabled)
        {
            Timber.e("Real-time pose is not enabled. It will not run.");
            return;
        }

        if (running)
        {
            Timber.e("Real-time pose is currently running. Only 1 task can run at a time. The current pose detection request will not run.");
            return;
        }

        if (side == null)
        {
            Timber.v("Side is null. Not running real-time pose detection.");
            return;
        }

        if (TextUtils.isEmpty(contourId))
        {
            Timber.e("Contour ID is empty or null. Not running real-time pose detection.");
            return;
        }

        running = true;

        Context context = GlobalContext.getContext();

        if (converter == null)
        {
            converter = new YuvToRgb(context);
        }

        long startTime = System.currentTimeMillis();

        MyFiziq myFiziqSdk = MyFiziq.getInstance();
        String[] results = myFiziqSdk.inspect(side, contourId, imageWidth, imageHeight, rotationDegrees, nv21ImageData);

        long endTime = System.currentTimeMillis();

        if (results != null && results.length == 1)
        {
            ModelInspect inspectionResults = Orm.newModel(ModelInspect.class);
            inspectionResults.deserialize(results[0]);

            if (overlay != null)
            {
                PoseGraphic poseGraphic = new PoseGraphic(overlay, inspectionResults, side);

                removeVisualisation();
                addVisualisation(poseGraphic);
            }
        }
        else
        {
            removeVisualisation();
        }

        Timber.e("Took %sms to run real time inspection. Results %s", (endTime - startTime), Arrays.toString(results));

        running = false;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;

        if (!enabled)
        {
            removeVisualisation();
        }
    }

    public boolean isRunning()
    {
        return running;
    }

    public void setSide(@Nullable PoseSide side)
    {
        this.side = side;
    }

    public void destroy()
    {
        if (overlay != null)
        {
            overlay.clear();
            overlay = null;
        }
    }

    private void addVisualisation(PoseGraphic poseGraphic)
    {
        if (overlay != null)
        {
            overlay.add(poseGraphic);
            overlay.postInvalidate();
        }
    }

    private void removeVisualisation()
    {
        if (overlay != null)
        {
            overlay.remove(PoseGraphic.class);
            overlay.postInvalidate();
        }

        if (converter != null)
        {
            converter.destroy();
            converter = null;
        }
    }
}
