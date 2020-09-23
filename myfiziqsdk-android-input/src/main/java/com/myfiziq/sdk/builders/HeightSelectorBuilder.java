package com.myfiziq.sdk.builders;

import android.content.res.Resources;
import android.widget.EditText;

import com.myfiziq.myfiziqsdk_android_input.R;
import com.myfiziq.sdk.components.MyMeasurementDialog;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Feet;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ImperialConverter;
import com.myfiziq.sdk.helpers.MetricConverter;
import com.myfiziq.sdk.helpers.MyMeasurementDialogVOGenerator;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryValueVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogVO;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import timber.log.Timber;

/**
 * Builds a dialog box to handle the selection of a height and the units of measurement the user wishes to use.
 */
public class HeightSelectorBuilder
{
    @Nullable
    private Length selectedHeight;

    private Resources resources;

    private static final String CENTIMETERS_KEY = Centimeters.internalName;
    private static final String FEET_KEY = Feet.internalName;

    /**
     * Retain the previously selected height in centimeters so that as the users moves from
     * centimeters to feet/inches (which is a less precise form of measurement) and then back to
     * centimeters, their originally selected value in centimeters is shown to them if chose not
     * to change the feet/inches value.
     */
    private int underlyingHeightInCm = 0;

    @Nullable
    private EditText associatedTextField;

    private Class<? extends Length> preferredUnit = null;


    public HeightSelectorBuilder(Resources resources)
    {
        this.resources = resources;
    }

    public HeightSelectorBuilder(Resources resources, EditText associatedTextField)
    {
        this.resources = resources;
        this.associatedTextField = associatedTextField;
    }

    public HeightSelectorBuilder(Resources resources, EditText associatedTextField, Class<? extends Length> unitOfMeas)
    {
        this.resources = resources;
        this.associatedTextField = associatedTextField;
        this.preferredUnit = unitOfMeas;
    }

    /**
     * Opens a dialog to allow the user to select a height.
     *
     * @param fragmentManager       Fragment manager.
     * @param builderOnDoneListener Called when the user has chosen a height and closes the dialog.
     *                              The chosen height is returned as part of the callback.
     */
    public void renderHeightSelectorDialog(FragmentManager fragmentManager, AsyncHelper.Callback<Length> builderOnDoneListener)
    {
        MyMeasurementDialogCategoryVO cmCategory = buildCentimetersCategory();
        MyMeasurementDialogCategoryVO feetCategory = buildFeetCategory();

        MyMeasurementDialogCategoryVO initialCategory;
        MyMeasurementDialogCategoryValueVO initialValue;


        Length displayedHeight = selectedHeight != null ? selectedHeight : getDefaultHeight();

        if (displayedHeight instanceof Centimeters)
        {
            initialCategory = cmCategory;

            int centimeters = (int) Math.round(displayedHeight.getValueInCm());
            initialValue = initialCategory.getValueVOFromInt(centimeters);
        }
        else if (displayedHeight instanceof Feet)
        {
            initialCategory = feetCategory;

            Feet feetValue = (Feet) displayedHeight;
            int nearestWholeInch = feetValue.getTotalValueInInches();
            initialValue = initialCategory.getValueVOFromInt(nearestWholeInch);
        }
        else
        {
            throw new IllegalArgumentException("Unknown height units of measurement: " + displayedHeight.getClass().getSimpleName());
        }


        ArrayList<MyMeasurementDialogCategoryVO> categories = new ArrayList<>();
        categories.add(cmCategory);
        categories.add(feetCategory);


        MyMeasurementDialogVO vo = new MyMeasurementDialogVO();
        vo.setDialogTitle("Height");
        vo.setCategories(categories);
        vo.setInitialCategory(initialCategory);
        vo.setInitialValue(initialValue);


        MyMeasurementDialog dialog = MyMeasurementDialog.newInstance(vo);
        dialog.show(fragmentManager, "heightMeasurementDialog");

        dialog.setOnCategoryChangedListener((newCategory, previousCategory, oldMeasurementValue, oldMeasurementFractionValue) ->
                onDialogCategoryChanged(dialog, newCategory, previousCategory, oldMeasurementValue, oldMeasurementFractionValue)
        );

        dialog.setOnDoneListener((chosenCategory, chosenValue, chosenFraction) ->
                onDoneListener(chosenCategory, chosenValue, chosenFraction, builderOnDoneListener));
    }


    /**
     * Generates a friendly representation of the selected height.
     * <p>
     * If no height has been selected, a blank string is returned.
     */
    public String getHeightLabel()
    {
        if (selectedHeight == null)
        {
            return "";
        }

        if (selectedHeight instanceof Centimeters)
        {
            selectedHeight.setFormat(Centimeters.heightFormat);
        }


        return selectedHeight.getFormatted();
    }

    /**
     * Returns whether the user has selected a height in the dialog box or if an initial one was
     * provided as part of the {@link #setSelectedHeight(Length)} method.
     */
    public boolean hasSelectedHeight()
    {
        return selectedHeight != null;
    }

    /**
     * Gets the weight that the user has selected.
     */
    @Nullable
    public Length getSelectedHeight()
    {
        return selectedHeight;
    }

    /**
     * Sets the weight that is shown to the user in the dialog box.
     */
    public void setSelectedHeight(@Nullable Length selectedHeight)
    {
        this.selectedHeight = selectedHeight;

        if (associatedTextField != null)
        {
            String formattedLabel = getHeightLabel();
            associatedTextField.setText(formattedLabel);
        }
    }

    /**
     * Gets the default height if the user has not selected any.
     *
     * @return A default height with a default units of measurement.
     * @throws IllegalArgumentException If the default height is unrecognised.
     */
    private Length getDefaultHeight()
    {
        Class<? extends Length> defaultHeightUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideHeightUnit();
        int defaultHeightValue = resources.getInteger(R.integer.default_height_in_cm);

        if (preferredUnit != null)
            defaultHeightUnitOfMeasurement = preferredUnit;

        // Create an empty class that represents our units of measurement (e.g. a Centimeters class)
        return Length.fromCentimeters(defaultHeightUnitOfMeasurement, defaultHeightValue);
    }

    /**
     * Builds a {@link MyMeasurementDialogCategoryVO} for the {@link Centimeters} units of measurement.
     * Limit to 250cm
     *
     * @return The built {@link MyMeasurementDialogCategoryVO}.
     */
    private MyMeasurementDialogCategoryVO buildCentimetersCategory()
    {
        ArrayList<MyMeasurementDialogCategoryValueVO> cmValues = MyMeasurementDialogVOGenerator.generateValues(
                50,
                250,
                1,
                input -> String.format(Locale.getDefault(), "%d cm", input)
        );

        MyMeasurementDialogCategoryVO cmCategory = new MyMeasurementDialogCategoryVO();
        cmCategory.setKey(CENTIMETERS_KEY);
        cmCategory.setLabel(resources.getString(R.string.centimeters));
        cmCategory.setValues(cmValues);

        return cmCategory;
    }

    /**
     * Builds a {@link MyMeasurementDialogCategoryVO} for the {@link Feet} units of measurement.
     * Limit to 250cm (or nearest whole inches) -> 98 inches.
     *
     * @return The built {@link MyMeasurementDialogCategoryVO}.
     */
    private MyMeasurementDialogCategoryVO buildFeetCategory()
    {
        ArrayList<MyMeasurementDialogCategoryValueVO> feetValues = MyMeasurementDialogVOGenerator.generateValues(
                19,
                98,
                1,
                ImperialConverter::convertInchesToImperialHeight
        );

        MyMeasurementDialogCategoryVO feetCategory = new MyMeasurementDialogCategoryVO();
        feetCategory.setKey(FEET_KEY);
        feetCategory.setLabel(resources.getString(R.string.feet));
        feetCategory.setValues(feetValues);

        return feetCategory;
    }

    /**
     * Triggered when the user has changes the units of measurement in the dialog.
     * <p>
     * At this point we should convert the measurement from the old units of measurement/category
     * to the new units of measurement/category.
     */
    private void onDialogCategoryChanged(MyMeasurementDialog dialog, MyMeasurementDialogCategoryVO newCategory, MyMeasurementDialogCategoryVO previousCategory,
                                         int oldMeasurementValue, int oldMeasurementFractionValues)
    {
        if (null == previousCategory || null == newCategory || previousCategory.getKey().equals(newCategory.getKey()))
        {
            // Category is unchanged
            dialog.setMeasurement(oldMeasurementValue);
        }
        else if (previousCategory.getKey().equals(CENTIMETERS_KEY) && newCategory.getKey().equals(FEET_KEY))
        {
            underlyingHeightInCm = oldMeasurementValue;

            int inches = (int) Math.round(MetricConverter.convertCentimetersToInches(oldMeasurementValue));
            dialog.setMeasurement(inches);
        }
        else if (previousCategory.getKey().equals(FEET_KEY) && newCategory.getKey().equals(CENTIMETERS_KEY))
        {
            int underlyingHeightInInches = (int) Math.round(MetricConverter.convertCentimetersToInches(underlyingHeightInCm));
            int centimeters = (int) Math.round(ImperialConverter.convertInchesToCentimeters(oldMeasurementValue));

            if (underlyingHeightInInches == oldMeasurementValue)
            {
                dialog.setMeasurement(underlyingHeightInCm);
            }
            else
            {
                dialog.setMeasurement(centimeters);
            }
        }
        else
        {
            Timber.e("Unrecognised previous category to new category conversion. Previous Category: %s. New Category: %s", previousCategory.getKey(), newCategory.getKey());
        }
    }

    /**
     * Triggered when the user closes the dialog box.
     * <p>
     * At this point we should callback to the original class which created this builder object.
     */
    private void onDoneListener(MyMeasurementDialogCategoryVO chosenCategory, MyMeasurementDialogCategoryValueVO chosenValue,
                                MyMeasurementDialogCategoryValueVO chosenFractionValue, AsyncHelper.Callback<Length> builderOnDoneListener)
    {
        int value = chosenValue.getValue();

        Length height;

        if (chosenCategory.getKey().equals(CENTIMETERS_KEY))
        {
            height = new Centimeters(value);
        }
        else if (chosenCategory.getKey().equals(FEET_KEY))
        {
            height = new Feet(0, value);
        }
        else
        {
            throw new IllegalArgumentException("Unknown height measurement category: " + chosenCategory.getKey() + ":" + chosenCategory.getLabel());
        }

        setSelectedHeight(height);

        builderOnDoneListener.execute(height);
    }
}
