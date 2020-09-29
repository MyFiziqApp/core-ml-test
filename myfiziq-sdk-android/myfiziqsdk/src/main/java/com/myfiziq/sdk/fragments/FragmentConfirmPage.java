package com.myfiziq.sdk.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.FactoryContour;
import com.myfiziq.sdk.util.ImageUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */
public class FragmentConfirmPage extends BaseFragment
{
    private ImageView image;
    private Bitmap imageBitmap;
    private ImageView contour;
    private Bitmap contourBitmap;

    private PoseSide mSide;


    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_confirm_page;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (image != null)
        {
            image.setImageBitmap(null);
            image = null;
        }

        if (imageBitmap != null)
        {
            imageBitmap.recycle();
            imageBitmap = null;
        }

        if (contour != null)
        {
            contour.setImageBitmap(null);
            contour = null;
        }

        if (contourBitmap != null)
        {
            contourBitmap.recycle();
            contourBitmap = null;
        }
    }

    private void initView(View rootView)
    {
        image = rootView.findViewById(R.id.image);
        contour = rootView.findViewById(R.id.contour);
        mSide = PoseSide.fromInt(getParamMapAsInt(R.id.TAG_ARG_SIDE));

        ParameterSet set = getParameterSet();
        if (null != set && set.hasParam(R.id.TAG_ARG_MODEL_AVATAR))
        {
            //TODO: type safety needed? (Parcelable parcelable instanceof ModelAvatar)
            ModelAvatar avatar = (ModelAvatar) set.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
            int backgroundColor = getResources().getColor(R.color.myfiziqsdk_capture_page_background_color);
            int dashColour1 = getResources().getColor(R.color.myfiziqsdk_contour_dash_colour_1);
            int dashColour2 = SisterColors.getInstance().getContourColor(getResources().getColor(R.color.myfiziqsdk_contour_dash_colour_2));

            FactoryContour.getContour(
                    255, 100, avatar, mSide, (float)avatar.getPitch(), 255,
                    (contourBitmap, id) -> styleContourImage(contourBitmap, backgroundColor, dashColour1, dashColour2));


            // TODO CHECK IF ROTATED USING EXIF DATA IN getSideFileFrame() AND IF SO USE THIS STACK OVERFLOW POST TO FIX
            // https://stackoverflow.com/questions/3647993/android-bitmaps-loaded-from-gallery-are-rotated-in-imageview

            AsyncHelper.run(
                    () -> mSide.getFirstSideFileFrame(avatar.getAttemptId()),
                    this::renderUserImage, true);
        }
    }

    /**
     * Styles a contour image with the given style.
     *
     * @param backgroundColor The desired background colour of the bitmap.
     *                        This is the area outside of the outline of the user's target pose.
     * @param dashColour1 The first dash line colour that will surround the contour.
     * @param dashColour2 The second dash line colour that will surround the contour.
     */
    private void styleContourImage(@Nullable Bitmap unstyledBitmap, @ColorInt int backgroundColor, @ColorInt int dashColour1, @ColorInt int dashColour2)
    {
        AsyncHelper.run(() ->
            {
                if (null == unstyledBitmap)
                {
                    return null;
                }

                Bitmap bitmapResult = unstyledBitmap;

                SparseIntArray colorReplacements = new SparseIntArray(3);
                colorReplacements.put(FactoryContour.DEFAULT_BACKGROUND_COLOR, backgroundColor);
                colorReplacements.put(FactoryContour.DEFAULT_DASH1_COLOR, dashColour1);
                colorReplacements.put(FactoryContour.DEFAULT_DASH1_COLOR, dashColour2);

                bitmapResult = ImageUtils.replaceColours(bitmapResult, colorReplacements);

                if (PoseSide.side == mSide)
                {
                    bitmapResult = ImageUtils.mirrorBitmap(bitmapResult);
                }

                return bitmapResult;
            },
            bitmap ->
            {
                if (bitmap != null)
                {
                    contourBitmap = bitmap;
                    contour.setImageBitmap(bitmap);

                    // Make the image and contour visible after processing has finished to stop flickering
                    // when you view the fragment
                    contour.setVisibility(View.VISIBLE);
                    image.setVisibility(View.VISIBLE);
                }
            },
            true
        );
    }

    private void renderUserImage(@Nullable Bitmap userBitmap)
    {
        if (userBitmap == null)
        {
            Timber.e("The user preview bitmap generated was null");
            return;
        }

        imageBitmap = userBitmap;
        image.setImageBitmap(userBitmap);
    }
}
