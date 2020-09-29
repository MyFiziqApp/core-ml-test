package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;

public class AvatarDataItem extends LinearLayout
{

    private TextView nameText;
    private TextView valueText;
    private View dividerTop;
    private View dividerBottom;


    public AvatarDataItem(Context context)
    {
        super(context);
        init(null, 0);
    }

    public AvatarDataItem(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    public AvatarDataItem(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    public AvatarDataItem(Context context, AttributeSet attrs, int defStyle, int defStyleRes)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    private void init(AttributeSet attrs, int defStyle)
    {
        // Load attributes
        final TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.AvatarDataItem, defStyle, 0);

        int layoutValue = attributes.getResourceId(R.styleable.AvatarDataItem_layout, -1);
        String nameTextValue = attributes.getString(R.styleable.AvatarDataItem_nameText);
        String valueTextValue = attributes.getString(R.styleable.AvatarDataItem_valueText);
        boolean dividerTopValue = attributes.getBoolean(R.styleable.AvatarDataItem_dividerTop, false);
        boolean dividerBottomValue = attributes.getBoolean(R.styleable.AvatarDataItem_dividerBottom, false);

        if (layoutValue == -1)
        {
            // Default layout is...
            layoutValue = R.layout.view_avatar_data_item;
        }

        inflate(getContext(), layoutValue, this);

        nameText = findViewById(R.id.avatarDataItemName);
        valueText = findViewById(R.id.avatarDataItemValue);
        dividerTop = findViewById(R.id.dividerTop);
        dividerBottom = findViewById(R.id.dividerBottom);



        if (null != nameTextValue)
        {
            setNameText(nameTextValue);
        }

        if (null != valueTextValue)
        {
            setValueText(valueTextValue);
        }

        dividerTop.setVisibility(dividerTopValue ? VISIBLE : GONE);
        dividerBottom.setVisibility(dividerBottomValue ? VISIBLE : GONE);

        attributes.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
    }

    public String getNameText()
    {
        return nameText.getText().toString();
    }

    public void setNameText(String text)
    {
        nameText.setText(text);
    }

    public TextView getNameTextView()
    {
        return nameText;
    }

    public String getValueText()
    {
        return valueText.getText().toString();
    }

    public void setValueText(String text)
    {
        valueText.setText(text);
    }

    public TextView getValueTextView()
    {
        return valueText;
    }
}
