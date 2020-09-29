package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.myfiziq.sdk.R;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

/**
 * @hide
 */
public class MYQToolbar extends Toolbar implements View.OnClickListener
{
    Context context;

    @Nullable
    //@BindView(R.id.toolbarImgLeft)
    ImageView imgLeft;

    @Nullable
    //@BindView(R.id.toolbarTitle)
    TextView toolbarTitle;

    //@BindView(R.id.toolbarImgRight)
    ImageView imgRight;

    OnClickListener mClickListener;

    public MYQToolbar(Context context)
    {
        super(context);
        this.context = context;
        init();
    }

    public MYQToolbar(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        init();
    }

    public MYQToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init()
    {
        LayoutInflater.from(context).inflate(R.layout.view_toolbar, this, true);
        imgLeft = findViewById(R.id.toolbarImgLeft);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        imgRight = findViewById(R.id.toolbarImgRight);
        imgLeft.setOnClickListener(this);
        imgRight.setOnClickListener(this);
        toolbarTitle.setOnClickListener(this);
    }

    public void setLeftIcon(int resId)
    {
        if (0 != resId)
            imgLeft.setImageDrawable(getResources().getDrawable(resId));
        else
            imgLeft.setImageDrawable(null);
    }

    public void setRightIcon(int resId)
    {
        if (0 != resId)
            imgRight.setImageDrawable(getResources().getDrawable(resId));
        else
            imgRight.setImageDrawable(null);
    }

    public void setTitle(String title)
    {
        toolbarTitle.setText(title);
    }

    public void setOnClickListener(OnClickListener listener)
    {
        mClickListener = listener;
    }

    @Override
    public void onClick(View v)
    {
        if (null != mClickListener)
        {
            mClickListener.onClick(this, v);
        }
    }

    public interface OnClickListener
    {
        void onClick(MYQToolbar toolbar, View v);
    }
}