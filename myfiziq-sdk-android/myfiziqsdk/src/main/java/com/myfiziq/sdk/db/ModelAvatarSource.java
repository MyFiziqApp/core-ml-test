package com.myfiziq.sdk.db;

import com.myfiziq.sdk.util.SensorUtils;

import java.io.File;

/**
 * @hide
 */
public class ModelAvatarSource extends Model
{
    @Persistent(idMap = true, serialize = false)
    public String mSourceFile = "";

    @Persistent
    public PoseSide type = PoseSide.front;

    @Persistent
    public double pitch = 0.0;

    @Persistent
    public double GravityZ = 0.0;

    @Persistent
    public double roll = 0.0;

    @Persistent
    public double GravityY = 0.0;

    @Persistent
    public double yaw = 0.0;

    @Persistent
    public double GravityX = 0.0;

    @Persistent(idMap = true, jsonMap = "index")
    public int fileIndex = 0;

    @Persistent(serialize = false)
    public int fileOffset = 0;

    @Persistent(serialize = false)
    public String inspectResult = "";

    public ModelAvatarSource()
    {
        init();
    }

    public void init()
    {
        id = getIdFromPathIx(mSourceFile, fileIndex);
    }

    public void setFile(File outputFile, int ms)
    {
        mSourceFile = outputFile.getAbsolutePath();
        fileOffset = ms;
    }

    public void setSensorValues(SensorUtils sensorUtils)
    {
        yaw = sensorUtils.getYaw();
        roll = sensorUtils.getRoll();
        pitch = sensorUtils.getPitch();
        GravityX = sensorUtils.getX();
        GravityY = sensorUtils.getY();
        GravityZ = sensorUtils.getZ();
    }

    public static String getIdFromPathIx(String path, int ix)
    {
        return path + String.valueOf(ix);
    }

    public void setSensorValues(double parYaw, double parPitch, double parRoll, double parX, double parY, double parZ)
    {
        yaw = parYaw;
        pitch = parPitch;
        roll = parRoll;
        GravityX = parX;
        GravityY = parY;
        GravityZ = parZ;
    }
}
