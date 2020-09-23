package com.myfiziq.myfiziq_android.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.util.TimeFormatUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @hide
 */
public class ItemViewAvatars extends FrameLayout implements BaseModelViewInterface<ModelAvatar>
{
    private DecimalFormat mDecimalFormat = new DecimalFormat("###,###.#");

    private TextView title;
    private TextView detail;

    public ItemViewAvatars(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatars(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatars(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        title = findViewById(R.id.title);
        detail = findViewById(R.id.detail);
    }

    public int getLayout()
    {
        return R.layout.view_avatar_list;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, ModelAvatar avatar)
    {
        if (avatar != null)
        {
            SpannableStringBuilder sb = new SpannableStringBuilder();

            sb.append("State: ");

            Spannable status = new SpannableString(avatar.getStatus().name());
            Spannable waist = new SpannableString(String.format(Locale.getDefault(), "  Waist: %s", avatar.getAdjustedWaist().getFormatted()));

            switch (avatar.getStatus())
            {
                case Completed:
                    status.setSpan(new ForegroundColorSpan(Color.rgb(16, 86, 37)), 0, status.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(status);
                    sb.append(waist);
                    break;

                case FailedGeneral:
                case FailedNoInternet:
                case FailedServerErr:
                case FailedTimeout:
                    status.setSpan(new ForegroundColorSpan(Color.rgb(98, 11, 11)), 0, status.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(status);
                    break;

                case Captured:
                case Pending:
                case Processing:
                case Uploading:
                    status.setSpan(new ForegroundColorSpan(Color.rgb(147, 73, 0)), 0, status.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(status);
                    break;
            }

            Date requestDate = avatar.getRequestDate();
            String date = TimeFormatUtils.formatDate(requestDate, TimeZone.getDefault(), TimeFormatUtils.PATTERN_YYYY_MM_DD);

            String titleValue = String.format(Locale.getDefault(), "%s | %s", date, avatar.getWeight().getFormatted());
            title.setText(titleValue);

            detail.setText(sb);
        }
    }

    @Override
    public void setViewOnClickListeners(List<OnClickListener> listeners)
    {
        if (null != listeners && !listeners.isEmpty())
        {
            setOnClickListener(listeners.get(0));
        }
    }

    @Override
    public void setModelTag(ModelAvatar model)
    {
        setTag(R.id.TAG_MODEL, model);
    }

    @Override
    public void setHolderTag(CursorHolder holder)
    {
        setTag(R.id.TAG_HOLDER, holder);
    }
}
