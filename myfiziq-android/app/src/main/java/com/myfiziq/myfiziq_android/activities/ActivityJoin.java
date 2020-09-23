package com.myfiziq.myfiziq_android.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.MyFiziqCrashHelper;
import com.myfiziq.myfiziq_android.helpers.Utils;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.components.MyDatePickerDialog;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.LocalUserDataKey;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.MyFiziqLocalUserDataHelper;
import com.myfiziq.sdk.helpers.RadioButtonHelper;
import com.myfiziq.sdk.helpers.RegexHelpers;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.listeners.HideKeyboardOnTouchListener;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import timber.log.Timber;

public class ActivityJoin extends AppCompatActivity
{
    private Toolbar toolbar;
    private NestedScrollView scrollView;
    private EditText dateOfBirth;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private EditText emailAddress;
    private TextInputLayout passwordContainer;
    private EditText password;
    private TextInputLayout confirmPasswordContainer;
    private EditText confirmPassword;

    private Button continueButton;

    private Date chosenDateOfBirth;
    private RadioButton[] genderRadioGroup;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        scrollView = findViewById(R.id.scrollView);
        scrollView.setOnTouchListener(new HideKeyboardOnTouchListener());

        toolbar = findViewById(R.id.toolbar);
        dateOfBirth = findViewById(R.id.dateOfBirth);
        maleRadioButton = findViewById(R.id.genderMale);
        femaleRadioButton = findViewById(R.id.genderFemale);
        emailAddress = findViewById(R.id.emailAddress);

        passwordContainer = findViewById(R.id.passwordContainer);
        password = passwordContainer.getEditText();

        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        confirmPassword = confirmPasswordContainer.getEditText();

        continueButton = findViewById(R.id.continueButton);


        genderRadioGroup = new RadioButton[]{maleRadioButton, femaleRadioButton};

        setSupportActionBar(toolbar);

        ActionBarHelper.showBackButton(this);
        ActionBarHelper.setActionBarTitle(this, "Join");

        StatusBarHelper.setStatusBarStyle(this, StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        bindListeners();

        UiUtils.disablePasswordToggleButtonRipple(passwordContainer);
        UiUtils.disablePasswordToggleButtonRipple(confirmPasswordContainer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindListeners()
    {
        dateOfBirth.setOnFocusChangeListener(this::onDateOfBirthFocusChanged);
        continueButton.setOnClickListener(v -> onContinueClicked());

        changeRadioButtonColourWhenToggled(maleRadioButton, genderRadioGroup);
        changeRadioButtonColourWhenToggled(femaleRadioButton, genderRadioGroup);

        RadioButtonHelper.processRadioButtonState(getResources(), maleRadioButton, genderRadioGroup);
        RadioButtonHelper.processRadioButtonState(getResources(), femaleRadioButton, genderRadioGroup);

        Utils.enablePasswordVisibilityButtonOnTextChange(passwordContainer);
        Utils.enablePasswordVisibilityButtonOnTextChange(confirmPasswordContainer);
    }

    private void onDateOfBirthFocusChanged(View view, boolean hasFocus)
    {
        if (hasFocus)
        {
            view.clearFocus();
            showDateOfBirthPickerDialog();
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
        dateOfBirth.setText(formattedDateOfBirth);
    }


    private void onContinueClicked()
    {
        if (validate())
        {
            Utils.hideSoftKeyboard(this);

            String usernameText = emailAddress.getText().toString();
            String passwordText = password.getText().toString();


            ModelUserProfile userProfile = Orm.newModel(ModelUserProfile.class);

            if (maleRadioButton.isChecked())
            {
                userProfile.setGender(Gender.M);
            }
            else if (femaleRadioButton.isChecked())
            {
                userProfile.setGender(Gender.F);
            }
            else
            {
                throw new IllegalStateException("Neither the male nor female radio button is checked");
            }

            if (!ConnectivityHelper.isNetworkAvailable(this))
            {
                DialogHelper.showInternetDownDialog(this);
                return;
            }

            final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, getString(R.string.register_progress), true, false, null);
            dialog.show();

            MyFiziqSdkManager.register(
                    usernameText,
                    passwordText,
                    userProfile,
                    (responseCode, result) ->
                            runOnUiThread(
                                    () -> joinCallback(dialog, usernameText, responseCode, result)
                            )
            );
        }
    }

    private void joinCallback(AsyncProgressDialog dialog, String username, SdkResultCode responseCode, String result)
    {
        if (responseCode.isOk())
        {
            commitAutofillAction();

            MyFiziqSdkManager.initialiseSdk((responseCode1, result1) ->
            {
                dialog.dismiss();

                if (responseCode.isOk())
                {
                    Intent mainActivity = new Intent(ActivityJoin.this, ActivityMain.class);
                    mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

                    MyFiziqCrashHelper.assignUserForCrashReporting(username, username, username);

                    // Persist the local data AFTER we've signed up and initialised the SDK.
                    // The database is in a good state at this point.
                    persistLocalData();

                    startActivity(mainActivity);
                }
                else
                {
                    Toast.makeText(ActivityJoin.this, getString(R.string.register_failed), Toast.LENGTH_LONG).show();
                    Timber.e("Registration failed. Reason: %s", result);
                }
            });


            // Setting up the SDK takes a long time, so lets try to cache the user profile on a best-effort
            // basis while it starts
            MyFiziqSdkManager.getUserProfile((userProfileResponseCode, userProfileResult, userProfile) ->
            {
                if (userProfileResponseCode.isOk() || userProfile != null)
                {
                    AsyncHelper.run(userProfile::save);
                }
            });
        }
        else if (responseCode.isInternetDown())
        {
            dialog.dismiss();

            DialogHelper.showInternetDownDialog(this);
            Timber.e("Registration failed. Internet down");
        }
        else
        {
            dialog.dismiss();

            Toast.makeText(ActivityJoin.this, getString(R.string.register_failed), Toast.LENGTH_LONG).show();
            Timber.e("Registration failed. Reason: %s", result);
        }
    }

    /**
     * Prompt the user to save their username and password to the Android Autofill service if
     * they're running Android Oreo or higher.
     */
    private void commitAutofillAction()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOFILL))
        {
            AutofillManager autofillManager = getSystemService(AutofillManager.class);
            autofillManager.commit();
        }
    }

    private boolean validate()
    {
        int minimumPasswordLength = getResources().getInteger(R.integer.minimum_password_length);

        dateOfBirth.setError(null);
        maleRadioButton.setError(null);
        femaleRadioButton.setError(null);
        emailAddress.setError(null);
        password.setError(null);
        confirmPassword.setError(null);

        if (TextUtils.isEmpty(dateOfBirth.getText()) || null == chosenDateOfBirth)
        {
            dateOfBirth.setError(getString(R.string.error_emptydateofbirth));
            return false;
        }
        else if (!maleRadioButton.isChecked() && !femaleRadioButton.isChecked())
        {
            maleRadioButton.setError(getString(R.string.error_emptygender));
            femaleRadioButton.setError(getString(R.string.error_emptygender));
            return false;
        }
        else if (TextUtils.isEmpty(emailAddress.getText()))
        {
            emailAddress.setError(getString(R.string.error_emptyemailaddress));
            emailAddress.requestFocus();
            return false;
        }
        else if (!Pattern.compile(RegexHelpers.emailPattern).matcher(emailAddress.getText()).matches())
        {
            emailAddress.setError(getString(R.string.error_notvalidemailaddress));
            emailAddress.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(password.getText()))
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            password.setError(getString(R.string.error_emptypassword));
            password.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(confirmPassword.getText()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPassword.setError(getString(R.string.error_emptyconfirmpassword));
            confirmPassword.requestFocus();
            return false;
        }
        else if (password.getText().toString().length() < minimumPasswordLength)
        {
            passwordContainer.setPasswordVisibilityToggleEnabled(false);
            password.setError(getString(R.string.error_tooshortpassword));
            password.requestFocus();
            return false;
        }
        else if (!password.getText().toString().equals(confirmPassword.getText().toString()))
        {
            confirmPasswordContainer.setPasswordVisibilityToggleEnabled(false);
            confirmPassword.setError(getString(R.string.error_passwordnomatch));
            confirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Changes a Radio Button's colour when toggled.
     *
     * @param button              The button to process.
     * @param radioButtonsInGroup An array of other RadioButtons that are in the same group (e.g. Male and Female are both part of the "Gender" radio group.
     */
    private void changeRadioButtonColourWhenToggled(RadioButton button, RadioButton[] radioButtonsInGroup)
    {
        button.setOnCheckedChangeListener((buttonView, isChecked) -> RadioButtonHelper.processRadioButtonState(getResources(), buttonView, radioButtonsInGroup));
    }

    private void persistLocalData()
    {
        DateOfBirthCoordinator.setDateOfBirth(chosenDateOfBirth);
    }
}
