package com.myfiziq.sdk.manager;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import androidx.annotation.Nullable;
import timber.log.Timber;


/**
 * Listens for file uploads.
 *
 * @hide
 */
// Package private. Should not be exposed to the customer app.
class MyFiziqS3TransferListener implements TransferListener
{
    interface OnCompleteListener
    {
        void onComplete();
    }

    interface OnErrorListener
    {
        void onError(@Nullable Exception e);
    }

    private OnCompleteListener mCompleteListener;
    private OnErrorListener mErrorListener;
    private int mTotalUploadCount = 0;
    private int mPendingUploads = 0;

    public MyFiziqS3TransferListener(int fileCount, OnCompleteListener completeListener, OnErrorListener errorListener)
    {
        mCompleteListener = completeListener;
        mErrorListener = errorListener;
        mTotalUploadCount = fileCount;
        mPendingUploads = fileCount;
    }

    @Override
    public void onStateChanged(int id, TransferState state)
    {
        Timber.d("S3TransferListener reported state: %s for Transfer ID: %d", state, id);

        if (state == TransferState.COMPLETED)
        {
            mPendingUploads--;
            if (mPendingUploads <= 0)
            {
                if (null != mCompleteListener)
                {
                    // Once all uploads have been completed and were successful, notify the listener.
                    mCompleteListener.onComplete();

                    // Ensure that we don't notify again for whatever reason
                    mCompleteListener = null;
                    mErrorListener = null;
                }
            }
        }
        else if (state == TransferState.CANCELED || state == TransferState.FAILED)
        {
            mPendingUploads--;
            if (null != mCompleteListener)
            {
                // Notify that an error has occurred regardless of how many uploads are still remaining.
                mErrorListener.onError(null);

                // Ensure that we don't notify again for whatever reason
                mCompleteListener = null;
                mErrorListener = null;
            }
        }
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal)
    {
        String loggingMessage;

        if (bytesCurrent == bytesTotal)
        {
            loggingMessage = String.format("S3 transfer completed - Transfer ID: %d.", id);
        }
        else
        {
            double transferPercentCompleted = ((double) bytesCurrent / (double) bytesTotal) * 100.0;
            loggingMessage = String.format("S3 transfer in progress - Transfer ID: %d. Transfered: %d bytes. Remaining: %d bytes. (%.0f%%)", id, bytesCurrent, bytesTotal, transferPercentCompleted);
        }

        Timber.d(loggingMessage);
    }

    @Override
    public void onError(int id, Exception ex)
    {
        Timber.e(ex, "S3 transfer error");

        mPendingUploads--;
        if (null != mCompleteListener)
        {
            // Notify that an error has occurred regardless of how many uploads are still remaining.
            mErrorListener.onError(ex);

            // Ensure that we don't notify again for whatever reason
            mCompleteListener = null;
            mErrorListener = null;
        }
    }
}