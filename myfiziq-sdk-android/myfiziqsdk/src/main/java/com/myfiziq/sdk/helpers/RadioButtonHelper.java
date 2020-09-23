package com.myfiziq.sdk.helpers;

import android.content.res.Resources;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import com.myfiziq.sdk.R;

/**
 * Provides helper methods for manipulating Radio Buttons in Android.
 */
public class RadioButtonHelper
{
    /**
     * Processes a Radio Button's state to ensure that it has the right colour and
     * the right checked state based on where it is in its
     * lifecycle.
     *
     * @param resources A reference to a {@link Resources} object.
     * @param button The button to process.
     * @param radioButtonsInGroup An array of other RadioButtons that are in the same group (e.g. Male and Female are both part of the "Gender" radio group.
     */
    public static void processRadioButtonState(Resources resources, CompoundButton button, final RadioButton[] radioButtonsInGroup)
    {
        View parent = (View) button.getParent();

        for (RadioButton buttonInGroup : radioButtonsInGroup)
        {
            if (buttonInGroup.getId() != button.getId() && buttonInGroup.isChecked() && button.isChecked())
            {
                // If there's another button in the "Radio Group" that is already checked, uncheck this one.
                // There can only be 1 radio button that is checked in a group!
                buttonInGroup.setChecked(false);
            }

            buttonInGroup.setError(null);
        }

        if (button.isChecked())
        {
            parent.setBackground(resources.getDrawable(R.drawable.shape_radio_button_background_checked));
        }
        else
        {
            parent.setBackground(resources.getDrawable(R.drawable.shape_radio_button_background_unchecked));
        }
    }
}
