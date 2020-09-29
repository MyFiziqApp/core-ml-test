package com.myfiziq.sdk.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.adapters.ConfirmPagerAdapter;
import com.myfiziq.sdk.components.ContourObservable;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelInspect;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.AvatarUploadWorker;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.CameraXContainer;
import com.myfiziq.sdk.util.CameraXImageAnalyzer;
import com.myfiziq.sdk.util.FactoryContour;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.ImageUtils;
import com.myfiziq.sdk.util.MiscUtils;
import com.myfiziq.sdk.util.SensorUtils;
import com.myfiziq.sdk.util.SoundUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.CaptureConfirmHelpView;
import com.myfiziq.sdk.views.CaptureCountdown;
import com.myfiziq.sdk.views.CaptureErrorUprightView;
import com.myfiziq.sdk.views.CaptureErrorView;
import com.myfiziq.sdk.views.CircleCountdown;
import com.myfiziq.sdk.views.FloatingRelativeLayout;
import com.myfiziq.sdk.views.GraphicOverlay;
import com.myfiziq.sdk.views.InspectionErrorView;
import com.myfiziq.sdk.vo.SaveImageRequest;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;

/**
 * @hide
 */
public class FragmentCapture extends BaseFragment implements
        CaptureCountdown.CaptureCountdownCallback,
        CircleCountdown.CircleCountdownCallback,
        SensorUtils.SensorUtilsListener,
        View.OnClickListener
{
    final static boolean mPostProcess = false;

    private enum State
    {
        ALIGN,
        ERROR_UPRIGHT,
        FRONT,
        SIDE,
        CONFIRM_FR,
        CONFIRM_SD,
        ERROR,
        INSPECTION_ERROR
    }

    private float screenW;
    private float screenH;
    private float aspect;
    private int orientation;
    private State state = State.ALIGN;
    private SensorUtils sensor;
    private boolean lockButtons = false;

    private View layoutCircleWhite;
    private View layoutCircleOrange;
    private View layoutCircleBlue;
    private View layoutRelative;
    private View bgOverlay;
    private CircleCountdown viewCircleBlue;
    private FrameLayout mCameraSurfaceLayout;
    private PreviewView mCameraSurface;
    private ViewTreeObserver.OnGlobalLayoutListener toolbarListener;
    private ImageView contourOverlay;
    private TextView textViewTitle;
    private TextView textViewDetail;
    private ImageView toolbarClose;
    private ImageView toolbarHelp;
    private CaptureErrorUprightView captureErrorUprightView;
    private CaptureErrorView captureErrorView;
    private CaptureConfirmHelpView captureConfirmHelpView;

    private InspectionErrorView inspectionErrorView;

    private CaptureCountdown countdown;
    private FloatingRelativeLayout toolbar;
    private View shutter;
    private TextView toolbarTitle;

    private View layoutConfirm;
    private ViewPager viewPager;
    private Button buttonRetake;
    private Button buttonConfirm;

    @Nullable
    private PoseSide side;

    private SoundUtils soundUtils = SoundUtils.getInstance();
    private ModelAvatar avatar = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean inPractiseMode = false;

    private CameraXImageAnalyzer cameraXImageAnalyzer;
    private CameraXContainer cameraXContainer;


    @Nullable
    private Bitmap frontContour;

    @Nullable
    private Bitmap sideContour;

    /**
     * Listens for when the front contour is ready if we cannot display it by the time the front capture screen is displayed.
     */
    private ContourObservable frontContourObservable;

    /**
     * Listens for when the side contour is ready if we cannot display it by the time the front capture screen is displayed.
     */
    private ContourObservable sideContourObservable;

    private boolean alreadyAskedPermissionsAtResume = false;

    private CountDownLatch imageCaptureCounter;

    private List<String> capturedFrontImages = new LinkedList<>();
    private List<String> capturedSideImages = new LinkedList<>();


    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_capture;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            Timber.e("SDK is no longer initialised. Shutting down...");
            getActivity().finish();
            return null;
        }

        View viewContainer = super.onCreateView(inflater, container, savedInstanceState);

        if (mParameterSet != null && mParameterSet.hasParam(R.id.TAG_ARG_CAPTURE_PRACTISE_MODE))
        {
            inPractiseMode = mParameterSet.getBooleanParamValue(R.id.TAG_ARG_CAPTURE_PRACTISE_MODE, false);
        }

        if (null != mParameterSet && mParameterSet.hasParam(R.id.TAG_ARG_MODEL_AVATAR))
        {
            //TODO: type safety needed? (Parcelable parcelable instanceof ModelAvatar)
            avatar = (ModelAvatar) mParameterSet.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
        }

        if (null != avatar && !mPostProcess)
        {
            AvatarUploadWorker.createWorker(avatar);
        }

        // Don't put any "findViewByIds()" in here!
        // They can be garbage collected in the background

        return viewContainer;
    }

    private void bindViews()
    {
        View view = getView();

        if (getActivity() == null || view == null)
        {
            // Fragment has detached from activity
            return;
        }

        generateContourImages();

        mCameraSurfaceLayout = view.findViewById(R.id.cameraSurfaceLayout);
        mCameraSurface = view.findViewById(R.id.cameraSurface);
        contourOverlay = view.findViewById(R.id.contourOverlay);

        aspect = UiUtils.getScreenAspect(getActivity());

        screenW = UiUtils.getScreenWidth(getActivity());
        screenH = UiUtils.getScreenHeight(getActivity());

        bgOverlay = view.findViewById(R.id.bgoverlay);
        layoutRelative = view.findViewById(R.id.layoutRelative);
        layoutCircleWhite = view.findViewById(R.id.layoutCircleWhite);
        layoutCircleOrange = view.findViewById(R.id.layoutCircleOrange);
        layoutCircleBlue = view.findViewById(R.id.layoutCircleBlue);
        viewCircleBlue = view.findViewById(R.id.viewCountdownAlign);
        textViewTitle = view.findViewById(R.id.textViewTitle);
        textViewDetail = view.findViewById(R.id.textViewDetail);
        toolbarClose = view.findViewById(R.id.toolbarClose);
        toolbarHelp = view.findViewById(R.id.toolbarHelp);
        countdown = view.findViewById(R.id.viewCapCountdown);

        toolbar = view.findViewById(R.id.toolbar);
        toolbarTitle = view.findViewById(R.id.toolbarTitle);
        shutter = view.findViewById(R.id.shutter);
        captureErrorView = view.findViewById(R.id.captureErrorView);
        captureErrorUprightView = view.findViewById(R.id.captureErrorUprightView);
        captureConfirmHelpView = view.findViewById(R.id.captureConfirmHelpView);

        inspectionErrorView = view.findViewById(R.id.inspectionErrorView);

        toolbarClose.setOnClickListener(this);
        toolbarHelp.setOnClickListener(this);
        layoutCircleWhite.setOnClickListener(this);
        layoutRelative.setOnClickListener(this);

        layoutConfirm = view.findViewById(R.id.layoutConfirm);
        viewPager = view.findViewById(R.id.viewPager);
        buttonRetake = view.findViewById(R.id.buttonRetake);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        buttonRetake.setOnClickListener(this);
        buttonConfirm.setOnClickListener(this);

        captureErrorView.setErrorText(getResources().getString(R.string.error_misaligned));
        captureErrorView.errorButtons.setButtonListeners(
                getMyActivity(),
                avatar,
                o -> setState(State.ALIGN),
                o -> doExitConfirm()
        );
        inspectionErrorView.errorButtons.setButtonListeners(
                getMyActivity(),
                avatar,
                o ->
                {
                    inspectionErrorView.cancelCallback();
                    setState(State.ALIGN);
                },
                o -> doExitConfirm()
        );
    }

    private void adjustToolbarPosition()
    {
        if (toolbarListener == null)
        {
            toolbarListener = () ->
            {
                float toolbarYOffset = mCameraSurfaceLayout.getY();

                if (toolbarYOffset > 0)
                {
                    toolbar.setY(toolbarYOffset);

                    mCameraSurfaceLayout.getViewTreeObserver().removeOnGlobalLayoutListener(toolbarListener);
                    toolbarListener = null;
                }
            };

            mCameraSurfaceLayout.getViewTreeObserver().addOnGlobalLayoutListener(toolbarListener);
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();

        bindViews();

        ActivityInterface myActivity = getMyActivity();

        if (myActivity == null || myActivity.getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            Timber.e("SDK is no longer initialised. Shutting down...");
            myActivity.finish();
            return;
        }

        setState(State.ALIGN);

        boolean bCamEnabled = checkPermissions(Manifest.permission.CAMERA);

        orientation = myActivity.getActivity().getRequestedOrientation();
        myActivity.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (!bCamEnabled && !alreadyAskedPermissionsAtResume)
        {
            alreadyAskedPermissionsAtResume = true;

            askPermissions(
                    getResources().getInteger(R.integer.TAG_PERMISSION_CAMERA),
                    getString(R.string.myfiziqsdk_align_circles_camera_reason),
                    Manifest.permission.CAMERA
            );
        }

        if (bCamEnabled)
        {
            // Views were previously created, restart them
            startViews();
            startSensors();

            if (cameraXContainer == null && null != mParameterSet && mParameterSet.hasParam(R.id.TAG_ARG_MODEL_AVATAR))
            {
                //TODO: type safety needed? (Parcelable parcelable instanceof ModelAvatar)
                ModelAvatar localAvatar = (ModelAvatar) mParameterSet.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();

                String contourId = MyFiziq.getInstance().getContourId(
                        255, 255, 1280, 720,
                        localAvatar.getHeight().getValueInCm(),
                        localAvatar.getWeight().getValueInKg(),
                        localAvatar.getGender(),
                        PoseSide.front, (float) sensor.getPitch(), 0
                );

                GraphicOverlay overlay = getActivity().findViewById(R.id.realtimePoseOverlay);

                // Create the analyzer and disable it until we're in the FRONT or SIDE state
                cameraXImageAnalyzer = new CameraXImageAnalyzer(false, overlay, side, contourId);

                cameraXContainer = new CameraXContainer(cameraXImageAnalyzer);
                cameraXContainer.startCameraX(this, mCameraSurface);
            }
        }

        // Trigger a sensor change to ensure we're in the correct state in case the phone is sitting
        // in a stand and doesn't move after resuming the activity
        deviceVerticalChanged();

        AsyncHelper.run(() -> MyFiziq.getInstance().initInspect(false));
    }

    @Override
    public void onPause()
    {
        super.onPause();

        stopViews();
        stopSensors();

        MyFiziq.getInstance().releaseInspect();

        ActivityInterface myActivity = getMyActivity();

        if (myActivity == null || myActivity.getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        myActivity.getActivity().setRequestedOrientation(orientation);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Timber.d("Stopping FragmentCapture");

        clearObservables();
        stopViews();
        stopSensors();

        if (toolbarListener != null)
        {
            mCameraSurfaceLayout.getViewTreeObserver().removeOnGlobalLayoutListener(toolbarListener);
            toolbarListener = null;
        }

        if (frontContour != null)
        {
            frontContour.recycle();
            frontContour = null;
        }

        if (sideContour != null)
        {
            sideContour.recycle();
            sideContour = null;
        }

        if (viewCircleBlue != null)
        {
            viewCircleBlue.setCallback(null);
            viewCircleBlue = null;
        }

        if (countdown != null)
        {
            countdown.setCallback(null);
            countdown = null;
        }

        if (toolbarClose != null)
        {
            toolbarClose.setOnClickListener(null);
            toolbarClose = null;
        }

        if (toolbarHelp != null)
        {
            toolbarHelp.setOnClickListener(null);
            toolbarHelp = null;
        }

        if (layoutCircleWhite != null)
        {
            layoutCircleWhite.setOnClickListener(null);
            layoutCircleWhite = null;
        }

        if (layoutRelative != null)
        {
            layoutRelative.setOnClickListener(null);
            layoutRelative = null;
        }

        if (buttonRetake != null)
        {
            buttonRetake.setOnClickListener(null);
            buttonRetake = null;
        }

        if (buttonConfirm != null)
        {
            buttonConfirm.setOnClickListener(null);
            buttonConfirm = null;
        }

        if (viewPager != null)
        {
            ConfirmPagerAdapter adapter = (ConfirmPagerAdapter) viewPager.getAdapter();

            if (adapter != null)
            {
                adapter.destroy();
            }

            viewPager = null;
        }

        if (captureConfirmHelpView != null)
        {
            captureConfirmHelpView = null;
        }

        if (cameraXImageAnalyzer != null)
        {
            cameraXImageAnalyzer.destroy();
            cameraXImageAnalyzer = null;
        }

        if (cameraXContainer != null)
        {
            cameraXContainer.destroy();
            cameraXContainer = null;
        }

        if (mCameraSurfaceLayout != null)
        {
            mCameraSurfaceLayout = null;
        }

        if (mCameraSurface != null)
        {
            mCameraSurface = null;
        }
    }

    private void startViews()
    {
        if (null != viewCircleBlue)
        {
            viewCircleBlue.start();
        }

        if (null != countdown)
        {
            countdown.start();
        }
    }

    private void continueSensors()
    {
        if (null == sensor)
        {
            sensor = new SensorUtils(getActivity().getApplicationContext());
            sensor.registerSensors();

            // Check if we've disabled the alignment screen for debugging purposes...
            boolean isAlignmentDisabled = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, false);

            if (!isAlignmentDisabled)
            {
                sensor.setListener(this);
            }
        }
    }

    private void startSensors()
    {
        stopSensors();
        continueSensors();
    }

    private void stopViews()
    {
        if (null != viewCircleBlue)
        {
            viewCircleBlue.cancel();
        }

        if (null != countdown)
        {
            countdown.cancel();
        }
    }

    private void stopSensors()
    {
        if (null != sensor)
        {
            sensor.unregisterSensors();
            sensor.setListener(null);
            sensor = null;
        }
    }

    private void restartSensors()
    {
        stopSensors();
        startSensors();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (getResources().getInteger(R.integer.TAG_PERMISSION_CAMERA) == requestCode)
        {
            if (allPermissionsGranted(grantResults))
            {
                textViewTitle.setText(R.string.myfiziqsdk_align_circles);
                textViewDetail.setText(R.string.myfiziqsdk_align_circles_table);
            }
            else
            {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA))
                {
                    Timber.w("User has denied permission to access the camera.");

                    // User has denied permissions, but not permanently
                    textViewTitle.setText(R.string.myfiziqsdk_align_circles_nocamera);
                    textViewDetail.setText(R.string.myfiziqsdk_align_circles_nocamera_detail);
                }
                else
                {
                    Timber.w("User has permanently denied permission to access the camera.");

                    // User has permanently denied permissions
                    Toast.makeText(getActivity(), getString(R.string.myfiziqsdk_align_circles_camera_reason), Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
            }
        }
    }

    @Override
    public void deviceVerticalChanged()
    {
        boolean bCamEnabled = checkPermissions(Manifest.permission.CAMERA);

        if (!bCamEnabled)
        {
            return;
        }

        switch (state)
        {
            case ALIGN:
            {
                if (!sensor.isYawInRange())
                {
                    Timber.i("Setting ERROR_UPRIGHT. Yaw not in range.");
                    setState(State.ERROR_UPRIGHT);
                }
                else if (sensor.isDeviceUpright())
                {
                    Timber.i("Device is upright");
                    SoundUtils.getInstance().play(SoundUtils.Sounds.SOUND_SUCCESS1);
                    UiUtils.setViewVisibility(layoutCircleWhite, View.INVISIBLE);
                    UiUtils.setViewVisibility(layoutCircleOrange, View.INVISIBLE);
                    UiUtils.setViewVisibility(layoutCircleBlue, View.VISIBLE);
                    viewCircleBlue.start();
                    textViewDetail.setText(R.string.myfiziqsdk_align_circles_ready);
                }
                else
                {
                    Timber.i("Device is not upright and yaw is in range");
                    UiUtils.setViewVisibility(layoutCircleWhite, View.VISIBLE);
                    UiUtils.setViewVisibility(layoutCircleOrange, View.VISIBLE);
                    UiUtils.setViewVisibility(layoutCircleBlue, View.INVISIBLE);
                    viewCircleBlue.cancel();
                    textViewDetail.setText(R.string.myfiziqsdk_align_circles_table);
                }
            }
            break;

            case FRONT:
            case SIDE:
            {
                // Check if we've disabled the alignment screen for debugging purposes...
                boolean isAlignmentDisabled = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, false);

                if (!sensor.isDeviceUpright() && !isAlignmentDisabled)
                {
                    countdown.cancel();
                    setState(State.ERROR);
                }
                else
                {
                    //TODO: remove.
                    countdown.start();
                }
            }
            break;
            case ERROR_UPRIGHT:
            {
                if (sensor.isYawInRange())
                {
                    Timber.i("Yaw in range. Setting ALIGN");
                    setState(State.ALIGN);
                }
                else
                {
                    Timber.i("Yaw is still not in range. Staying in ERROR_UPRIGHT");
                }
            }
            break;
            case INSPECTION_ERROR:
            {
                if (sensor != null && !sensor.isDeviceUpright())
                {
                    inspectionErrorView.cancelCallback();
                    inspectionErrorView.setViewCountdownVisible(false);
                    inspectionErrorView.setButtonsVisible(true);
                    UiUtils.setOnScreenNavigationVisibility(getActivity(), true);
                }
            }
        }
    }

    private void doExitConfirm()
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        String alertTitle = getString(R.string.myfiziqsdk_are_you_sure);
        String alertDetail = getString(R.string.myfiziqsdk_confirm_close_detail);

        switch (state)
        {
            case ALIGN:
                alertTitle = getString(R.string.myfiziqsdk_confirm_exit);
                alertDetail = "";
                break;
            case CONFIRM_FR:
            case CONFIRM_SD:
                alertDetail = getString(R.string.myfiziqsdk_photos_will_not_save);
                break;
        }

        UiUtils.showAlertDialog(
                getActivity(),
                alertTitle,
                alertDetail,
                getString(R.string.myfiziqsdk_exit),
                getString(R.string.myfiziqsdk_cancel),
                (dialog, which) ->
                {
                    MyFiziq.getInstance().post(MyFiziq.NativeOpType.NativeOpCancel);
                    if (null != avatar)
                    {
                        avatar.delete();
                        avatar = null;
                    }
                    getActivity().finish();
                },
                (dialog, which) -> UiUtils.setOnScreenNavigationVisibility(getActivity(), false)
        );
    }

    @Override
    public void onClick(View v)
    {
        if (lockButtons)
        {
            return;
        }

        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        boolean bCamEnabled = checkPermissions(Manifest.permission.CAMERA);

        int id = v.getId();
        if (R.id.toolbarClose == id)
        {
            doExitConfirm();
        }
        else if (!bCamEnabled)
        {
            askPermissions(
                    getResources().getInteger(R.integer.TAG_PERMISSION_CAMERA),
                    getString(R.string.myfiziqsdk_align_circles_camera_reason),
                    Manifest.permission.CAMERA);
        }
        else if (R.id.toolbarHelp == id)
        {
            if (state == State.CONFIRM_FR || state == State.CONFIRM_SD)
            {
                UiUtils.setViewVisibility(captureConfirmHelpView, View.VISIBLE);
            }
            else
            {
                UiUtils.showAlertDialog(
                        getActivity(),
                        getString(R.string.myfiziqsdk_align_circles_title),
                        getString(R.string.myfiziqsdk_align_circles_help),
                        getString(R.string.myfiziqsdk_align_circles_confirm),
                        null,
                        (dialog, which) -> UiUtils.setOnScreenNavigationVisibility(getActivity(), false),
                        (dialog, which) -> UiUtils.setOnScreenNavigationVisibility(getActivity(), false)
                );
            }
        }
        else if (R.id.buttonConfirm == id)
        {
            switch (state)
            {
                case CONFIRM_FR:
                {
                    setState(State.CONFIRM_SD);
                }
                break;

                case CONFIRM_SD:
                {
                    lockButtons = true;

                    if (inPractiseMode)
                    {
                        showPractiseCompleteDialog();
                    }
                    else
                    {
                        saveAvatar();
                    }
                }
                break;
            }
        }
        else if (R.id.buttonRetake == id)
        {
            UiUtils.showAlertDialog(
                    getActivity(),
                    getString(R.string.myfiziqsdk_are_you_sure),
                    getString(R.string.myfiziqsdk_confirm_close_detail),
                    getString(R.string.myfiziqsdk_confirm_retake),
                    getString(R.string.myfiziqsdk_cancel),
                    (dialog, which) ->
                    {
                        capturedFrontImages.clear();
                        capturedSideImages.clear();
                        setState(State.ALIGN);
                    },
                    (dialog, which) -> UiUtils.setOnScreenNavigationVisibility(getActivity(), false)
            );
        }
    }

    private void showError(String errorMessage, AsyncHelper.CallbackVoid callback)
    {
        mHandler.post(() ->
        {
            inspectionErrorView.setErrorText(errorMessage);
            inspectionErrorView.start(callback);
            setState(State.INSPECTION_ERROR);
        });
    }

    public boolean checkInspectResult(String inspectResult, PoseSide side)
    {
        ModelInspect inspect = Orm.newModel(ModelInspect.class);

        if (null == inspect || null == inspect.result)
        {
            Timber.e("ModelInspect object was null when preparing side images to save");
            return false;
        }

        final State curState = state;

        if (!TextUtils.isEmpty(inspectResult))
        {
            inspect.deserialize(inspectResult);
            inspect.result.Image = side;

            if (!inspect.result.isValid())
            {
                showError(inspect.result.getError(getResources()), () -> setState(curState));
                return false;
            }
        }
        else
        {
            inspect.result.Image = side;
            showError(inspect.result.getError(getResources()), () -> setState(curState));
            return false;
        }

        return true;
    }

    @Override
    public void sensorChanged()
    {
        // The sensor object might be null if this method was triggered as it was being destroyed
        if (sensor != null)
        {
            float valueZ = sensor.getZAsScreen(screenH - layoutCircleOrange.getHeight());
            layoutCircleOrange.setY(getRelativeTop(layoutRelative, layoutCircleWhite) + valueZ);
        }
    }

    @Override
    public void noSensorDataReceived()
    {
        /*
        if (sensor == null)
        {
            return;
        }
        long timeSinceLastReading = sensor.getTimeSinceLastSensorReading();
        if (timeSinceLastReading >= SENSOR_TIMEOUT
                && (state == State.ALIGN || state == State.ERROR_UPRIGHT))
        {
            Timber.e(
                    "We haven't received a sensor reading for %sms. We will now bypass the alignment screen.",
                    timeSinceLastReading
            );
            alignmentIsDisabled = true;
            setState(State.FRONT);
        }
        */
    }

    @Override
    public void countDownHalfway()
    {
        if (state != State.ALIGN)
        {
            ObjectAnimator yAnim = ObjectAnimator.ofFloat(toolbar, "translationY", -toolbar.getHeight());
            yAnim.start();
        }
    }

    @Override
    public void countDown3Sec()
    {
    }

    @Override
    public void countDown2Sec()
    {
        if (state != State.ALIGN)
        {
            soundUtils.play(SoundUtils.Sounds.SOUND_BUTTON7);
        }
    }

    @Override
    public void countDown1Sec()
    {
        if (state != State.ALIGN)
        {
            soundUtils.play(SoundUtils.Sounds.SOUND_BUTTON7);
        }
    }

    @Override
    public void countDown0Sec()
    {
        if (state != State.ALIGN)
        {
            takeImages(ModelAvatar.getCaptureFrames(), ModelAvatar.getCaptureOverTimeFrame());
            soundUtils.play(SoundUtils.Sounds.SOUND_BUTTON7);
        }
    }

    private void fireCountdownShutter()
    {
        CompletableFuture<Boolean> animationDone = new CompletableFuture<>();

        long duration = 500;

        shutter.setVisibility(View.VISIBLE);
        ObjectAnimator visAnim = ObjectAnimator.ofFloat(shutter, "alpha", 1f, 0.8f, 0f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(shutter, "scaleX", 0.01f, aspect * 2f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(shutter, "scaleY", 0.01f, aspect * 2f);
        visAnim.setDuration(duration);
        scaleXAnim.setDuration(duration);
        scaleYAnim.setDuration(duration);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(visAnim, scaleXAnim, scaleYAnim);
        set.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                animationDone.complete(true);
            }
        });
        set.start();

        soundUtils.play(SoundUtils.Sounds.SOUND_SHUTTER);

        AsyncHelper.run(() ->
                {
                    if (imageCaptureCounter == null)
                    {
                        Timber.w("Image capture counter is null. Maybe we minimised the app to the background and it was garbage collected?");
                        return false;
                    }

                    try
                    {
                        // Wait until the animation is done
                        animationDone.get();

                        long startTime = System.currentTimeMillis();

                        // Wait until we have taken all images
                        imageCaptureCounter.await();

                        long endTime = System.currentTimeMillis();
                        Timber.i("Waited for %sms after the capture animation finished for all images to be saved", (endTime - startTime));

                        return true;
                    }
                    catch (Exception e)
                    {
                        Timber.e(e);
                        return false;
                    }
                },
                result ->
                {
                    if (result)
                    {
                        runInspection();
                    }
                    else
                    {
                        Timber.e("Not running inspection. Either the photo could not be captured or the shutter animation could not be rendered");
                        // TODO Handle when the photo fails to capture
                    }
                }, true);
    }

    @Override
    public boolean onBackPressed()
    {
        switch (state)
        {
            case ALIGN:
                doExitConfirm();
                break;

            case FRONT:
                setState(State.ALIGN);
                break;

            case SIDE:
                setState(State.FRONT);
                break;

            case CONFIRM_FR:
                setState(State.ALIGN);
                break;

            case CONFIRM_SD:
                setState(State.CONFIRM_FR);
                break;

            default:
                doExitConfirm();
                break;
        }

        return true;
    }

    @Override
    public void countDownExpired()
    {
        ActivityInterface myActivity = getMyActivity();
        if (myActivity == null || myActivity.getActivity() == null)
        {
            // Fragment has been detached
            return;
        }

        if (cameraXImageAnalyzer != null)
        {
            cameraXImageAnalyzer.setRealtimePoseEnabled(false);
        }

        switch (state)
        {
            case ALIGN:
            {
                if (capturedFrontImages.isEmpty())
                {
                    setState(State.FRONT);
                }
                else
                {
                    setState(State.SIDE);
                }
            }
            break;

            case FRONT:
            case SIDE:
            {
                fireCountdownShutter();
            }
            break;
        }
    }

    private void setState(State state)
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        // Stop listening for when the front and side contours change in case rendering is done for another state when we're in this one.
        clearObservables();

        if (cameraXImageAnalyzer != null)
        {
            cameraXImageAnalyzer.setRealtimePoseEnabled(false);
        }

        State fromState = this.state;
        this.state = state;
        switch (this.state)
        {
            case ALIGN:
            {
                adjustToolbarPosition();
                restartSensors();
                countdown.cancel();

                viewCircleBlue.setCallback(this);
                setContourOverlay(null);

                toolbar.setBackgroundResource(android.R.color.transparent);
                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(layoutConfirm, View.INVISIBLE);
                UiUtils.setViewVisibility(countdown, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarTitle, View.INVISIBLE);
                UiUtils.setViewVisibility(bgOverlay, View.VISIBLE);
                UiUtils.setViewVisibility(layoutCircleWhite, View.VISIBLE);
                UiUtils.setViewVisibility(layoutCircleOrange, View.VISIBLE);
                UiUtils.setViewVisibility(layoutCircleBlue, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewTitle, View.VISIBLE);
                UiUtils.setViewVisibility(textViewDetail, View.VISIBLE);
                UiUtils.setViewVisibility(toolbarClose, View.VISIBLE);
                UiUtils.setViewVisibility(toolbarHelp, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);

                ModelSetting.getSettingAsync(
                        ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT,
                        false,
                        debugDisableAlignment ->
                        {
                            // If we've disabled the alignment screen for debugging purposes...
                            if (Boolean.TRUE.equals(debugDisableAlignment))
                            {
                                setState(State.FRONT);
                            }
                        }
                );
            }
            break;

            case FRONT:
            {
                side = PoseSide.front;
                if (mCameraSurface == null) return;

                if (frontContour != null)
                {
                    setContourOverlay(frontContour);
                }
                else
                {
                    // If the contour hasn't been generated yet, listen for when it's ready and then display it
                    setContourOverlay(null);
                    frontContourObservable = new ContourObservable();
                    frontContourObservable.addObserver((o, arg) -> setContourOverlay(frontContour));
                }

                viewCircleBlue.cancel();
                viewCircleBlue.setCallback(null);

                continueSensors();
                countdown.cancel();
                countdown.setCallback(this);

                if (cameraXImageAnalyzer != null)
                {
                    cameraXImageAnalyzer.setSide(side);

                    ModelSetting.getSettingAsync(
                            ModelSetting.Setting.DEBUG_VISUALIZE_POSE,
                            false,
                            cameraXImageAnalyzer::setRealtimePoseEnabled
                    );
                }
                if (SisterColors.getInstance().getChartLineColor() != null)
                {

                    countdown.setTextColor(SisterColors.getInstance().getChartLineColor());
                }
                else
                {
                    countdown.setTextColor(getResources().getColor(R.color.myfiziqsdk_colorPrimary));
                }
                countdown.start();

                toolbar.setTranslationY(0);
                Integer topBarColor = SisterColors.getInstance().getCaptureTopBarColor();
                if (topBarColor != null)
                {
                    toolbar.setBackgroundColor(topBarColor);
                }
                else
                {
                    toolbar.setBackgroundColor(getResources().getColor(R.color.myfiziqsdk_colorPrimary));
                }
                toolbarTitle.setText(R.string.myfiziqsdk_front);


                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(layoutConfirm, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarClose, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarHelp, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewTitle, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewDetail, View.INVISIBLE);
                UiUtils.setViewVisibility(bgOverlay, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleWhite, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleOrange, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleBlue, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarTitle, View.VISIBLE);
                UiUtils.setViewVisibility(countdown, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);
            }
            break;

            case SIDE:
            {
                side = PoseSide.side;
                if (mCameraSurface == null) return;

                if (frontContour != null)
                {
                    setContourOverlay(sideContour);
                }
                else
                {
                    // If the contour hasn't been generated yet, listen for when it's ready and then display it
                    //mCameraSurface.setBitmap(null);
                    sideContourObservable = new ContourObservable();
                    sideContourObservable.addObserver((o, arg) -> setContourOverlay(sideContour));
                }

                continueSensors();
                countdown.cancel();
                countdown.setCallback(this);
                if (SisterColors.getInstance().getChartLineColor() != null)
                {

                    countdown.setTextColor(SisterColors.getInstance().getChartLineColor());
                }
                else
                {
                    countdown.setTextColor(getResources().getColor(R.color.myfiziqsdk_colorPrimary));
                }
                countdown.start();

                if (cameraXImageAnalyzer != null)
                {
                    cameraXImageAnalyzer.setSide(side);

                    if (ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE_POSE, false))
                    {
                        cameraXImageAnalyzer.setRealtimePoseEnabled(true);
                    }
                }

                toolbar.setTranslationY(0);
                Integer topBarColor = SisterColors.getInstance().getCaptureTopBarColor();
                if (topBarColor != null)
                {
                    toolbar.setBackgroundColor(topBarColor);
                }
                else
                {
                    toolbar.setBackgroundColor(getResources().getColor(R.color.myfiziqsdk_colorPrimary));
                }
                toolbarTitle.setText(R.string.myfiziqsdk_side);

                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(layoutConfirm, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarClose, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarHelp, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewTitle, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewDetail, View.INVISIBLE);
                UiUtils.setViewVisibility(bgOverlay, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleWhite, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleOrange, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleBlue, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarTitle, View.VISIBLE);
                UiUtils.setViewVisibility(countdown, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);
            }
            break;

            case CONFIRM_FR:
            {
                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);

                countdown.cancel();
                toolbarTitle.setText(R.string.myfiziqsdk_image_confirmation);

                toolbar.setTranslationY(0);
                toolbar.setBackgroundResource(android.R.color.transparent);

                UiUtils.setViewVisibility(toolbarClose, View.VISIBLE);
                UiUtils.setViewVisibility(toolbarHelp, View.VISIBLE);
                UiUtils.setViewVisibility(textViewTitle, View.INVISIBLE);
                UiUtils.setViewVisibility(textViewDetail, View.INVISIBLE);
                UiUtils.setViewVisibility(bgOverlay, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleWhite, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleOrange, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutCircleBlue, View.INVISIBLE);
                UiUtils.setViewVisibility(toolbarTitle, View.INVISIBLE);
                UiUtils.setViewVisibility(countdown, View.INVISIBLE);
                UiUtils.setViewVisibility(layoutConfirm, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);
                viewPager.setAdapter(new ConfirmPagerAdapter(getMyActivity(), mParameterSet));
                buttonConfirm.setText(R.string.myfiziqsdk_confirm_front);
            }
            break;

            case CONFIRM_SD:
            {
                viewPager.setCurrentItem(1);
                buttonConfirm.setText(R.string.myfiziqsdk_confirm_side);
            }
            break;


            case ERROR:
            {
                countdown.cancel();
                stopSensors();

                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(captureErrorView, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), true);
            }
            break;

            case ERROR_UPRIGHT:
            {
                countdown.cancel();

                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);
            }
            break;

            case INSPECTION_ERROR:
            {
                if (fromState == State.FRONT)
                {
                    capturedFrontImages.clear();
                }
                else if (fromState == State.SIDE)
                {
                    capturedSideImages.clear();
                }
                countdown.cancel();
                inspectionErrorView.setViewCountdownVisible(true);
                inspectionErrorView.setButtonsVisible(false);

                UiUtils.setViewVisibility(captureErrorView, View.GONE);
                UiUtils.setViewVisibility(captureErrorUprightView, View.GONE);
                UiUtils.setViewVisibility(inspectionErrorView, View.VISIBLE);
                UiUtils.setOnScreenNavigationVisibility(getActivity(), false);
            }
            break;
        }
    }

    public void runInspection()
    {
        if (null == sensor)
        {
            return;
        }

        if (BuildConfig.DEBUG && ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, false))
        {
            fileContentSwap();
        }

        sensor.unregisterSensors();

        AsyncProgressDialog asyncProgressDialog = AsyncProgressDialog.showProgress(getActivity(), getString(R.string.please_wait), new AsyncProgressDialog.AsyncProgress()
        {
            private boolean success = true;

            @Override
            public void run(AsyncProgressDialog dlg)
            {
                List<String> sourceImages;

                if (side == PoseSide.front)
                {
                    sourceImages = capturedFrontImages;
                }
                else
                {
                    sourceImages = capturedSideImages;
                }

                String[] sourceImageArray = new String[sourceImages.size()];
                sourceImageArray = sourceImages.toArray(sourceImageArray);


                MyFiziq myFiziqSdk = MyFiziq.getInstance();

                String imageBaseName = sourceImages.get(0).replace(PoseSide.CAPTURE_IMAGE_EXTENSION, "");         // TODO What if we don't have the first image
                String id = myFiziqSdk.getContourId(255, 255, 1280, 720, avatar.getHeight().getValueInCm(), avatar.getWeight().getValueInKg(), avatar.getGender(), side, (float) avatar.getPitch(), 0);

                String[] results = myFiziqSdk.inspect(
                        side,
                        id,
                        sourceImageArray,
                        imageBaseName
                );

                // At least 2 passing results are required to perform inspection & get an avatar.
                if (null != results && results.length > 1)
                {
                    for (int i = 0; i < results.length; i++)
                    {
                        ModelLog.d(String.format("Inspect result [%d]: %s", i, results[i]));
                        if (!checkInspectResult(results[i], side))
                        {
                            success = false;
                            break;
                        }
                    }
                }
                else
                {
                    checkInspectResult(null, side);
                    success = false;
                }

                if (success)
                {
                    switch (side)
                    {
                        case front:
                        {
                            avatar.setSensorValues(sensor);

                            String result = MiscUtils.join(Character.toString((char) 1), results);
                            avatar.setFrontInspectResult(result);
                            avatar.save();
                            MyFiziq.getInstance().post(MyFiziq.NativeOpType.NativeOpPoseFront);
                            //AvatarSegmentWorker.createWorker(avatar, side);
                        }
                        break;

                        case side:
                        {
                            String result = MiscUtils.join(Character.toString((char) 1), results);
                            avatar.setSideInspectResult(result);
                            avatar.save();
                            MyFiziq.getInstance().post(MyFiziq.NativeOpType.NativeOpPoseSide);
                            //AvatarSegmentWorker.createWorker(avatar, side);
                        }
                        break;
                    }
                }

                startSensors();
            }

            @Override
            public void onError()
            {
                success = false;

                /*
                String errorString = getString(R.string.myfiziqsdk_error_unknown);
                Handler handler = new Handler(Looper.getMainLooper());

                handler.post(() -> UiUtils.showAlertDialog(
                        getActivity(),
                        getString(R.string.myfiziqsdk_error_title),
                        errorString,
                        getString(android.R.string.ok),
                        null,
                        null,
                        null));
                */
            }

            @Override
            public void onComplete()
            {
                if (success)
                {
                    switch (state)
                    {
                        case FRONT:
                        {
                            setState(State.SIDE);
                        }
                        break;

                        case SIDE:
                        {
                            setState(State.CONFIRM_FR);
                        }
                        break;
                    }
                }
            }
        });
        UiUtils.setAlertDialogColours(getActivity(), asyncProgressDialog);
    }

    private void fileContentSwap()
    {
        String attemptId = avatar.getAttemptId();

        if (side.equals(PoseSide.front))
        {
            MiscUtils.swapImagesForAttemptId(attemptId, side, "M_180_80_front_pass.png", ModelAvatar.getCaptureFrames());
        }
        else
        {
            MiscUtils.swapImagesForAttemptId(attemptId, side, "M_180_80_side_pass.png", ModelAvatar.getCaptureFrames());
        }
    }

    private void generateContourImages()
    {
        long startFrontTime = System.currentTimeMillis();

        FactoryContour.getContour(255, 100, avatar, PoseSide.front, (float) avatar.getPitch(), 255,
                (contourBitmap, id) ->
                        AsyncHelper.run(
                                () -> styleBitmapImages(contourBitmap, PoseSide.front),
                                styledBitmap ->
                                {
                                    frontContour = styledBitmap;

                                    if (frontContourObservable != null)
                                    {
                                        frontContourObservable.notifyObservers();
                                    }

                                    long endFrontTime = System.currentTimeMillis();
                                    Timber.d("Took %sms to generate and style front contour", endFrontTime - startFrontTime);
                                },
                                true
                        )
        );


        long startSideTime = System.currentTimeMillis();

        FactoryContour.getContour(255, 100, avatar, PoseSide.side, (float) avatar.getPitch(), 255,
                (contourBitmap, id) ->
                        AsyncHelper.run(
                                () -> styleBitmapImages(contourBitmap, PoseSide.side),
                                styledBitmap ->
                                {
                                    sideContour = styledBitmap;

                                    if (sideContourObservable != null)
                                    {
                                        sideContourObservable.notifyObservers();
                                    }

                                    long endSideTime = System.currentTimeMillis();
                                    Timber.d("Took %sms to generate and style side contour", endSideTime - startSideTime);
                                },
                                true
                        )
        );
    }

    @Nullable
    private Bitmap styleBitmapImages(Bitmap bitmap, PoseSide side)
    {
        if (null == bitmap)
        {
            return null;
        }

        int dashColour1 = getResources().getColor(R.color.myfiziqsdk_contour_dash_colour_1);
        int dashColour2 = getResources().getColor(R.color.myfiziqsdk_contour_dash_colour_2);
        int backgroundColour = getResources().getColor(R.color.myfiziqsdk_black_opaque);

        if (SisterColors.getInstance().getChartLineColor() != null)
        {
            dashColour2 = SisterColors.getInstance().getChartLineColor();
        }

        SparseIntArray colorReplacements = new SparseIntArray(3);
        colorReplacements.put(FactoryContour.DEFAULT_DASH1_COLOR, dashColour1);
        colorReplacements.put(FactoryContour.DEFAULT_DASH2_COLOR, dashColour2);
        colorReplacements.put(FactoryContour.DEFAULT_BACKGROUND_COLOR, backgroundColour);

        bitmap = ImageUtils.replaceColours(bitmap, colorReplacements);

        if (side == PoseSide.side)
        {
            bitmap = ImageUtils.mirrorBitmap(bitmap);
        }

        return bitmap;
    }

    private void clearObservables()
    {
        if (frontContourObservable != null)
        {
            frontContourObservable.deleteObservers();
        }

        if (sideContourObservable != null)
        {
            sideContourObservable.deleteObservers();
        }
    }

    private void takeImages(int imageCount, int captureOverTimeFrameMs)
    {
        if (side == PoseSide.front)
        {
            capturedFrontImages.clear();
        }
        else if (side == PoseSide.side)
        {
            capturedSideImages.clear();
        }
        else
        {
            throw new UnsupportedOperationException("Unknown side: " + side);
        }

        imageCaptureCounter = new CountDownLatch(imageCount);

        int captureInterval = captureOverTimeFrameMs / imageCount;

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(imageCount);

        for (int i = 0; i < imageCount; i++)
        {
            int captureAfterMs = i * captureInterval;
            int captureId = i;

            // Schedule the photo to be taken sometime in the near future based on the number of images over the given time frame
            // If the time frame is 1000ms and we're capturing 4 images, we'll capture them every 250 ms.
            executor.schedule(() ->
            {
                Timber.d("Taking photo NOW!");

                takePicture(captureId, imageFilePath ->
                {
                    if (side == PoseSide.front)
                    {
                        capturedFrontImages.add(imageFilePath);
                    }
                    else if (side == PoseSide.side)
                    {
                        capturedSideImages.add(imageFilePath);
                    }
                    else
                    {
                        throw new UnsupportedOperationException("Unknown side: " + side);
                    }

                    imageCaptureCounter.countDown();
                });

            }, captureAfterMs, TimeUnit.MILLISECONDS);
        }
    }

    private void takePicture(int imageNumber, AsyncHelper.Callback<String> callback)
    {
        if (cameraXContainer == null || !cameraXContainer.isBound())
        {
            // TODO Finish the activity or show an error?
            Timber.e("Camera is not initialised. Cannot take a photo");
            return;
        }

        String attemptId = avatar.getAttemptId();
        String sideFilename = side.getSideImageFilename(attemptId, imageNumber);

        String filePath = GlobalContext.getContext().getFilesDir().getAbsolutePath() + "/" + sideFilename;

        SaveImageRequest request = new SaveImageRequest(getContext(), filePath, callback);
        cameraXContainer.captureNextImage(request);
    }

    private void setContourOverlay(Bitmap bitmap)
    {
        contourOverlay.setImageBitmap(bitmap);
    }

    /**
     * Saves the avatar to the filesystem and begins generation.
     * <p>
     * The last step in the capture process.
     */
    private void saveAvatar()
    {
        AsyncHelper.run(
                () ->
                {
                    try
                    {
                        avatar.setStatus(Status.Pending);
                        MyFiziq.getInstance().post(MyFiziq.NativeOpType.NativeOpConfirm);
                        if (mPostProcess)
                        {
                            AvatarUploadWorker.createWorker(avatar);
                        }

                        return true;
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Exception occurred when preparing side images to save");
                        String errorMessage = getString(R.string.myfiziqsdk_error_unknown);
                        showError(errorMessage, () -> setState(State.CONFIRM_SD));
                        lockButtons = false;

                        // Roll back the avatar save operation if an exception occurred.
                        // Don't save an avatar if we can't start the worker. Otherwise it won't generate.
                        // Otherwise the pending avatar will block new avatars from being created.
                        avatar.delete();

                        return false;
                    }
                },
                success ->
                {
                    if (Boolean.TRUE.equals(success))
                    {
                        mParameterSet.startNext(getMyActivity(), true);
                    }
                },
                true
        );
    }

    private void showPractiseCompleteDialog()
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        String titleText = getString(R.string.dialog_practise_scan_complete_title);
        String messageText = getString(R.string.dialog_practise_scan_complete_message);
        String buttonText = getString(R.string.dialog_practise_scan_complete_button);

        new AlertDialog.Builder(getActivity())
                .setTitle(titleText)
                .setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(buttonText, (dialog, which) ->
                {
                    dialog.dismiss();
                    avatar.delete();
                    mParameterSet.startNext(getMyActivity(), true);
                })
                .show();
    }

}
