package com.myfiziq.sdk.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.util.NumberFormatUtils;
import com.myfiziq.sdk.util.TimeFormatUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * @hide
 */
public class ItemViewAvatarsCompare extends FrameLayout implements BaseModelViewInterface<ModelAvatar>
{
    private TextView title;
    private TextView valueOne;
    private TextView valueTwo;
    private ImageView deltaImg;
    private TextView delta;

    public ItemViewAvatarsCompare(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatarsCompare(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatarsCompare(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View view = LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        title = view.findViewById(R.id.title);
        valueOne = view.findViewById(R.id.valueOne);
        valueTwo = view.findViewById(R.id.valueTwo);
        deltaImg = view.findViewById(R.id.deltaImg);
        delta = view.findViewById(R.id.delta);
    }

    public int getLayout()
    {
        return R.layout.view_compare_measurement_item;
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
            valueOne.setText(sb);
        }
    }

    public void bind(String titleText, DecimalFormat format, double value1, double value2)
    {
        title.setText(titleText);

        BigDecimal value1Rounded = NumberFormatUtils.roundDoubleHalfUp(value1, 1);
        BigDecimal value2Rounded = NumberFormatUtils.roundDoubleHalfUp(value2, 1);

        valueOne.setText(format.format(value1Rounded));
        valueTwo.setText(format.format(value2Rounded));

        Double differenceOfRounded = value1Rounded.subtract(value2Rounded).doubleValue();
        int comparedResult = value1Rounded.compareTo(value2Rounded);

        if (comparedResult > 0)
        {
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_down);
            delta.setText(format.format(differenceOfRounded));
        }
        else if (comparedResult < 0)
        {
            deltaImg.setVisibility(VISIBLE);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_up);
            delta.setText(format.format(-differenceOfRounded));
        }
        else
        {
            deltaImg.setVisibility(GONE);
            delta.setText(getContext().getString(R.string.empty_comparation_sign));
        }
    }

    public void bind(String titleText, Length inputValue1, Length inputValue2, Class<? extends Length> preferredUnitOfMeasurement)
    {
        // Convert measurements to the user's preferred unit of measurement
        Length value1 = Length.fromCentimeters(preferredUnitOfMeasurement, inputValue1.getValueInCm());
        Length value2 = Length.fromCentimeters(preferredUnitOfMeasurement, inputValue2.getValueInCm());

        if (value1 == null || value2 == null)
        {
            Timber.e("Input was empty");
            return;
        }

        Length deltaValue = Length.fromCentimeters(preferredUnitOfMeasurement, 0);
        if (deltaValue == null)
        {
            Timber.e("Generated length delta object was empty");
            return;
        }

        if (titleText.equals("HEIGHT") && preferredUnitOfMeasurement == Centimeters.class)
        {
            value1.setFormat(Centimeters.heightFormat);
            value2.setFormat(Centimeters.heightFormat);
            deltaValue.setFormat(Centimeters.heightFormat);
        }

        title.setText(titleText);
        valueOne.setText(value1.getFormatted());
        valueTwo.setText(value2.getFormatted());

        BigDecimal value1Numeric = value1.getValueForComparison();
        BigDecimal value2Numeric = value2.getValueForComparison();


        int comparisonResult = value1Numeric.compareTo(value2Numeric);

        if (comparisonResult > 0)
        {
            BigDecimal deltaNumeric = value1Numeric.subtract(value2Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_down);
        }
        else if (comparisonResult < 0)
        {
            BigDecimal deltaNumeric = value2Numeric.subtract(value1Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_up);
        }
        else
        {
            deltaValue.setValueInCm(0);
        }

        String deltaValueFormatted = deltaValue.getFormatted();
        String deltaValueFormattedOnlyNumbers = deltaValueFormatted.replaceAll("[^\\d]", "");

        if (TextUtils.isEmpty(deltaValueFormatted) || TextUtils.isEmpty(deltaValueFormattedOnlyNumbers) || Integer.parseInt(deltaValueFormattedOnlyNumbers) == 0)
        {
            // If the delta value is either empty or 0, do not show it. Both measurements are equal.
            deltaImg.setVisibility(GONE);
            delta.setText(getContext().getString(R.string.empty_comparation_sign));
        }
        else
        {
            // If the delta value is not empty or blank, show it.
            deltaImg.setVisibility(VISIBLE);
            delta.setText(deltaValue.getFormatted());
        }
    }

    public void bind(String titleText, Weight inputValue1, Weight inputValue2, Class<? extends Weight> preferredUnitOfMeasurement)
    {
        // Convert measurements to the user's preferred unit of measurement
        Weight value1 = Weight.fromKilograms(preferredUnitOfMeasurement, inputValue1.getValueInKg());
        Weight value2 = Weight.fromKilograms(preferredUnitOfMeasurement, inputValue2.getValueInKg());

        if (value1 == null || value2 == null)
        {
            Timber.e("Input was empty");
            return;
        }

        title.setText(titleText);
        valueOne.setText(value1.getFormatted());
        valueTwo.setText(value2.getFormatted());

        BigDecimal value1Numeric = value1.getValueForComparison();
        BigDecimal value2Numeric = value2.getValueForComparison();

        Weight deltaValue = Weight.fromKilograms(preferredUnitOfMeasurement, 0);

        if (deltaValue == null)
        {
            Timber.e("Generated weight delta object was empty");
            return;
        }

        int comparisonResult = value1Numeric.compareTo(value2Numeric);

        if (comparisonResult > 0)
        {
            BigDecimal deltaNumeric = value1Numeric.subtract(value2Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_down);
        }
        else if (comparisonResult < 0)
        {
            BigDecimal deltaNumeric = value2Numeric.subtract(value1Numeric);
            deltaValue.setTransformedValueFromComparison(deltaNumeric);
            deltaImg.setImageResource(R.drawable.ic_mini_arrow_up);
        }
        else
        {
            deltaValue.setValueInKg(0);
        }

        String deltaValueFormatted = deltaValue.getFormatted();
        String deltaValueFormattedOnlyNumbers = deltaValueFormatted.replaceAll("[^\\d]", "");

        if (TextUtils.isEmpty(deltaValueFormatted) || TextUtils.isEmpty(deltaValueFormattedOnlyNumbers) || Integer.parseInt(deltaValueFormattedOnlyNumbers) == 0)
        {
            // If the delta value is either empty or 0, do not show it. Both measurements are equal.
            deltaImg.setVisibility(GONE);
            delta.setText(getContext().getString(R.string.empty_comparation_sign));
        }
        else
        {
            // If the delta value is not empty or blank, show it.
            deltaImg.setVisibility(VISIBLE);
            delta.setText(deltaValue.getFormatted());
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
