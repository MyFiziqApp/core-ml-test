package com.myfiziq.sdk.db;

import android.content.res.Resources;

import com.myfiziq.sdk.R;

/**
 * The current status of Avatar processing.
 */
public enum Status
{
    /**
     * The avatar has been captured.
     */
    Captured,

    /**
     * The avatar has been captured and is currently waiting for processing.
     */
    Pending,

    /**
     * The avatar is currently being processed locally.
     */
    Processing,

    /**
     * The avatar is currently being uploaded or processed remotely.
     */
    Uploading,

    /**
     * The avatar has finished being processed. Its results are now available to use.
     */
    Completed,

    /**
     * The avatar has failed to be processed.
     */
    FailedGeneral,
    FailedTimeout,
    FailedNoInternet,
    FailedServerErr;


    public static Status fromInt(int val)
    {
        Status status = Status.FailedGeneral;
        if (val >= 0 && val < Status.values().length)
            status = Status.values()[val];

        return status;
    }

    public String getDescription(Resources resources)
    {
        switch (this)
        {
            //TODO: use resource strings...
            case Captured:
            case Pending:
                return resources.getString(R.string.processing_session);

            case Processing:
            case Uploading:
            case Completed:
                return resources.getString(R.string.processing_session);

            case FailedGeneral:
                return resources.getString(R.string.error_processing);

            case FailedNoInternet:
                return resources.getString(R.string.error_processing_no_internet);

            case FailedServerErr:
                return resources.getString(R.string.error_processing_server_err);

            case FailedTimeout:
                return resources.getString(R.string.error_processing_timeout);
        }
        return "";
    }
}
