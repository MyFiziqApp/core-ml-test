package com.myfiziq.sdk.views;

import android.os.Handler;

import com.myfiziq.sdk.enums.AvatarRotationSpeed;

public class AvatarRotationService
{
    private AvatarLayout avatarLayout;
    private boolean isRotating = false;
    private Runnable currentRotationRunnable;
    private Handler rotationHandler = new Handler();

    /**
     * The desired number of frames per second to render when rotating the avatar.
     */
    private final int DESIRED_FPS = 30;

    /**
     * The number of milliseconds to wait between rendering frames.
     */
    private final int INTERVAL_BETWEEN_FRAMES = 1000 / DESIRED_FPS;


    public AvatarRotationService(AvatarLayout layout)
    {
        avatarLayout = layout;
    }

    /**
     * Whether the avatar is currently rotating.
     */
    public boolean isRotating()
    {
        return isRotating;
    }

    /**
     * Starts rotating the avatar.
     */
    public void startRotating()
    {
        if (isRotating)
        {
            return;
        }

        currentRotationRunnable = () ->
        {
            AvatarRotationSpeed rotationSpeed = AvatarRotationSpeed.SLOW;
            float pointsToMove = rotationSpeed.getDegreesToMovePerFrame(DESIRED_FPS);

            avatarLayout.rotate(pointsToMove, 0, 0);
            rotationHandler.postDelayed(currentRotationRunnable, INTERVAL_BETWEEN_FRAMES);
        };

        rotationHandler.postDelayed(currentRotationRunnable, INTERVAL_BETWEEN_FRAMES);

        isRotating = true;
    }

    /**
     * Stops rotating the avatar.
     */
    public void pauseRotating()
    {
        if (!isRotating)
        {
            return;
        }

        if (currentRotationRunnable != null)
        {
            rotationHandler.removeCallbacks(currentRotationRunnable);
            currentRotationRunnable = null;
        }

        isRotating = false;
    }

    /**
     * Resets the rotation of the avatar back to its original location.
     */
    public void resetRotation()
    {
        if (isRotating)
        {
            pauseRotating();
        }

        final float desiredX = getNearestCenterXValue();

        currentRotationRunnable = () ->
        {
            AvatarRotationSpeed rotationSpeed = AvatarRotationSpeed.VERY_FAST;
            float pointsToMove = rotationSpeed.getDegreesToMovePerFrame(DESIRED_FPS);

            if (null != avatarLayout.getAngle())
            {
                float currentX = avatarLayout.getAngle().mX;

                if (currentX > desiredX)
                {
                    pointsToMove *= -1;
                }

                // If we still need to rotate after we do now, keep rotating.
                if ((currentX > desiredX && (currentX + pointsToMove > desiredX))
                        || (currentX < desiredX && (currentX + pointsToMove < desiredX)))
                {
                    avatarLayout.rotate(pointsToMove, 0, 0);
                    rotationHandler.postDelayed(currentRotationRunnable, INTERVAL_BETWEEN_FRAMES);
                }
                // Else, we are at the last frame that will rotate the avatar.
                // Get only the exact amount we need to rotate and do not rotate anymore after this.
                else
                {
                    boolean rotationWasPositive = pointsToMove > 0;

                    if (currentX > desiredX)
                    {
                        pointsToMove = currentX - desiredX;
                    }
                    else
                    {
                        pointsToMove = desiredX - currentX;
                    }

                    if ((rotationWasPositive && pointsToMove < 0) || !rotationWasPositive && pointsToMove > 0)
                    {
                        pointsToMove *= -1;
                    }

                    avatarLayout.rotate(pointsToMove, 0, 0);
                }
            }
        };

        rotationHandler.postDelayed(currentRotationRunnable, INTERVAL_BETWEEN_FRAMES);
    }

    /**
     * Calculates the nearest center X value.
     *
     * If the avatar spins fully left once, the X value will be 360.
     * If the avatar spins fully left twice, the X value will be 720.
     * If the avatar spins fully right once, the X value will be -360.
     * If the avatar spins fully right twice, the X value will be -720.
     *
     * If the user does not do a full spin, we need to find the nearest center value if they had done a full spin.
     */
    private float getNearestCenterXValue()
    {
        float currentX = 0.0f;

        if (null != avatarLayout.getAngle())
        {
            currentX = avatarLayout.getAngle().mX;
        }

        // If the avatar spins once, the X value will be 360.
        // If the avatar spins twice, the X value will be 720.
        // We need to bring the value down so it's between 0 and 360 and we can perform calculations on it.
        float scaledDownX;

        if (currentX > 0)
        {
            scaledDownX = currentX % 360;
        }
        else
        {
            scaledDownX = currentX % -360;
        }

        // The X value we're looking to rotate to so that the avatar will be back in the center again.
        final float desiredX;

        if (scaledDownX >= 0 && scaledDownX <= 180)
        {
            // The user originally rotated left.
            // Rotate left to be bring the avatar back to the center
            desiredX = currentX - scaledDownX;
        }
        else if (scaledDownX >= 180 && scaledDownX <= 360)
        {
            // The user originally rotated left.
            // Rotate right to bring the avatar back to the center
            desiredX = currentX + (360 - scaledDownX);
        }
        else if (scaledDownX >= -180 && scaledDownX <= 0)
        {
            // The user originally rotated right.
            // Rotate left to be bring the avatar back to the center
            desiredX = currentX - scaledDownX;
        }
        else if (scaledDownX >= -360 && scaledDownX <= -180)
        {
            // The user originally rotated right.
            // Rotate right to bring the avatar back to the center
            desiredX = currentX + (-360 - scaledDownX);
        }
        else
        {
            // Some logic error occurred. Go right back to the start.
            desiredX = 0;
        }

        return desiredX;
    }
}
