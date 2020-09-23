package com.myfiziq.myfiziq_android.helpers;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

/**
 * @hide
 */

public class Utils
{
    public static void hideSoftKeyboard(Context context)
    {
        if (context != null)
        {
            try
            {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                View view = null;
                // check if no view has focus:
                if (context instanceof Activity)
                {
                    view = ((Activity) context).getCurrentFocus();
                }

                if (view == null)
                    return;

                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            catch (NullPointerException e)
            { // Keyboard was already hidden?
            }
        }
    }

    /**
     * Ensures that the password visibility button is visible after the user enters text.
     *
     * The button may become invisible after the user tries to submit the form and an error is shown.
     *
     * @param container The TextInputLayout container of the EditText that can be used to enter a password.
     */
    public static void enablePasswordVisibilityButtonOnTextChange(TextInputLayout container)
    {
        EditText editText = container.getEditText();

        if (null == editText)
        {
            return;
        }

        editText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (!TextUtils.isEmpty(s))
                {
                    container.setPasswordVisibilityToggleEnabled(true);
                }
            }
        });
    }
}
