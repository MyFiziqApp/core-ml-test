package com.myfiziq.sdk.builders;

import android.content.res.Resources;
import android.widget.EditText;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.components.MyMeasurementDialog;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Pounds;
import com.myfiziq.sdk.db.StoneUK;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.ImperialConverter;
import com.myfiziq.sdk.helpers.MetricConverter;
import com.myfiziq.sdk.helpers.MyMeasurementDialogVOGenerator;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryValueVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogVO;
import com.myfiziq.sdk.vo.MyMeasurementFractionLimitValueVO;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import timber.log.Timber;

/**
 * Builds a dialog box to handle the selection of a weight and the units of measurement the user wishes to use.
 */
public class WeightSelectorBuilder
{
    @Nullable
    private Weight selectedWeight;

    private Resources resources;

    private static final String POUNDS_KEY = Pounds.internalName;
    private static final String KILOGRAMS_KEY = Kilograms.internalName;
    private static final String STONE_KEY = StoneUK.internalName;

    @Nullable
    private EditText associatedTextField;

    private Class<? extends Weight> preferredUnit = null;

    public WeightSelectorBuilder(Resources resources)
    {
        this.resources = resources;
    }

    public WeightSelectorBuilder(Resources resources, EditText associatedTextField)
    {
        this.resources = resources;
        this.associatedTextField = associatedTextField;
    }

    public WeightSelectorBuilder(Resources resources, EditText associatedTextField, Class<? extends Weight> unitOfMeas)
    {
        this.resources = resources;
        this.associatedTextField = associatedTextField;
        this.preferredUnit = unitOfMeas;
    }

    /**
     * Opens a dialog to allow the user to select a weight.
     *
     * @param fragmentManager Fragment manager.
     * @param onDoneListener Called when the user has chosen a weight and closes the dialog.
     *                       The chosen weight is returned as part of the callback.
     */
    public void openWeightSelectorDialog(FragmentManager fragmentManager, AsyncHelper.Callback<Weight> onDoneListener)
    {
        MyMeasurementDialogCategoryVO kgCategory = buildKgCategory();
        MyMeasurementDialogCategoryVO poundsCategory = buildPoundsCategory();
        MyMeasurementDialogCategoryVO stoneCategory = buildStoneCategory();

        MyMeasurementDialogCategoryVO initialCategory;
        MyMeasurementDialogCategoryValueVO initialValue;
        MyMeasurementDialogCategoryValueVO initialFractionValue = null;


        Weight displayedWeight = selectedWeight != null ? selectedWeight : getDefaultWeight();

        if (displayedWeight instanceof Kilograms)
        {
            double valueInKg = displayedWeight.getValueInKg();

            initialCategory = kgCategory;
            initialValue = initialCategory.getValueVOFromInt((int) valueInKg);

            if(null != initialCategory.getFractionValues())
            {
                int fractionComponent = (int) Math.round(valueInKg % 1 * 10);
                initialFractionValue = initialCategory.getFractionValueVOFromInt(fractionComponent);
            }
        }
        else if (displayedWeight instanceof Pounds)
        {
            double valueInPounds = ((Pounds) displayedWeight).getValueInPounds();
            int roundedDown = (int) valueInPounds;
            initialCategory = poundsCategory;
            initialValue = initialCategory.getValueVOFromInt(roundedDown);

            if (null != initialCategory.getFractionValues())
            {
                int fraction = (int) Math.round((valueInPounds - roundedDown) * 10);
                initialFractionValue = initialCategory.getFractionValueVOFromInt(fraction);
            }
        }
        else if (displayedWeight instanceof StoneUK)
        {
            int[] values = ((StoneUK) displayedWeight).getValueInStoneAndPounds();

            initialCategory = stoneCategory;
            initialValue = initialCategory.getValueVOFromInt(values[0]);

            if(null != initialCategory.getFractionValues())
            {
                initialFractionValue = initialCategory.getFractionValueVOFromInt(values[1]);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unknown weight units of measurement: " + displayedWeight.getClass().getSimpleName());
        }


        ArrayList<MyMeasurementDialogCategoryVO> categories = new ArrayList<>();
        categories.add(kgCategory);
        categories.add(poundsCategory);
        categories.add(stoneCategory);


        MyMeasurementDialogVO vo = new MyMeasurementDialogVO();
        vo.setDialogTitle(resources.getString(R.string.weight));
        vo.setCategories(categories);
        vo.setInitialCategory(initialCategory);
        vo.setInitialValue(initialValue);
        vo.setInitialFractionValue(initialFractionValue);


        MyMeasurementDialog dialog = MyMeasurementDialog.newInstance(vo);
        dialog.show(fragmentManager, "weightMeasurementDialog");

        dialog.setOnCategoryChangedListener(
                (newCategory, previousCategory, oldMeasurementValue, oldMeasurementFractionValue) ->
                        onDialogCategoryChanged(dialog, newCategory, previousCategory, oldMeasurementValue, oldMeasurementFractionValue)
        );

        dialog.setOnDoneListener((chosenCategory, chosenValue, chosenFraction) ->
                onDialogDoneListener(chosenCategory, chosenValue, chosenFraction, onDoneListener)
        );
    }

    /**
     * Generates a friendly representation of the selected weight.
     *
     * If no weight has been selected, a blank string is returned.
     */
    public String getWeightLabel()
    {
        if (selectedWeight == null)
        {
            return "";
        }

        return selectedWeight.getFormatted();
    }

    /**
     * Returns whether the user has selected a weight in the dialog box or if an initial one was
     * provided as part of the {@link #setSelectedWeight(Weight)} method.
     */
    public boolean hasSelectedWeight()
    {
        return selectedWeight != null;
    }

    /**
     * Gets the weight that the user has selected.
     */
    @Nullable
    public Weight getSelectedWeight()
    {
        return selectedWeight;
    }

    /**
     * Sets the weight that is shown to the user in the dialog box.
     */
    public void setSelectedWeight(@Nullable Weight selectedWeight)
    {
        this.selectedWeight = selectedWeight;

        if (associatedTextField != null)
        {
            String formattedLabel = getWeightLabel();
            associatedTextField.setText(formattedLabel);
        }
    }

    /**
     * Gets the default weight if the user has not selected any.
     *
     * @return A default weight with a default units of measurement.
     * @throws IllegalArgumentException If the default weight is unrecognised.
     */
    private Weight getDefaultWeight()
    {
        Class<? extends Weight> defaultWeightUnitOfMeasurement = AppWideUnitSystemHelper.getAppWideWeightUnit();

        int defaultWeightValue = resources.getInteger(R.integer.default_weight_in_kg);

        if (preferredUnit != null)
            defaultWeightUnitOfMeasurement = preferredUnit;

        // Create an empty class that represents our units of measurement (e.g. a Kilograms class)
        return Weight.fromKilograms(defaultWeightUnitOfMeasurement, defaultWeightValue);
    }

    private boolean isFractionCloseToMain(int fractionValue, int fractionMaxValue)
    {
        return fractionValue > fractionMaxValue;
    }

    /**
     * Builds a {@link MyMeasurementDialogCategoryVO} for the {@link Kilograms} units of measurement.
     * @return The built {@link MyMeasurementDialogCategoryVO}.
     */
    private MyMeasurementDialogCategoryVO buildKgCategory()
    {
        //Create Primary Spinner Value to Kg Category
        ArrayList<MyMeasurementDialogCategoryValueVO> kgValues = MyMeasurementDialogVOGenerator.generateValues(
                16,
                300,
                1,
                String::valueOf
        );

        //Create Fraction Spinner Value to Kg Category
        ArrayList<MyMeasurementDialogCategoryValueVO> kgFractionValues = MyMeasurementDialogVOGenerator.generateValues(
                0,
                9,
                1,
                input -> {
                    double value = ((double) input) / 10;

                    Kilograms kilogramsValue = new Kilograms(value);
                    String formattedValue = kilogramsValue.getFormatted();

                    return formattedValue.replace("0.", ".");
                }
        );

        //Create Fraction Limiter to Kg Category
        MyMeasurementFractionLimitValueVO kgFractionLimit = new MyMeasurementFractionLimitValueVO(
                0, 9, 0, 0);

        MyMeasurementDialogCategoryVO kgCategory = new MyMeasurementDialogCategoryVO();
        kgCategory.setKey(KILOGRAMS_KEY);
        kgCategory.setLabel("kg");
        kgCategory.setValues(kgValues);
        kgCategory.setFractionValues(kgFractionValues);
        kgCategory.setFractionLimit(kgFractionLimit);

        return kgCategory;
    }

    /**
     * Builds a {@link MyMeasurementDialogCategoryVO} for the {@link Pounds} units of measurement.
     * @return The built {@link MyMeasurementDialogCategoryVO}.
     */
    private MyMeasurementDialogCategoryVO buildPoundsCategory()
    {
        //Create Primary Spinner Value to Pound Category
        ArrayList<MyMeasurementDialogCategoryValueVO> poundValues = MyMeasurementDialogVOGenerator.generateValues(
                35,
                660,
                1,
                String::valueOf
        );
        //Create Fraction Spinner Value to Pound Category
        ArrayList<MyMeasurementDialogCategoryValueVO> poundFractionValues = MyMeasurementDialogVOGenerator.generateValues(
                0,
                9,
                1,
                input -> String.format(Locale.getDefault(), ".%d lbs", input)
        );
        MyMeasurementDialogCategoryVO poundsCategory = new MyMeasurementDialogCategoryVO();
        poundsCategory.setKey(POUNDS_KEY);
        poundsCategory.setLabel("lb");
        poundsCategory.setValues(poundValues);
        poundsCategory.setFractionValues(poundFractionValues);
        poundsCategory.setFractionLimit(new MyMeasurementFractionLimitValueVO(0, 9, 0, 0));

        return poundsCategory;
    }

    /**
     * Builds a {@link MyMeasurementDialogCategoryVO} for the {@link StoneUK} units of measurement.
     * @return The built {@link MyMeasurementDialogCategoryVO}.
     */
    private MyMeasurementDialogCategoryVO buildStoneCategory()
    {
        //Create Primary Spinner Value to Stone Category
        ArrayList<MyMeasurementDialogCategoryValueVO> stoneValues = MyMeasurementDialogVOGenerator.generateValues(
                2,
                47,
                1,
                input -> String.format(Locale.getDefault(), "%d st", input)
        );

        //Create Fraction Spinner Value to Stone Category
        ArrayList<MyMeasurementDialogCategoryValueVO> stoneFractionValues = MyMeasurementDialogVOGenerator.generateValues(
                0,
                13,
                1,
                input -> String.format(Locale.getDefault(), "%d lbs", input)
        );

        //Create Fraction Limiter to Stone Category
        MyMeasurementFractionLimitValueVO fractionLimit = new MyMeasurementFractionLimitValueVO(
                7, 13, 0, 3);

        MyMeasurementDialogCategoryVO stoneCategory = new MyMeasurementDialogCategoryVO();
        stoneCategory.setKey(STONE_KEY);
        stoneCategory.setLabel("st");
        stoneCategory.setValues(stoneValues);
        stoneCategory.setFractionValues(stoneFractionValues);
        stoneCategory.setFractionLimit(fractionLimit);

        return stoneCategory;
    }

    /**
     * Triggered when the user has changes the units of measurement in the dialog.
     *
     * At this point we should convert the measurement from the old units of measurement/category
     * to the new units of measurement/category.
     */
    private void onDialogCategoryChanged(MyMeasurementDialog dialog, MyMeasurementDialogCategoryVO newCategory, MyMeasurementDialogCategoryVO previousCategory,
                                         int oldMeasurementValue, int oldMeasurementFractionValue)
    {
        if (null == previousCategory || null == newCategory)
        {
            // Category is unchanged
            dialog.setMeasurement(oldMeasurementValue);
        }
        else if (previousCategory.getKey().equals(newCategory.getKey()))
        {
            if (null == previousCategory.getFractionValues())
            {
                dialog.setMeasurement(oldMeasurementValue);
            }
            else if(previousCategory.hasFractionLimit())
            {
                dialog.setMeasurementWithFractionLimit(oldMeasurementValue, oldMeasurementFractionValue, newCategory);
            }
            else
            {
                dialog.setMeasurement(oldMeasurementValue, oldMeasurementFractionValue);
            }

        }
        else if (previousCategory.getKey().equals(KILOGRAMS_KEY) && newCategory.getKey().equals(POUNDS_KEY))
        {
            double kilograms = oldMeasurementValue + ((double) oldMeasurementFractionValue / 10);
            double dblpounds = MetricConverter.convertKilogramsToPounds(kilograms);
            int pounds = (int) Math.round(MetricConverter.convertKilogramsToPounds(kilograms));
            int fraction = (int) ((dblpounds - pounds) * 10);
            dialog.setMeasurementWithFractionLimit(pounds, fraction, newCategory);
        }
        else if (previousCategory.getKey().equals(KILOGRAMS_KEY) && newCategory.getKey().equals(STONE_KEY))
        {
            double kilograms = oldMeasurementValue + ((double) oldMeasurementFractionValue / 10);

            // Split kilograms old measurement to stones(primary) and pounds(fraction)
            double totalStones = MetricConverter.convertKilogramsToStone(kilograms);
            int stones = (int) totalStones;
            int stonesFraction = (int) Math.round(ImperialConverter.convertStonesToPounds(totalStones - stones));

            // This will adjust the value of fraction if the value of fraction is more that fraction
            // spinner max value
            //
            // For example : When the calculated value is 7st 13.9lb, the value will be 7st 14lb due to
            // rounded fraction
            //
            // Since 7st 14lb is not valid in spinner, this function will change it to 8st 0lb
            int newFractionMaxValue = newCategory.getFractionValues()
                    .get(newCategory.getFractionValues().size() - 1).getValue();
            int newFractionMinValue = newCategory.getFractionValues()
                    .get(0).getValue();

            if (isFractionCloseToMain(stonesFraction, newFractionMaxValue))
            {
                stones += 1;
                stonesFraction = newFractionMinValue;
            }

            dialog.setMeasurementWithFractionLimit(stones, stonesFraction, newCategory);
        }
        else if (previousCategory.getKey().equals(POUNDS_KEY) && newCategory.getKey().equals(KILOGRAMS_KEY))
        {
            // Split pounds old measurement to kilogram(primary) and kilogram decimals(fraction)
            double totalKilograms = ImperialConverter.convertPoundsToKilograms(oldMeasurementValue);
            int kilogram = (int) totalKilograms;

            int kilogramFraction = (int) ((totalKilograms - kilogram) * 10);

            dialog.setMeasurementWithFractionLimit(kilogram, kilogramFraction, newCategory);
        }
        else if (previousCategory.getKey().equals(POUNDS_KEY) && newCategory.getKey().equals(STONE_KEY))
        {
            // Split pounds old measurement to stones(primary) and pounds(fraction)
            double stones = ImperialConverter.convertPoundsToStones(oldMeasurementValue);
            int stonesPart = (int) stones;
            int poundsPart = (int) Math.round(ImperialConverter.convertStonesToPounds(stones - stonesPart));

            dialog.setMeasurementWithFractionLimit(stonesPart, poundsPart, newCategory);
        }
        else if (previousCategory.getKey().equals(STONE_KEY) && newCategory.getKey().equals(POUNDS_KEY))
        {
            double dblPounds = ImperialConverter.convertStonesToPounds(oldMeasurementValue);
            int pounds = (int) dblPounds;
            int fraction = (int)((dblPounds - pounds)*10);
            dialog.setMeasurementWithFractionLimit(pounds,fraction,newCategory);
        }
        else if (previousCategory.getKey().equals(STONE_KEY) && newCategory.getKey().equals(KILOGRAMS_KEY))
        {
            // Split stones old measurement to kilogram(primary) and kilogram decimals(fraction)
            double totalKilograms = ImperialConverter.convertStonesToKilogramsWithFraction(
                    oldMeasurementValue, oldMeasurementFractionValue);
            int kilogram = (int) totalKilograms;
            int kilogramFraction = (int) ((totalKilograms - kilogram) * 10);

            dialog.setMeasurementWithFractionLimit(kilogram, kilogramFraction, newCategory);
        }
        else
        {
            Timber.e("Unrecognised previous category to new category conversion. Previous Category: %s. New Category: %s", previousCategory.getKey(), newCategory.getKey());
        }
    }

    /**
     * Triggered when the user closes the dialog box.
     *
     * At this point we should callback to the original class which created this builder object.
     */
    private void onDialogDoneListener(MyMeasurementDialogCategoryVO chosenCategory, MyMeasurementDialogCategoryValueVO chosenValue,
                                      MyMeasurementDialogCategoryValueVO chosenFraction, AsyncHelper.Callback<Weight> builderOnDoneListener)
    {
        int selectedWholeNumberWeight = 0;
        int selectedFractionWeight = 0;

        if (null != chosenValue)
        {
            selectedWholeNumberWeight = chosenValue.getValue();
        }

        if (null != chosenFraction)
        {
            selectedFractionWeight = chosenFraction.getValue();
        }

        Weight weight;

        switch (chosenCategory.getKey())
        {
            case KILOGRAMS_KEY:
                double kilograms = selectedWholeNumberWeight + ((double) selectedFractionWeight / 10);
                weight = new Kilograms(kilograms);
                break;
            case POUNDS_KEY:
                weight = new Pounds(selectedWholeNumberWeight + ((double) selectedFractionWeight / 10));
                break;
            case STONE_KEY:
                weight = new StoneUK(selectedWholeNumberWeight, selectedFractionWeight);
                break;
            default:
                throw new IllegalArgumentException("Unknown weight measurement category: " + chosenCategory.getKey() + ":" + chosenCategory.getLabel());
        }

        setSelectedWeight(weight);

        builderOnDoneListener.execute(weight);
    }
}


