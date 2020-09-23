package com.myfiziq.sdk.adapters;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.widget.Filter;

import androidx.annotation.Keep;
import timber.log.Timber;

/**
 * @hide
 */
@Keep
class CursorFilter extends Filter
{
    private final CursorFilterClient mClient;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    interface CursorFilterClient
    {
        CharSequence convertToString(Cursor cursor);

        Cursor runQueryOnBackgroundThread(CharSequence constraint);

        Cursor getCursor();

        void changeCursor(Cursor cursor);
    }

    public CursorFilter(CursorFilterClient client)
    {
        mClient = client;
    }

    @Override
    public CharSequence convertResultToString(Object resultValue)
    {
        return mClient.convertToString((Cursor) resultValue);
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint)
    {
        FilterResults results = new FilterResults();

        try
        {
            Cursor cursor = mClient.runQueryOnBackgroundThread(constraint);

            if (cursor != null)
            {
                results.count = cursor.getCount();
                results.values = cursor;
            }
            else
            {
                results.count = 0;
                results.values = null;
            }
        }
        catch (Throwable t)
        {
            Timber.e(t, "performFiltering error");
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, final FilterResults results)
    {
        Cursor oldCursor = mClient.getCursor();

        if (results.values != null && results.values != oldCursor)
        {
            if (null != oldCursor)
                oldCursor.close();

            mHandler.post(() -> mClient.changeCursor((Cursor) results.values));
        }
    }
}
