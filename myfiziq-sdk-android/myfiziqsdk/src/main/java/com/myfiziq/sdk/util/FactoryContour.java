package com.myfiziq.sdk.util;

import android.graphics.Color;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.PoseSide;

/**
 * @hide
 */
public class FactoryContour
{
    public static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;
    public static final int DEFAULT_DASH1_COLOR = Color.WHITE;
    public static final int DEFAULT_DASH2_COLOR = Color.rgb(100, 100, 100);

    public static void getContour(int onColor, int offColor, ModelAvatar avatar, PoseSide side, float theta, int fill, MyFiziq.ContourEvents callback)
    {
        MyFiziq.getInstance().getContour(onColor, offColor, 1280, 720, avatar.getHeight().getValueInCm(), avatar.getWeight().getValueInKg(), avatar.getGender(), side, theta, fill, callback);
    }
}
