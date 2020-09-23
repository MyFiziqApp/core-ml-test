package com.myfiziq.sdk.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.myfiziq.sdk.helpers.RegexHelpers;

/**
 * Listens for changes to a text field and formats the text to have a suffix
 * that has been specified in the constructor.
 */
public class GenericUnitsTextWatcher implements TextWatcher
{
    private EditText view;
    private String suffix;
    private int maxLength;


    public GenericUnitsTextWatcher(EditText view, String suffix, int maxLength)
    {
        this.view = view;
        this.suffix = suffix;
        this.maxLength = maxLength;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
        // Strip all non-numeric text to get the raw units/numbers that the user has entered.
        String text = s.toString();
        String textWithOnlyNumbers = text.replaceAll(RegexHelpers.onlyNumbersAndDecimals, "");

        String newText = textWithOnlyNumbers;

        if (textWithOnlyNumbers.length() > maxLength)
        {
            newText = newText.substring(0, maxLength);
        }

        // Add the suffix to the number that the user has entered
        newText += suffix;

        // Remove the text changed listener so we don't go into an infinite loop as we're about to change the text.
        view.removeTextChangedListener(this);

        int usersCursor = view.getSelectionEnd();
        view.setText(newText);

        if (usersCursor > textWithOnlyNumbers.length())
        {
            // Ensure that the user's cursor is at the end of the number, not the end of the suffix
            usersCursor = textWithOnlyNumbers.length();
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
}
