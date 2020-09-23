package com.myfiziq.sdk.adapters;

import android.view.View;

import java.util.List;

import androidx.annotation.Keep;

/**
 * Callback interface for a <code>RecyclerManager</code>
 */
@Keep
public interface RecyclerManagerInterface
{
    /**
     * Create a list of item selection listeners for a <code>RecyclerView</code> item.
     * @return list of item selection listeners.
     */
    List<View.OnClickListener> getItemSelectListeners();
}
