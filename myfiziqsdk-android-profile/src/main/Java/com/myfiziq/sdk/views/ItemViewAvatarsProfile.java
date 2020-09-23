package com.myfiziq.sdk.views;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.models.MeasurementFormat;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziqsdk_android_profile.R;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @hide
 */
public class ItemViewAvatarsProfile extends FrameLayout implements BaseModelViewInterface<ModelAvatar>
{
    // A difference of less than 0.1 in a measurement will be treated as being "equal" when displayed to the user.
    private float measurementPrecision = 0.1f;


    TextView title;
    TextView detail;
    ImageView deltaImg;
    TextView delta;

    private List<MeasurementFormat> measurementFormatList;

    public ItemViewAvatarsProfile(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatarsProfile(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatarsProfile(Context context, AttributeSet attrs, int defStyleAttr)
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
        deltaImg = findViewById(R.id.deltaImg);
        delta = findViewById(R.id.delta);
    }

    public int getLayout()
    {
        return R.layout.view_measurement_item;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, ModelAvatar avatar)
    {
        if (avatar != null)
        {
            SpannableStringBuilder sb = new SpannableStringBuilder();

            sb.append("State: ");

            String waistFormatted = avatar.getAdjustedWaist().getFormatted();

            Spannable status = new SpannableString(avatar.getStatus().name());
            Spannable waist = new SpannableString(String.format(Locale.getDefault(), "  Waist: %s", waistFormatted));

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

            String weightFormatted = avatar.getWeight().getFormatted();

            Date requestDate = avatar.getRequestDate();
            String date = TimeFormatUtils.formatDate(requestDate, TimeZone.getDefault(), TimeFormatUtils.PATTERN_YYYY_MM_DD);

            title.setText(String.format(Locale.getDefault(), "%s | %s", date, weightFormatted));
            detail.setText(sb);
        }
    }

    public void bind(String titleText, String detailText)
    {
        title.setText(titleText);
        detail.setText(detailText);
    }

    public void bind(String titleText, DecimalFormat format, float value)
    {
        title.setText(titleText);
        detail.setText(format.format(value));
    }

    public void bind(String titleText, Length value)
    {
        Length emptySecondValue = value.clone();
        emptySecondValue.setValueInCm(0);

        bind(titleText, value, emptySecondValue);
    }

    public void bind(String titleText, Weight value)
    {
        Weight emptySecondValue = value.clone();
        emptySecondValue.setValueInKg(0);

        bind(titleText, value, emptySecondValue);
    }

    public void bind(String titleText, Length currentMeasurement, Length previousMeasurement)
    {
        title.setText(titleText);
        detail.setText(currentMeasurement.getFormatted());

        // TODO Should we be using currentMeasurement as the default units of measurement?
        Length deltaValue = currentMeasurement.clone();

        if (previousMeasurement.getValueInCm() == 0)
        {
            // No previous measurements were specified
            deltaImg.setVisibility(GONE);
            delta.setVisibility(GONE);

            deltaValue.setValueInCm(0);
        }
        else if (currentMeasurement.greaterThan(previousMeasurement))
        {
            // The current measurement is greater than the previous one
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_triangle_up);

            deltaValue.setValueInCm(currentMeasurement.getValueInCm()-previousMeasurement.getValueInCm());
        }
        else if (currentMeasurement.lessThan(previousMeasurement))
        {
            // The current measurement is less than the previous one
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_triangle_down);

            deltaValue.setValueInCm(previousMeasurement.getValueInCm()-currentMeasurement.getValueInCm());
        }
        else
        {
            // The current measurement is equal to the last one
            deltaImg.setVisibility(INVISIBLE);
            delta.setVisibility(INVISIBLE);

            deltaValue.setValueInCm(0);
        }

        delta.setText(deltaValue.getFormatted());
    }

    public void bind(String titleText, Weight currentMeasurement, Weight previousMeasurement)
    {
        title.setText(titleText);
        detail.setText(currentMeasurement.getFormatted());

        // TODO Should we be using currentMeasurement as the default units of measurement?
        Weight deltaValue = currentMeasurement.clone();

        if (previousMeasurement.getValueInKg() == 0)
        {
            // No previous measurements were specified
            deltaImg.setVisibility(GONE);
            delta.setVisibility(GONE);

            deltaValue.setValueInKg(0);
        }
        else if (currentMeasurement.greaterThan(previousMeasurement))
        {
            // The current measurement is greater than the previous one
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_triangle_up);

            deltaValue.setValueInKg(currentMeasurement.getValueInKg()-previousMeasurement.getValueInKg());
        }
        else if (currentMeasurement.lessThan(previousMeasurement))
        {
            // The current measurement is less than the previous one
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_triangle_down);

            deltaValue.setValueInKg(previousMeasurement.getValueInKg()-currentMeasurement.getValueInKg());
        }
        else
        {
            // The current measurement is equal to the last one
            deltaImg.setVisibility(INVISIBLE);
            delta.setVisibility(INVISIBLE);

            deltaValue.setValueInKg(0);
        }

        delta.setText(deltaValue.getFormatted());
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
