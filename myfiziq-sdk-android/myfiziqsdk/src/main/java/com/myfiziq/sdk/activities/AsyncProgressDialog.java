package com.myfiziq.sdk.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.myfiziq.sdk.R;

import java.lang.ref.WeakReference;

/**
 * AsyncProgressDialog combines a <code>ProgressDialog</code> with an <code>AsyncTask</code>.
 * - The Dialog is displayed while the task is running and dismissed when it is complete.
 */
public class AsyncProgressDialog extends ProgressDialog
{
    /**
     * Similar to <code>Runnable</code> but also has an <code>onComplete</code> callback that is
     * called on the UI thread.
     */
    public interface AsyncProgress
    {
        void run(AsyncProgressDialog dlg);
        void onError();
        void onComplete();
    }

    private Handler mHandler = null;
    private WeakReference<Activity> mActivity = null;
    private AsyncProgress mRunnable;
    private String mMessage = "";
    private boolean mVisible = true;

    private Runnable mUpdateMessage = () -> setMessage(mMessage);

    private AsyncProgressTask mTask = null;

    /**
     * Creates an <code>AsyncProgressDialog</code> with a message.
     * @param activity - Parent <code>Activity</code>
     * @param message - Message to display.
     * @param runnable - Task to run on the background.
     * @return <code>AsyncProgressDialog</code>
     */
    public static AsyncProgressDialog showProgress(Activity activity, String message, AsyncProgress runnable)
    {
        return showProgress(activity, message, true, false, runnable);
    }

    /**
     * Creates an <code>AsyncProgressDialog</code>.
     * @param activity - Parent <code>Activity</code>
     * @param message - Message to display.
     * @param bVisible - If false no dialog will be displayed.
     * @param bCanCancel - If false the dialog can not be dismissed by the user.
     * @param runnable - Task to run on the background.
     */
    public static AsyncProgressDialog showProgress(Activity activity, String message, boolean bVisible, boolean bCanCancel, AsyncProgress runnable)
    {
        AsyncProgressDialog dlg = new AsyncProgressDialog(activity, message, runnable);
        dlg.mActivity = new WeakReference<>(activity);
        dlg.mVisible = bVisible;
        dlg.setCancelable(bCanCancel);
        dlg.start();

        return dlg;
    }

    private AsyncProgressDialog(Context context, String message, AsyncProgress runnable)
    {
        super(context, R.style.MFAsyncProgressDialog);
        setIndeterminate(true);
        setProgressStyle(STYLE_SPINNER);

        mRunnable = runnable;
        mMessage = message;

        if (!TextUtils.isEmpty(mMessage))
            setMessage(mMessage);

        if (null != mRunnable)
            mTask = new AsyncProgressTask(this);
    }

    void start()
    {
        if (null != mTask)
            mTask.execute();
        else
            showSuper();
    }

    void showSuper()
    {
        super.show();
    }

    @Override
    public void show()
    {
        start();
    }

    /**
     * Update the message in the progress dialog.
     * @param message
     */
    public void updateMessage(String message)
    {
        mMessage = message;
        if (null == mHandler)
        {
            mHandler = new Handler(Looper.getMainLooper());
        }
        mHandler.post(mUpdateMessage);
    }

    @Override
    public void dismiss()
    {
        // Dismiss safely.
        try
        {
            if (null != mActivity && null != mActivity.get())
            {
                if (mActivity.get().isFinishing())
                {
                    return;
                }
            }

            if (isShowing())
            {
                super.dismiss();
            }

            mRunnable = null;
            mHandler = null;
            mActivity = null;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private static class AsyncProgressTask extends AsyncTask<Void, Void, Void>
    {
        WeakReference<AsyncProgressDialog> mDlg;

        AsyncProgressTask(AsyncProgressDialog dialog)
        {
            mDlg = new WeakReference<>(dialog);
        }

        @Override
        protected void onPreExecute()
        {
            AsyncProgressDialog dlg = mDlg.get();

            if (null != dlg && dlg.mVisible)
            {
                dlg.showSuper();
            }
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            AsyncProgressDialog dlg = mDlg.get();
            if (null != dlg && null != dlg.mRunnable)
            {
                try
                {
                    dlg.mRunnable.run(dlg);
                }
                catch(Exception e)
                {
                    cancel(true);
                }
            }
            return null;
        }

        @Override
        protected void onCancelled()
        {
            AsyncProgressDialog dlg = mDlg.get();
            if (null != dlg && null != dlg.mRunnable)
            {
                // Hold onto the runnable temporarily since it will be made null in the dismiss() method.
                AsyncProgress tempRunnable = dlg.mRunnable;

                dlg.dismiss();
                tempRunnable.onError();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            AsyncProgressDialog dlg = mDlg.get();
            if (null != dlg && null != dlg.mRunnable)
            {
                // Hold onto the runnable temporarily since it will be made null in the dismiss() method.
                AsyncProgress tempRunnable = dlg.mRunnable;

                dlg.dismiss();
                tempRunnable.onComplete();
            }
        }
    }
}
