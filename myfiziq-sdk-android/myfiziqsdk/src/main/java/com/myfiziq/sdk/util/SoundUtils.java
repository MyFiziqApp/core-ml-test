package com.myfiziq.sdk.util;

import android.media.AudioManager;
import android.media.SoundPool;

import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.R;

/**
 * @hide
 */

public class SoundUtils
{
    public enum Sounds
    {
        SOUND_SHUTTER,
        SOUND_ALERT1,
        SOUND_BUTTON7,
        SOUND_COMPLETE1,
        SOUND_COMPLETE2,
        SOUND_ERROR1,
        SOUND_SUCCESS1,
        SOUND_SUCCESS3
    }

    private static SoundUtils mThis;

    SoundPool mSoundPool = new SoundPool(Sounds.values().length, AudioManager.STREAM_ALARM, 0);
    int[] mSoundIds = new int[Sounds.values().length];

    private SoundUtils()
    {
        mSoundIds[Sounds.SOUND_SHUTTER.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.camera, 0);
        mSoundIds[Sounds.SOUND_ALERT1.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.alert1, 0);
        mSoundIds[Sounds.SOUND_BUTTON7.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.button7, 0);
        mSoundIds[Sounds.SOUND_COMPLETE1.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.complete1, 0);
        mSoundIds[Sounds.SOUND_COMPLETE2.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.complete2, 0);
        mSoundIds[Sounds.SOUND_ERROR1.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.error1, 0);
        mSoundIds[Sounds.SOUND_SUCCESS1.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.success1, 0);
        mSoundIds[Sounds.SOUND_SUCCESS3.ordinal()] = mSoundPool.load(GlobalContext.getContext(), R.raw.success3, 0);

        //MediaActionSound mSound = new MediaActionSound();
        //mSound.play(MediaActionSound.SHUTTER_CLICK);
    }

    public static synchronized SoundUtils getInstance()
    {
        if (null == mThis)
        {
            mThis = new SoundUtils();
        }
        return mThis;
    }

    public void play(Sounds sound)
    {
        if (!BuildConfig.DEBUG)
            mSoundPool.play(mSoundIds[sound.ordinal()], 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
