package com.myfiziq.sdk.views;


import android.view.MotionEvent;

/**
 * @hide
 */

public abstract class OnTouchHandler
{
    public enum ScrollState
    {
        None,
        Vertical,
        Horizontal
    }

    public abstract void onTranslate(float x, float y);
    public abstract void onRotate(float x, float y);
    public abstract void onScale(float scale);
    public abstract void onClick();
    public abstract void setDisableScrollView(boolean bDisabled);

    // rotation
    private final float TOUCH_SCALE_FACTOR_X = 180.0f / 360;
    private final float TOUCH_SCALE_FACTOR_Y = 180.0f / 360;
    private float mPreviousX;
    private float mPreviousY;

    /**
     * The X distance the cursor has moved since we touched the view.
     */
    private float distanceTravelledX;

    /**
     * The Y distance the cursor has moved since we touched the view.
     */
    private float distanceTravelledY;


    private float oldDistance;

    // touch events
    private final int NONE = 0;
    private final int DRAG = 1;
    private final int ZOOM = 2;

    private boolean mDistanceReady = false;

    int mode = NONE;

    long mTapTimer = System.currentTimeMillis();

    private float[] startMidPoint;

    private ScrollState mScrollState = ScrollState.None;

    public boolean onTouchEvent(MotionEvent e)
    {
        boolean bHandled = true;
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction() & e.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:            // one touch: drag
                mDistanceReady = false;
                mode = DRAG;
                mTapTimer = System.currentTimeMillis();
                mScrollState = ScrollState.None;
                setDisableScrollView(true);
                break;

            case MotionEvent.ACTION_UP:        // no mode
            case MotionEvent.ACTION_POINTER_UP:        // no mode
                mode = NONE;
                mScrollState = ScrollState.None;
                setDisableScrollView(false);
                if (System.currentTimeMillis() - mTapTimer < 333)
                {
                    // Tap event...
                    bHandled = false;
                    onClick();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mDistanceReady = false;
                startMidPoint = getMiddlePoint(e);
                oldDistance = (float) Math.sqrt((e.getX(0) - e.getX(1)) * (e.getX(0) - e.getX(1)) + (e.getY(0) - e.getY(1)) * (e.getY(0) - e.getY(1)));
                if (oldDistance > 10f)
                {
                    mode = ZOOM;
                }
                setDisableScrollView(true);
                break;

            case MotionEvent.ACTION_MOVE:                        // rotation
                if (!mDistanceReady)
                {
                    mDistanceReady = true;
                    mPreviousX = x;
                    mPreviousY = y;
                    distanceTravelledX = 0;
                    distanceTravelledY = 0;
                }

                if (mode == DRAG)
                {
                    float deltaX = x - mPreviousX;
                    float deltaY = y - mPreviousY;
                    distanceTravelledX += deltaX;
                    distanceTravelledY += deltaY;

                    switch (mScrollState)
                    {
                        case None:
                            float distanceXTravelled = Math.abs(distanceTravelledX);
                            float distanceYTravelled = Math.abs(distanceTravelledY);

                            if (distanceXTravelled > 150)
                            {
                                mScrollState = ScrollState.Horizontal;
                                //Timber.d("Scroll Lock Horizontal");
                            }
                            else if (distanceYTravelled > 150)
                            {
                                mScrollState = ScrollState.Vertical;
                                //Timber.d("Scroll Lock Vertical");
                            }

                            setDisableScrollView(mScrollState != ScrollState.Vertical);
                            // FALLTHROUGH... (no break;)

                        case Horizontal:
                            onRotate(deltaX * TOUCH_SCALE_FACTOR_X, -(deltaY * TOUCH_SCALE_FACTOR_Y));
                            break;

                        case Vertical:
                            // DO NOTHING.
                            break;
                    }
                }
                else if (mode == ZOOM)
                {
                    //Check movement
                    float[] nowMidPoint = getMiddlePoint(e);
                    float moveDist = spacing(nowMidPoint, startMidPoint);
                    if (moveDist > 10f)
                    {
                        onTranslate(((nowMidPoint[0] - startMidPoint[0]) / 300), -((nowMidPoint[1] - startMidPoint[1]) / 300));
                        startMidPoint = nowMidPoint;
                    }

                    float newDistance;
                    newDistance = (float) Math.sqrt((e.getX(0) - e.getX(1)) * (e.getX(0) - e.getX(1)) + (e.getY(0) - e.getY(1)) * (e.getY(0) - e.getY(1)));
                    if (Math.abs(newDistance - oldDistance) > 50f)
                    {
                        onScale(newDistance / oldDistance);
                        oldDistance = newDistance;
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDistanceReady = false;
                //mScrollState = ScrollState.None;
                //setDisableScrollView(false);
                break;

            case MotionEvent.ACTION_OUTSIDE:
                mDistanceReady = false;
                mScrollState = ScrollState.None;
                setDisableScrollView(false);
                break;
        }
        mPreviousX = x;
        mPreviousY = y;
        return bHandled;
    }

    /**
     * Gets the X distance that the cursor is from the original starting point when the user touched the view.
     * The distance is relative to the initial starting point and is not the total distance.
     */
    public float getXDistanceTravelled()
    {
        return distanceTravelledX;
    }

    /**
     * Gets the X distance that the cursor is from the original starting point when the user touched the view.
     * The distance is relative to the initial starting point and is not the total distance.
     */
    public float getYDistanceTravelled()
    {
        return distanceTravelledY;
    }

    private float[] getMiddlePoint(MotionEvent event)
    {
        float x = Math.abs((event.getX(1) - event.getX(0)) / 2 + event.getX(0));
        float y = Math.abs((event.getY(1) - event.getY(0)) / 2 + event.getY(0));
        return new float[]{x, y};
    }

    private float spacing(float[] p1, float[] p2)
    {
        float x = p1[0] - p2[0];
        float y = p1[1] - p2[1];
        return (float) Math.sqrt(x * x + y * y);
    }
}
