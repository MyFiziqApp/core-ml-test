package com.myfiziq.sdk.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * @hide
 */

public abstract class SecondsTimer
{
    private static final int MSG = 1;

    public abstract void onSecondsTick(long millisUntilFinished);

    public abstract void onMillisecondsTick(long millisUntilFinished);

    public abstract void onFinish();

    public interface Listener
    {
        void onTimeReached(long timeRemaining);
    }

    int mSeconds = 0;
    long mTimeRemaining = 0;
    long mMillisInFuture;
    long mCountdownInterval;
    long mStopTimeInFuture;
    boolean mCancelled = false;
    Handler mHandler;

    LinkedList<ListenerInstance> mTimeListeners = new LinkedList<>();

    @SuppressLint("HandlerLeak")
    public SecondsTimer(long millisInFuture, long countDownInterval)
    {
        mMillisInFuture = millisInFuture;
        mCountdownInterval = countDownInterval;
        mHandler = new Handler()
        {

            @Override
            public void handleMessage(Message msg)
            {

                synchronized (SecondsTimer.this)
                {
                    final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();

                    if (millisLeft <= 0)
                    {
                        removeCallbacksAndMessages(null);
                        onFinish();
                    }
                    else if (millisLeft < mCountdownInterval)
                    {
                        // no tick, just delay until done
                        sendMessageDelayed(obtainMessage(MSG), millisLeft);
                    }
                    else
                    {
                        long lastTickStart = SystemClock.elapsedRealtime();
                        mTimeRemaining = millisLeft;

                        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(millisLeft);
                        if (seconds != mSeconds)
                        {
                            mSeconds = seconds;
                            onSecondsTick(millisLeft);
                        }

                        onMillisecondsTick(millisLeft);

                        checkListeners();

                        // take into account user's onTick taking time to execute
                        long delay = lastTickStart + mCountdownInterval - SystemClock.elapsedRealtime();

                        // special case: user's onTick took more than interval to
                        // complete, skip to next interval
                        while (delay < 0) delay += mCountdownInterval;

                        if (!mCancelled)
                        {
                            sendMessageDelayed(obtainMessage(MSG), delay);
                        }
                    }
                }
            }
        };
    }

    public void addSecondsListener(Listener listener, long atSeconds)
    {
        synchronized (mTimeListeners)
        {
            mTimeListeners.add(new ListenerInstance(listener, atSeconds, true));
        }
    }

    public synchronized SecondsTimer start()
    {
        if (mMillisInFuture <= 0)
        {
            onFinish();
            return this;
        }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        mCancelled = false;
        return this;
    }

    public void cancel()
    {
        mHandler.removeMessages(MSG);
        mCancelled = true;
    }

    void checkListeners()
    {
        Iterator<ListenerInstance> iterator = mTimeListeners.iterator();
        while (iterator.hasNext())
        {
            ListenerInstance i = iterator.next();
            if (i.mBIsSeconds)
            {
                if (mSeconds >= i.mMSeconds)
                {
                    i.mListener.onTimeReached(mTimeRemaining);
                    iterator.remove();
                }
            }
            else
            {
                if (mMillisInFuture - mTimeRemaining >= i.mMSeconds)
                {
                    i.mListener.onTimeReached(mTimeRemaining);
                    iterator.remove();
                }
            }
        }
    }

    public int getSeconds()
    {
        return mSeconds;
    }

    public float getProgress()
    {
        return (float) mTimeRemaining / mMillisInFuture;
    }

    public void reset()
    {
        cancel();
        mSeconds = 0;
        mTimeRemaining = mMillisInFuture;
    }

    class ListenerInstance
    {
        Listener mListener;
        long mMSeconds;
        boolean mBIsSeconds;

        public ListenerInstance(Listener listener, long millisecs, boolean bIsSeconds)
        {
            mListener = listener;
            mMSeconds = millisecs;
            mBIsSeconds = bIsSeconds;
        }
    }
}
