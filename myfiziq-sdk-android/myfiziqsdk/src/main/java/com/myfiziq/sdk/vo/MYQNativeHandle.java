package com.myfiziq.sdk.vo;

import com.myfiziq.sdk.helpers.AsyncHelper;

import org.sqlite.database.sqlite.CloseGuard;


/**
 * Encapsulates a reference to a native memory address (i.e. a C++ pointer).
 */
public class MYQNativeHandle
{
    private final CloseGuard guard = CloseGuard.get();

    private long memoryAddress;
    private AsyncHelper.Callback<Long> destructor;


    /**
     * Constructs a reference to a native memory address.
     *
     * @param memoryAddress A reference to a native piece of a memory (i.e. a C++ pointer).
     * @param destructor Contains a JNI method that will release the native piece of memory back to the operation system.
     */
    public MYQNativeHandle(long memoryAddress, AsyncHelper.Callback<Long> destructor)
    {
        this.memoryAddress = memoryAddress;
        this.destructor = destructor;

        guard.open("MYQNativeHandle");
    }

    /**
     * Returns a reference to a native piece of a memory (i.e. a C++ pointer).
     */
    public long getMemoryAddress()
    {
        return memoryAddress;
    }

    /**
     * Determines if this object has been initialised.
     */
    public boolean isInitialised()
    {
        return (memoryAddress > 0);
    }

    /**
     * Releases the native piece of memory back to the operation system.
     */
    public void close()
    {
        if (isInitialised())
        {
            destructor.execute(memoryAddress);

            memoryAddress = 0;
            guard.close();
        }
    }

    /**
     * Called by the garbage collector when garbage collection
     * determines that there are no more references to this object.
     *
     * If this object hasn't been closed yet, a {@link CloseGuard}
     * warning will be printed to Logcat.
     */
    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            if (guard != null && isInitialised())
            {
                guard.warnIfOpen();
            }

            close();
        }
        finally
        {
            super.finalize();
        }
    }
}
