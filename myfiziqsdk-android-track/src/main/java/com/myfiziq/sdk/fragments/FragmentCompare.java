package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentRequests;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.MeasurementType;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.SettingsHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.intents.parcels.ViewAvatarRouteRequest;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateTrack;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.CircleTextView;
import com.myfiziq.sdk.views.ItemViewAvatarsCompare;
import com.myfiziq.sdk.views.LockableNestedScrollView;

import java.text.DecimalFormat;
import java.util.EnumMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FragmentCompare extends BaseFragment implements FragmentInterface
{
    AvatarViewSpinner mAvatarOne;
    CircleTextView avatarOneChange;
    CircleTextView avatarOneView;
    LinearLayout avatarOneSel;
    TextView avatarOneDate;

    AvatarViewSpinner mAvatarTwo;
    CircleTextView avatarTwoChange;
    CircleTextView avatarTwoView;
    LinearLayout avatarTwoSel;
    TextView avatarTwoDate;

    AvatarLayout progressAvatarContainer;
    LinearLayout mMeasurements;
    LockableNestedScrollView mAvatarContainer;

    private ModelAvatar mModelOne;
    private ModelAvatar mModelTwo;

    private EnumMap<MeasurementType, ItemViewAvatarsCompare> mMeasurementsMap = new EnumMap<>(MeasurementType.class);

    private IntentManagerService<ParameterSet> intentManagerService;

    private ParameterSet mViewAvatarSet = null;
    private ParameterSet mSelAvatarSet = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mAvatarContainer = view.findViewById(R.id.avatarContainer);
        progressAvatarContainer = view.findViewById(R.id.progressAvatarContainer);
        progressAvatarContainer.setScrollingParent(mAvatarContainer);

        mAvatarOne = view.findViewById(R.id.avatarOne);
        mAvatarOne.getAvatarView().setOnClickListener(v ->
        {
            onAvatarOneClicked();
            onAvatarTwoClick();
        });
        avatarOneSel = view.findViewById(R.id.avatarOneSel);
        avatarOneChange = view.findViewById(R.id.avatarOneChange);
        avatarOneChange.setOnClickListener(v -> onAvatarOneSelClick());
        avatarOneView = view.findViewById(R.id.avatarOneView);
        avatarOneView.setOnClickListener(v -> onAvatarOneViewClick());
        avatarOneDate = view.findViewById(R.id.avatarOneDate);

        mAvatarTwo = view.findViewById(R.id.avatarTwo);
        mAvatarTwo.getAvatarView().setOnClickListener(v ->
        {
            onAvatarOneClicked();
            onAvatarTwoClick();
        });
        avatarTwoSel = view.findViewById(R.id.avatarTwoSel);
        avatarTwoChange = view.findViewById(R.id.avatarTwoChange);
        avatarTwoChange.setOnClickListener(v -> onAvatarTwoSelClick());
        avatarTwoView = view.findViewById(R.id.avatarTwoView);
        avatarTwoView.setOnClickListener(v -> onAvatarTwoViewClick());
        avatarTwoDate = view.findViewById(R.id.avatarTwoDate);
        mMeasurements = view.findViewById(R.id.layoutMeasurements);

        //mAvatarOne.getAvatarView().linkView(mAvatarTwo.getAvatarView());
        //mAvatarTwo.getAvatarView().linkView(mAvatarOne.getAvatarView());

        Bundle bundle = getArguments();

        if (null != bundle)
        {
            mViewAvatarSet = bundle.getParcelable(StateTrack.BUNDLE_VIEWAVATAR);
            mSelAvatarSet = bundle.getParcelable(StateTrack.BUNDLE_SELAVATAR);
        }

        if (null != mParameterSet)
        {
            applyParameters(view);
        }

        listenForAvatarsSelected();
        renderAvatars();

        return view;
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_compare;
    }

    @Override
    public void onStop()
    {
        //mAvatarOne.clear();
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (null != intentManagerService)
        {
            intentManagerService.unbindAll();
        }
    }

    private void listenForAvatarsSelected()
    {
        intentManagerService = new IntentManagerService<ParameterSet>(getActivity());

        intentManagerService.listenOnce(
                IntentResponses.AVATAR_ONE_SELECTED,
                result ->
                {
                    mModelOne = (ModelAvatar) result.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
                    mModelTwo = (ModelAvatar) result.getParam(R.id.TAG_MODEL).getParcelableValue();
                    renderAvatars();
                });

        intentManagerService.listenOnce(
                IntentResponses.AVATAR_TWO_SELECTED,
                result ->
                {
                    mModelOne = (ModelAvatar) result.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
                    mModelTwo = (ModelAvatar) result.getParam(R.id.TAG_MODEL).getParcelableValue();
                    renderAvatars();
                });

        intentManagerService.listenIndefinitely(
                IntentResponses.SWAP_AVATARS,
                result ->
                {
                    onAvatarOneClicked();
                    onAvatarTwoClick();
                });
    }

    void renderAvatars()
    {
        if (null != getActivity() && null != mModelOne && mModelOne.isCompleted()
                && null != mModelTwo && mModelTwo.isCompleted())
        {

            mMeasurements.removeAllViews();

            DecimalFormat percentFormat = new DecimalFormat("0.0'%'");

            setAvatar(mAvatarOne, mModelOne);
            setAvatar(mAvatarTwo, mModelTwo);

            String leftDate = TimeFormatUtils.formatShortDate(mModelOne.getRequestDate());
            String rightDate = TimeFormatUtils.formatShortDate(mModelTwo.getRequestDate());

            avatarOneDate.setText(leftDate.toUpperCase());
            avatarTwoDate.setText(rightDate.toUpperCase());


            // Only show the Body Fat percent if there's a body fat measurement for BOTH avatars
            if (mModelOne.getAdjustedPercentBodyFat() > 0 && mModelTwo.getAdjustedPercentBodyFat() > 0)
            {
                mMeasurementsMap.put(MeasurementType.TBF, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
                mMeasurements.addView(mMeasurementsMap.get(MeasurementType.TBF));

                mMeasurementsMap.get(MeasurementType.TBF).bind("TBF", percentFormat, mModelOne.getAdjustedPercentBodyFat(), mModelTwo.getAdjustedPercentBodyFat());
            }

            Class<? extends Length> preferredChestUnitOfMeasurement = SettingsHelper.getPreferredChestUnitOfMeasurement(mModelOne, mModelTwo);
            Class<? extends Length> preferredWaistUnitOfMeasurement = SettingsHelper.getPreferredWaistUnitOfMeasurement(mModelOne, mModelTwo);
            Class<? extends Length> preferredHipsUnitOfMeasurement = SettingsHelper.getPreferredHipsUnitOfMeasurement(mModelOne, mModelTwo);
            Class<? extends Length> preferredThighUnitOfMeasurement = SettingsHelper.getPreferredThighUnitOfMeasurement(mModelOne, mModelTwo);
            Class<? extends Length> preferredHeightUnitOfMeasurement = SettingsHelper.getPreferredHeightUnitOfMeasurement(mModelOne, mModelTwo);
            Class<? extends Weight> preferredWeightUnitOfMeasurement = SettingsHelper.getPreferredWeightUnitOfMeasurement(mModelOne, mModelTwo);

            mMeasurementsMap.put(MeasurementType.CHEST, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.CHEST));
            mMeasurementsMap.put(MeasurementType.WAIST, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.WAIST));
            mMeasurementsMap.put(MeasurementType.HIPS, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.HIPS));
            mMeasurementsMap.put(MeasurementType.THIGH, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.THIGH));
            mMeasurementsMap.put(MeasurementType.HEIGHT, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.HEIGHT));
            mMeasurementsMap.put(MeasurementType.WEIGHT, (ItemViewAvatarsCompare) LayoutInflater.from(getActivity()).inflate(R.layout.view_measurement_track, mMeasurements, false));
            mMeasurements.addView(mMeasurementsMap.get(MeasurementType.WEIGHT));

            mMeasurementsMap.get(MeasurementType.CHEST).bind("CHEST", mModelOne.getAdjustedChest(), mModelTwo.getAdjustedChest(), preferredChestUnitOfMeasurement);
            mMeasurementsMap.get(MeasurementType.WAIST).bind("WAIST", mModelOne.getAdjustedWaist(), mModelTwo.getAdjustedWaist(), preferredWaistUnitOfMeasurement);
            mMeasurementsMap.get(MeasurementType.HIPS).bind("HIPS", mModelOne.getAdjustedHip(), mModelTwo.getAdjustedHip(), preferredHipsUnitOfMeasurement);
            mMeasurementsMap.get(MeasurementType.THIGH).bind("THIGHS", mModelOne.getAdjustedThigh(), mModelTwo.getAdjustedThigh(), preferredThighUnitOfMeasurement);
            mMeasurementsMap.get(MeasurementType.HEIGHT).bind("HEIGHT", mModelOne.getHeight(), mModelTwo.getHeight(), preferredHeightUnitOfMeasurement);
            mMeasurementsMap.get(MeasurementType.WEIGHT).bind("WEIGHT", mModelOne.getWeight(), mModelTwo.getWeight(), preferredWeightUnitOfMeasurement);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (null != activity)
        {
            ActionBarHelper.enableSwapButton(activity);
        }
    }

    private void setAvatar(AvatarViewSpinner avatarView, ModelAvatar modelAvatar)
    {
        if (modelAvatar != null && modelAvatar.isCompleted())
        {
            avatarView.setModel(modelAvatar);
            avatarView.setScaleToFit(true);
        }
    }

    private void onAvatarOneClicked()
    {
        if (avatarOneSel.getVisibility() == View.VISIBLE)
        {
            avatarOneSel.setVisibility(View.INVISIBLE);
        }
        else
        {
            avatarOneSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarOneSelClick()
    {
        if (avatarOneSel.getVisibility() == View.VISIBLE)
        {
            avatarOneSel.setVisibility(View.INVISIBLE);

            ParameterSet parameterSet = new ParameterSet.Builder(FragmentAvatarSelector.class)
                    .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_select)))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_RESPONSE, IntentResponses.AVATAR_ONE_SELECTED.name()))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_MODEL, mModelOne))
                    .addParam(new Parameter(R.id.TAG_ARG_EXCLUDE_MODEL, mModelTwo))     // Do not let the user choose the same avatar on both the left and right
                    .build();

            IntentManagerService<ParameterSet> intentManagerServiceAvatarSelector = new IntentManagerService<>(getActivity());
            intentManagerServiceAvatarSelector.request(
                    IntentRequests.AVATAR_SELECTOR,
                    parameterSet
            );
        }
        else
        {
            avatarOneSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarOneViewClick()
    {
        if (null != mModelOne)
        {
            ViewAvatarRouteRequest requestParcel = new ViewAvatarRouteRequest(mModelOne.getId());
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.VIEW_AVATAR_ROUTE,
                    requestParcel,
                    result -> result.start(getMyActivity())
            );
        }
    }

    private void onAvatarTwoClick()
    {
        if (avatarTwoSel.getVisibility() == View.VISIBLE)
        {
            avatarTwoSel.setVisibility(View.INVISIBLE);
        }
        else
        {
            avatarTwoSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarTwoSelClick()
    {
        if (avatarTwoSel.getVisibility() == View.VISIBLE)
        {
            avatarTwoSel.setVisibility(View.INVISIBLE);

            ParameterSet parameterSet = new ParameterSet.Builder(FragmentAvatarSelector.class)
                    .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_select)))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_RESPONSE, IntentResponses.AVATAR_TWO_SELECTED.name()))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_MODEL, mModelTwo))
                    .addParam(new Parameter(R.id.TAG_ARG_EXCLUDE_MODEL, mModelOne))     // Do not let the user choose the same avatar on both the left and right
                    .build();

            IntentManagerService<ParameterSet> intentManagerServiceAvatarSelector = new IntentManagerService<>(getActivity());
            intentManagerServiceAvatarSelector.request(
                    IntentRequests.AVATAR_SELECTOR,
                    parameterSet
            );
        }
        else
        {
            avatarTwoSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarTwoViewClick()
    {
        if (null != mModelTwo)
        {
            ViewAvatarRouteRequest requestParcel = new ViewAvatarRouteRequest(mModelTwo.getId());
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.VIEW_AVATAR_ROUTE,
                    requestParcel,
                    result -> result.start(getMyActivity())
            );
        }
    }
}
