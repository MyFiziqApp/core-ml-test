package com.myfiziq.sdk.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @hide
 */
public class ItemViewLog extends FrameLayout implements BaseModelViewInterface<ModelLog>
{
    public final static DateFormat LOGFMT = new SimpleDateFormat("dd/MM HH:mm:ss ", Locale.getDefault());

    TextView log;
    Date mDate = new Date();

    public ItemViewLog(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewLog(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewLog(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View view = LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        log = view.findViewById(R.id.log);
    }

    public int getLayout()
    {
        return R.layout.view_log_item;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, ModelLog model)
    {
        if (model != null)
        {
            String logText = model.value.replace("\\n", "");
            if (!TextUtils.isEmpty(logText))
            {
                mDate.setTime(model.timestamp);
                String dateText = LOGFMT.format(mDate);
                Spannable text = new SpannableString(dateText + logText);
                text.setSpan(new ForegroundColorSpan(Color.rgb(128, 128, 128)), 0, dateText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                switch (ModelLog.Type.values()[model.type])
                {
                    case WARN:
                        text.setSpan(new ForegroundColorSpan(Color.rgb(128, 70, 0)), dateText.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;

                    case ERROR:
                        text.setSpan(new ForegroundColorSpan(Color.rgb(128, 0, 0)), dateText.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;

                    case NDK:
                        text.setSpan(new ForegroundColorSpan(Color.rgb(0, 64, 0)), dateText.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }

                log.setText(text);
                setVisibility(VISIBLE);
                return;
            }
        }
        setVisibility(GONE);
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
    public void setModelTag(ModelLog model)
    {
        setTag(R.id.TAG_MODEL, model);
    }

    @Override
    public void setHolderTag(CursorHolder holder)
    {
        setTag(R.id.TAG_HOLDER, holder);
    }
}
