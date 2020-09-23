package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.myfiziq.sdk.R;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ItemViewAvatarLoading extends ConstraintLayout
{
    public ItemViewAvatarLoading(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatarLoading(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatarLoading(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        LayoutInflater.from(context).inflate(getLayout(), this, true);
    }

    public int getLayout()
    {
        return R.layout.view_avatar_item_loading;
    }
}
