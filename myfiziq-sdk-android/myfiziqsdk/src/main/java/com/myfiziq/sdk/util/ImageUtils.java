package com.myfiziq.sdk.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.SparseIntArray;

import androidx.annotation.DrawableRes;


/**
 * @hide
 */

public class ImageUtils
{
    public static Bitmap getTextureBitmap(Bitmap src, int widthMax, int heightMax)
    {
        Bitmap bitmap;

        // We need to flip the textures vertically:
        Matrix matrix = new Matrix();
        matrix.postScale(1f, -1f);
        matrix.postRotate(-90);

        int actualWidth = src.getWidth();
        int actualHeight = src.getHeight();

        int desiredWidth = getResizedDimension(widthMax, heightMax, actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(heightMax, widthMax, actualHeight, actualWidth);

        Bitmap tempBitmap = Bitmap.createBitmap(src, 0, 0, desiredWidth, desiredHeight, matrix, true);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight))
        {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        }
        else
        {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    public static Bitmap resizeBitmap(@DrawableRes int id, int widthMax, int heightMax)
    {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap;

        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(GlobalContext.getContext().getResources(), id, decodeOptions);

        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        int desiredWidth = getResizedDimension(widthMax, heightMax, actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(heightMax, widthMax, actualHeight, actualWidth);

        decodeOptions.inPreferQualityOverSpeed = true;
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);

        // Decode to the nearest power of two scaling factor.
        decodeOptions.inJustDecodeBounds = false;
        Bitmap tempBitmap = BitmapFactory.decodeResource(GlobalContext.getContext().getResources(), id, decodeOptions);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight))
        {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        }
        else
        {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    public static Bitmap resizeBitmap(String srcPath, int widthMax, int heightMax)
    {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap;

        // If we have to resize this image, first get the natural bounds.
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcPath, decodeOptions);
        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;

        int desiredWidth = getResizedDimension(widthMax, heightMax, actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(heightMax, widthMax, actualHeight, actualWidth);

        decodeOptions.inPreferQualityOverSpeed = true;
        decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);

        // Decode to the nearest power of two scaling factor.
        decodeOptions.inJustDecodeBounds = false;
        Bitmap tempBitmap = BitmapFactory.decodeFile(srcPath, decodeOptions);

        // If necessary, scale down to the maximal acceptable size.
        if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight))
        {
            bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
            tempBitmap.recycle();
        }
        else
        {
            bitmap = tempBitmap;
        }

        return bitmap;
    }

    /**
     * Mirrors the bitmap and recycles the old Bitmap.
     *
     * @param src The bitmap to mirror. After being mirrored, this instance will be recycled. You must use the returned bitmap.
     * @return The mirrored bitmap.
     */
    public static Bitmap mirrorBitmap(Bitmap src)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);

        Bitmap newBitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        src.recycle();

        return newBitmap;
    }

    /**
     * Replaces one colour in a bitmap with another and recycles the old Bitmap.
     *
     * @param src The source bitmap. After having the colours replaced, this instance will be recycled. You must use the returned bitmap.
     * @param colorReplacements The colours the replace. The key is the value to look for, the value is the replacement colour.
     * @return A new bitmap with the desired colours replaced.
     */
    public static Bitmap replaceColours(Bitmap src, SparseIntArray colorReplacements)
    {
        if (src == null || colorReplacements == null)
        {
            return null;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = new int[width * height];

        // Load the pixels array with those from the bitmap
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        // Go to each pixel and replace the old colour with the new colour
        for (int x = 0; x < pixels.length; ++x)
        {
            // Checks to see if the pixel we're looking at now needs to be replaced with another colour
            // If it doesn't, -1 is returned
            int replacementValue = colorReplacements.get(pixels[x], -1);

            // If we need to replace the pixel with another colour...
            if (replacementValue != -1)
            {
                // Replace it with the desired colour
                pixels[x] = replacementValue;
            }
        }

        // Create a new Bitmap to return as the result
        Bitmap newBitmap = Bitmap.createBitmap(width, height, src.getConfig());

        // Insert the pixel array back into the new Bitmap
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        src.recycle();

        return newBitmap;
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth  Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param reqWidth     Desired width of the bitmap
     * @param reqHeight    Desired height of the bitmap
     */
    private static int findBestSampleSize(int actualWidth, int actualHeight, int reqWidth, int reqHeight)
    {
        int inSampleSize = 1;
        if (actualHeight > reqHeight || actualWidth > reqWidth)
        {
            if (actualWidth > actualHeight)
            {
                inSampleSize = Math.round((float) actualHeight / (float) reqHeight);
            }
            else
            {
                inSampleSize = Math.round((float) actualWidth / (float) reqWidth);
            }
            final float totalPixels = actualWidth * actualHeight;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap)
            {
                // inSampleSize++;
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary      Maximum size of the primary dimension (i.e. width for max
     *                        width), or zero to maintain aspect ratio with secondary
     *                        dimension
     * @param maxSecondary    Maximum size of the secondary dimension, or zero to maintain
     *                        aspect ratio with primary dimension
     * @param actualPrimary   Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary, int actualSecondary)
    {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0)
        {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling
        // ratio.
        if (maxPrimary == 0)
        {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0)
        {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;

        if (resized * ratio > maxSecondary)
        {
            resized = (int) (maxSecondary / ratio);
        }

        return resized;
    }
}
