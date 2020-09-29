package com.myfiziq.sdk.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.util.List;

/**
 * @hide
 */
public class ItemViewGuest extends LinearLayout implements BaseModelViewInterface<ModelAvatar>
{
    ImageView selectedGuestIcon;
    TextView guestName;
    ImageView deleteButton;

    public ItemViewGuest(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewGuest(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewGuest(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View view = LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        selectedGuestIcon = view.findViewById(R.id.selectedGuestIcon);
        guestName = view.findViewById(R.id.guestName);
        deleteButton = view.findViewById(R.id.delete_button);
    }

    public int getLayout()
    {
        return R.layout.view_guest_item;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, ModelAvatar model)
    {
        if (model != null)
        {
            String modelGuestName = model.getGuestName();

            if (!TextUtils.isEmpty(modelGuestName))
            {
                guestName.setText(modelGuestName);

                String selectedGuest = MyFiziq.getInstance().getGuestUser();

                if (modelGuestName.equals(selectedGuest))
                {
                    selectedGuestIcon.setVisibility(VISIBLE);
                }
            }
        }
    }

    @Override
    public void setViewOnClickListeners(List<OnClickListener> listeners)
    {
        if (listeners != null)
        {
            if (listeners.size() >= 1)
            {
                setOnClickListener(listeners.get(0));
            }

            if (listeners.size() >= 2)
            {
                deleteButton.setVisibility(VISIBLE);
                deleteButton.setOnClickListener(listeners.get(1));
            }
        }
    }


    @Override
    public void setModelTag(ModelAvatar model)
    {
        setTag(R.id.TAG_MODEL, model);
        deleteButton.setTag(R.id.TAG_MODEL, model);
    }

    @Override
    public void setHolderTag(CursorHolder holder)
    {
        setTag(R.id.TAG_HOLDER, holder);
        deleteButton.setTag(R.id.TAG_HOLDER, holder);
    }
}
