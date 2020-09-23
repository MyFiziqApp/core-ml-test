package com.myfiziq.sdk;

import android.content.Context;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.myfiziq.sdk.util.GlobalContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;

/**
 * Handles the downloading of avatar metadata and the processing of pending avatars.
 */
public class MyFiziqAvatarDownloadManager
{
    private final ArrayList<Callbacks> mCallbacks = new ArrayList<>();
    private Handler uiThreadHandle = new Handler(Looper.getMainLooper());

    @Nullable
    private CountDownTimer mTimer;

    private static MyFiziqAvatarDownloadManager INSTANCE;


    public static MyFiziqAvatarDownloadManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new MyFiziqAvatarDownloadManager();
        }

        return INSTANCE;
    }

    public interface Callbacks
    {
        /**
         * A callback that will be executed before downloading the latest avatars.
         */
        void refreshStart();

        /**
         * A callback that will be executed after downloading the latest avatars.
         */
        void refreshEnd(WorkInfo.State state);
    }

    private MyFiziqAvatarDownloadManager()
    {
        // Empty constructor for the singleton
    }

    /**
     * Starts periodically checking for new avatar metadata and for avatars that have finished processing remotely.
     * <p>
     * We will not get the latest avatars immediately, but rather after a waiting period has elapsed.
     */
    public void startCheckingForAvatars()
    {
        if (mTimer != null)
        {
            mTimer.cancel();
        }

        Context context = GlobalContext.getContext();
        Resources resources = context.getResources();
        long intervalInSeconds = resources.getInteger(R.integer.avatar_background_fetch_interval_seconds);

        mTimer = new CountDownTimer(TimeUnit.SECONDS.toMillis(intervalInSeconds), TimeUnit.SECONDS.toMillis(intervalInSeconds))
        {
            @Override
            public void onTick(long millisUntilFinished)
            {

            }

            @Override
            public void onFinish()
            {
                getAvatarsNow();
            }
        };

        mTimer.start();
    }

    /**
     * Stops periodically checking for avatars.
     * <p>
     * We can start checking again by calling {@link #startCheckingForAvatars()}.
     */
    public void stopCheckingForAvatars()
    {
        // Cancel and delete the timer since we don't want it to periodically check for avatars anymore.
        // If the app wants to start checking again we'll recreate it.
        if (mTimer != null)
        {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void getAvatarsNow(MyFiziqAvatarDownloadManager.Callbacks callbacks)
    {
        addListener(callbacks);
        getAvatarsNow();
    }

    /**
     * Gets the latest avatars immediately.
     */
    public void getAvatarsNow()
    {
        if (mTimer != null)
        {
            // Stop the timer which triggers getAvatarsNow() so we don't double up this operation
            mTimer.cancel();
        }

        // Notify all listeners that we're about to start the worker
        notifyRefreshStart();

        // Create the worker
        UUID workerId = AvatarDownloadWorker.createWorker();

        // Listen until the worker has completed and restart the timer.
        listenForCompletionAndRequeueAsync(workerId);
    }

    public List<WorkInfo> getRunningWorkers() throws ExecutionException, InterruptedException
    {
        return AvatarDownloadWorker.getAllWork().get();
    }

    public WorkInfo.State waitForWorkerCompletionSync(UUID uuid) throws ExecutionException, InterruptedException
    {
        LiveData<WorkInfo> liveData = WorkManager.getInstance().getWorkInfoByIdLiveData(uuid);

        CompletableFuture<WorkInfo.State> future = new CompletableFuture<>();

        Handler uiThread = new Handler(Looper.getMainLooper());
        Runnable runnable = () ->
        {
            // Create a 1 element array which becomes effectively "final" and thus allows us to access it inside the observer
            // The array is final but the elements are modifiable
            Observer<WorkInfo>[] observer = new Observer[1];
            observer[0] = workInfo ->
            {
                WorkInfo.State state = workInfo.getState();

                Timber.v("AvatarDownloadWorker state is currently %s", state);

                if (state.isFinished())
                {
                    // Once the worker has finished, stop listening to it
                    liveData.removeObserver(observer[0]);

                    future.complete(state);
                }
            };

            // Start listening to the worker for lifecycle changes
            liveData.observeForever(observer[0]);
        };

        // Ugh, we need to observe on the UI thread or else we get "IllegalStateException: Cannot invoke observeForever on a background thread"
        uiThread.post(runnable);

        // Wait here, on the BACKGROUND thread, until the worker has finished
        return future.get();
    }

    /**
     * Listens until the worker has completed and restarts the timer.
     *
     * @param workerId The Worker UUID to listen to.
     */
    private void listenForCompletionAndRequeueAsync(UUID workerId)
    {
        LiveData<WorkInfo> liveData = WorkManager.getInstance().getWorkInfoByIdLiveData(workerId);

        // Create a 1 element array which becomes effectively "final" and thus allows us to access it inside the observer
        // The array is final but the elements are modifiable
        Observer<WorkInfo>[] observer = new Observer[1];
        observer[0] = workInfo ->
        {
            Timber.v("AvatarDownloadWorker state is currently %s", workInfo.getState());

            if (workInfo.getState().isFinished())
            {
                // Once the worker has finished, stop listening to it
                liveData.removeObserver(observer[0]);

                // Notify everything listening to MyFiziqAvatarDownloadManager that the worker has finished
                notifyRefreshEnd(workInfo.getState());

                // If we're using a timer, restart it so we'll run the worker again in the future
                // If there's no timer, we're probably making a one-time request to download avatars
                if (mTimer != null)
                {
                    mTimer.start();
                }
            }
        };

        // Start listening to the worker for lifecycle changes
        liveData.observeForever(observer[0]);
    }

    /**
     * Notify all listeners that we're about to start downloading avatars.
     */
    private void notifyRefreshStart()
    {
        uiThreadHandle.post(() ->
        {
            synchronized (mCallbacks)
            {
                for (Callbacks listener : mCallbacks)
                {
                    if (null != listener)
                    {
                        listener.refreshStart();
                    }
                }
            }
        });
    }

    /**
     * Notify all listeners that we have finished downloading avatars.
     */
    private void notifyRefreshEnd(WorkInfo.State state)
    {
        uiThreadHandle.post(() ->
        {
            synchronized (mCallbacks)
            {
                Iterator<Callbacks> iterator = mCallbacks.iterator();

                while (iterator.hasNext())
                {
                    Callbacks callbacks = iterator.next();

                    if (null != callbacks)
                    {
                        callbacks.refreshEnd(state);
                        iterator.remove();
                    }
                }
            }
        });
    }

    /**
     * Determines if a specific callback is attached to this instance.
     *
     * @param callbacks The callback to check if it is attached to this instance.
     * @return Whether the callback is already attached to this instance.
     */
    private boolean hasListener(final Callbacks callbacks)
    {
        synchronized (mCallbacks)
        {
            for (Callbacks listenerRef : mCallbacks)
            {
                if (listenerRef == callbacks)
                    return true;
            }
        }

        return false;
    }

    /**
     * Adds callbacks to be executed before and after a download operation takes place.
     *
     * @param callbacks The callbacks to be added.
     */
    private void addListener(final Callbacks callbacks)
    {
        if (null != callbacks && !hasListener(callbacks))
        {
            synchronized (mCallbacks)
            {
                mCallbacks.add(callbacks);
            }
        }
    }

    /**
     * Removes callbacks to be executed before and after a download operation takes place.
     *
     * @param callbacks The callbacks to be removed.
     */
    private void remListener(final Callbacks callbacks)
    {
        if (null != callbacks)
        {
            synchronized (mCallbacks)
            {
                Iterator<Callbacks> iterator = mCallbacks.iterator();
                while (iterator.hasNext())
                {
                    Callbacks callbackRef = iterator.next();
                    if (callbackRef == callbacks)
                    {
                        iterator.remove();
                        return;
                    }
                }
            }
        }
    }
}
