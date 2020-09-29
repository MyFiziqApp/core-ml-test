package com.myfiziq.sdk.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.myfiziq.sdk.util.GlobalContext;

import java.io.File;

/**
 * The literal side for an Avatar image - i.e front position or side position.
 */
public enum PoseSide implements ModelInterface
{

    //TODO: can these be in uppercase?
    front("front"),
    side("side");


    public final static String CAPTURE_IMAGE_EXTENSION = ".bmp";

    String mLabel = "";

    PoseSide(String label)
    {
        mLabel = label;
    }

    public static PoseSide fromInt(int val)
    {
        PoseSide side = PoseSide.front;
        if (val >= 0 && val < PoseSide.values().length)
            side = PoseSide.values()[val];

        return side;
    }

    public String getSideImageFilename(String attemptId, int index)
    {
        return name() + attemptId + "_" + index + CAPTURE_IMAGE_EXTENSION;
    }

    public Bitmap getSideFileFrame(String attemptId, int index)
    {
        File directory = GlobalContext.getContext().getFilesDir();
        String sideFilename = getSideImageFilename(attemptId, index);
        File sideFile = new File(directory, sideFilename);
        if (sideFile.exists())
        {
            return BitmapFactory.decodeFile(sideFile.getAbsolutePath());
        }
        return null;
    }

    public Bitmap getFirstSideFileFrame(String attemptId)
    {
        for (int i=0; i<4; i++)
        {
            Bitmap result = getSideFileFrame(attemptId, i);
            if (null != result)
                return result;
        }

        return null;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
