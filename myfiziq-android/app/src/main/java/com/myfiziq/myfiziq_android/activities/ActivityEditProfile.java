package com.myfiziq.myfiziq_android.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.Utils;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.components.MyDatePickerDialog;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.LocalUserDataKey;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.MyFiziqLocalUserDataHelper;
import com.myfiziq.sdk.helpers.RadioButtonHelper;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public class ActivityEditProfile extends AppCompatActivity
{

    private EditText dateOfBirthEditText;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private Button saveProfileButton;
    private RadioButton[] genderRadioGroup;
    private Date chosenDateOfBirth;
    private ModelUserProfile userProfile;

    private TextInputLayout oldPasswordContainer;
    private EditText oldPasswordEditText;
    private TextInputLayout passwordContainer;
    private EditText passwordEditText;
    private TextInputLayout confirmPasswordContainer;
    private EditText confirmPasswordEditText;
    private Button resetPasswordButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        setSupportActionBar(findViewById(R.id.mainToolbar));
        ActionBarHelper.setActionBarTitle(this, getString(R.string.profile_settings));
        ActionBarHelper.showBackButton(this);
        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        dateOfBirthEditText = findViewById(R.id.dateOfBirth);
        maleRadioButton = findViewById(R.id.genderMale);
        femaleRadioButton = findViewById(R.id.genderFemale);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        genderRadioGroup = new RadioButton[]{maleRadioButton, femaleRadioButton};
        bindListeners();
        checkUserProfileCache();

        oldPasswordContainer = findViewById(R.id.oldPasswordContainer);
        oldPasswordEditText = oldPasswordContainer.getEditText();
        passwordContainer = findViewById(R.id.passwordContainer);
        passwordEditText = passwordContainer.getEditText();
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        confirmPasswordEditText = confirmPasswordContainer.getEditText();
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        resetPasswordButton.setOnClickListener(view -> onSavePasswordClicked());
        Utils.enablePasswordVisibilityButtonOnTextChange(passwordContainer);
        Utils.enablePasswordVisibilityButtonOnTextChange(confirmPasswordContainer);

        UiUtils.disablePasswordToggleButtonRipple(oldPasswordContainer);
        UiUtils.disablePasswordToggleButtonRipple(passwordContainer);
        UiUtils.disablePasswordToggleButtonRipple(confirmPasswordContainer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUserProfileCache()
    {
        ModelUserProfile cachedUserProfile = ORMTable.getModel(ModelUserProfile.class, null);

        if (null == cachedUserProfile)
        {
            obtainUserProfile(this::populateValuesFromUserProfile);
        }
        else
        {
            this.userProfile = cachedUserProfile;
            populateValuesFromUserProfile(cachedUserProfile);
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
        datePicker.show(getSupportFragmentManager(), "datePicker");

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
     * When the user clicks on the Date of Birth textbox.
     */
    private void onDateOfBirthClicked()
    {
        showDateOfBirthPickerDialog();
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

    /**
     * Binds listeners to views.
     */
    private void bindListeners()
    {
        Resources resources = getResources();

        dateOfBirthEditText.setFocusableInTouchMode(false);
        dateOfBirthEditText.setOnClickListener(v -> onDateOfBirthClicked());

        maleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, maleRadioButton, genderRadioGroup)
        );

        femaleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, femaleRadioButton, genderRadioGroup)
        );


        saveProfileButton.setOnClickListener(v -> onSaveProfileClicked());
    }

    /**
     * Populates the values from the user's profile.
     */
    private void populateValuesFromUserProfile(ModelUserProfile userProfile)
    {
        Gender gender = userProfile.getGender();

        if (gender == Gender.M)
        {
            maleRadioButton.setChecked(true);
        }
        else if (gender == Gender.F)
        {
            femaleRadioButton.setChecked(true);
        }
        else
        {
            Timber.e("Unknown gender: %s", gender);
            maleRadioButton.setChecked(true);
        }

        RadioButtonHelper.processRadioButtonState(getResources(), maleRadioButton, genderRadioGroup);
        RadioButtonHelper.processRadioButtonState(getResources(), femaleRadioButton, genderRadioGroup);


        try
        {
            chosenDateOfBirth = DateOfBirthCoordinator.getDateOfBirth();
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot parse date of birth");
        }

        if (null != chosenDateOfBirth)
        {
            String formattedDateOfBirth = TimeFormatUtils.formatDateForDisplay(chosenDateOfBirth);
            dateOfBirthEditText.setText(formattedDateOfBirth);
        }

        this.userProfile = userProfile;
    }

    /**
     * Validates the user input before proceeding to the next screen.
     *
     * @return Whether the validation was successful.
     */
    private boolean validate()
    {
        dateOfBirthEditText.setError(null);

        if (TextUtils.isEmpty(dateOfBirthEditText.getText()))
        {
            dateOfBirthEditText.setError(getString(R.string.error_emptydate_of_birth));
            return false;
        }

        return true;
    }

    /**
     * Executed when the user clicks the "Save Profile" button.
     */
    private void onSaveProfileClicked()
    {
        if (validate())
        {
            UiUtils.hideSoftKeyboard(this);


            if (!maleRadioButton.isChecked() && !femaleRadioButton.isChecked())
            {
                throw new IllegalArgumentException("No Gender was selected");
            }


            boolean userProfileNeedsSync = false;

            if (maleRadioButton.isChecked() && userProfile.getGender() != Gender.M)
            {
                // The user has changed their gender
                userProfile.setGender(Gender.M);
                userProfileNeedsSync = true;
            }
            else if (femaleRadioButton.isChecked() && userProfile.getGender() != Gender.F)
            {
                // The user has changed their gender
                userProfile.setGender(Gender.F);
                userProfileNeedsSync = true;
            }

            DateOfBirthCoordinator.setDateOfBirth(chosenDateOfBirth);


            if (userProfileNeedsSync)
            {
                String pleaseWaitString = getString(R.string.please_wait);

                if (!ConnectivityHelper.isNetworkAvailable(this))
                {
                    DialogHelper.showInternetDownDialog(this);
                    return;
                }


                final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, pleaseWaitString, true, false, null);
                dialog.show();
                // The user changed fields that need to be synchronised with their profile in Cognito
                updateUserProfile(
                        () -> setUserAndStartNext(dialog),
                        () ->
                        {
                            Toast.makeText(this, getResources().getString(R.string.error_update_profile), Toast.LENGTH_LONG).show();
                            dialog.dismiss();
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
     * Obtains the user's profile from Cognito asynchronously
     */
    private void obtainUserProfile(AsyncHelper.Callback<ModelUserProfile> onSuccessCallback)
    {
        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            reinitialiseSdk();
            return;
        }

        String pleaseWaitString = getString(R.string.please_wait);

        final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, pleaseWaitString, true, false, null);
        dialog.show();

        MyFiziqSdkManager.getUserProfile((responseCode, result, userProfile1) ->
        {
            dialog.dismiss();

            if (responseCode.isOk() || null != userProfile1)
            {
                onSuccessCallback.execute(userProfile1);

                AsyncHelper.run(userProfile1::save);
            }
            else
            {
                Toast.makeText(this, getResources().getString(R.string.error_cannot_obtain_user_profile), Toast.LENGTH_LONG).show();
                finish();
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
                this.runOnUiThread(onSuccessCallback::execute);
            }
            else
            {
                this.runOnUiThread(onFailureCallback::execute);
            }
        });
    }

    /**
     * Update the user in the MyFiziq activity and starts the next item in the ParameterSet.
     */
    private void setUserAndStartNext(@Nullable AsyncProgressDialog dialog)
    {
        AsyncHelper.run(
                () -> userProfile.save(),
                () ->
                {
                    if (dialog != null)
                    {
                        dialog.dismiss();
                    }

                    Snackbar sb = Snackbar.make(saveProfileButton, R.string.details_updated, Snackbar.LENGTH_LONG);
                    sb.setAction(R.string.thanks_caps, v -> onBackPressed());
                    sb.show();
                },
                true);
    }

    private void reinitialiseSdk()
    {
        startActivity(new Intent(this, ActivityEntrypoint.class));
    }

    private void onSavePasswordClicked()
    {
        if (!validateLoginDetails())
        {
            Timber.e("Form validation failed");
            return;
        }

        String oldPassword = oldPasswordEditText.getText().toString();
        String newPassword = passwordEditText.getText().toString();

        if (!ConnectivityHelper.isNetworkAvailable(this))
        {
            DialogHelper.showInternetDownDialog(this);
            return;
        }

        AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, getString(com.myfiziq.sdk.R.string.please_wait), null);
        Activity act = this;
        AWSMobileClient.getInstance().changePassword(oldPassword, newPassword, new Callback<Void>()
        {
            @Override
            public void onResult(Void result)
            {
                runOnUiThread(() ->
                {
                    dialog.dismiss();
                    new AlertDialog.Builder(act)
                            .setTitle(getString(R.string.forgot_password_reset_successful_title))
                            .setMessage(R.string.forgot_password_reset_successful_message)
                            .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                            {
                            })
                            .show();
                });
            }

            @Override
            public void onError(Exception e)
            {
                runOnUiThread(() ->
                {
                    dialog.dismiss();
                    Toast.makeText(act, getString(R.string.forgot_password_reset_error_generic), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean validateLoginDetails()
    {
        int minimumPasswordLength = getResources().getInteger(R.integer.minimum_password_length);

        oldPasswordEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        if (TextUtils.isEmpty(oldPasswordEditText.getText()))
        {
            oldPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            oldPasswordEditText.setError(getString(R.string.error_emptypassword));
            oldPasswordEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(passwordEditText.getText()))
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            passwordEditText.setError(getString(R.string.error_emptypassword));
            passwordEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(confirmPasswordEditText.getText()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPasswordEditText.setError(getString(R.string.error_emptyconfirmpassword));
            confirmPasswordEditText.requestFocus();
            return false;
        }
        else if (passwordEditText.getText().toString().length() < minimumPasswordLength)
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            passwordEditText.setError(getString(R.string.error_tooshortpassword));
            passwordEditText.requestFocus();
            return false;
        }
        else if (!passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPasswordEditText.setError(getString(R.string.error_passwordnomatch));
            confirmPasswordEditText.requestFocus();
            return false;
        }
        return true;
    }
}
