package com.myfiziq.sdk;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.myfiziq.sdk.helpers.InsightsFormulas;
import com.myfiziq.sdk.util.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EthnicityPickerDialog extends DialogFragment
{
    private AlertDialog dialog;
    View dialogView;
    NumberPicker numberPicker;
    public EditText linkedEditText;
    public int chosenEthnicity = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        dialogView = inflater.inflate(com.myfiziq.myfiziqsdk_android_input.R.layout.ethnicitypicker, null);
        numberPicker = dialogView.findViewById(com.myfiziq.myfiziqsdk_android_input.R.id.numpicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(3);
        numberPicker.setDisplayedValues(new String[]{
                InsightsFormulas.getEthnicityDisplayFromNumber(1, getResources()),
                InsightsFormulas.getEthnicityDisplayFromNumber(2, getResources()),
                InsightsFormulas.getEthnicityDisplayFromNumber(3, getResources())
        });
        numberPicker.setValue(chosenEthnicity);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setPositiveButton("DONE", (dialog, whichButton) ->
                {
                    linkedEditText.setText(numberPicker.getDisplayedValues()[numberPicker.getValue() - 1]);
                    chosenEthnicity = numberPicker.getValue();
                    dialog.dismiss();
                })
                .setNegativeButton("CANCEL", (dialog, whichButton) ->
                {
                    dialog.dismiss();
                })
                .setView(dialogView);

        dialog = dialogBuilder.create();
        return dialog;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        UiUtils.setAlertDialogColours(getActivity(), dialog);
    }
}
