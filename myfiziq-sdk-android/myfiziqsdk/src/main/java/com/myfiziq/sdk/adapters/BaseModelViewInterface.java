package com.myfiziq.sdk.adapters;

import android.view.View;

import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.util.List;

/**
 * Template class that defines the interface for a <code>RecyclerView</code> <code>View</code> item.
 * <br>
 * Custom views that implement this interface can be bound to a <code>Model</code> by a <code>RecyclerCursorAdapter</code>
 */
public interface BaseModelViewInterface<T extends Model>
{
    /**
     * Callback to bind (render) the view based on the passed in model.
     * @param holder - The <code>CursorHolder</code> that is binding this view.
     * @param fragment
     * @param model - The <code>Model</code> instance that is being bound.
     */
    void bind(CursorHolder holder, FragmentInterface fragment, T model);

    /**
     * Callback to get the resource id that will be inflated for this view.
     * @return - layout resource id.
     */
    int getLayout();

    /**
     * Callback to set <code>View</code> <code>onClick</code> listener(s) for the view/view children.
     * @param listener - A list of listeners.
     */
    void setViewOnClickListeners(List<View.OnClickListener> listener);

    /**
     * Callback to set the model that is associated with this view.
     * @param Model - The model currently bound to this view.
     */
    void setModelTag(T Model);

    /**
     * Callback to set the holder that is associated with this view.
     * @param holder - The holder currently bound to this view.
     */
    void setHolderTag(CursorHolder holder);
}
