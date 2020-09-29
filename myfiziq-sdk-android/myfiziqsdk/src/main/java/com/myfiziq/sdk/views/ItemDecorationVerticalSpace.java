package com.myfiziq.sdk.views;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @hide
 */
public class ItemDecorationVerticalSpace extends RecyclerView.ItemDecoration
{
    private final int mVerticalSpaceHeight;

    public ItemDecorationVerticalSpace(int mVerticalSpaceHeight)
    {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state)
    {
        outRect.bottom = mVerticalSpaceHeight;
    }
}