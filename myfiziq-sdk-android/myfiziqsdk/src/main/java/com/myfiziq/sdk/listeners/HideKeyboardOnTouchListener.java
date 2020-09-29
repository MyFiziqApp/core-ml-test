package com.myfiziq.sdk.listeners;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class HideKeyboardOnTouchListener implements View.OnTouchListener
{
    @Override
    public boolean onTouch(View view, MotionEvent event)
    {
        Context context = view.getContext();

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            view.performClick();
        }

        return false;
    }
}
