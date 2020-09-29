package com.myfiziq.sdk.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.myfiziq.sdk.helpers.RegexHelpers;

/**
 * Listens for changes to a text field and formats the text using the imperial units of measurement.
 */
public class ImperialHeightTextWatcher implements TextWatcher
{
    private EditText view;

    public ImperialHeightTextWatcher(EditText view)
    {
        this.view = view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
        String newText = formatInputInImperialUnits(s.toString());

        // Remove the text changed listener so we don't go into an infinite loop as we're about to change the text.
        view.removeTextChangedListener(this);

        int usersCursor = view.getSelectionEnd();
        view.setText(newText);

        if (usersCursor > newText.length())
        {
            usersCursor = newText.length();
        }

        // Restore the user's cursor location to the position before updating the text
        view.setSelection(usersCursor);

        // Start listening for user input again.
        view.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable oldInput)
    {

    }

    private String formatInputInImperialUnits(String input)
    {
        String textWithOnlyNumbers = input.replaceAll(RegexHelpers.onlyNumbers, "");

        if (textWithOnlyNumbers.length() > 3)
        {
            // Height is too big. Can't have someone taller than 9 feet.
            textWithOnlyNumbers = textWithOnlyNumbers.substring(0, 3);
        }

        int feet = 0;
        int inches = 0;

        if (textWithOnlyNumbers.length() > 0)
        {
            int height = Integer.parseInt(textWithOnlyNumbers);

            if (height > 99)
            {
                feet = height / 100;
                inches = height % 100;

                if (inches > 12)
                {
                    // There's only 12 inches in a foot
                    inches = 12;
                }
            }
            else
            {
                inches = height;

                if (inches >= 10)
                {
                    // The user is probably trying to enter both feet and inches, so lets make the first digit a foot and the second an inch.
                    feet = inches / 10;
                    inches = inches % 10;
                }
            }
        }


        String newText = "";

        if (feet > 0 && inches > 0)
        {
            newText = feet + "' " + inches + "\"";
        }
        else if (feet > 0 && inches == 0)
        {
            newText = feet + "' " + inches + "\"";
        }
        else if (feet == 0 && inches > 0)
        {
            newText = inches + "\"";
        }

        return newText;
    }
}
