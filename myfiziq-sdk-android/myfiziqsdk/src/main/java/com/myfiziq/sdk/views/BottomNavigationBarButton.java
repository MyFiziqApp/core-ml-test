package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;

import androidx.annotation.Nullable;

public class BottomNavigationBarButton extends LinearLayout
{
    private LinearLayout layout;
    private ImageView imageView;
    private TextView textView;


    public BottomNavigationBarButton(Context context)
    {
        super(context);
    }

    public BottomNavigationBarButton(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public BottomNavigationBarButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public BottomNavigationBarButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        inflate(getContext(), R.layout.view_bottom_navigation_bar_button, this);

        layout = findViewById(R.id.bottomNavigationBarLayout);
        imageView = findViewById(R.id.bottomNavigationBarImage);
        textView = findViewById(R.id.bottomNavigationBarText);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationBarButton);
        Drawable imageDrawable = typedArray.getDrawable(R.styleable.BottomNavigationBarButton_navigationImage);
        String textString = typedArray.getString(R.styleable.BottomNavigationBarButton_navigationText);

        if (null == imageDrawable)
        {
            imageView.setVisibility(GONE);
        }
        else
        {
            imageView.setVisibility(VISIBLE);
            imageView.setImageDrawable(imageDrawable);
        }


        if (null == textString)
        {
            textView.setVisibility(GONE);
        }
        else
        {
            textView.setVisibility(VISIBLE);
            textView.setText(textString);
        }

        typedArray.recycle();
    }
}
