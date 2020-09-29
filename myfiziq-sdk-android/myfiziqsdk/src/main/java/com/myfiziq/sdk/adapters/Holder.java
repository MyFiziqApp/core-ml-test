package com.myfiziq.sdk.adapters;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * A ViewHolder wrapper that defines a type.
 */
class Holder extends RecyclerView.ViewHolder
{
    enum Type
    {
        HEADER,
        MODEL
    }

    public final Type mType;

    public Holder(Type type, View view)
    {
        super(view);
        mType = type;
    }
}
