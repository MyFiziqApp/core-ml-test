package com.myfiziq.sdk.helpers;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provides various helper methods to perform operations Asynchronously
 * (such as disk and network calls) without blocking the UI thread.
 *
 * Once an operation has been performed, a callback can (optionally) be made with (optionally)
 * the result of the operation.
 */
public class AsyncHelper
{
    private static ExecutorService executor;

    private AsyncHelper()
    {
    }

    /**
     * Performs an operation in a separate thread.
     *
     * The result of the operation is discarded. There is no callback.
     *
     * @param operation The operation to perform.
     */
    public static void run(OperationVoid operation)
    {
        getSingletonExecutor().submit(operation::execute);
    }

    /**
     * Performs an operation in a separate thread and then executes the requested callback.
     *
     * The callback may optionally run on the UI thread or the same thread that the operation
     * was performed on.
     *
     * @param operation The operation to perform.
     * @param callback A callback that is executed after the operation is performed.
     * @param runCallbackOnUiThread Whether the callback should run on the UI thread.
     */
    public static void run(@NonNull OperationVoid operation, @Nullable CallbackVoid callback, boolean runCallbackOnUiThread)
    {
        getSingletonExecutor().submit(() -> {

            operation.execute();

            if (null != callback)
            {
                // Trigger the callback on the UI thread if we requested it
                if (runCallbackOnUiThread)
                {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(callback::execute);
                }
                else
                {
                    callback.execute();
                }
            }

        });
    }

    /**
     * Performs an operation in a separate thread and then executes the requested callback.
     *
     * The callback may optionally run on the UI thread or the same thread that the operation
     * was performed on.
     *
     * @param operation The operation to perform.
     * @param callback A callback that is executed after the operation is performed.
     *                 The result of the operation is passed into the callback.
     * @param runCallbackOnUiThread Whether the callback should run on the UI thread.
     */
    public static <T> void run(@NonNull Operation<T> operation, @Nullable Callback<T> callback, boolean runCallbackOnUiThread)
    {
        getSingletonExecutor().submit(() -> {

            T result = operation.execute();

            if (null != callback)
            {
                // Trigger the callback on the UI thread if we requested it
                if (runCallbackOnUiThread)
                {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> callback.execute(result));
                }
                else
                {
                    callback.execute(result);
                }
            }

        });
    }

    private static synchronized ExecutorService getSingletonExecutor()
    {
        if (null == executor)
        {
            executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        return executor;
    }


    /**
     * An interface to perform an operation with no input and no output.
     */
    public interface OperationVoid
    {
        /**
         * Executes an operation with no input and no output.
         */
        void execute();
    }

    /**
     * An interface to perform an operation no input and a generic output.
     * @param <T> The class of the output that will be returned.
     */
    public interface Operation<T>
    {
        /**
         * Executes an operation with no input and a generic output.
         */
        T execute();
    }

    /**
     * An interface to execute a callback after performing an operation with no input
     */
    public interface CallbackVoid
    {
        /**
         * Executes a callback after performing an operation with no input.
         */
        void execute();
    }

    /**
     * An interface to execute a generic callback after performing an operation.
     * @param <T> The class that will represent the input data.
     */
    public interface Callback<T>
    {
        /**
         * Executes a callback after performing an operation.
         * @param t The result of the work performed.
         */
        void execute(T t);
    }

    /**
     * An interface to perform an operation with a generic input and a generic output.
     * @param <T> The class that will represent the input data.
     * @param <B> The class that will represent the output data.
     */
    public interface CallbackOperation<T, B>
    {
        /**
         * Executes a generic callback after performing an operation with a generic input.
         * @param t The input data.
         * @return The data to be returned.
         */
        B execute(T t);
    }
}
