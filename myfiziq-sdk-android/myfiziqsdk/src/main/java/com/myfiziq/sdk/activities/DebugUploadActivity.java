package com.myfiziq.sdk.activities;

import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.builders.HeightSelectorBuilder;
import com.myfiziq.sdk.builders.WeightSelectorBuilder;
import com.myfiziq.sdk.components.MyDatePickerDialog;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.RadioButtonHelper;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.myfiziq.sdk.activities.DebugActivity.SELECT_IMAGE_FRONT;
import static com.myfiziq.sdk.activities.DebugActivity.SELECT_IMAGE_SIDE;
import static com.myfiziq.sdk.util.GlobalContext.getContext;

public class DebugUploadActivity extends AppCompatActivity
{
    private EditText heightEditText;
    private EditText weightEditText;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private Button continueButton;
    private Button selectFrontImageButton;
    private Button selectSideImageButton;
    private View layoutProcessing;

    private RadioButton[] genderRadioGroup;

    private HeightSelectorBuilder heightSelectorBuilder;
    private WeightSelectorBuilder weightSelectorBuilder;

    private String frontImageSource = null;
    private String sideImageSource = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first_avatar_v2);

        View view = getWindow().getDecorView().getRootView();

        ActionBarHelper.setActionBarTitle(this, getString(R.string.myfiziqsdk_title_new_measurement));

        heightEditText = view.findViewById(R.id.heightEditText);
        weightEditText = view.findViewById(R.id.weightEditText);
        maleRadioButton = view.findViewById(R.id.genderMale);
        femaleRadioButton = view.findViewById(R.id.genderFemale);
        continueButton = view.findViewById(R.id.btnCapture);
        selectFrontImageButton = view.findViewById(R.id.btnFront);
        selectSideImageButton = view.findViewById(R.id.btnSide);
        layoutProcessing = view.findViewById(R.id.layoutProcessing);

        genderRadioGroup = new RadioButton[]{maleRadioButton, femaleRadioButton};

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

        if(null == frontImageSource)
        {
            Toast.makeText(getContext(), "select front image", Toast.LENGTH_LONG).show();
            return false;
        }

        if(null == sideImageSource)
        {
            Toast.makeText(getContext(), "select side image", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
    /**
     * Executed when the user clicks the "Continue" button to move to the next screen in the wizard.
     */
    private void onContinueClicked()
    {
        if (validate())
        {
            UiUtils.hideSoftKeyboard(this);

            DebugActivity.DebugModel.DebugItem debugItem = DebugActivity.DebugModel.DebugItem.TEST_AVATAR_IMAGE_UPLOAD;
            try
            {
                Weight weight = weightSelectorBuilder.getSelectedWeight();
                Length height = heightSelectorBuilder.getSelectedHeight();
                Gender gender =  maleRadioButton.isChecked()? Gender.M : Gender.F;
                MyFiziq.getInstance().initInspect(true);
                ModelAvatar avatar = DebugActivity.DebugModel.generateAvatar(this, DebugActivity.DebugModel.DebugItem.TEST_AVATAR_IMAGE_UPLOAD, weight.getValueInKg(), height.getValueInCm(), gender, true, frontImageSource, sideImageSource);
                //MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, true);
            }catch (Throwable t)
            {
                Timber.e(t, "Error in %s", debugItem.mTitle);
            }
        }
    }

    private void onSelectFrontImageClicked()
    {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, SELECT_IMAGE_FRONT);
    }

    private void onSelectSideImageClicked()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_IMAGE_SIDE);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_IMAGE_FRONT && resultCode == RESULT_OK)
        {
            Uri fullPhotoUri = data.getData();

            String result = "";
            Cursor cursor = getContentResolver().query(fullPhotoUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    frontImageSource = result;
                }
            } finally {
                cursor.close();
            }
        }
        if(requestCode == SELECT_IMAGE_SIDE  && resultCode == RESULT_OK)
        {
            Uri fullPhotoUri = data.getData();

            String result = "";
            Cursor cursor = getContentResolver().query(fullPhotoUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    sideImageSource = result;
                }
            } finally {
                cursor.close();
            }
        }
        if (resultCode == RESULT_CANCELED) {
            return;
        }
    }
}
