package com.myfiziq.sdk.adapters;

import android.view.View;

/**
 * Defines the interface for a callback when a view is bound by the <code>RecyclerView</code>.
 */
public interface HolderViewBinder
{
    void bind(int holderId, int id, int position, View view);
}
