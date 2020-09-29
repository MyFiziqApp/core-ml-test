package com.myfiziq.sdk.enums;


public enum AvatarRotationSpeed
{
    SLOW(0.97f), VERY_FAST(15);

    /**
     * The number of degrees to move per second when using the DEFAULT_FPS.
     */
    private float degreesPerSecond;

    /**
     * The number of frames per second that was used to baseline the degrees per second rotation speed.
     */
    private static final int DEFAULT_FPS = 30;


    AvatarRotationSpeed(float degreesPerSecond)
    {
        this.degreesPerSecond = degreesPerSecond;
    }

    /**
     * Gets the number of degrees that the avatar should rotate by per frame based on the
     * number of frames per second we're looking to render on the screen.
     *
     * @param desiredFps The frames per second that the rotation should be animated by.
     * @return The number of degrees the avatar should rotate by per frame.
     */
    public float getDegreesToMovePerFrame(int desiredFps)
    {
        float fpsMultiplier = DEFAULT_FPS / desiredFps;
        return fpsMultiplier * degreesPerSecond;
    }
}
