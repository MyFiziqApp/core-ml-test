package com.myfiziq.sdk.db;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewParent;

import androidx.cardview.widget.CardView;

public class ModelCardColorMap extends ModelSisterColour
{
    public void applyStyle(View view, ViewParent parent)
    {
        CardView cv = (CardView)view;

        ColorStateList colorStateList = cv.getCardBackgroundColor();

        if (getSourceColour().nullOrMatch(colorStateList.getDefaultColor()))
        {
            getDestinationColour().apply(cv::setCardBackgroundColor);
        }
    }
}
