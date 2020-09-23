package com.myfiziq.myfiziq_android.views;

import android.text.style.ClickableSpan;
import android.view.View;

public class ClickSpan extends ClickableSpan
{
    private String url;
    private OnClickListener listener;

    public ClickSpan(String url, OnClickListener listener)
    {
        this.url = url;
        this.listener = listener;
    }

    @Override
    public void onClick(View widget)
    {
        if (listener != null) listener.onClick(url);
    }

    public interface OnClickListener
    {
        void onClick(String url);
    }
}