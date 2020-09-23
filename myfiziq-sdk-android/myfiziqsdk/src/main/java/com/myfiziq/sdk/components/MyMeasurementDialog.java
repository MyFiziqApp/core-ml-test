package com.myfiziq.sdk.components;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.MyMeasurementDialogAdapter;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.MYQDialogView;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryValueVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogResultVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogVO;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import timber.log.Timber;


public class MyMeasurementDialog extends DialogFragment
{
    private AsyncHelper.Callback<MyMeasurementDialogResultVO> onSelectedValueCallback;

    private MyMeasurementDialogVO parameters;

    private MyMeasurementDialogAdapter categoryAdapter;

    private AlertDialog dialog;
    private MYQDialogView dialogView;

    private TextView heading;
    private Spinner categorySpinner;
    private NumberPicker measurementPicker;
    private NumberPicker measurementFractionPicker;


    private OnCategoryChanged onCategoryChangedListener;
    private OnDoneListener onDoneListener;

    private MyMeasurementDialogCategoryVO currentCategory;


    /**
     * Create a new instance of MyMeasurementDialog.
     */
    // Reference implementation for passing arguments to a DialogFragment:
    // https://developer.android.com/reference/android/app/DialogFragment.html
    public static MyMeasurementDialog newInstance(MyMeasurementDialogVO parameters)
    {
        MyMeasurementDialog f = new MyMeasurementDialog();

        Bundle args = new Bundle();
        args.putParcelable("parameters", parameters);
        f.setArguments(args);

        return f;
    }


    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState)
    {
        if (null == getArguments() || !getArguments().containsKey("parameters"))
        {
            throw new IllegalStateException("No parameters were supplied to the " + MyMeasurementDialog.class.getSimpleName());
        }

        parameters = getArguments().getParcelable("parameters");



        LayoutInflater inflater = getActivity().getLayoutInflater();
        dialogView = (MYQDialogView) inflater.inflate(R.layout.dialog_measurement_2_spinner, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setPositiveButton("DONE", (dialog, whichButton) -> {
                    onDone();
                    dialog.dismiss();
                })
                .setNegativeButton("CANCEL", (dialog, whichButton) -> {
                    dialog.dismiss();
                })
                .setView(dialogView);

        dialog = dialogBuilder.create();
        UiUtils.setAlertDialogColours(getActivity(),dialog);

        bindViews(dialogView);
        constructDialog();

        return dialog;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        UiUtils.setAlertDialogColours(getActivity(), dialog);
    }

    public void setOnDoneListener(OnDoneListener onDoneListener)
    {
        this.onDoneListener = onDoneListener;
    }

    public void setCategory(MyMeasurementDialogCategoryVO requestedSelection)
    {
        int requestedSelectionPosition = categoryAdapter.getPosition(requestedSelection);
        categorySpinner.setSelection(requestedSelectionPosition);
    }

    public void setMeasurement(int value)
    {
        setProperValue(measurementPicker, value);
    }

    public void setMeasurement(int value, int fractionValue)
    {
        setProperValue(measurementPicker, value);
        setProperValue(measurementFractionPicker, fractionValue);
    }

    public void setMeasurementWithFractionLimit (int value, int fractionValue, MyMeasurementDialogCategoryVO category)
    {
        setDynamicFractionSpinner(category, value);
        setProperValue(measurementPicker, value);
        setProperFractionValue(measurementPicker, measurementFractionPicker, value, fractionValue);
    }

    private void setProperValue(NumberPicker picker, int value){
        if(value < picker.getMinValue())
        {
            value = picker.getMinValue();
        }
        else if(value > picker.getMaxValue())
        {
            value = picker.getMaxValue();
        }

        picker.setValue(value);
    }

    private void setProperFractionValue(NumberPicker picker, NumberPicker fractionPicker,
                                        int value, int fractionValue){
        if(value < picker.getMinValue() || fractionValue < fractionPicker.getMinValue())
        {
            fractionValue = fractionPicker.getMinValue();
        }
        else if(value > picker.getMaxValue() || fractionValue > fractionPicker.getMaxValue())
        {
            fractionValue = fractionPicker.getMaxValue();
        }

        fractionPicker.setValue(fractionValue);
    }

    public void setOnCategoryChangedListener(OnCategoryChanged onCategoryChangedListener)
    {
        this.onCategoryChangedListener = onCategoryChangedListener;
    }

    private void bindViews(View parent)
    {
        heading = parent.findViewById(R.id.heading);
        categorySpinner = parent.findViewById(R.id.unitsOfMeasurement);
        measurementPicker = parent.findViewById(R.id.measurement);
        measurementFractionPicker = parent.findViewById(R.id.measurement_second);

        // Ensure that the user cannot edit the values inside the NumberPicker
        measurementPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        measurementFractionPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    }

    private void constructDialog()
    {
        String title = parameters.getDialogTitle();
        ArrayList<MyMeasurementDialogCategoryVO> categories = parameters.getCategories();
        int defaultCategoryIndex = categories.indexOf(parameters.getDefaultCategory());

        heading.setText(title);

        categoryAdapter = new MyMeasurementDialogAdapter(
                getActivity(),
                android.R.layout.simple_spinner_item,
                categories
        );


        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setSelection(defaultCategoryIndex);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                MyMeasurementDialogCategoryVO previousCategory = currentCategory;
                currentCategory = categoryAdapter.getItem(position);

                onCategorySelected(currentCategory, previousCategory, measurementPicker.getValue(),
                        measurementFractionPicker.getValue());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });


        int defaultMeasurement = 0;

        if (null != parameters.getDefaultValue())
        {
            defaultMeasurement = parameters.getDefaultValue().getValue();
        }

        if(null != parameters.getDefaultFractionValue())
        {
            int defaultFractionMeasurement = parameters.getDefaultFractionValue().getValue();
            onCategorySelected(parameters.getDefaultCategory(), parameters.getDefaultCategory(),
                    defaultMeasurement, defaultFractionMeasurement);
        }
        else
        {
            onCategorySelected(parameters.getDefaultCategory(), parameters.getDefaultCategory(),
                    defaultMeasurement, 0);
        }

    }

    /**
     * Called when the user clicks the positive button (e.g. "Done" button).
     */
    private void onDone()
    {
        MyMeasurementDialogCategoryVO selectedCategory = (MyMeasurementDialogCategoryVO) categorySpinner.getSelectedItem();

        //Whole Value
        int selectedRawValue = measurementPicker.getValue();
        MyMeasurementDialogCategoryValueVO selectedValue = null;

        for (MyMeasurementDialogCategoryValueVO potentialValue : selectedCategory.getValues())
        {
            if (potentialValue.getValue() == selectedRawValue)
            {
                selectedValue = potentialValue;
            }
        }

        //Fraction Value
        //Checking whether fraction value is exist or not
        MyMeasurementDialogCategoryValueVO selectedFractionValue = null;

        if(null != selectedCategory.getFractionValues())
        {
            int selectedRawFractionValue = measurementFractionPicker.getValue();
            for (MyMeasurementDialogCategoryValueVO potentialValue : selectedCategory.getFractionValues())
            {
                if (potentialValue.getValue() == selectedRawFractionValue)
                {
                    selectedFractionValue = potentialValue;
                }
            }
        }


        if (null != selectedValue)
        {
            onDoneListener.onDone(selectedCategory, selectedValue, selectedFractionValue);
        }
        else
        {
            Timber.e("Cannot find the selected measurement value in the list of allowed values. The user selected: %s", selectedRawValue);
        }

    }

    private void onCategorySelected(MyMeasurementDialogCategoryVO newCategory,
                                    MyMeasurementDialogCategoryVO previousCategory,
                                    int oldMeasurementValue, int oldMeasurementFractionValue)
    {
        if (onCategoryChangedListener == null || getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        // Set primary values to first spinner
        setCategoryValueToSpinner(newCategory.getValues(), measurementPicker);

        //Set fraction values to second spinner
        fractionLimitChecker(newCategory);

        if(previousCategory != null && previousCategory.getFractionValues() != null)
        {
            onCategoryChangedListener.onChanged(newCategory, previousCategory, oldMeasurementValue, oldMeasurementFractionValue);
        }
        else
        {
            onCategoryChangedListener.onChanged(newCategory, previousCategory, oldMeasurementValue , 0);
        }

    }

    private void setCategoryValueToSpinner(ArrayList<MyMeasurementDialogCategoryValueVO> values,
                                           NumberPicker picker) {
        if(values != null)
        {
            String[] displayedValueLabels = new String[values.size()];

            for (int i = 0; i<displayedValueLabels.length; i++)
            {
                displayedValueLabels[i] = values.get(i).getLabel();
            }

            int firstValue = values.get(0).getValue();
            int lastValue = values.get(values.size()-1).getValue();

            // Ensure that the displayed values are cleared if there's any in there at the moment
            // to prevent an ArrayIndexOutOfBoundsException when calling setMinValue and setMaxValue.
            picker.setDisplayedValues(null);

            picker.setMinValue(firstValue);
            picker.setMaxValue(lastValue);
            picker.setDisplayedValues(displayedValueLabels);

            picker.setVisibility(View.VISIBLE);
        }
        else
        {
            picker.setVisibility(View.GONE);
        }

    }

    /**
     * Create a Spinner with range from @firstValue to @lastValue
     * Creates a Spinner within a specified range.
     *
     * @param values The values to show in the {@link NumberPicker}.
     * @param picker The picker to apply the values to.
     * @param minValue The minimum value in the {@link NumberPicker}.
     * @param maxValue The maximum value in the {@link NumberPicker}.
     */
    private void setCategoryValueToSpinner(ArrayList<MyMeasurementDialogCategoryValueVO> values,
                                           NumberPicker picker, int minValue, int maxValue) {
        if(values != null)
        {
            int size = maxValue - minValue + 1;

            String[] displayedValueLabels = new String[size];

            for (int i = 0; i < size; i++)
            {
                displayedValueLabels[i] = values.get(minValue + i).getLabel();
            }

            // Ensure that the displayed values are cleared if there's any in there at the moment
            // to prevent an ArrayIndexOutOfBoundsException when calling setMinValue and setMaxValue.
            picker.setDisplayedValues(null);

            picker.setMinValue(minValue);
            picker.setMaxValue(maxValue);
            picker.setDisplayedValues(displayedValueLabels);

            picker.setVisibility(View.VISIBLE);
        }
        else
        {
            picker.setVisibility(View.GONE);
        }

    }

    private void fractionLimitChecker(MyMeasurementDialogCategoryVO categoryVO){
        if(categoryVO.getFractionValues() != null && categoryVO.hasFractionLimit())
        {

            measurementPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    setDynamicFractionSpinner(categoryVO, newVal);
                }
            });
        }
        else
        {
            measurementPicker.setOnValueChangedListener(null);
            measurementFractionPicker.setVisibility(View.GONE);
        }
    }

    /**
     * Create a Dynamic Fraction Values To Spinner. Fraction Values will changed based on Primary
     * spinner values position.
     */
    private void setDynamicFractionSpinner(MyMeasurementDialogCategoryVO categoryVO, int currentValue){
        ArrayList<MyMeasurementDialogCategoryValueVO> fractionValues = categoryVO.getFractionValues();

        int lowerValue = categoryVO.getValues().get(0).getValue();
        int upperValue = categoryVO.getValues().get(categoryVO.getValues().size()-1).getValue();
        int lowerMinValue = categoryVO.getFractionLimit().getLowerMinValue();
        int lowerMaxValue = categoryVO.getFractionLimit().getLowerMaxValue();
        int upperMinValue = categoryVO.getFractionLimit().getUpperMinValue();
        int upperMaxValue = categoryVO.getFractionLimit().getUpperMaxValue();

        if(currentValue == lowerValue)
        {
            setCategoryValueToSpinner(fractionValues, measurementFractionPicker, lowerMinValue, lowerMaxValue);
        }
        else if (currentValue == upperValue)
        {
            setCategoryValueToSpinner(fractionValues, measurementFractionPicker, upperMinValue, upperMaxValue);
        }
        else
        {
            setCategoryValueToSpinner(fractionValues, measurementFractionPicker);
        }

    }

}
