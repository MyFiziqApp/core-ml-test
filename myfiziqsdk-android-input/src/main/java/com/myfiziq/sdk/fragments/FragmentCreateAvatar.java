package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.activities.BaseActivityInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.MyFiziqLoaderManager;
import com.myfiziq.sdk.builders.HeightSelectorBuilder;
import com.myfiziq.sdk.builders.WeightSelectorBuilder;
import com.myfiziq.sdk.components.MyDatePickerDialog;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.GuestHelper;
import com.myfiziq.sdk.helpers.RadioButtonHelper;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateGuest;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.strategies.InputDateOfBirthStrategy;
import com.myfiziq.sdk.strategies.InputGenderStrategy;
import com.myfiziq.sdk.strategies.InputHeightStrategy;
import com.myfiziq.sdk.strategies.InputWeightStrategy;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.validators.BmiMeasurementValidatorService;
import com.myfiziq.sdk.validators.LengthMeasurementValidatorService;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;


public class FragmentCreateAvatar extends BaseFragment implements FragmentInterface, FragmentHomeInterface, CursorHolder.CursorChangedListener
{
    private MyFiziqLoaderManager mMQYLoaderManager;
    private EditText heightEditText;
    private EditText weightEditText;
    private View dateOfBirthHeading;
    private View dateOfBirthContainer;
    private EditText dateOfBirthEditText;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private Button continueButton;
    private Button continueGuestButton;
    private View layoutProcessing;

    private RadioButton[] genderRadioGroup;

    // DoB should not be enabled by default.
    private boolean enableDateOfBirth = false;
    private Date chosenDateOfBirth;

    private HeightSelectorBuilder heightSelectorBuilder;
    private WeightSelectorBuilder weightSelectorBuilder;

    // This is a VO which represents the AWS user profile
    private ModelUserProfile userProfile;

    private IntentManagerService<Void> intentManagerServiceGuest;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (willSoonClose())
        {
            Timber.i("%s won't be rendered since the fragment will soon close", FragmentCreateAvatar.class.getSimpleName());
            return null;
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);

        ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_title_new_measurement));

        if (SisterColors.getInstance().isSisterMode() && !MyFiziqSdkManager.isCaptureEnabled())
            return view;

        heightEditText = view.findViewById(R.id.heightEditText);
        weightEditText = view.findViewById(R.id.weightEditText);
        dateOfBirthHeading = view.findViewById(R.id.dateOfBirthHeading);
        dateOfBirthContainer = view.findViewById(R.id.dateOfBirthContainer);
        dateOfBirthEditText = view.findViewById(R.id.dateOfBirth);
        maleRadioButton = view.findViewById(R.id.genderMale);
        femaleRadioButton = view.findViewById(R.id.genderFemale);
        continueButton = view.findViewById(R.id.btnCapture);
        continueGuestButton = view.findViewById(R.id.btnGuest);
        layoutProcessing = view.findViewById(R.id.layoutProcessing);

        genderRadioGroup = new RadioButton[]{maleRadioButton, femaleRadioButton};

        enableDateOfBirth = InputDateOfBirthStrategy.isDateOfBirthEnabled(mParameterSet, dateOfBirthEditText, enableDateOfBirth);

        retrieveUserProfile(userProfile1 ->
        {
            this.userProfile = userProfile1;

            initializeBuilder();
            bindListeners();

            populateFields();

            if (mParameterSet != null)
            {
                applyParameters(view);
            }
        });

        mMQYLoaderManager = new MyFiziqLoaderManager(getActivity(), getLoaderManager());
        mMQYLoaderManager.loadCursor(0,
                ORMContentProvider.uri(ModelAvatar.class),
                ModelAvatar.getWhere(
                        String.format("Status <> '%s'", Status.Completed)
                ),
                null,
                ModelAvatar.class,
                null,
                this);


        return view;
    }

    @Override
    public void onDestroy()
    {
        continueButton = null;
        continueGuestButton = null;
        layoutProcessing = null;

        if (intentManagerServiceGuest != null)
        {
            intentManagerServiceGuest.unbindAll();
        }

        super.onDestroy();
    }

    private void retrieveUserProfile(AsyncHelper.Callback<ModelUserProfile> onSuccessCallback)
    {
        AsyncHelper.run(
                () -> ORMTable.getModel(ModelUserProfile.class, null),
                cachedUserProfile -> retrieveUserProfileHandler(cachedUserProfile, onSuccessCallback),
                true
        );
    }

    private void retrieveUserProfileHandler(@Nullable ModelUserProfile cachedUserProfile, AsyncHelper.Callback<ModelUserProfile> onSuccessCallback)
    {
        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        // If we don't have a cached user profile locally, try to download it from the remote server
        if (null == cachedUserProfile)
        {
            // If we don't have a cached user profile AND the network is still down...
            if (!ConnectivityHelper.isNetworkAvailable(activity))
            {
                DialogHelper.showInternetDownDialog(activity, () ->
                {
                    IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(activity);
                    intentManagerService.respond(IntentResponses.NAVIGATE_HOME, null);
                });

                return;
            }

            // We don't have a cached user profile, so let's get their latest details
            obtainUserProfile(onSuccessCallback);
        }
        else
        {
            // Populate the form based on cached user profile
            this.userProfile = cachedUserProfile;
            onSuccessCallback.execute(cachedUserProfile);
        }
    }

    @Override
    protected int getFragmentLayout()
    {
        if (!SisterColors.getInstance().isSisterMode())
        {
            return R.layout.fragment_first_avatar_v2;
        }
        else
        {
            if (MyFiziqSdkManager.isCaptureEnabled())
                return R.layout.fragment_first_avatar_v2;
            else
                return R.layout.fragment_capture_disabled;
        }
    }

    @Override
    public int getIcon()
    {
        return R.drawable.ic_add_measurement_circle;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (getMyActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }
    }

    private void showDateOfBirthPickerDialog()
    {
        MyDatePickerDialog datePicker;

        if (null != chosenDateOfBirth)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(chosenDateOfBirth);

            datePicker = MyDatePickerDialog.newInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        else
        {
            int defaultUserAge = getResources().getInteger(R.integer.myfiziqsdk_default_user_age);

            Date todayDate = new Date();
            Calendar calendar = Calendar.getInstance();

            calendar.setTime(todayDate);
            calendar.add(Calendar.YEAR, defaultUserAge * -1);

            datePicker = MyDatePickerDialog.newInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }

        datePicker.setOnDateSetListener(this::onDateOfBirthSelected);
        datePicker.show(getFragmentManager(), "datePicker");

        Resources resources = getResources();
        new Handler().post(() ->
                datePicker.setContainerPadding(
                        resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_left),
                        resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_top),
                        resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_right),
                        resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_bottom)
                )
        );
    }

    /**
     * When the user has selected a Date of Birth.
     *
     * @param result The user's selection.
     */
    private void onDateOfBirthSelected(DatePickerResultVO result)
    {
        int chosenYear = result.getYear();
        int chosenMonth = result.getMonth();
        int chosenDay = result.getDay();

        chosenDateOfBirth = new GregorianCalendar(chosenYear, chosenMonth, chosenDay).getTime();

        // If the user has decided that they will be born in the future, set their date of birth to today
        if (chosenDateOfBirth.getTime() >= new Date().getTime())
        {
            chosenDateOfBirth = new Date();
        }

        String formattedDateOfBirth = TimeFormatUtils.formatDateForDisplay(chosenDateOfBirth);
        dateOfBirthEditText.setText(formattedDateOfBirth);
    }

    private void initializeBuilder()
    {
        Class<? extends Length> heightUnitsOfMeasurement = InputHeightStrategy.determineUnitsOfMeasurement(mParameterSet);
        Class<? extends Weight> weightUnitsOfMeasurement = InputWeightStrategy.determineUnitsOfMeasurement(mParameterSet);

        if (null == heightSelectorBuilder)
        {
            heightSelectorBuilder = new HeightSelectorBuilder(getResources(), heightEditText, heightUnitsOfMeasurement);
        }

        if (null == weightSelectorBuilder)
        {
            weightSelectorBuilder = new WeightSelectorBuilder(getResources(), weightEditText, weightUnitsOfMeasurement);
        }
    }

    /**
     * Binds listeners to views.
     */
    private void bindListeners()
    {
        Resources resources = getResources();
        bindMeasurementUnitListeners();

        if (enableDateOfBirth)
        {
            dateOfBirthEditText.setFocusableInTouchMode(false);
            dateOfBirthEditText.setOnClickListener(v -> showDateOfBirthPickerDialog());
            UiUtils.setViewVisibility(dateOfBirthHeading, View.VISIBLE);
            UiUtils.setViewVisibility(dateOfBirthContainer, View.VISIBLE);
            UiUtils.setViewVisibility(dateOfBirthEditText, View.VISIBLE);
        }
        else
        {
            UiUtils.setViewVisibility(dateOfBirthHeading, View.GONE);
            UiUtils.setViewVisibility(dateOfBirthContainer, View.GONE);
            UiUtils.setViewVisibility(dateOfBirthEditText, View.GONE);
        }

        maleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, maleRadioButton, genderRadioGroup)
        );

        femaleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, femaleRadioButton, genderRadioGroup)
        );


        continueButton.setOnClickListener(v -> onContinueClicked());

        if (continueGuestButton != null)
        {
            continueGuestButton.setOnClickListener(v -> onContinueGuestClicked());
        }
    }

    /**
     * Populates the initial values in the form, if available
     */
    private void populateFields()
    {
        setHeightAndWeight();
        setGender();
        setDateOfBirth();
    }

    private void setGender()
    {
        Gender userGender = InputGenderStrategy.determineGender(mParameterSet, userProfile);

        if (userGender == Gender.M)
        {
            maleRadioButton.setChecked(true);
        }
        else if (userGender == Gender.F)
        {
            femaleRadioButton.setChecked(true);
        }
        else
        {
            Timber.e("Unknown gender: %s", userGender);
            maleRadioButton.setChecked(true);
        }

        RadioButtonHelper.processRadioButtonState(getResources(), maleRadioButton, genderRadioGroup);
        RadioButtonHelper.processRadioButtonState(getResources(), femaleRadioButton, genderRadioGroup);
    }

    private void setHeightAndWeight()
    {
        Length userHeight = InputHeightStrategy.determineHeight(mParameterSet, userProfile);
        Weight userWeight = InputWeightStrategy.determineWeight(mParameterSet, userProfile);

        if (!heightSelectorBuilder.hasSelectedHeight() && userHeight != null)
        {
            heightSelectorBuilder.setSelectedHeight(userHeight);
        }

        if (!weightSelectorBuilder.hasSelectedWeight() && userWeight != null)
        {
            weightSelectorBuilder.setSelectedWeight(userWeight);
        }
    }

    private void setDateOfBirth()
    {
        if (enableDateOfBirth)
        {
            chosenDateOfBirth = InputDateOfBirthStrategy.determineDateOfBirth(mParameterSet);

            if (null != chosenDateOfBirth)
            {
                String formattedDateOfBirth = TimeFormatUtils.formatDateForDisplay(chosenDateOfBirth);
                dateOfBirthEditText.setText(formattedDateOfBirth);
            }
        }
    }

    /**
     * Listens for input changes to text fields that are units of measurement and applies a mask to them.
     */
    private void bindMeasurementUnitListeners()
    {
        heightEditText.setFocusableInTouchMode(false);
        heightEditText.setOnClickListener(v -> onHeightClicked());

        weightEditText.setFocusableInTouchMode(false);
        weightEditText.setOnClickListener(v -> onWeightClicked());
    }

    /**
     * Validates the user input before proceeding to the next screen.
     *
     * @return Whether the validation was successful.
     */
    private boolean validate()
    {
        if (getActivity() == null || getContext() == null)
        {
            // Fragment has detached from activity
            return false;
        }

        LengthMeasurementValidatorService validatorService = new LengthMeasurementValidatorService(getContext());
        BmiMeasurementValidatorService bmiValidatorService = new BmiMeasurementValidatorService(getContext());

        heightEditText.setError(null);
        weightEditText.setError(null);

        if (enableDateOfBirth)
        {
            dateOfBirthEditText.setError(null);
        }

        if (!heightSelectorBuilder.hasSelectedHeight())
        {
            heightEditText.setError(getString(R.string.error_emptyheight));
            return false;
        }

        Length height = heightSelectorBuilder.getSelectedHeight();

        if (height == null || !validatorService.isValid(height))
        {
            UiUtils.showAlertDialog(getActivity(), null, validatorService.getErrorText(height),
                    null, null, null, null);
            return false;
        }

        if (!weightSelectorBuilder.hasSelectedWeight())
        {
            weightEditText.setError(getString(R.string.error_emptyweight));
            return false;
        }

        if (enableDateOfBirth && TextUtils.isEmpty(dateOfBirthEditText.getText()))
        {
            dateOfBirthEditText.setError(getString(R.string.error_emptydate_of_birth));
            return false;
        }

        if (!maleRadioButton.isChecked() && !femaleRadioButton.isChecked())
        {
            Toast.makeText(getContext(), getString(R.string.error_emptygender), Toast.LENGTH_LONG).show();
            return false;
        }

        Weight weight = weightSelectorBuilder.getSelectedWeight();

        if (weight == null)
        {
            Timber.e("Logic error. Weight is null");
            return false;
        }

        if (!bmiValidatorService.isValid(height, weight))
        {
            UiUtils.showAlertDialog(getActivity(), null, bmiValidatorService.getErrorText(height, weight),
                    null, null, null, null);
            return false;
        }

        return true;
    }

    /**
     * Copies the data that the user has entered into their AWS user profile.
     *
     * @return Whether we need to synchronise the user's profile with the S3 profile store since
     * the user has updated their details.
     */
    private boolean reconcileInputWithAwsProfile()
    {
        boolean userProfileNeedsSync = false;

        if (maleRadioButton.isChecked() && userProfile.getGender() != Gender.M)
        {
            // The user has changed their gender
            userProfile.setGender(Gender.M);
            Timber.i("User has updated gender. Will synchronise with profile");

            userProfileNeedsSync = true;
        }
        else if (femaleRadioButton.isChecked() && userProfile.getGender() != Gender.F)
        {
            // The user has changed their gender
            userProfile.setGender(Gender.F);
            Timber.i("User has updated gender. Will synchronise with profile");

            userProfileNeedsSync = true;
        }

        if (enableDateOfBirth)
        {
            // Store locally. NOT in AWS. Just like iOS.
            DateOfBirthCoordinator.setDateOfBirth(chosenDateOfBirth);
        }

        Weight weight = weightSelectorBuilder.getSelectedWeight();
        Length height = heightSelectorBuilder.getSelectedHeight();

        if (weight != null && userProfile != null && userProfile.updateWeight(weight))
        {
            Timber.i("User has updated weight. Will synchronise with profile");

            // The user has changed their weight. Need to sync
            userProfileNeedsSync = true;
        }

        if (height != null && userProfile != null && userProfile.updateHeight(height))
        {
            Timber.i("User has updated height. Will synchronise with profile");

            // The user has changed their weight. Need to sync
            userProfileNeedsSync = true;
        }

        // The return statement MUST be at the end of the method
        // First we need to go through all fields and copy the data into their AWS user profile
        return userProfileNeedsSync;
    }

    /**
     * Executed when the user clicks the "Continue" button to move to the next screen in the wizard.
     */
    private void onContinueClicked()
    {
        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (validate())
        {
            UiUtils.hideSoftKeyboard(getActivity());

            // User clicked the "Continue" button instead of the guest button.
            // We don't want to create a guest avatar so clear the guest selection.
            GuestHelper.persistGuestSelection("");


            boolean userProfileNeedsSync = reconcileInputWithAwsProfile();

            if (userProfileNeedsSync)
            {
                String pleaseWaitString = getString(R.string.please_wait);

                final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(getActivity(), pleaseWaitString, true, false, null);
                UiUtils.setAlertDialogColours(getActivity(), dialog);

                dialog.show();

                if (!ConnectivityHelper.isNetworkAvailable(activity))
                {
                    dialog.dismiss();
                    DialogHelper.showInternetDownDialog(activity);
                    return;
                }

                // The user changed fields that need to be synchronised with their profile in Cognito
                updateUserProfile(
                        () -> setUserAndStartNext(dialog),
                        () ->
                        {
                            Toast.makeText(getActivity(), getResources().getString(R.string.error_cannot_obtain_user_profile), Toast.LENGTH_LONG).show();

                            dialog.dismiss();
                            reinitialiseSdk();
                        }
                );
            }
            else
            {
                setUserAndStartNext(null);
            }
        }
    }

    /**
     * When the user clicks on the "Guest" button
     */
    private void onContinueGuestClicked()
    {
        Activity activity = getActivity();

        if (activity == null)
        {
            // Fragment has detached from activity
            return;
        }

        if (validate())
        {
            UiUtils.hideSoftKeyboard(getActivity());

            openGuestSelector();
        }
    }

    /**
     * When the user clicks on the height textbox.
     */
    private void onHeightClicked()
    {
        heightSelectorBuilder.renderHeightSelectorDialog(getFragmentManager(), length ->
        {
            // No callback
        });
    }

    /**
     * When the user clicks on the weight textbox.
     */
    private void onWeightClicked()
    {
        weightSelectorBuilder.openWeightSelectorDialog(getFragmentManager(), weight ->
        {
            // No callback
        });
    }

    /**
     * Obtains the user's profile from Cognito Asynchronously
     */
    private void obtainUserProfile(AsyncHelper.Callback<ModelUserProfile> onSuccessCallback)
    {
        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            reinitialiseSdk();
            return;
        }

        String pleaseWaitString = getString(R.string.please_wait);

        final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(getActivity(), pleaseWaitString, true, false, null);
        dialog.show();

        MyFiziqSdkManager.getUserProfile((responseCode, result, userProfile1) ->
        {
            dialog.dismiss();

            Activity activity = getActivity();

            if (activity == null)
            {
                // Fragment has detached from activity
                return;
            }

            if (responseCode.isOk() || null != userProfile1)
            {
                if (null == getActivity())
                {
                    // Fragment has detached from activity
                    return;
                }

                onSuccessCallback.execute(userProfile1);

                AsyncHelper.run(userProfile1::save);
            }
            else
            {
                /*
                Toast.makeText(activity, getResources().getString(R.string.error_cannot_obtain_user_profile), Toast.LENGTH_LONG).show();

                IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(activity);
                intentManagerService.respond(IntentResponses.NAVIGATE_HOME, null);
                */

                // Fallback to new user profile instance.
                onSuccessCallback.execute(Orm.newModel(ModelUserProfile.class));
            }
        });
    }

    /**
     * Updates the users profile that is stored in Cognito.
     */
    private void updateUserProfile(AsyncHelper.CallbackVoid onSuccessCallback, AsyncHelper.CallbackVoid onFailureCallback)
    {
        MyFiziqSdkManager.updateUserProfile(userProfile, (responseCode, result) ->
        {
            if (responseCode.isOk())
            {
                onSuccessCallback.execute();
            }
            else
            {
                onFailureCallback.execute();
            }
        });
    }

    /**
     * Update the user in the MyFiziq activity and starts the next item in the ParameterSet.
     */
    private void setUserAndStartNext(@Nullable AsyncProgressDialog dialog)
    {
        AsyncHelper.run(
                () ->
                {
                    userProfile.save();
                    ModelAvatar avatar = Orm.newModel(ModelAvatar.class);
                    avatar.set(userProfile.getGender(), heightSelectorBuilder.getSelectedHeight(), weightSelectorBuilder.getSelectedWeight(), ModelAvatar.getCaptureFrames());
                    avatar.save();
                    return avatar;
                },
                (avatar) ->
                {
                    if (dialog != null)
                    {
                        dialog.dismiss();
                    }

                    // Add a new ModelAvatar parameter to the next set (prob capture).
                    ParameterSet set = mParameterSet.getSubSet("CAPSIDE");
                    if (null != set)
                    {
                        set.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR, avatar));
                    }
                    mParameterSet.startNext(getMyActivity(), false);
                }, true);
    }

    private void openGuestSelector()
    {

        Gender selectedGender = femaleRadioButton.isChecked() ? Gender.F : Gender.M;

        ModelAvatar avatar = Orm.newModel(ModelAvatar.class);
        avatar.set(selectedGender, heightSelectorBuilder.getSelectedHeight(), weightSelectorBuilder.getSelectedWeight(), ModelAvatar.getCaptureFrames());

        // Add a new ModelAvatar parameter to the next set (prob capture).
        ParameterSet set = mParameterSet.getSubSet("CAPSIDE");
        if (set != null)
        {
            set.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR, avatar));
            set.addParam(new Parameter(R.id.TAG_ARG_DOB, DateOfBirthCoordinator.formatDate(chosenDateOfBirth)));
        }

        ParameterSet selectOrCreateGuest = StateGuest.getSelectOrCreateGuest();
        selectOrCreateGuest.addNextSet(mParameterSet.getNext());
        selectOrCreateGuest.start(getMyActivity(), true);
    }

    private void reinitialiseSdk()
    {
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.requestAndListenForResponse(
                IntentPairs.REINITIALISE_SDK,
                result ->
                {
                    ActivityInterface activityInterface = getMyActivity();
                    if (activityInterface instanceof BaseActivityInterface)
                    {
                        ((BaseActivityInterface)getMyActivity()).fragmentPopAll();
                    }
                    result.start(getMyActivity());
                }
        );
    }

    @Override
    public void onCursorChanged(CursorHolder cursorHolder)
    {
        boolean isHolderEmpty = (null == cursorHolder || cursorHolder.getItemCount() < 1);
        setCreateEnabled(isHolderEmpty);
    }

    private void setCreateEnabled(boolean enabled)
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        getActivity().runOnUiThread(() ->
        {
            if (layoutProcessing != null)
            {
                layoutProcessing.setVisibility(enabled ? View.GONE : View.VISIBLE);
            }

            if (continueButton != null)
            {
                continueButton.setEnabled(enabled);
                continueButton.setVisibility(View.VISIBLE);
            }

            ModelSetting.getSettingAsync(
                    ModelSetting.Setting.FEATURE_GUEST_USERS,
                    false,
                    guestUserEnabled ->
                    {
                        if (continueGuestButton != null && guestUserEnabled)
                        {
                            continueGuestButton.setEnabled(enabled);
                            continueGuestButton.setVisibility(View.VISIBLE);
                        }
                    }
            );
        });
    }
}
