package com.myfiziq.sdk.components;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.vo.DatePickerResultVO;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class MyDatePickerDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener
{
    private AsyncHelper.Callback<DatePickerResultVO> onDateSetListener;

    private DatePickerDialog dialog;
    private LinearLayout pickers;

    private ViewGroup pickersContainer;
    private NumberPicker dayPicker;
    private NumberPicker monthPicker;
    private NumberPicker yearPicker;

    private int defaultYear;
    private int defaultMonth;
    private int defaultDay;


    /**
     * Create a new instance of MyDatePickerDialog using the default values.
     */
    public static MyDatePickerDialog newInstance()
    {
        return new MyDatePickerDialog();
    }

    /**
     * Create a new instance of MyDatePickerDialog with a default date set.
     */
    // Reference implementation for passing arguments to a DialogFragment:
    // https://developer.android.com/reference/android/app/DialogFragment.html
    public static MyDatePickerDialog newInstance(int defaultYear, int defaultMonth, int defaultDay)
    {
        MyDatePickerDialog f = new MyDatePickerDialog();

        Bundle args = new Bundle();
        args.putInt("defaultYear", defaultYear);
        args.putInt("defaultMonth", defaultMonth);
        args.putInt("defaultDay", defaultDay);
        f.setArguments(args);

        return f;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (null != getArguments()
                && getArguments().containsKey("defaultYear")
                && getArguments().containsKey("defaultMonth")
                && getArguments().containsKey("defaultDay"))
        {
            defaultYear = getArguments().getInt("defaultYear", -1);
            defaultMonth = getArguments().getInt("defaultMonth", -1);
            defaultDay = getArguments().getInt("defaultDay", -1);
        }
        else
        {
            // Use the current date as the default date in the picker if no default date was pased to the DialogFragment
            Calendar c = Calendar.getInstance();
            defaultYear = c.get(Calendar.YEAR);
            defaultMonth = c.get(Calendar.MONTH);
            defaultDay = c.get(Calendar.DAY_OF_MONTH);
        }

        // Create a new instance of DatePickerDialog and return it
        dialog = new DatePickerDialog(getActivity(), R.style.MFDatePickerTheme, this, defaultYear, defaultMonth, defaultDay);

        bindDatePickers(dialog);

        return dialog;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        UiUtils.setAlertDialogColours(getActivity(), dialog);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        if (null != onDateSetListener)
        {
            DatePickerResultVO result = new DatePickerResultVO(year, month, day);
            onDateSetListener.execute(result);
        }
    }

    public void setOnDateSetListener(AsyncHelper.Callback<DatePickerResultVO> onDateSetListener)
    {
        this.onDateSetListener = onDateSetListener;
    }

    private void bindDatePickers(DatePickerDialog dialog)
    {
        DatePicker datePicker = dialog.getDatePicker();

        int pickersContainerId = getActivity().getResources().getIdentifier("android:id/pickers", "id", null);
        int dayPickerId = getActivity().getResources().getIdentifier("android:id/day", "id", null);
        int monthPickerId = getActivity().getResources().getIdentifier("android:id/month", "id", null);
        int yearPickerId = getActivity().getResources().getIdentifier("android:id/year", "id", null);

        pickersContainer = datePicker.findViewById(pickersContainerId);
        dayPicker = datePicker.findViewById(dayPickerId);
        monthPicker = datePicker.findViewById(monthPickerId);
        yearPicker = datePicker.findViewById(yearPickerId);
    }

    public void setContainerPadding(int left, int top, int right, int bottom)
    {
        if (null != pickersContainer)
        {
            Resources resources = getResources();
            pickersContainer.setPadding(
                    resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_left),
                    resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_top),
                    resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_right),
                    resources.getInteger(R.integer.myfiziqsdk_datepicker_padding_bottom)
            );

            pickersContainer.invalidate();
        }
    }
}
