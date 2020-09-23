package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.google.android.material.tabs.TabLayout;
import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.MYQFragmentPagerAdapter;
import com.myfiziq.sdk.adapters.MyFiziqLoaderManager;
import com.myfiziq.sdk.adapters.ViewPagerAdapter;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.db.TypeOfMeasurement;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.MeasurementType;
import com.myfiziq.sdk.enums.ProgressTimeSpan;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ChartEntriesDataFormatter;
import com.myfiziq.sdk.helpers.SettingsHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.intents.parcels.ViewAvatarRouteRequest;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateTrack;
import com.myfiziq.sdk.models.MyFiziqChartData;
import com.myfiziq.sdk.util.NumberFormatUtils;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.CircleTextView;
import com.myfiziq.sdk.views.LockableNestedScrollView;
import com.myfiziq.sdk.views.SwipeableViewPager;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

public class FragmentProgress extends BaseFragment implements FragmentInterface, CursorHolder.CursorChangedListener
{
    private final static String BUNDLE_SAVED_AVATARS = "BUNDLE_SAVED_AVATARS";

    private LockableNestedScrollView mProgressScrollview;

    private AvatarLayout mProgressAvatarContainer;
    private RelativeLayout mProgressPagerContainer;
    private ProgressBar mProgressLoadingSpinner;

    private AvatarViewSpinner mAvatarOne;
    private ModelAvatar mModelOne;

    private AvatarViewSpinner mAvatarTwo;
    private ModelAvatar mModelTwo;

    private SwipeableViewPager mChartPager;
    private ViewPager mMeasurementPager;
    private TabLayout mDurationTabLayout;
    private ImageView mPageLeft;
    private ImageView mPageRight;
    private ViewPagerAdapter mAdapter;

    private CircleTextView avatarOneChange;
    private CircleTextView avatarOneView;
    private LinearLayout avatarOneSel;
    private TextView avatarOneDate;
    private TextView avatarOneMeasurement;

    private CircleTextView avatarTwoChange;
    private CircleTextView avatarTwoView;
    private LinearLayout avatarTwoSel;
    private TextView avatarTwoDate;
    private TextView avatarTwoMeasurement;

    private ImageView avatarDeltaIndicator;
    private TextView avatarDeltaMeasurement;

    private MYQFragmentPagerAdapter mChartAdapter;

    private IntentManagerService<ModelAvatar> intentManagerService;
    private IntentManagerService<ParameterSet> intentManagerAvatarSelectorService;

    private CursorHolder mHolder;
    private MyFiziqLoaderManager mMQYLoaderManager;

    private static final boolean ROLLUP_DAYS = false;
    private static final boolean ROLLUP_WEEKS = true;
    private static final boolean ROLLUP_MONTHS = true;

    /**
     * If the range of the graph (the difference between minimum and maximum) is less than this value,
     * we will add padding to the chart so that it appears that the graph is showing a data set
     * with this range
     */
    private static final float CHART_NARROWNESS_RANGE_THRESHOLD = 5f;

    private static final float CHART_PADDING_TOP_PERCENT = 20f;
    private static final float CHART_PADDING_BOTTOM_PERCENT = 20f;

    private View view;
    private Bundle mSavedInstanceState;
    //private ParameterSet mViewAvatarSet = null;
    private ParameterSet mSelAvatarSet = null;

    HashMap<MeasurementType, View> mMeasurementViews = new HashMap<>();
    private boolean hasParentFragmentFinishedRendering = false;
    private boolean animationStarted = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        this.view = super.onCreateView(inflater, container, savedInstanceState);

        // Default parameters...
        mSavedInstanceState = getArguments();

        // Saved parameters from session...
        if (savedInstanceState != null)
        {
            mSavedInstanceState = savedInstanceState;
        }

        listenForAvatarsSelected();
        renderPage();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (null != mSelAvatarSet)
        {
            outState.putParcelable(BUNDLE_SAVED_AVATARS, mSelAvatarSet);
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (mChartPager != null)
        {
            // Make sure this gets called in "onDestroy()"
            // Calling it in "onDestroyView()" will have it execute before the fragment transition animation
            // starts which will cause some items to disappear to the user before the transition starts.
            mChartPager.setAdapter(null);
        }

        // Clean up the chart fragments and ensure they're completely destroyed
        //mChartAdapter.clearFragments();

        if (null != intentManagerService)
        {
            intentManagerService.unbindAll();
        }

        if (null != intentManagerAvatarSelectorService)
        {
            intentManagerAvatarSelectorService.unbindAll();
        }
    }

    private ModelAvatar getModelOne()
    {
        if (null != mModelOne)
        {
            return mModelOne;
        }

        if (null != mHolder)
        {
            mModelOne = (ModelAvatar)mHolder.getItem(0);
        }

        return mModelOne;
    }

    private ModelAvatar getModelTwo()
    {
        if (null != mModelTwo)
        {
            return mModelTwo;
        }

        if (null != mHolder)
        {
            mModelTwo = (ModelAvatar)mHolder.getItem(mHolder.getItemCount()-1);
        }

        return mModelTwo;
    }

    private void renderPage()
    {
        if (getActivity() == null || isDetached())
        {
            // Fragment has detached from activity
            return;
        }

        mHolder = new CursorHolder(
                0,
                ORMContentProvider.uri(ModelAvatar.class),
                ModelAvatar.getWhere(
                        String.format("Status='%s'", Status.Completed)
                ),
                ModelAvatar.getOrderBy(2),
                Model.DEFAULT_DEPTH,
                ModelAvatar.class,
                null,
                this);

        mMQYLoaderManager = new MyFiziqLoaderManager(getActivity(), getLoaderManager());
        mProgressScrollview = view.findViewById(R.id.progressTabScrollview);
        mProgressAvatarContainer = view.findViewById(R.id.progressAvatarContainer);
        mProgressAvatarContainer.setScrollingParent(mProgressScrollview);
        mProgressPagerContainer = view.findViewById(R.id.measurementTypePagerContainer);
        mProgressLoadingSpinner = view.findViewById(R.id.progressTabLoadingSpinner);

        mChartPager = view.findViewById(R.id.chartPager);
        mAvatarOne = view.findViewById(R.id.avatarOne);
        mAvatarOne.getAvatarView().setOnClickListener(v ->
        {
            onAvatarOneClicked();
            onAvatarTwoClick();
        });
        mAvatarTwo = view.findViewById(R.id.avatarTwo);
        mAvatarTwo.getAvatarView().setOnClickListener(v ->
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
        avatarOneMeasurement = view.findViewById(R.id.avatarOneMeasurement);

        avatarTwoSel = view.findViewById(R.id.avatarTwoSel);
        avatarTwoChange = view.findViewById(R.id.avatarTwoChange);
        avatarTwoChange.setOnClickListener(v -> onAvatarTwoSelClick());
        avatarTwoView = view.findViewById(R.id.avatarTwoView);
        avatarTwoView.setOnClickListener(v -> onAvatarTwoViewClick());
        avatarTwoDate = view.findViewById(R.id.avatarTwoDate);
        avatarTwoMeasurement = view.findViewById(R.id.avatarTwoMeasurement);

        avatarDeltaIndicator = view.findViewById(R.id.avatarDeltaIndicator);
        avatarDeltaMeasurement = view.findViewById(R.id.avatarDeltaMeasurement);

        //mAvatarOne.getAvatarView().linkView(mAvatarTwo.getAvatarView());
        //mAvatarTwo.getAvatarView().linkView(mAvatarOne.getAvatarView());

        mMeasurementPager = view.findViewById(R.id.measurementPager);
        mDurationTabLayout = view.findViewById(R.id.durationTabs);

        mPageLeft = view.findViewById(R.id.pageLeft);
        mPageRight = view.findViewById(R.id.pageRight);

        mPageLeft.setOnClickListener(v -> onPageLeftClick());
        mPageRight.setOnClickListener(v -> onPageRightClick());

        // Show a preview of the next and previous measurement page titles
        mMeasurementPager.setClipToPadding(false);
        mMeasurementPager.setPadding(10, 0, 10, 0);
        mMeasurementPager.setPageMargin(5);

        // Update the chart when the measurement pager changes
        mMeasurementPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                MeasurementType measurementType = getCurrentMeasurementType();

                if (measurementType == null)
                {
                    Timber.e("Cannot determine measurement type");
                    return;
                }

                try
                {
                    updateChart(measurementType, getModelOne(), getModelTwo());
                    updateMeasurementDelta(measurementType, getModelOne(), getModelTwo());
                }
                catch (ParseException e)
                {
                    Timber.e(e, "Cannot render chart");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });

        mChartAdapter = new MYQFragmentPagerAdapter(getActivity(), getChildFragmentManager(), mChartPager, mDurationTabLayout);
        mChartPager.setAdapter(mChartAdapter);
        mChartPager.setSwipeable(false);
        mDurationTabLayout.setupWithViewPager(mChartPager);

        if (null != mSavedInstanceState)
        {
            if (mSavedInstanceState.containsKey(StateTrack.BUNDLE_SELAVATAR))
            {
                //mViewAvatarSet = bundle.getParcelable(StateTrack.BUNDLE_VIEWAVATAR);
                mSelAvatarSet = mSavedInstanceState.getParcelable(StateTrack.BUNDLE_SELAVATAR);
            }
            else if (mSavedInstanceState.containsKey(BUNDLE_SAVED_AVATARS))
            {
                mSelAvatarSet = mSavedInstanceState.getParcelable(BUNDLE_SAVED_AVATARS);
            }
        }

        if (null != mSelAvatarSet)
        {
            updateCursor(mSelAvatarSet, false);
        }

        if (null != mParameterSet)
        {
            applyParameters(view);
        }

        updateViewPager();
        mMQYLoaderManager.loadCursor(getLoaderManager(), mHolder);
        hideLoadingSpinner();
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_progress;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mChartPager != null)
        {
            // Make sure this gets called in "onDestroy()"
            // Calling it in "onDestroyView()" will have it execute before the fragment transition animation
            // starts which will cause some items to disappear to the user before the transition starts.
            mChartPager.setAdapter(null);
        }

        // Clean up the chart fragments and ensure they're completely destroyed
        //mChartAdapter.clearFragments();

        if (null != intentManagerService)
        {
            intentManagerService.unbindAll();
        }

        if (null != intentManagerAvatarSelectorService)
        {
            intentManagerAvatarSelectorService.unbindAll();
        }
    }

    private void listenForAvatarsSelected()
    {
        intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerAvatarSelectorService = new IntentManagerService<>(getActivity());

        intentManagerAvatarSelectorService.listenIndefinitely(IntentResponses.AVATAR_ONE_SELECTED, (parameterSet)->
        {
            mSelAvatarSet = parameterSet;
            updateCursor(parameterSet, true);
        });

        intentManagerAvatarSelectorService.listenIndefinitely(IntentResponses.AVATAR_TWO_SELECTED, (parameterSet)->
        {
            mSelAvatarSet = parameterSet;
            updateCursor(parameterSet, true);
        });

        intentManagerService.listenIndefinitely(
                IntentResponses.SWAP_AVATARS,
                result ->
                {
                    onAvatarOneClicked();
                    onAvatarTwoClick();
                });
    }

    private void renderAvatars()
    {
        ModelAvatar mModelOne = getModelOne();
        ModelAvatar mModelTwo = getModelTwo();

        if (null != getActivity() && null != mModelOne && null != mModelTwo)
        {
            setAvatar(mAvatarOne, mModelOne);
            setAvatar(mAvatarTwo, mModelTwo);

            updateViewPager();

            String leftDate = TimeFormatUtils.formatShortDate(mModelOne.getRequestDate());
            String rightDate = TimeFormatUtils.formatShortDate(mModelTwo.getRequestDate());

            avatarOneDate.setText(leftDate.toUpperCase());
            avatarTwoDate.setText(rightDate.toUpperCase());

            if (mAdapter != null && mAdapter.getCount() > 0)
            {
                MeasurementType measurementType = getCurrentMeasurementType();

                if (measurementType == null)
                {
                    Timber.e("Cannot determine measurement type");
                    return;
                }

                try
                {
                    updateChart(measurementType, mModelOne, mModelTwo);
                    updateMeasurementDelta(measurementType, mModelOne, mModelTwo);
                }
                catch (ParseException e)
                {
                    Timber.e(e, "Cannot render chart");
                }
            }

            hideLoadingSpinner();
        }
    }

    private void updateMeasurementDelta(MeasurementType measurementType, ModelAvatar mModelOne, ModelAvatar mModelTwo)
    {
        switch (measurementType)
        {
            case TBF:
                updatePercentMeasurementDelta(mModelOne.getAdjustedPercentBodyFat(), mModelTwo.getAdjustedPercentBodyFat());
                break;

            case CHEST:
                Class<? extends Length> preferredChestUnitOfMeasurement = SettingsHelper.getPreferredChestUnitOfMeasurement(mModelOne, mModelTwo);
                updateLengthMeasurementDelta(MeasurementType.CHEST, mModelOne.getAdjustedChest(), mModelTwo.getAdjustedChest(), preferredChestUnitOfMeasurement);
                break;

            case WAIST:
                Class<? extends Length> preferredWaistUnitOfMeasurement = SettingsHelper.getPreferredWaistUnitOfMeasurement(mModelOne, mModelTwo);
                updateLengthMeasurementDelta(MeasurementType.WAIST, mModelOne.getAdjustedWaist(), mModelTwo.getAdjustedWaist(), preferredWaistUnitOfMeasurement);
                break;

            case HIPS:
                Class<? extends Length> preferredHipsUnitOfMeasurement = SettingsHelper.getPreferredHipsUnitOfMeasurement(mModelOne, mModelTwo);
                updateLengthMeasurementDelta(MeasurementType.HIPS, mModelOne.getAdjustedHip(), mModelTwo.getAdjustedHip(), preferredHipsUnitOfMeasurement);
                break;

            case THIGH:
                Class<? extends Length> preferredThighUnitOfMeasurement = SettingsHelper.getPreferredThighUnitOfMeasurement(mModelOne, mModelTwo);
                updateLengthMeasurementDelta(MeasurementType.THIGH, mModelOne.getAdjustedThigh(), mModelTwo.getAdjustedThigh(), preferredThighUnitOfMeasurement);
                break;

            case WEIGHT:
                Class<? extends Weight> preferredWeightUnitOfMeasurement = SettingsHelper.getPreferredWeightUnitOfMeasurement(mModelOne, mModelTwo);
                updateWeightMeasurementDelta(MeasurementType.WEIGHT, mModelOne.getWeight(), mModelTwo.getWeight(), preferredWeightUnitOfMeasurement);
                break;

            case HEIGHT:
                Class<? extends Length> preferredHeightUnitOfMeasurement = SettingsHelper.getPreferredHeightUnitOfMeasurement(mModelOne, mModelTwo);
                updateLengthMeasurementDelta(MeasurementType.HEIGHT, mModelOne.getHeight(), mModelTwo.getHeight(), preferredHeightUnitOfMeasurement);
                break;

            default:
                throw new UnsupportedOperationException("Unknown measurement type: " + measurementType);
        }
    }

    private void updateLengthMeasurementDelta(MeasurementType measurementType, Length inputValue1, Length inputValue2, Class<? extends Length> preferredUnitOfMeasurement)
    {
        // Convert measurements to the user's preferred unit of measurement
        Length value1 = Length.fromCentimeters(preferredUnitOfMeasurement, inputValue1.getValueInCm());
        Length value2 = Length.fromCentimeters(preferredUnitOfMeasurement, inputValue2.getValueInCm());

        if (value1 == null || value2 == null)
        {
            Timber.e("Input was empty");
            return;
        }

        if (preferredUnitOfMeasurement == Centimeters.class && measurementType == MeasurementType.HEIGHT)
        {
            value1.setFormat(Centimeters.heightFormat);
            value2.setFormat(Centimeters.heightFormat);
        }

        avatarOneMeasurement.setText(value1.getFormatted());
        avatarTwoMeasurement.setText(value2.getFormatted());

        BigDecimal value1Numeric = value1.getValueForComparison();
        BigDecimal value2Numeric = value2.getValueForComparison();

        Length deltaValue = Length.fromCentimeters(preferredUnitOfMeasurement, 0);

        if (deltaValue == null)
        {
            Timber.e("Generated length delta object was empty");
            return;
        }

        int comparisonResult = value1Numeric.compareTo(value2Numeric);

        if (comparisonResult > 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_down);
            BigDecimal deltaNumeric = value1Numeric.subtract(value2Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);

        }
        else if (comparisonResult < 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_up);
            BigDecimal deltaNumeric = value2Numeric.subtract(value1Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);

        }
        else
        {
            avatarDeltaIndicator.setVisibility(View.GONE);
            deltaValue.setValueInCm(0);
        }

        String deltaValueFormatted = deltaValue.getFormatted();
        String deltaValueFormattedOnlyNumbers = deltaValueFormatted.replaceAll("[^\\d]", "");

        if (TextUtils.isEmpty(deltaValueFormatted) || TextUtils.isEmpty(deltaValueFormattedOnlyNumbers) || Integer.parseInt(deltaValueFormattedOnlyNumbers) == 0)
        {
            // If the delta value is either empty or 0, do not show it. Both measurements are equal.
            avatarDeltaMeasurement.setText(R.string.empty_comparation_sign);
        }
        else
        {
            if (measurementType == MeasurementType.HEIGHT && deltaValue instanceof Centimeters)
            {
                deltaValue.setFormat(Centimeters.heightFormat);
            }

            // If the delta value is not empty or blank, show it.
            avatarDeltaMeasurement.setText(deltaValue.getFormatted());
        }
    }

    private void updateWeightMeasurementDelta(MeasurementType measurementType, Weight inputValue1, Weight inputValue2, Class<? extends Weight> preferredUnitOfMeasurement)
    {
        // Convert measurements to the user's preferred unit of measurement
        Weight value1 = Weight.fromKilograms(preferredUnitOfMeasurement, inputValue1.getValueInKg());
        Weight value2 = Weight.fromKilograms(preferredUnitOfMeasurement, inputValue2.getValueInKg());

        if (value1 == null || value2 == null)
        {
            Timber.e("Input was empty");
            return;
        }

        avatarOneMeasurement.setText(value1.getFormatted());
        avatarTwoMeasurement.setText(value2.getFormatted());

        BigDecimal value1Numeric = value1.getValueForComparison();
        BigDecimal value2Numeric = value2.getValueForComparison();

        Weight deltaValue = Weight.fromKilograms(preferredUnitOfMeasurement, 0);

        if (deltaValue == null)
        {
            Timber.e("Generated weight delta object was empty");
            return;
        }

        int comparisonResult = value1Numeric.compareTo(value2Numeric);

        if (comparisonResult > 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_down);
            BigDecimal deltaNumeric = value1Numeric.subtract(value2Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
        }
        else if (comparisonResult < 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_up);
            BigDecimal deltaNumeric = value2Numeric.subtract(value1Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
        }
        else
        {
            avatarDeltaIndicator.setVisibility(View.GONE);
            deltaValue.setValueInKg(0);
        }

        String deltaValueFormatted = deltaValue.getFormatted();
        String deltaValueFormattedOnlyNumbers = deltaValueFormatted.replaceAll("[^\\d]", "");

        if (TextUtils.isEmpty(deltaValueFormatted) || TextUtils.isEmpty(deltaValueFormattedOnlyNumbers) || Integer.parseInt(deltaValueFormattedOnlyNumbers) == 0)
        {
            // If the delta value is either empty or 0, do not show it. Both measurements are equal.
            avatarDeltaMeasurement.setText(R.string.empty_comparation_sign);
        }
        else
        {
            // If the delta value is not empty or blank, show it.
            avatarDeltaMeasurement.setText(deltaValue.getFormatted());
        }
    }

    private void updatePercentMeasurementDelta(double value1, double value2)
    {
        DecimalFormat percentFormat = new DecimalFormat("0.0'%'");

        BigDecimal value1Rounded = NumberFormatUtils.roundDoubleHalfUp(value1, 1);
        BigDecimal value2Rounded = NumberFormatUtils.roundDoubleHalfUp(value2, 1);

        avatarOneMeasurement.setText(percentFormat.format(value1Rounded));
        avatarTwoMeasurement.setText(percentFormat.format(value2Rounded));

        Double differenceOfRounded = value1Rounded.subtract(value2Rounded).doubleValue();
        int comparedResult = value1Rounded.compareTo(value2Rounded);

        if (comparedResult > 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_down);
            avatarDeltaMeasurement.setText(percentFormat.format(differenceOfRounded));
        }
        else if (comparedResult < 0)
        {
            avatarDeltaIndicator.setVisibility(View.VISIBLE);
            avatarDeltaIndicator.setImageResource(R.drawable.ic_mini_arrow_up);
            avatarDeltaMeasurement.setText(percentFormat.format(-differenceOfRounded));
        }
        else
        {
            avatarDeltaIndicator.setVisibility(View.GONE);
            avatarDeltaMeasurement.setText(R.string.empty_comparation_sign);
        }
    }

    private void updateChart(MeasurementType measurementType, ModelAvatar mModelOne, ModelAvatar mModelTwo) throws ParseException
    {
        if (null != mModelOne && null != mModelTwo)
        {
            Activity activity = getActivity();

            long maxTimestamp;
            long minTimestamp;

            if (mModelOne.getRequestTime() > mModelTwo.getRequestTime())
            {
                maxTimestamp = mModelOne.getRequestTime();
                minTimestamp = mModelTwo.getRequestTime();
            }
            else
            {
                maxTimestamp = mModelTwo.getRequestTime();
                minTimestamp = mModelOne.getRequestTime();
            }

            int itemCount = mHolder.getItemCount();

            if (null != mHolder && itemCount > 1)
            {
                ArrayList<Entry> sampleEntries = new ArrayList<>(itemCount);
                ArrayList<Entry> adjustedValueEntries = new ArrayList<>(itemCount);

                for (int i = 0; i < itemCount; i++)
                {
                    ModelAvatar avatar = (ModelAvatar) mHolder.getItem(i);
                    if (null != avatar)
                    {
                        long avatarTimestamp = avatar.getRequestTime();

                        if (avatarTimestamp >= minTimestamp && avatarTimestamp <= maxTimestamp)
                        {
                            Entry sampleEntry = new Entry(avatar.getRequestTime(), (float) avatar.getSampleValueInCm(measurementType));
                            sampleEntries.add(sampleEntry);

                            double adjustedValue = avatar.getAdjustedValueInCm(measurementType);

                            Entry adjustedValueEntry = new Entry(avatar.getRequestTime(), (float) adjustedValue);
                            adjustedValueEntries.add(adjustedValueEntry);
                        }
                    }
                }


                AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatterDays = generateXAxisLabelFormatter(ProgressTimeSpan.DAYS);
                AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatterWeeks = generateXAxisLabelFormatter(ProgressTimeSpan.WEEKS);
                AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatterMonths = generateXAxisLabelFormatter(ProgressTimeSpan.MONTHS);
                AsyncHelper.CallbackOperation<Float, String> dataPointFormatter = generateDataPointLabelFormatter(measurementType);
                AsyncHelper.CallbackOperation<Float, String> emptyDataPointFormatter = input -> "";

                MyFiziqChartData daysEntryList;
                MyFiziqChartData weeksEntryList;
                MyFiziqChartData monthsEntryList;

                // Scenario where we only show adjusted values
                removeEmptyValues(adjustedValueEntries);

                if (!adjustedValueEntries.isEmpty() && adjustedValueEntries.get(0).getX() > adjustedValueEntries.get(adjustedValueEntries.size() - 1).getX())
                {
                    // Don't sort the list of entries since they're already ordered by the attempt ID (just like iOS :(  )
                    // Don't change this or else some entries will be out of order
                    Collections.reverse(adjustedValueEntries);
                }

                // An empty data point formatter hides the data point labels on the graph
                List<Entry> daysDataAdjusted = formatEntryData(adjustedValueEntries, ProgressTimeSpan.DAYS, xAxisLabelFormatterDays, dataPointFormatter, ROLLUP_DAYS);
                List<Entry> weeksDataAdjusted = formatEntryData(adjustedValueEntries, ProgressTimeSpan.WEEKS, xAxisLabelFormatterWeeks, dataPointFormatter, ROLLUP_WEEKS);
                List<Entry> monthsDataAdjusted = formatEntryData(adjustedValueEntries, ProgressTimeSpan.MONTHS, xAxisLabelFormatterMonths, dataPointFormatter, ROLLUP_MONTHS);

                daysEntryList = new MyFiziqChartData(daysDataAdjusted);
                weeksEntryList = new MyFiziqChartData(weeksDataAdjusted);
                monthsEntryList = new MyFiziqChartData(monthsDataAdjusted);

                // TODO Refactor so we return an EnumMap for days, weeks and months. All this code can be in a separate method!
                computeAxisForChartData(daysEntryList);
                computeAxisForChartData(weeksEntryList);
                computeAxisForChartData(monthsEntryList);

                int page = 0;
                int lastPosition = mChartPager.getCurrentItem();

                if (null == mChartAdapter)
                {
                    mChartAdapter = new MYQFragmentPagerAdapter(activity, getChildFragmentManager(), mChartPager, mDurationTabLayout);
                    mChartPager.setAdapter(mChartAdapter);
                }
                else
                {
                    mChartAdapter.clear();
                }

                Bundle daysBundle = new ParameterSet.Builder(FragmentTrackChart.class)
                        .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_days)))
                        .addParam(new Parameter(R.id.TAG_CHART_DATA, daysEntryList))
                        .build()
                        .toBundle(activity);
                mChartAdapter.fragmentCommit(page++, FragmentTrackChart.class, daysBundle);


                Bundle weeksBundle = new ParameterSet.Builder(FragmentTrackChart.class)
                        .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_weeks)))
                        .addParam(new Parameter(R.id.TAG_CHART_DATA, weeksEntryList))
                        .build()
                        .toBundle(activity);
                mChartAdapter.fragmentCommit(page++, FragmentTrackChart.class, weeksBundle);


                Bundle monthsBundle = new ParameterSet.Builder(FragmentTrackChart.class)
                        .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_months)))
                        .addParam(new Parameter(R.id.TAG_CHART_DATA, monthsEntryList))
                        .build()
                        .toBundle(activity);
                mChartAdapter.fragmentCommit(page++, FragmentTrackChart.class, monthsBundle);

                // TODO Make this customisable through the ParameterSet?
                mChartPager.setCurrentItem(lastPosition, false);
            }
        }
    }

    private List<Entry> formatEntryData(List<Entry> entries,
                                        ProgressTimeSpan rollupType,
                                        AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatter,
                                        AsyncHelper.CallbackOperation<Float, String> dataPointFormatter,
                                        boolean rollupValues) throws ParseException
    {
        if (rollupValues)
        {
            return ChartEntriesDataFormatter.getLatestValuesForEachTimePeriod(entries, rollupType, xAxisLabelFormatter, dataPointFormatter);
        }
        else
        {
            return ChartEntriesDataFormatter.generateContinuousListOfEntries(entries, xAxisLabelFormatter, dataPointFormatter);
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

    private void onPageLeftClick()
    {
        int item = mMeasurementPager.getCurrentItem();
        if (item > 0)
        {
            mMeasurementPager.setCurrentItem(item - 1);
        }
        else
        {
            int nextPos = mMeasurementPager.getAdapter().getCount() - 1;
            mMeasurementPager.setCurrentItem(nextPos);
        }
    }

    private void onPageRightClick()
    {
        int item = mMeasurementPager.getCurrentItem();
        if (item + 1 < mMeasurementPager.getAdapter().getCount())
        {
            mMeasurementPager.setCurrentItem(item + 1);
        }
        else
        {
            mMeasurementPager.setCurrentItem(0);
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
        ModelAvatar exclude = getModelTwo();
        if (avatarOneSel.getVisibility() == View.VISIBLE)
        {
            avatarOneSel.setVisibility(View.INVISIBLE);

            ParameterSet parameterSet = new ParameterSet.Builder(FragmentAvatarSelector.class)
                    .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_select)))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_RESPONSE, IntentResponses.AVATAR_ONE_SELECTED.name()))
                    .addParam(new Parameter(R.id.TAG_ARG_EXCLUDE_MODEL, exclude))     // Do not let the user choose the same avatar on both the left and right
                    .build();

            mParameterSet.addNextSet(parameterSet);
            mParameterSet.startNext(getMyActivity(), false);
        }
        else
        {
            avatarOneSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarOneViewClick()
    {
        ModelAvatar modelOne = getModelOne();
        if (null != modelOne)
        {
            ViewAvatarRouteRequest requestParcel = new ViewAvatarRouteRequest(modelOne.getId());
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
        ModelAvatar exclude = getModelOne();
        if (avatarTwoSel.getVisibility() == View.VISIBLE)
        {
            avatarTwoSel.setVisibility(View.INVISIBLE);

            ParameterSet parameterSet = new ParameterSet.Builder(FragmentAvatarSelector.class)
                    .addParam(new Parameter(R.id.TAG_TITLE, getString(R.string.myfiziqsdk_track_select)))
                    .addParam(new Parameter(R.id.TAG_ARG_SELECTION_RESPONSE, IntentResponses.AVATAR_TWO_SELECTED.name()))
                    .addParam(new Parameter(R.id.TAG_ARG_EXCLUDE_MODEL, exclude))     // Do not let the user choose the same avatar on both the left and right
                    .build();
            parameterSet.start(getMyActivity());
        }
        else
        {
            avatarTwoSel.setVisibility(View.VISIBLE);
        }
    }

    private void onAvatarTwoViewClick()
    {
        ModelAvatar modelTwo = getModelTwo();
        if (null != modelTwo)
        {
            ViewAvatarRouteRequest requestParcel = new ViewAvatarRouteRequest(modelTwo.getId());
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.VIEW_AVATAR_ROUTE,
                    requestParcel,
                    result -> result.start(getMyActivity())
            );
        }
    }

    @Nullable
    private MeasurementType getCurrentMeasurementType()
    {
        TextView textView = mAdapter.getCurrentView().findViewById(R.id.text);
        Object tag = textView.getTag();

        if (tag instanceof MeasurementType)
        {
            return (MeasurementType) tag;
        }
        else
        {
            return null;
        }
    }

    /**
     * Inflates the view pager based on the data available to us in the selected models.
     */
    private void updateViewPager()
    {
        if (0 == mMeasurementViews.size())
        {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            inflatePagerTextView(inflater, mMeasurementViews, R.string.tbf_caps, MeasurementType.TBF);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.chest_caps, MeasurementType.CHEST);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.waist_caps, MeasurementType.WAIST);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.hips_caps, MeasurementType.HIPS);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.thighs_caps, MeasurementType.THIGH);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.weight_caps, MeasurementType.WEIGHT);
            inflatePagerTextView(inflater, mMeasurementViews, R.string.height_caps, MeasurementType.HEIGHT);
        }

        ModelAvatar mModelOne = getModelOne();
        ModelAvatar mModelTwo = getModelTwo();

        ArrayList<View> views = new ArrayList<>();
        Centimeters zeroCentimeters = new Centimeters(0);

        if (null != mModelOne && null != mModelTwo)
        {
            if (mModelOne.getAdjustedPercentBodyFat() > 0 || mModelTwo.getAdjustedPercentBodyFat() > 0)
            {
                views.add(mMeasurementViews.get(MeasurementType.TBF));
            }

            if (mModelOne.getAdjustedChest().greaterThan(zeroCentimeters) || mModelTwo.getAdjustedChest().greaterThan(zeroCentimeters))
            {
                views.add(mMeasurementViews.get(MeasurementType.CHEST));
            }

            if (mModelOne.getAdjustedWaist().greaterThan(zeroCentimeters) || mModelTwo.getAdjustedWaist().greaterThan(zeroCentimeters))
            {
                views.add(mMeasurementViews.get(MeasurementType.WAIST));
            }

            if (mModelOne.getAdjustedHip().greaterThan(zeroCentimeters) || mModelTwo.getAdjustedHip().greaterThan(zeroCentimeters))
            {
                views.add(mMeasurementViews.get(MeasurementType.HIPS));
            }

            if (mModelOne.getAdjustedThigh().greaterThan(zeroCentimeters) || mModelTwo.getAdjustedThigh().greaterThan(zeroCentimeters))
            {
                views.add(mMeasurementViews.get(MeasurementType.THIGH));
            }

            if (mModelOne.getWeight().greaterThan(new Kilograms(0)) || mModelTwo.getWeight().greaterThan(new Kilograms(0)))
            {
                views.add(mMeasurementViews.get(MeasurementType.WEIGHT));
            }

            if (mModelOne.getHeight().greaterThan(zeroCentimeters) || mModelTwo.getHeight().greaterThan(zeroCentimeters))
            {
                views.add(mMeasurementViews.get(MeasurementType.HEIGHT));
            }
        }

//        if (null == mAdapter)
//        {
        mAdapter = new ViewPagerAdapter(mMeasurementPager, views);
//        }
//        else
//        {
//            mAdapter.setViews(views);
//        }
    }

    private void inflatePagerTextView(LayoutInflater inflater, HashMap<MeasurementType, View> views, @StringRes int titleTextRef, MeasurementType measurementType)
    {
        View tv = inflater.inflate(R.layout.view_textview, null, false);

        String title = getResources().getString(titleTextRef);

        TextView textView = tv.findViewById(R.id.text);
        textView.setText(title);
        textView.setTag(measurementType);

        views.put(measurementType, tv);
    }

    private void showLoadingSpinner()
    {
        UiUtils.setViewVisibility(mProgressLoadingSpinner, View.VISIBLE);
        UiUtils.setViewVisibility(mProgressScrollview, View.GONE);
    }

    private void hideLoadingSpinner()
    {
        UiUtils.setViewVisibility(mProgressLoadingSpinner, View.GONE);
        UiUtils.setViewVisibility(mProgressScrollview, View.VISIBLE);
    }

    private void updateCursor(ParameterSet parameterSet, boolean bReload)
    {
        mModelOne = (ModelAvatar) parameterSet.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
        mModelTwo = (ModelAvatar) parameterSet.getParam(R.id.TAG_MODEL).getParcelableValue();

        if (null != mModelOne && null != mModelTwo)
        {
            // We sort by the attempt ID, just like iOS :(
            // Don't change this or else some entries will be out of order
            mHolder.setWhere(String.format("datetime(requestdate) BETWEEN min(datetime('%s'), datetime('%s')) AND max(datetime('%s'), datetime('%s')) AND Status='%s'",
                    mModelOne.getRequestDateString(),
                    mModelTwo.getRequestDateString(),
                    mModelOne.getRequestDateString(),
                    mModelTwo.getRequestDateString(),
                    Status.Completed));
            mHolder.setOrder(ModelAvatar.getOrderBy(0));
        }
        else
        {
            // We sort by the attempt ID, just like iOS :(
            // Don't change this or else some entries will be out of order
            mHolder.setWhere(String.format("Status='%s'", Status.Completed));
            mHolder.setOrder(ModelAvatar.getOrderBy(2));
        }

        if (bReload)
        {
            showLoadingSpinner();
            mMQYLoaderManager.reloadCursor(getLoaderManager(), mHolder);
        }
    }

    @Override
    public void onCursorChanged(CursorHolder cursorHolder)
    {
        hideLoadingSpinner();
        renderAvatars();
    }

    private AsyncHelper.CallbackOperation<Long, String> generateXAxisLabelFormatter(ProgressTimeSpan timeSpanType)
    {
        return input ->
        {
            if (timeSpanType == ProgressTimeSpan.DAYS || timeSpanType == ProgressTimeSpan.WEEKS)
            {
                String format = TimeFormatUtils.formatDay(new Date(input));
                return format.replace(".", "");
            }
            else if (timeSpanType == ProgressTimeSpan.MONTHS)
            {
                String format = TimeFormatUtils.formatMonth(new Date(input));
                return format.replace(".", "");
            }
            else
            {
                throw new UnsupportedOperationException("Unknown time span type: " + timeSpanType);
            }
        };
    }

    private AsyncHelper.CallbackOperation<Float, String> generateDataPointLabelFormatter(MeasurementType measurementType)
    {
        return input ->
        {
            if (measurementType == MeasurementType.TBF)
            {
                DecimalFormat percentFormat = new DecimalFormat("0.0'%'");
                return percentFormat.format(input);
            }
            else if (measurementType == MeasurementType.CHEST)
            {
                Class<? extends Length> preferredChestUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideLengthUnit();
                TypeOfMeasurement measurement = Length.fromCentimeters(preferredChestUnitOfMeasurement, input);
                return measurement.getFormatted();
            }
            else if (measurementType == MeasurementType.WAIST)
            {
                Class<? extends Length> preferredWaistUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideLengthUnit();
                TypeOfMeasurement measurement = Length.fromCentimeters(preferredWaistUnitOfMeasurement, input);
                return measurement.getFormatted();
            }
            else if (measurementType == MeasurementType.HIPS)
            {
                Class<? extends Length> preferredHipsUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideLengthUnit();
                TypeOfMeasurement measurement = Length.fromCentimeters(preferredHipsUnitOfMeasurement, input);
                return measurement.getFormatted();
            }
            else if (measurementType == MeasurementType.THIGH)
            {
                Class<? extends Length> preferredThighUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideLengthUnit();
                TypeOfMeasurement measurement = Length.fromCentimeters(preferredThighUnitOfMeasurement, input);
                return measurement.getFormatted();
            }
            else if (measurementType == MeasurementType.WEIGHT)
            {
                Class<? extends Weight> preferredWeightUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideWeightUnit();
                TypeOfMeasurement measurement = Weight.fromKilograms(preferredWeightUnitOfMeasurement, input);
                return measurement.getFormatted();
            }
            else if (measurementType == MeasurementType.HEIGHT)
            {
                Class<? extends Length> preferredHeightUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideHeightUnit();
                TypeOfMeasurement measurement = Length.fromCentimeters(preferredHeightUnitOfMeasurement, input);

                if (measurement instanceof Centimeters)
                {
                    measurement.setFormat(Centimeters.heightFormat);
                }

                return measurement.getFormatted();
            }
            else
            {
                throw new IllegalArgumentException("Unknown measurement type:" + measurementType);
            }
        };
    }

    /**
     * Calculates the Axis minimum and maximum values based on the available chart data.
     *
     * @param chartData The chart data to source values from and input the X Axis into.
     */
    private void computeAxisForChartData(MyFiziqChartData chartData)
    {
        float xAxisMinimum = Float.MAX_VALUE;
        float xAxisMaximum = 0;
        float leftAxisMinimum = Float.MAX_VALUE;
        float leftAxisMaximum = 0;

        // Iterate through the data set to find the lowest and highest X and Y values
        for (Entry entry : chartData.getPrimaryDataSetEntries())
        {
            if (entry.getX() > xAxisMaximum)
            {
                xAxisMaximum = entry.getX();
            }

            if (entry.getY() > leftAxisMaximum)
            {
                leftAxisMaximum = entry.getY();
            }

            if (entry.getX() < xAxisMinimum)
            {
                xAxisMinimum = entry.getX();
            }

            if (entry.getY() < leftAxisMinimum)
            {
                leftAxisMinimum = entry.getY();
            }
        }


        if (xAxisMaximum > 0 && xAxisMaximum < 4)
        {
            // Add some padding to the right of the X Axis for data sets that have between 1 and 3 values
            xAxisMaximum += 1;
        }


        // If we have a really narrow data set, get the middle point in the Left Axis and add some
        // padding to the top and bottom to zoom in on the graph
        if (leftAxisMaximum - leftAxisMinimum < CHART_NARROWNESS_RANGE_THRESHOLD)
        {
            float yAxisMinMaxDiff = (leftAxisMaximum - leftAxisMinimum);
            float yAxisMiddlePoint = (yAxisMinMaxDiff / 2) + leftAxisMinimum;

            float padding = CHART_NARROWNESS_RANGE_THRESHOLD / 2f;
            leftAxisMinimum = yAxisMiddlePoint - padding;
            leftAxisMaximum = yAxisMiddlePoint + padding;
        }

        // Add 20% padding to the top and bottom
        leftAxisMinimum *= 1 - (CHART_PADDING_BOTTOM_PERCENT * 0.01);
        leftAxisMaximum *= 1 + (CHART_PADDING_TOP_PERCENT * 0.01);


        chartData.setLeftAxisMin(leftAxisMinimum);
        chartData.setLeftAxisMax(leftAxisMaximum);
        chartData.setXAxisMin(xAxisMinimum);
        chartData.setXAxisMax(xAxisMaximum);
    }

    /**
     * Removes values from an arrays if it is close to zero.
     */
    private void removeEmptyValues(ArrayList<Entry> entries)
    {
        Iterator<Entry> iterator = entries.iterator();

        while (iterator.hasNext())
        {
            Entry entry = iterator.next();

            if (0.1f > entry.getY())
            {
                // If the value is less than 0.1 (i.e. really close to 0, remove it)
                iterator.remove();
            }
        }
    }

    /**
     * Remove a row in both arrays if either is close to zero.
     */
    private void removeEmptyValues(ArrayList<Entry> entries1, ArrayList<Entry> entries2)
    {
        Iterator<Entry> iterator1 = entries1.iterator();
        Iterator<Entry> iterator2 = entries2.iterator();

        while (iterator1.hasNext() && iterator2.hasNext())
        {
            Entry entry1 = iterator1.next();
            Entry entry2 = iterator2.next();

            if (0.1f > entry1.getY() || 0.1f > entry2.getY())
            {
                // If the value is less than 0.1 (i.e. really close to 0, remove it)
                iterator1.remove();
                iterator2.remove();
            }
        }
    }
}
