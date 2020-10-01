package com.myfiziq.sdk.activities;

import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.builders.HeightSelectorBuilder;
import com.myfiziq.sdk.builders.WeightSelectorBuilder;
import com.myfiziq.sdk.components.MyDatePickerDialog;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.ConnectivityHelper;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.GuestHelper;
import com.myfiziq.sdk.helpers.RadioButtonHelper;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.myfiziq.sdk.util.GlobalContext.getContext;

public class DebugUploadActivity extends AppCompatActivity
{
    private EditText heightEditText;
    private EditText weightEditText;
    private View dateOfBirthHeading;
    private View dateOfBirthContainer;
    private EditText dateOfBirthEditText;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private Button continueButton;
    private Button selectFrontImageButton;
    private Button selectSideImageButton;
    private View layoutProcessing;

    private RadioButton[] genderRadioGroup;

    private boolean enableDateOfBirth = true;
    private Date chosenDateOfBirth;

    private HeightSelectorBuilder heightSelectorBuilder;
    private WeightSelectorBuilder weightSelectorBuilder;

    // This is a VO which represents the AWS user profile
    private ModelUserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first_avatar_v2);

        View view = getWindow().getDecorView().getRootView();

        ActionBarHelper.setActionBarTitle(this, getString(R.string.myfiziqsdk_title_new_measurement));


        heightEditText = view.findViewById(R.id.heightEditText);
        weightEditText = view.findViewById(R.id.weightEditText);
        dateOfBirthHeading = view.findViewById(R.id.dateOfBirthHeading);
        dateOfBirthContainer = view.findViewById(R.id.dateOfBirthContainer);
        dateOfBirthEditText = view.findViewById(R.id.dateOfBirth);
        maleRadioButton = view.findViewById(R.id.genderMale);
        femaleRadioButton = view.findViewById(R.id.genderFemale);
        continueButton = view.findViewById(R.id.btnCapture);
        selectFrontImageButton = view.findViewById(R.id.btnFront);
        selectSideImageButton = view.findViewById(R.id.btnSide);
        layoutProcessing = view.findViewById(R.id.layoutProcessing);

        genderRadioGroup = new RadioButton[]{maleRadioButton, femaleRadioButton};

        enableDateOfBirth = true;

        setCreateEnabled(true);

        initializeBuilder();
        bindListeners();
    }

    @Override
    public void onDestroy()
    {
        continueButton = null;
        layoutProcessing = null;

        super.onDestroy();
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
        if (null == heightSelectorBuilder)
        {
            heightSelectorBuilder = new HeightSelectorBuilder(getResources(), heightEditText, Centimeters.class);
        }

        if (null == weightSelectorBuilder)
        {
            weightSelectorBuilder = new WeightSelectorBuilder(getResources(), weightEditText, Kilograms.class);
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
        }

        maleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, maleRadioButton, genderRadioGroup)
        );

        femaleRadioButton.setOnCheckedChangeListener((buttonView, isChecked) ->
                RadioButtonHelper.processRadioButtonState(resources, femaleRadioButton, genderRadioGroup)
        );


        continueButton.setOnClickListener(v -> onContinueClicked());
        selectFrontImageButton.setOnClickListener(v -> onSelectFrontImageClicked());
        selectSideImageButton.setOnClickListener(v -> onSelectSideImageClicked());
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
        heightEditText.setError(null);
        weightEditText.setError(null);

        if (enableDateOfBirth)
        {
            dateOfBirthEditText.setError(null);
        }

        if (!heightSelectorBuilder.hasSelectedHeight())
        {
            heightEditText.setError("empty height");//getString(R.string.error_emptyheight));
            return false;
        }

        Length height = heightSelectorBuilder.getSelectedHeight();

        if (height == null )
        {
            UiUtils.showAlertDialog(this, null, "empty height",
                    null, null, null, null);
            return false;
        }

        if (!weightSelectorBuilder.hasSelectedWeight())
        {
            weightEditText.setError("empty weight");
            return false;
        }

        if (enableDateOfBirth && TextUtils.isEmpty(dateOfBirthEditText.getText()))
        {
            dateOfBirthEditText.setError("empty DOB");
            return false;
        }

        if (!maleRadioButton.isChecked() && !femaleRadioButton.isChecked())
        {
            Toast.makeText(getContext(), "empty gender", Toast.LENGTH_LONG).show();
            return false;
        }

        Weight weight = weightSelectorBuilder.getSelectedWeight();

        if (weight == null)
        {
            Timber.e("Logic error. Weight is null");
            return false;
        }

        return true;
    }
    /**
     * Executed when the user clicks the "Continue" button to move to the next screen in the wizard.
     */
    private void onContinueClicked()
    {
        //put go to img capture here?

        if (validate())
        {
            UiUtils.hideSoftKeyboard(this);

            // User clicked the "Continue" button instead of the guest button.
            // We don't want to create a guest avatar so clear the guest selection.
            GuestHelper.persistGuestSelection("");


            if (true)
            {
                String pleaseWaitString = getString(R.string.please_wait);

                final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(this, pleaseWaitString, true, false, null);
                UiUtils.setAlertDialogColours(this, dialog);

                dialog.show();

                if (!ConnectivityHelper.isNetworkAvailable(this))
                {
                    dialog.dismiss();
                    DialogHelper.showInternetDownDialog(this);
                    return;
                }
            }
        }
    }

    private void onSelectFrontImageClicked()
    {

    }

    private void onSelectSideImageClicked()
    {

    }

    /**
     * When the user clicks on the height textbox.
     */
    private void onHeightClicked()
    {
        heightSelectorBuilder.renderHeightSelectorDialog(getSupportFragmentManager(), length ->
        {
            // No callback
        });
    }

    /**
     * When the user clicks on the weight textbox.
     */
    private void onWeightClicked()
    {
        weightSelectorBuilder.openWeightSelectorDialog(getSupportFragmentManager(), weight ->
        {
            // No callback
        });
    }

    private void setCreateEnabled(boolean enabled)
    {
        runOnUiThread(() ->
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
        });
    }
}
