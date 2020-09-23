package com.myfiziq.sdk.fragments;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.adapters.MyFiziqLoaderManager;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Imperial;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.LocalUserDataKey;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.SystemOfMeasurement;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.InsightsFormulas;
import com.myfiziq.sdk.helpers.BodyFatCategoryCalculator;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.MyFiziqLocalUserDataHelper;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import com.myfiziq.sdk.lifecycle.PendingMessageRepository;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarRotationService;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.InsightsBottomSheet;
import com.myfiziq.sdk.views.ScrollViewDisableable;
import com.myfiziq.sdk.vo.BodyFatCategory;
import com.myfiziqsdk_android_profile.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import timber.log.Timber;


public class FragmentViewAvatarInsights extends BaseFragment
{
    protected ScrollViewDisableable scrollView;
    protected AvatarLayout avatarLayout;
    protected AvatarViewSpinner avatar;
    protected LinearLayout layoutData;
    protected LinearLayout layoutBodyFatContainer;
    protected TextView headerBodyComposition;

    protected CardView genderCard;
    protected CardView heightCard;
    protected CardView weightCard;
    protected CardView ageCard;
    protected CardView ethnicityCard;

    protected ImageView playButton;
    protected ImageView resetButton;

    protected CardView waisthipcard;
    protected CardView waistheightcard;
    protected CardView waistcircumferencecard;
    protected CardView healthriskcard;

    protected TextView overviewBubble;
    protected TextView ethnicityBubble;

    protected CardView TBFcard;
    protected CardView chestcard;
    protected CardView waistcard;
    protected CardView hipscard;
    protected CardView thighscard;

    final String htmlBaseUrl = "file:///android_res/raw/";

    private MyFiziqLoaderManager userProfileCacheLoader;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        scrollView = view.findViewById(R.id.viewAvatarScrollView);
        avatar = view.findViewById(R.id.avatar);
        avatar.getAvatarView().setBackgroundColor(getResources().getColor(R.color.myfiziqsdk_grey6));

        waisthipcard = view.findViewById(R.id.waisthipratiocard);
        waistheightcard = view.findViewById(R.id.waistheightcard);
        waistcircumferencecard = view.findViewById(R.id.waistcircumferencecard);
        healthriskcard = view.findViewById(R.id.healthriskcard);
        overviewBubble = view.findViewById(R.id.overviewbubble);
        ethnicityBubble = view.findViewById(R.id.ethnicitybubble);
        TBFcard = view.findViewById(R.id.TBFcard);
        ((TextView) TBFcard.findViewById(R.id.rowunit)).setVisibility(View.INVISIBLE);
        ((ImageView) TBFcard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.tbf_icon);

        avatarLayout = view.findViewById(R.id.avatarLayout);
        avatarLayout.setScrollingParent(scrollView);
        layoutBodyFatContainer = view.findViewById(R.id.layout_body_fat_container);
        headerBodyComposition = view.findViewById(R.id.header_body_composition);
        layoutData = view.findViewById(R.id.layout_data);

        chestcard = view.findViewById(R.id.chestcard);
        chestcard.findViewById(R.id.info).setVisibility(View.GONE);
        ((TextView) chestcard.findViewById(R.id.rowtitle)).setText(R.string.chest);
        ((ImageView) chestcard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.lilmanchest);
        (chestcard.findViewById(R.id.rowpill)).setVisibility(View.GONE);

        waistcard = view.findViewById(R.id.waistcard);
        waistcard.findViewById(R.id.info).setVisibility(View.GONE);
        ((TextView) waistcard.findViewById(R.id.rowtitle)).setText(R.string.waist);
        ((ImageView) waistcard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.lilmanwaist);
        (waistcard.findViewById(R.id.rowpill)).setVisibility(View.GONE);

        hipscard = view.findViewById(R.id.hipscard);
        hipscard.findViewById(R.id.info).setVisibility(View.GONE);
        ((TextView) hipscard.findViewById(R.id.rowtitle)).setText(R.string.hips);
        ((ImageView) hipscard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.lilmanhips);
        (hipscard.findViewById(R.id.rowpill)).setVisibility(View.GONE);

        thighscard = view.findViewById(R.id.thighscard);
        thighscard.findViewById(R.id.info).setVisibility(View.GONE);
        ((TextView) thighscard.findViewById(R.id.rowtitle)).setText(R.string.thighs);
        ((ImageView) thighscard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.lilmanthigh);
        (thighscard.findViewById(R.id.rowpill)).setVisibility(View.GONE);

        genderCard = view.findViewById(R.id.gendercard);
        (genderCard.findViewById(R.id.rowpill)).setVisibility(View.GONE);
        ((ImageView) genderCard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.gender_icon);
        ((ImageView) genderCard.findViewById(R.id.info)).setVisibility(View.GONE);
        ((TextView) genderCard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
        ((TextView) genderCard.findViewById(R.id.rowtitle)).setText(R.string.gender);
        sendViewToEnd(genderCard);

        ageCard = view.findViewById(R.id.agecard);
        (ageCard.findViewById(R.id.rowpill)).setVisibility(View.GONE);
        ((ImageView) ageCard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.birthday_cake);
        ((TextView) ageCard.findViewById(R.id.rowtitle)).setText(R.string.age);
        ((ImageView) ageCard.findViewById(R.id.info)).setVisibility(View.GONE);

        heightCard = view.findViewById(R.id.heightcard);
        ((TextView) heightCard.findViewById(R.id.rowtitle)).setText(R.string.height);
        (heightCard.findViewById(R.id.rowpill)).setVisibility(View.GONE);
        ((ImageView) heightCard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.height_icon);
        ((ImageView) heightCard.findViewById(R.id.rowlilman)).setPadding(17, 0, 0, 0);
        ((ImageView) heightCard.findViewById(R.id.info)).setVisibility(View.GONE);

        weightCard = view.findViewById(R.id.weightcard);
        ((TextView) weightCard.findViewById(R.id.rowtitle)).setText(R.string.weight);
        (weightCard.findViewById(R.id.rowpill)).setVisibility(View.GONE);
        ((ImageView) weightCard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.weight_icon);
        ((ImageView) weightCard.findViewById(R.id.info)).setVisibility(View.GONE);

        ethnicityCard = view.findViewById(R.id.ethnicitycard);
        ((TextView) ethnicityCard.findViewById(R.id.rowtitle)).setText(R.string.ethnicity);
        ((TextView) ethnicityCard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
        (ethnicityCard.findViewById(R.id.rowpill)).setVisibility(View.GONE);
        ((ImageView) ethnicityCard.findViewById(R.id.rowlilman)).setImageResource(R.drawable.ethnicity_icon);
        ((ImageView) ethnicityCard.findViewById(R.id.info)).setVisibility(View.GONE);
        ((TextView) ethnicityCard.findViewById(R.id.rowvalue)).setTextSize(15);
        sendViewToEnd(ethnicityCard);

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

    private void handleToolbarIcons(ModelAvatar model)
    {
        Bundle bundle = getArguments();
        ParameterSet set;

        if (null != bundle)
        {
            set = bundle.getParcelable(BaseFragment.BUNDLE_PARAMETERS);

            if (null != set && set.hasParam(R.id.TAG_ARG_VIEW))
            {
                ActionBarHelper.showBackButton(getActivity());
                IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
                intentManagerService.respond(IntentResponses.SHOW_VIEW_SUPPORT_BUTTON, null);
                setTitle(model);
            }
            else
            {
                ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_home));
            }
        }
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_view_avatar_insights;
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

    String getWeightAbbreviation()
    {
        if (AppWideUnitSystemHelper.getAppWideUnitSystemSync() == Imperial.class)
        {
            return "lbs";
        }
        else
        {
            return "kg";
        }
    }

    /**
     * Renders the avatar on the screen.
     *
     * @param model The model to render.
     */
    protected void renderAvatar(@Nullable ModelAvatar model)
    {
        if (model != null)
        {
            avatar.setModel(model);
            avatar.setScaleToFit(true);
        }
    }

    private void handleOverviewBubbles(ModelAvatar model, int age)
    {
        String genderdisplay = "";
        switch (model.getGender())
        {
            case F:
                genderdisplay = getString(R.string.female);
                break;

            case M:
                genderdisplay = getString(R.string.male);
                break;
        }

        String overviewBubbletext = genderdisplay;
        Weight weight = model.getWeight();
        weight.setFormat(new DecimalFormat("0"));
        String roundedWeight = weight.getFormatted();

        Length height = model.getHeight();
        if (height instanceof Centimeters)
        {
            height.setFormat(Centimeters.heightFormat);
        }
        String roundedHeight = height.getFormatted();

        overviewBubbletext = overviewBubbletext + " • " + age + " yrs";
        overviewBubbletext = overviewBubbletext + " • " + roundedWeight + " " + getWeightAbbreviation();
        overviewBubbletext = overviewBubbletext + " • " + roundedHeight;

        overviewBubble.setText(overviewBubbletext);
    }

    /**
     * Renders the information on the screen.
     *
     * @param model The model to obtain render information for on the screen.
     */
    protected void renderPage(@Nullable ModelAvatar model)
    {
        if (null != getActivity() && null != model && model.isCompleted())
        {
            ActivityInterface activityInterface = getMyActivity();
            if (activityInterface instanceof BaseActivityInterface)
            {
                PendingMessageRepository.putMessage(IntentResponses.MESSAGE_MODEL_AVATAR, model);
            }

            handleToolbarIcons(model);
            renderAvatar(model);

            Class<? extends SystemOfMeasurement> unitSystem = AppWideUnitSystemHelper.getAppWideUnitSystemSync();

            String chestrowDisplay = model.getAdjustedChest().getFormatted();
            String waistrowDisplay = model.getAdjustedWaist().getFormatted();
            String hiprowDisplay = model.getAdjustedHip().getFormatted();
            String thighrowDisplay = model.getAdjustedThigh().getFormatted();
            String heightrowDisplay = model.getHeight().getFormatted();

            if (unitSystem == Imperial.class)
            {
                ((TextView) chestcard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
                ((TextView) waistcard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
                ((TextView) hipscard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
                ((TextView) thighscard.findViewById(R.id.rowunit)).setVisibility(View.GONE);
                ((TextView) heightCard.findViewById(R.id.rowunit)).setVisibility(View.GONE);

                sendViewToEnd(chestcard);
                sendViewToEnd(waistcard);
                sendViewToEnd(hipscard);
                sendViewToEnd(thighscard);
                sendViewToEnd(heightCard);

            }
            else
            {
                chestrowDisplay = Double.toString(model.getAdjustedChest().getValueForComparison().doubleValue());
                waistrowDisplay = Double.toString(model.getAdjustedWaist().getValueForComparison().doubleValue());
                hiprowDisplay = Double.toString(model.getAdjustedHip().getValueForComparison().doubleValue());
                thighrowDisplay = Double.toString(model.getAdjustedThigh().getValueForComparison().doubleValue());
                heightrowDisplay = Double.toString(model.getHeight().getValueForComparison().doubleValue());
            }

            ((TextView) chestcard.findViewById(R.id.rowvalue)).setText(chestrowDisplay);
            ((TextView) waistcard.findViewById(R.id.rowvalue)).setText(waistrowDisplay);
            ((TextView) hipscard.findViewById(R.id.rowvalue)).setText(hiprowDisplay);
            ((TextView) thighscard.findViewById(R.id.rowvalue)).setText(thighrowDisplay);
            ((TextView) heightCard.findViewById(R.id.rowvalue)).setText(heightrowDisplay);

            String weightRowDisplay = new DecimalFormat("0.0").format(model.getWeight().getValueForComparison().doubleValue());
            ((TextView) weightCard.findViewById(R.id.rowvalue)).setText(weightRowDisplay);
            ((TextView) weightCard.findViewById(R.id.rowunit)).setText(getWeightAbbreviation());

            switch (model.getGender())
            {
                case F:
                    ((TextView) genderCard.findViewById(R.id.rowvalue)).setText(R.string.female);
                    break;

                case M:
                    ((TextView) genderCard.findViewById(R.id.rowvalue)).setText(R.string.male);
                    break;
            }

            Length height = model.getHeight();

            if (height instanceof Centimeters)
            {
                height.setFormat(Centimeters.heightFormat);
            }

            UiUtils.setViewVisibility(scrollView, View.VISIBLE);

            userProfileCacheLoader = new MyFiziqLoaderManager(getActivity(), getLoaderManager());
            userProfileCacheLoader.loadCursor(1,
                    ORMContentProvider.uri(ModelUserProfile.class),
                    "",
                    null,
                    ModelUserProfile.class,
                    null,
                    cursorHolder ->
                    {
                    }
//                            onUserProfileCacheChanged(cursorHolder, model)
            );

            Date dateOfBirth = DateOfBirthCoordinator.getDateOfBirth();
            String ethString = MyFiziqLocalUserDataHelper.getValue(LocalUserDataKey.ETHNICITY);
            int ethnicitynum = 0;
            if (ethString != null)
            {
                ethnicitynum = Integer.parseInt(ethString);
            }
            renderBodyFatCategory(dateOfBirth, model, ethnicitynum);

        }
    }

    void sendViewToEnd(CardView cv)
    {
        TextView valtv = ((TextView) cv.findViewById(R.id.rowvalue));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) valtv.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.rightMargin = 30;
        valtv.setLayoutParams(params);
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
    private void setTitle(ModelAvatar model)
    {
        if (getActivity() != null && model != null)
        {
            Date requestDate = model.getRequestDate();

            if (requestDate != null)
            {
                Locale currentLocale = getLocale();
                SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", currentLocale);
                String formattedDate = dateFormat.format(requestDate);

                ActionBarHelper.setActionBarTitle(getActivity(), formattedDate);
            }
        }
    }

    /**
     * Renders the body fat category on the screen if we have enough data to render it.
     * <p>
     * A date of birth and body fat percent must exist in the model.
     *
     * @param model A model which contains the body fat percent and date of birth of the user.
     * @return True if the body fat category was rendered. False otherwise.
     */
    private boolean renderBodyFatCategory(@Nullable Date dateOfBirth, ModelAvatar model, int ethnicitynum)
    {
        long avatarRequestUnixTimestamp = model.getRequestTime();
        double abfp = model.getAdjustedPercentBodyFat();
        boolean ageAvailable = false;
        int age = 0;
        BodyFatCategory bodyFatCategory = BodyFatCategory.UNKNOWN;

        if (null != dateOfBirth && avatarRequestUnixTimestamp != 0)
        {
            Date avatarRequestedDate = new Date(avatarRequestUnixTimestamp);
            bodyFatCategory = BodyFatCategoryCalculator.determineBodyFatCategory(
                    model.getGender(),
                    dateOfBirth,
                    avatarRequestedDate,
                    abfp
            );
            age = TimeFormatUtils.yearsBetweenTwoDatesJoda(dateOfBirth, avatarRequestedDate);
            ageAvailable = true;
        }

        boolean ethnicityAvailable = ethnicitynum > 0 && ethnicitynum < 4;
        boolean bodyFatCatagoryAvailable = bodyFatCategory != BodyFatCategory.UNKNOWN;

        if (ethnicityAvailable)
        {
            String abbreviation = InsightsFormulas.getEthnicityAbbreviation(ethnicitynum, getResources());
            ethnicityBubble.setText(getString(R.string.ethnicity) + ": " + abbreviation);
            ethnicityBubble.setOnClickListener(view ->
            {
                UiUtils.showAlertDialog(
                        getActivity(),
                        getString(R.string.ethnicity),
                        InsightsFormulas.getEthnicityDisplayFromNumber(ethnicitynum, getResources()),
                        getString(R.string.myfiziqsdk_ok),
                        null,
                        null,
                        null
                );
            });
        }
        else
        {
            ethnicityBubble.setVisibility(View.GONE);
        }

        if (ageAvailable)
        {
            handleOverviewBubbles(model, age);
            ((TextView) ageCard.findViewById(R.id.rowvalue)).setText(Integer.toString(age));
            ((TextView) ageCard.findViewById(R.id.rowunit)).setText("yrs");
        }
        else
        {
            overviewBubble.setVisibility(View.GONE);
            ageCard.setVisibility(View.GONE);
        }

        if (bodyFatCatagoryAvailable)
        {
            DecimalFormat percentFormat = new DecimalFormat("0.0'%'");
            InsightsFormulas.InsightsOutcome outcome = InsightsFormulas.convertBFCtoBctOutcome(bodyFatCategory);
            Drawable bfPill = InsightsFormulas.getPillDrawable(outcome, getResources());
            ((TextView) TBFcard.findViewById(R.id.rowvalue)).setText(percentFormat.format(abfp));
            ((TextView) TBFcard.findViewById(R.id.rowpill)).setBackground(bfPill);
            InsightsFormulas.InsightsOutcome tbfoutcome = InsightsFormulas.convertBFCtoBctOutcome(bodyFatCategory);
            ((TextView) TBFcard.findViewById(R.id.rowpill)).setText(tbfoutcome.displayName());
            ((ImageView) TBFcard.findViewById(R.id.info)).setOnClickListener((View.OnClickListener) view ->
            {
                InsightsBottomSheet bbs = new InsightsBottomSheet();
                bbs.rawres = R.raw.bodyfatpercentage;
                bbs.show(getFragmentManager(), "bctbottomsheet");
            });
        }
        else
        {
            TBFcard.setVisibility(View.GONE);
            headerBodyComposition.setVisibility(View.GONE);
        }


        ((TextView) healthriskcard.findViewById(R.id.cardtitle)).setText(getString(R.string.overall_health_risk));
        ((TextView) healthriskcard.findViewById(R.id.cardunit)).setText("");
        ((TextView) healthriskcard.findViewById(R.id.cardtitle)).setTypeface(null, Typeface.BOLD);
        ((TextView) healthriskcard.findViewById(R.id.moreinfo)).setOnClickListener((View.OnClickListener) view ->
        {
            InsightsBottomSheet bbs = new InsightsBottomSheet();
            bbs.rawres = R.raw.bcthealthrisk;
            bbs.show(getFragmentManager(), "bctbottomsheet");

        });
        if (ethnicityAvailable && bodyFatCatagoryAvailable)
        {
            InsightsFormulas.InsightsOutcome overallRiskOutcome = InsightsFormulas.OverallRisk(model.getAdjustedWaist().getValueInCm(), model.getGender(), ethnicitynum, bodyFatCategory);
            int overallRiskNum = overallRiskOutcome.ordinal() + 1;
            ((TextView) healthriskcard.findViewById(R.id.cardvalue)).setText(Integer.toString(overallRiskNum));
            Drawable healthRiskDrawable = InsightsFormulas.getPillDrawable(overallRiskOutcome, getResources());
            ((TextView) healthriskcard.findViewById(R.id.pill)).setBackground(healthRiskDrawable);
            ((TextView) healthriskcard.findViewById(R.id.pill)).setText(overallRiskOutcome.displayName());
        }
        else
        {
            if (!ethnicityAvailable)
            {
                ((TextView) healthriskcard.findViewById(R.id.pill)).setVisibility(View.GONE);
            }
            ((TextView) healthriskcard.findViewById(R.id.cardunit)).setText("");
            ((TextView) healthriskcard.findViewById(R.id.cardvalue)).setText("-");
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        ((TextView) waisthipcard.findViewById(R.id.cardtitle)).setTypeface(null, Typeface.BOLD);
        ((TextView) waisthipcard.findViewById(R.id.cardtitle)).setText(getString(R.string.waist_to_hip_ratio));
        ((TextView) waisthipcard.findViewById(R.id.cardunit)).setText("");
        double waistToHipRatio = model.getAdjustedWaist().getValueInCm() / model.getAdjustedHip().getValueInCm();
        ((TextView) waisthipcard.findViewById(R.id.cardvalue)).setText(decimalFormat.format(waistToHipRatio));
        ((TextView) waisthipcard.findViewById(R.id.moreinfo)).setOnClickListener((View.OnClickListener) view ->
        {
            InsightsBottomSheet bbs = new InsightsBottomSheet();
            bbs.rawres = R.raw.waisttohipratio;
            bbs.show(getFragmentManager(), "bctbottomsheet");
        });

        if (ethnicityAvailable)
        {
            InsightsFormulas.InsightsOutcome whipoutcome = InsightsFormulas.WaistHipRatio(waistToHipRatio, model.getGender(), ethnicitynum);
            Drawable whippill = InsightsFormulas.getPillDrawable(whipoutcome, getResources());
            ((TextView) waisthipcard.findViewById(R.id.pill)).setBackground(whippill);
            ((TextView) waisthipcard.findViewById(R.id.pill)).setText(whipoutcome.displayName());
        }
        else
        {
            ((TextView) waisthipcard.findViewById(R.id.pill)).setVisibility(View.GONE);
        }

        ((TextView) waistheightcard.findViewById(R.id.cardtitle)).setTypeface(null, Typeface.BOLD);
        ((TextView) waistheightcard.findViewById(R.id.cardtitle)).setText(getString(R.string.waist_to_height_ratio));
        ((TextView) waistheightcard.findViewById(R.id.cardunit)).setText("");
        double waistToHeightRatio = model.getAdjustedWaist().getValueInCm() / model.getHeight().getValueInCm();
        ((TextView) waistheightcard.findViewById(R.id.cardvalue)).setText(decimalFormat.format(waistToHeightRatio));
        ((TextView) waistheightcard.findViewById(R.id.moreinfo)).setOnClickListener((View.OnClickListener) view ->
        {
            InsightsBottomSheet bbs = new InsightsBottomSheet();
            bbs.rawres = R.raw.waisttoheightratio;
            bbs.show(getFragmentManager(), "bctbottomsheet");

        });

        if (ethnicityAvailable)
        {
            InsightsFormulas.InsightsOutcome waistHeightOutcome = InsightsFormulas.WaistHeightRatio(waistToHeightRatio, model.getGender(), ethnicitynum);
            Drawable wHeightPill = InsightsFormulas.getPillDrawable(waistHeightOutcome, getResources());
            ((TextView) waistheightcard.findViewById(R.id.pill)).setBackground(wHeightPill);
            ((TextView) waistheightcard.findViewById(R.id.pill)).setText(waistHeightOutcome.displayName());
        }
        else
        {
            ((TextView) waistheightcard.findViewById(R.id.pill)).setVisibility(View.GONE);
        }


        ((TextView) waistcircumferencecard.findViewById(R.id.cardtitle)).setTypeface(null, Typeface.BOLD);
        ((TextView) waistcircumferencecard.findViewById(R.id.cardtitle)).setText(getString(R.string.waist_circumference));
        String lengthUnitAbbreiviated = "cm";
        double waistCircumference = model.getAdjustedWaist().getValueForComparison().doubleValue();
        String waistDisplay = new DecimalFormat("0.0").format(waistCircumference);
        if (AppWideUnitSystemHelper.getAppWideUnitSystemSync() == Imperial.class)
        {
            lengthUnitAbbreiviated = "";
            waistDisplay = waistDisplay + "\"";
        }
        ((TextView) waistcircumferencecard.findViewById(R.id.cardvalue)).setText(waistDisplay);
        ((TextView) waistcircumferencecard.findViewById(R.id.cardunit)).setText(lengthUnitAbbreiviated);
        ((TextView) waistcircumferencecard.findViewById(R.id.moreinfo)).setOnClickListener((View.OnClickListener) view ->
        {
            InsightsBottomSheet bbs = new InsightsBottomSheet();
            bbs.rawres = R.raw.waistcircumference;
            bbs.show(getFragmentManager(), "bctbottomsheet");
        });
        if (ethnicityAvailable)
        {
            InsightsFormulas.InsightsOutcome waistCircumOutcome = InsightsFormulas.WaistCircumference(model.getAdjustedWaist().getValueInCm(), model.getGender(), ethnicitynum);
            Drawable waistCircumPill = InsightsFormulas.getPillDrawable(waistCircumOutcome, getResources());
            ((TextView) waistcircumferencecard.findViewById(R.id.pill)).setBackground(waistCircumPill);
            ((TextView) waistcircumferencecard.findViewById(R.id.pill)).setText(waistCircumOutcome.displayName());
        }
        else
        {
            ((TextView) waistcircumferencecard.findViewById(R.id.pill)).setVisibility(View.GONE);
        }
        if (ethnicityAvailable)
        {
            ((TextView) ethnicityCard.findViewById(R.id.rowvalue)).setText(InsightsFormulas.getEthnicityDisplayFromNumber(ethnicitynum, getResources()));
        }
        else
        {
            ethnicityCard.setVisibility(View.GONE);
        }
        return true;
    }

    @Override
    public void onStop()
    {
        super.onStop();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.respond(IntentResponses.HIDE_VIEW_SUPPORT_BUTTON, null);
    }


}
