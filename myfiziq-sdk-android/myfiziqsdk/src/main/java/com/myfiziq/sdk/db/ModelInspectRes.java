package com.myfiziq.sdk.db;

import android.content.res.Resources;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.R;

import timber.log.Timber;

/**
 * @hide
 */
public class ModelInspectRes extends Model
{
    private final String TAG = ModelInspectRes.class.getSimpleName();


    @Persistent
    public PoseSide Image = PoseSide.front;

    @Persistent
    public int GE = 0;

    @Persistent
    public int Face = 0;

    @Persistent
    public int LA = 0;

    @Persistent
    public int RA = 0;

    @Persistent
    public int LL = 0;

    @Persistent
    public int RL = 0;

    @Persistent
    public int BG = 0;

    @Persistent
    public int DP = 0;

    @Persistent
    public int UB = 0;

    @Persistent
    public int LB = 0;

    @Persistent
    public int FaceInExpectedContour = 0;

    @Persistent
    public int CameraIsPotentiallyHigh = 0;

    @Persistent
    public int CameraIsPotentiallyLow = 0;

    public boolean isValid()
    {
        if (BuildConfig.DEBUG)
        {
            if (ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, false))
                return true;

            if (ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INSPECT_FAIL, false))
                return false;
        }

        switch (Image)
        {
            case front:
                return (GE == 0 && Face == 1 && LA == 1 && RA == 1 && LL == 1 && RL == 1 &&
                        FaceInExpectedContour  == 1 && CameraIsPotentiallyHigh  == 0 && CameraIsPotentiallyLow == 0
                );

            case side:
                // no need to check face position on side view (see Zenhub ticket #626)
                return (GE == 0 && Face == 1 && UB == 1 && LB == 1);
        }

        return false;
    }

    // Left/Right inspection results are flipped -> we need to flip the result strings back for the user...
    public String getError(Resources resources)
    {
        //TODO: fix logging.

        if (0 == GE && 0 == Face && 0 == LA && 0 == RA && 0 == LL && 0 == RL && 0 == BG && 0 == DP && 0 == UB && 0 == LB)
            return resources.getString(R.string.myfiziqsdk_error_unknown);

        if (Face < 1)
        {
            return resources.getString(R.string.myfiziqisdk_error_no_face);
        }

        if (Face > 1)
        {
            return resources.getString(R.string.myfiziqisdk_error_multiple_faces);
        }

        switch (Image)
        {
            case front:
            {
                boolean bBothArmsMissing = (1 != LA && 1 != RA);
                boolean bBothLegsMissing = (1 != LL && 1 != RL);

                if (bBothArmsMissing && bBothLegsMissing)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_arms_legs);
                }

                if (bBothArmsMissing)
                {
                    if (1 != LL)
                        return resources.getString(R.string.myfiziqisdk_error_no_arms_left_leg);

                    if (1 != RL)
                        return resources.getString(R.string.myfiziqisdk_error_no_arms_right_leg);

                    return resources.getString(R.string.myfiziqisdk_error_no_arms);
                }

                if (bBothLegsMissing)
                {
                    if (1 != LA)
                        return resources.getString(R.string.myfiziqisdk_error_no_legs_left_arm);

                    if (1 != RA)
                        return resources.getString(R.string.myfiziqisdk_error_no_legs_right_arm);

                    return resources.getString(R.string.myfiziqisdk_error_no_legs);
                }

                if (1 != LA && 1 != LL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_left_arm_left_leg);
                }

                if (1 != LA && 1 != RL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_left_arm_right_leg);
                }

                if (1 != RA && 1 != RL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_right_arm_right_leg);
                }

                if (1 != RA && 1 != LL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_right_arm_left_leg);
                }

                if (1 != LA)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_left_arm);
                }

                if (1 != RA)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_right_arm);
                }

                if (1 != LL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_left_leg);
                }

                if (1 != RL)
                {
                    return resources.getString(R.string.myfiziqisdk_error_no_right_leg);
                }

                if (0 != GE)
                {
                    return resources.getString(R.string.myfiziqsdk_error_unknown);
                }

                if (CameraIsPotentiallyHigh == 1)
                {
                    Timber.d(resources.getString(R.string.myfiziqsdk_error_camera_too_high));
                    return resources.getString(R.string.myfiziqsdk_error_camera_too_high);
                }

                if (CameraIsPotentiallyLow == 1)
                {
                    Timber.d(resources.getString(R.string.myfiziqsdk_error_camera_too_low));
                    return resources.getString(R.string.myfiziqsdk_error_camera_too_low);
                }
            }
            break;

            case side:
            {
                if (1 != UB)
                {
                    logIfFailed("upper body", UB);
                    return resources.getString(R.string.myfiziqisdk_error_no_arms_outline);
                }

                if (1 != LB)
                {
                    logIfFailed("lower body", LB);
                    return resources.getString(R.string.myfiziqisdk_error_no_legs_outline);
                }

                if (0 != GE)
                {
                    return resources.getString(R.string.myfiziqsdk_error_unknown);
                }
            }
            break;
        }

        return "";
    }

    private void logIfFailed(String bodyPart, int value)
    {
        if (1 != value)
        {
            ModelLog.e(TAG, "Could not detect " + bodyPart);
        }
    }

    public boolean matches(ModelInspectRes r)
    {
        return (
                Image == r.Image &&
                Face == r.Face &&
                GE == r.GE &&
                LA == r.LA &&
                RA == r.RA &&
                LL == r.LL &&
                RL == r.RL &&
                BG == r.BG &&
                DP == r.DP &&
                UB == r.UB &&
                LB == r.LB &&
                FaceInExpectedContour == r.FaceInExpectedContour &&
                CameraIsPotentiallyHigh == r.CameraIsPotentiallyHigh &&
                CameraIsPotentiallyLow == r.CameraIsPotentiallyLow
        );
    }
}
