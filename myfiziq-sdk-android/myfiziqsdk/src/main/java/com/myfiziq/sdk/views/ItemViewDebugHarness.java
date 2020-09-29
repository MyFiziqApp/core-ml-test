package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.DebugActivity;
import com.myfiziq.sdk.activities.DebugHarnessActivity;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.util.List;

/**
 * @hide
 */
public class ItemViewDebugHarness extends FrameLayout implements BaseModelViewInterface<DebugHarnessActivity.DebugModel>
{
    TextView debugItem;

    public ItemViewDebugHarness(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewDebugHarness(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewDebugHarness(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View view = LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        debugItem = view.findViewById(R.id.debug);
    }

    public int getLayout()
    {
        return R.layout.view_debug_harness_item;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, DebugHarnessActivity.DebugModel model)
    {
        if (model != null)
        {
            debugItem.setText(model.id);
        }
    }

    @Override
    public void setViewOnClickListeners(List<OnClickListener> listeners)
    {
        if (null != listeners && listeners.size() >= 1)
        {
            setOnClickListener(listeners.get(0));
        }
    }

    @Override
    public void setModelTag(DebugHarnessActivity.DebugModel model)
    {
        setTag(R.id.TAG_MODEL, model);
    }

    @Override
    public void setHolderTag(CursorHolder holder)
    {
        setTag(R.id.TAG_HOLDER, holder);
    }
}
