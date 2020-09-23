package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.ModelAvatar;

import androidx.annotation.Nullable;

public class ItemViewAvatarCompleted extends LinearLayout
{
    private AvatarImageView mAvatar;
    private TextView mTitle;


    public ItemViewAvatarCompleted(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatarCompleted(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatarCompleted(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        LayoutInflater.from(context).inflate(getLayout(), this, true);

        mAvatar = findViewById(R.id.avatar);
        mTitle = findViewById(R.id.title);
    }

    public int getLayout()
    {
        return R.layout.view_avatar_item_completed;
    }

    public void setAvatar(ModelAvatar avatar)
    {
        mAvatar.render(avatar);
    }

    public void setTitle(String title)
    {
        mTitle.setText(title);
    }
}
