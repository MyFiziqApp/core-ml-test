package com.myfiziq.sdk.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.myfiziq.sdk.BuildConfig;

import java.lang.ref.WeakReference;

/**
 * @hide
 */

public class SensorUtils implements SensorEventListener
{
    final static float Z_RANGE = 180.0f;

    // The number of degrees from absolute vertical that is allowed for correct phone alignment.
    final static double PITCH_RANGE_DEG = 2.0;
    final static double YAW_RANGE_DEG = 10.0;
    final static double PITCH_OFFSET_DEG = -(PITCH_RANGE_DEG/5);

    private static final float vGapZ = 8f;
    private static final float vGapX = 8f;

    private double mYawRangeDeg = YAW_RANGE_DEG;
    private double mPitchRangeDeg = PITCH_RANGE_DEG;
    boolean mIsDeviceUpright = false;
    boolean mIsYawInRange = false;
    boolean mIsPitchInRange = false;
    boolean mPreviousYawInRange = false;
    boolean mPreviousPitchInRange = false;
    long timeStartedReceivingBadReadings = 0;
    private float mFilteredAccels[] = new float[3];
    private float mFilteredMags[] = new float[3];
    private float mFilteredGravs[] = new float[3];
    SensorFilter mYaw = new SensorFilter(10);
    SensorFilter mRoll = new SensorFilter(10);
    SensorFilter mPitch = new SensorFilter(10);

    SensorFilter mX = new SensorFilter(10);
    SensorFilter mY = new SensorFilter(10);
    SensorFilter mZ = new SensorFilter(10);

    SensorUtilsListener mListener;
    WeakReference<Context> mContext;
    private Sensor mSensorAcc = null;
    private Sensor mSensorMag = null;
    private Sensor mSensorGrav = null;

    private float gravity[] = new float[9];
    private float magnetic[] = new float[9];
    private float values[] = new float[3];
    private float[] outGravity = new float[9];
    private float yaw;
    private float pitch;
    private float roll;
    private float mLastAccels[] = null;
    private float mLastMags[] = null;
    private float mLastGravs[] = null;

    public interface SensorUtilsListener
    {
        void deviceVerticalChanged();

        void sensorChanged();

        void noSensorDataReceived();
    }

    public SensorUtils(Context context)
    {
        mContext = new WeakReference<>(context);
        initVerticalRange();
    }

    public SensorUtils(Context context, SensorUtilsListener listener)
    {
        mContext = new WeakReference<>(context);
        mListener = listener;
        initVerticalRange();
    }

    private void initVerticalRange()
    {
        if (BuildConfig.DEBUG)
        {
            //mYawRangeDeg = YAW_RANGE_DEG * 2;
            //mPitchRangeDeg = PITCH_RANGE_DEG * 2;
        }
    }

    public void setListener(SensorUtilsListener listener)
    {
        mListener = listener;
    }

    public void registerSensors()
    {
        if (null != mContext.get() && (null == mSensorAcc || null == mSensorMag))
        {
            SensorManager sensorManager = (SensorManager) mContext.get().getSystemService(Context.SENSOR_SERVICE);

            if (null == mSensorAcc)
            {
                mSensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (mSensorAcc != null)
                {
                    sensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_GAME);
                }
            }

            if (null == mSensorMag)
            {
                mSensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                if (mSensorMag != null)
                {
                    sensorManager.registerListener(this, mSensorMag, SensorManager.SENSOR_DELAY_GAME);
                }
            }

            if (null == mSensorGrav)
            {
                mSensorGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                if (mSensorGrav != null)
                {
                    sensorManager.registerListener(this, mSensorGrav, SensorManager.SENSOR_DELAY_GAME);
                }
            }
        }
    }

    public void unregisterSensors()
    {
        mListener = null;
        mSensorAcc = null;
        mSensorMag = null;
        mSensorGrav = null;
        if (null != mContext.get())
        {
            SensorManager sensorManager = (SensorManager) mContext.get().getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mLastMags = event.values.clone();
                mFilteredMags = SensorFilter.lowPass(mLastMags, mFilteredMags);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mLastAccels = event.values.clone();
                mFilteredAccels = SensorFilter.lowPass(mLastAccels, mFilteredAccels);
                break;
            case Sensor.TYPE_GRAVITY:
                mLastGravs = event.values.clone();
                mFilteredGravs = SensorFilter.lowPass(mLastGravs, mFilteredGravs);
                if (mFilteredGravs != null)
                {
                    mX.newValue(mFilteredGravs[0]);
                    mY.newValue(mFilteredGravs[1]);
                    mZ.newValue(mFilteredGravs[2]);
                }
                break;
        }

        // We need aceel and mag readings to use getRotationMatrix
        if (null != mSensorAcc && null != mSensorMag)
        {
            // Ensure we have at least one mag and accel reading...
            if (mLastMags != null && mLastAccels != null)
            {
                if (SensorManager.getRotationMatrix(gravity, magnetic, mFilteredAccels, mFilteredMags))
                {
                    SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X, SensorManager.AXIS_Z, outGravity);
                    SensorManager.getOrientation(outGravity, values);
                    roll = (float) Math.toDegrees(values[0]);
                    yaw = (float) Math.toDegrees(values[2]);
                    pitch = (float)Math.toDegrees(values[1]);

                    mYaw.newValue(yaw);
                    mRoll.newValue(roll);
                    mPitch.newValue(pitch);

                    mIsYawInRange = isInRange(0, getYaw(), mYawRangeDeg);
                    mIsPitchInRange = isInRange(0, getPitch()+PITCH_OFFSET_DEG, mPitchRangeDeg);
                    mIsDeviceUpright = (mIsYawInRange && mIsPitchInRange);
                }
            }
        }
        // Fallback for devices lacking mag/gravity sensor.
        else if (null != mSensorAcc)
        {
            float G = 9.81f;
            float k = 90 / G;
            //pitch = -accels[1] * k;
            yaw = mFilteredAccels[0] * k;
            //roll = accels[2] * k;

            pitch = (float)(-(Math.atan2(mFilteredAccels[1], mFilteredAccels[2]) * 180 / Math.PI) + 90.0f + PITCH_OFFSET_DEG);
            roll = (float)(Math.atan2(-mFilteredAccels[0], Math.sqrt(mFilteredAccels[1] * mFilteredAccels[1] + mFilteredAccels[2] * mFilteredAccels[2])) * 180 / Math.PI);

            mYaw.newValue(yaw);
            mRoll.newValue(roll);
            mPitch.newValue(pitch);

            mIsYawInRange = isInRange(0, getYaw(), mYawRangeDeg);
            mIsPitchInRange = isInRange(0, getPitch()+PITCH_OFFSET_DEG, mPitchRangeDeg);
            mIsDeviceUpright = (mIsYawInRange && mIsPitchInRange);
        }

        if (null != mListener)
        {
            mListener.sensorChanged();
            //Timber.e(String.format("P:%.5f R:%.5f Y:%.5f X:%.5f Y:%.5f Z:%.5f", getPitch(), getRoll(), getYaw(), getX(), getY(), getZ()));
        }

        if (mPreviousYawInRange != mIsYawInRange || mPreviousPitchInRange != mIsPitchInRange)
        {
            mPreviousYawInRange = mIsYawInRange;
            mPreviousPitchInRange = mIsPitchInRange;

            if (null != mListener)
            {
                mListener.deviceVerticalChanged();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    public boolean isDeviceUpright()
    {
        return mIsDeviceUpright;
    }

    public boolean isYawInRange()
    {
        return mIsYawInRange;
    }

    public boolean isPitchInRange()
    {
        return mIsPitchInRange;
    }

    public double getYaw()
    {
        return mYaw.average_value();
    }

    public double getRoll()
    {
        return mRoll.average_value();
    }

    public double getPitch()
    {
        return mPitch.average_value();
    }

    public double getX()
    {
        return mX.average_value();
    }

    public double getY()
    {
        return mY.average_value();
    }

    public double getZ()
    {
        return mZ.average_value();
    }

    public float getZAsScreen(float screenRange)
    {
        return (float) -adjustRangeToScreen(clamp(Math.round(mPitch.average_value()), -Z_RANGE, Z_RANGE), Z_RANGE, screenRange);
    }

    public long getTimeSinceLastSensorReading()
    {
        if (timeStartedReceivingBadReadings <= 0)
        {
            return 0;
        }
        else
        {
            return System.currentTimeMillis() - timeStartedReceivingBadReadings;
        }
    }

    public static float clamp(float val, float min, float max)
    {
        return Math.max(min, Math.min(max, val));
    }

    public static boolean isInRange(double test, double value, double range)
    {
        double abs = Math.abs(range);
        return (test >= value - abs && test <= value + abs);
    }

    public static double adjustRangeToScreen(double val, double range, double screenRange)
    {
        // range is += so we use the double of it.
        double halfScreen = screenRange / 2;
        return val / range * halfScreen;
    }
}
