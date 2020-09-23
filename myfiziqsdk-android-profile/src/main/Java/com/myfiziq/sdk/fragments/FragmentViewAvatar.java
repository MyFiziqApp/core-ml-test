package com.myfiziq.sdk.fragments;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.BodyFatCategoryCalculator;
import com.myfiziq.sdk.helpers.BodyFatCategoryFormatter;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.PendingMessageRepository;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarDataItem;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarRotationService;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.ScrollViewDisableable;
import com.myfiziq.sdk.vo.BodyFatCategory;
import com.myfiziqsdk_android_profile.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;


// TODO Consolidate layout.xml with FragmentViewAvatarHome
public class FragmentViewAvatar extends BaseFragment
{
    protected ScrollViewDisableable scrollView;
    protected AvatarLayout avatarLayout;
    protected AvatarViewSpinner avatar;
    protected LinearLayout layoutData;
    protected LinearLayout layoutBodyFatContainer;
    protected TextView headerBodyComposition;
    protected AvatarDataItem layoutTotalBodyFat;
    protected AvatarDataItem layoutBodyCompositionCategory;
    protected AvatarDataItem layoutChest;
    protected AvatarDataItem layoutWaist;
    protected AvatarDataItem layoutHips;
    protected AvatarDataItem layoutThighs;
    protected AvatarDataItem layoutGender;
    protected AvatarDataItem layoutHeight;
    protected AvatarDataItem layoutWeight;
    protected AvatarDataItem layoutAge;

    protected ImageView playButton;
    protected ImageView resetButton;


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        scrollView = view.findViewById(R.id.viewAvatarScrollView);
        avatar = view.findViewById(R.id.avatar);
        avatarLayout = view.findViewById(R.id.avatarLayout);
        avatarLayout.setScrollingParent(scrollView);
        layoutBodyFatContainer = view.findViewById(R.id.layout_body_fat_container);
        headerBodyComposition = view.findViewById(R.id.header_body_composition);
        layoutTotalBodyFat = view.findViewById(R.id.layout_total_body_fat);
        layoutBodyCompositionCategory = view.findViewById(R.id.layout_body_composition_category);
        layoutData = view.findViewById(R.id.layout_data);
        layoutChest = view.findViewById(R.id.layout_chest);
        layoutWaist = view.findViewById(R.id.layout_waist);
        layoutHips = view.findViewById(R.id.layout_hips);
        layoutThighs = view.findViewById(R.id.layout_thighs);

        layoutGender = view.findViewById(R.id.layout_gender);
        layoutHeight = view.findViewById(R.id.layout_height);
        layoutWeight = view.findViewById(R.id.layout_weight);
        layoutAge = view.findViewById(R.id.layout_age);

        playButton = view.findViewById(R.id.button_play);
        resetButton = view.findViewById(R.id.button_reset);

        if (SisterColors.getInstance().getChartLineColor() == null)
        {
            playButton.setColorFilter(
                    UiUtils.getColorFromStyle(
                            getContext(),
                            R.style.MFPViewAvatarPlayButton,
                            android.R.attr.color
                    )
            );
            resetButton.setColorFilter(
                    UiUtils.getColorFromStyle(
                            getContext(),
                            R.style.MFPViewAvatarResetButton,
                            android.R.attr.color)
            );
        }

        AsyncHelper.run(
                this::retrieveModel,
                this::renderPage,
                true);

        bindListeners();
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_profile;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // If we're viewing an avatar, we've probably come from somewhere that we can navigate back to
        ActionBarHelper.showBackButton(getActivity());

        AsyncHelper.run(
                () ->
                {
                    // Make new avatars "visible"
                    ModelAvatar.makeAvatarsSeen();
                },
                () ->
                {
                },
                true
        );
    }

    @Override
    public void onEnterAnimationEnd()
    {
        super.onExitAnimationStart();
        UiUtils.setScrollBarVisibility(scrollView, true);
    }

    @Override
    public void onExitAnimationStart()
    {
        super.onExitAnimationStart();
        UiUtils.setScrollBarVisibility(scrollView, false);
    }

    /**
     * Obtains a ModelAvatar based on the ID of the model that has been passed into the fragment.
     */
    @Nullable
    private ModelAvatar retrieveModel()
    {
        Bundle bundle = getArguments();
        ParameterSet set;

        if (null != bundle)
        {
            set = bundle.getParcelable(BaseFragment.BUNDLE_PARAMETERS);

            if (null != set && set.hasParam(R.id.TAG_MODEL))
            {
                String avatarId = set.getParam(R.id.TAG_MODEL).getValue();
                return ORMTable.getModel(ModelAvatar.class, avatarId);
            }
            else
            {
                Timber.w("Avatar model not set");
                return null;
            }
        }
        else
        {
            Timber.w("Bundle is null in FragmentViewAvatar");
            return null;
        }
    }

    /**
     * Renders the avatar on the screen.
     *
     * @param model The model to render.
     */
    protected void renderAvatar(@NonNull ModelAvatar model)
    {
        avatar.setModel(model);
        avatar.setScaleToFit(true);
    }

    /**
     * Renders the information on the screen.
     *
     * @param model The model to obtain render information for on the screen.
     */
    protected void renderPage(@Nullable ModelAvatar model)
    {
        if (getActivity() == null || model == null || !model.isCompleted())
        {
            // Not ready to render
            return;
        }

        renderAvatar(model);

        layoutChest.setValueText(model.getAdjustedChest().getFormatted());
        layoutWaist.setValueText(model.getAdjustedWaist().getFormatted());
        layoutHips.setValueText(model.getAdjustedHip().getFormatted());
        layoutThighs.setValueText(model.getAdjustedThigh().getFormatted());


        switch (model.getGender())
        {
            case F:
                layoutGender.setValueText(getString(R.string.female));
                break;

            case M:
                layoutGender.setValueText(getString(R.string.male));
                break;
        }

        Length height = model.getHeight();

        if (height instanceof Centimeters)
        {
            height.setFormat(Centimeters.heightFormat);
        }

        layoutHeight.setValueText(height.getFormatted());
        layoutWeight.setValueText(model.getWeight().getFormatted());

        UiUtils.setViewVisibility(scrollView, View.VISIBLE);

        renderAvatarData(model);
    }

    private void bindListeners()
    {
        final AvatarRotationService rotationService = avatarLayout.getRotationService();

        playButton.setOnClickListener(v ->
        {
            if (!avatar.getAvatarView().isMeshReady())
            {
                return;
            }

            if (rotationService.isRotating())
            {
                rotationService.pauseRotating();
                playButton.setImageResource(R.drawable.ic_play);
            }
            else
            {
                rotationService.startRotating();
                playButton.setImageResource(R.drawable.ic_pause);
                if (SisterColors.getInstance().getChartLineColor() != null)
                {
                    playButton.getDrawable().setTint(SisterColors.getInstance().getChartLineColor());
                }
            }
        });

        resetButton.setOnClickListener(v ->
        {
            if (!avatar.getAvatarView().isMeshReady())
            {
                return;
            }

            if (rotationService.isRotating())
            {
                rotationService.pauseRotating();
                playButton.setImageResource(R.drawable.ic_play);
            }

            rotationService.resetRotation();
        });
    }

    /**
     * Sets the title of the toolbar based on the date the avatar's measurements were taken.
     */
    private void setTitle(@NonNull ModelAvatar model)
    {
        Date requestDate = model.getRequestDate();

        if (requestDate != null)
        {
            String formattedDate = TimeFormatUtils.formatDateForDisplay(requestDate);

            ActionBarHelper.setActionBarTitle(getActivity(), formattedDate);
        }
    }

    /**
     * Renders body fat measurements and categories.
     */
    private void renderBodyFatData(@NonNull ModelAvatar model)
    {
        boolean wasBodyFatPercentRendered = renderBodyFatPercent(model);

        if (wasBodyFatPercentRendered)
        {
            renderBodyFatCategory(model);
        }
    }

    /**
     * Renders the body fat percentage on the screen if we have enough data to render it.
     *
     * @param model A model which contains the body fat percent.
     * @return True if the body fat percent was rendered. False otherwise.
     */
    private boolean renderBodyFatPercent(@NonNull ModelAvatar model)
    {
        DecimalFormat percentFormat = new DecimalFormat("0.0'%'");

        if (model.getAdjustedPercentBodyFat() == 0)
        {
            hideBodyFatMeasurements();
            return false;
        }
        else
        {
            showBodyFatMeasurements();
            layoutTotalBodyFat.setValueText(percentFormat.format(model.getAdjustedPercentBodyFat()));
            return true;
        }
    }

    /**
     * Renders the body fat category on the screen if we have enough data to render it.
     * <p>
     * A date of birth and body fat percent must exist in the model.
     *
     * @param model A model which contains the body fat percent and date of birth of the user.
     */
    private void renderBodyFatCategory(@NonNull ModelAvatar model)
    {
        Resources resources = getResources();

        // Date of birth can be null since we might have had a date of birth when we created the avatar, stored it locally, logged out and now the date of birth is gone
        Date dateOfBirth = DateOfBirthCoordinator.getDateOfBirth();
        long avatarRequestUnixTimestamp = model.getRequestTime();

        if (null == dateOfBirth || avatarRequestUnixTimestamp == 0)
        {
            //Timber.i("Either the date of birth of avatar request time was empty. Not rendering body fat.");
            hideBodyFatCategory();
            return;
        }


        Date avatarRequestedDate = new Date(avatarRequestUnixTimestamp);

        BodyFatCategory bodyFatCategory = BodyFatCategoryCalculator.determineBodyFatCategory(
                model.getGender(),
                dateOfBirth,
                avatarRequestedDate,
                model.getAdjustedPercentBodyFat()
        );

        String indicatorText = BodyFatCategoryFormatter.getIndicatorText(resources, bodyFatCategory);
        int indicatorColour = BodyFatCategoryFormatter.getIndicatorColour(resources, bodyFatCategory);

        if (null == indicatorText || indicatorColour == 0)
        {
            Timber.w("Could not determine indicator for body fat category. Will not be rendered.");
            hideBodyFatCategory();
            return;
        }

        showBodyFatCategory();


        layoutBodyCompositionCategory.setValueText(indicatorText);


        TextView textViewValueBodyCompositionCategory = layoutBodyCompositionCategory.getValueTextView();

        Drawable image = getResources().getDrawable(R.drawable.ic_body_fat_normal);
        image.setColorFilter(new PorterDuffColorFilter(indicatorColour, PorterDuff.Mode.SRC));
        int h = (int) (textViewValueBodyCompositionCategory.getTextSize() * 0.8);
        int w = (int) (h * 2.5);
        image.setBounds(0, 0, w, h);


        textViewValueBodyCompositionCategory.setCompoundDrawables(image, null, null, null);
        textViewValueBodyCompositionCategory.setCompoundDrawablePadding(25);
    }

    /**
     * Hides ALL body fat information on the screen.
     */
    private void hideBodyFatMeasurements()
    {
        headerBodyComposition.setVisibility(View.GONE);
        layoutTotalBodyFat.setVisibility(View.GONE);
        layoutBodyCompositionCategory.setVisibility(View.GONE);
    }

    private void showBodyFatMeasurements()
    {
        headerBodyComposition.setVisibility(View.VISIBLE);
        layoutTotalBodyFat.setVisibility(View.VISIBLE);
        layoutBodyCompositionCategory.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the body fat category on the screen.
     */
    private void hideBodyFatCategory()
    {
        layoutBodyCompositionCategory.setVisibility(View.GONE);
    }

    private void showBodyFatCategory()
    {
        layoutBodyCompositionCategory.setVisibility(View.VISIBLE);
    }

    /**
     * Renders the age on the screen or hides it if it cannot be calculated.
     *
     * @param modelAvatar The avatar to render the age for.
     */
    private void renderAge(@NonNull ModelAvatar modelAvatar)
    {
        Date dateOfBirth = DateOfBirthCoordinator.getDateOfBirth();
        Date completedDate = modelAvatar.getRequestDate();

        if (dateOfBirth == null || completedDate == null)
        {
            Timber.e("Failed to calculate age, either the date of birth is null or the date the avatar was completed is null");
            layoutAge.setVisibility(View.GONE);
            return;
        }

        int age = TimeFormatUtils.yearsBetweenTwoDatesJoda(dateOfBirth, completedDate);

        String ageString = getString(R.string.years, age);
        layoutAge.setValueText(ageString);
    }

    private void renderAvatarData(@NonNull ModelAvatar modelAvatar)
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        renderBodyFatData(modelAvatar);
        renderAge(modelAvatar);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.respond(IntentResponses.HIDE_VIEW_SUPPORT_BUTTON, null);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());

        AsyncHelper.run(
                this::retrieveModel,
                avatar ->
                {
                    if (getActivity() == null)
                    {
                        // Fragment has detached from activity
                        return;
                    }

                    if (avatar != null)
                    {
                        // Ensure that the model is rendered as we resume the fragment incase the
                        // OpenGL Context dies in the background
                        renderAvatar(avatar);
                        setTitle(avatar);

                        PendingMessageRepository.putMessage(IntentResponses.MESSAGE_MODEL_AVATAR, avatar);

                        intentManagerService.respond(IntentResponses.SHOW_VIEW_SUPPORT_BUTTON, null);
                    }
                    else
                    {
                        Timber.w("null avatar model in FragmentViewAvatar");
                    }
                },
                true);
    }
}
