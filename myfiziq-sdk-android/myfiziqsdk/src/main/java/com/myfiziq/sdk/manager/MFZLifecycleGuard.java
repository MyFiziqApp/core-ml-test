package com.myfiziq.sdk.manager;

import com.myfiziq.sdk.lifecycle.MFZLifecycleException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Contains various guards to assert that the MyFiziq SDK is in the correct lifecycle state to
 * perform an operation.
 */
public class MFZLifecycleGuard
{
    private boolean configurationAssigned = false;
    private boolean sdkInitialised = false;
    private boolean signedIn = false;

    private ReentrantLock signInLock = new ReentrantLock();
    private ReentrantLock sdkInitLock = new ReentrantLock();


    // Constructor is package-private
    // Customer apps shouldn't be able to create their own lifecycle
    MFZLifecycleGuard()
    {
        // This class CANNOT be a singleton
        // If MyFiziq.java or MyFiziqSdkManager.java gets garbage collected but this class doesn't,
        // this class will have the wrong state
    }

    /**
     * Asserts that the MyFiziq SDK is in a state that is ready to perform operations.
     *
     * @throws MFZLifecycleException If we are not ready to perform operations.
     */
    public void assertReady()
    {
        assertConfigurationAssigned();
        assertSdkInitialised();
        assertSignedIn();
    }

    /**
     * Checks that the MyFiziq SDK is in a state that is ready to perform operations.
     */
    public boolean isReady()
    {
        return (configurationAssigned && sdkInitialised && signedIn);
    }

    /**
     * Checks if we're currently signed in.
     */
    public boolean isSignedIn()
    {
        return signedIn;
    }

    /**
     * Asserts that configuration has been assigned to the MyFiziq SDK.
     *
     * @throws MFZLifecycleException If the configuration has not been assigned.
     */
    public void assertConfigurationAssigned()
    {
        if (!configurationAssigned)
        {
            throw new MFZLifecycleException("Configuration is not assigned. Please assign using MyFiziqSdkManager.assignConfiguration()");
        }
    }

    /**
     * Asserts that the SDK has been initialised.
     *
     * @throws MFZLifecycleException If the SDK is not currently initialised.
     */
    public void assertSdkInitialised()
    {
        if (!sdkInitialised)
        {
            throw new MFZLifecycleException("SDK is not initialised. Please initialise using MyFiziqSdkManager.initialiseSdk()");
        }
    }

    /**
     * Asserts that we're currently signed in.
     *
     * If we are currently signing in this user, this method will wait until the sign in process has
     * been completed either successfully or unsuccessfully.
     *
     * @throws MFZLifecycleException If the user is not currently signed in.
     */
    public void assertSignedIn()
    {
        signInLock.lock();

        // Note, we don't use the AWS libraries for determining if we're signed in since they often
        // make HTTP calls to AWS which would make this method very slow.
        if (!signedIn)
        {
            signInLock.unlock();
            throw new MFZLifecycleException("User is not signed in. Please sign in using MyFiziqSdkManager.signIn()");
        }

        signInLock.unlock();
    }

    /**
     * Indicate that we're currently signing in and that any methods dependant on us being signed
     * in should wait until we have signed in.
     */
    public void lockForSignIn()
    {
        signInLock.lock();
    }

    /**
     * Indicate that we've finished signing in and that any methods dependant on us being signed in
     * can now be executed.
     */
    public void unlockSignIn()
    {
        signInLock.unlock();
    }

    /**
     * Indicate that we're currently initialising the SDK and that any methods dependant on the SDK
     * being initialised should wait until we have finished.
     */
    public void lockForSdkInit()
    {
        sdkInitLock.lock();
    }

    /**
     * Indicate that we've finished the SDK initialisation and that any methods dependant on the SDK being initialised
     * can now be executed.
     */
    public void unlockForSdkInit()
    {
        sdkInitLock.unlock();
    }

    /**
     * If we are currently signing in, this method will suspend the thread until the signin process
     * has been completed.
     */
    public void waitUntilSignedIn()
    {
        signInLock.lock();
        signInLock.unlock();
    }

    /**
     * If we are currently signing in, this method will suspend the thread until the signin process
     * has been completed.
     */
    public void waitUntilSdkInit()
    {
        sdkInitLock.lock();
        sdkInitLock.unlock();
    }

    void setConfigurationAssigned(boolean isConfigurationAssigned)
    {
        this.configurationAssigned = isConfigurationAssigned;
    }

    void setSdkInitialised(boolean isSdkInitialised)
    {
        this.sdkInitialised = isSdkInitialised;
    }

    void setSignedIn(boolean signedIn)
    {
        this.signedIn = signedIn;
    }
}
