package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.BaseModelViewInterface;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.fragments.FragmentInterface;
import com.myfiziq.sdk.helpers.BodyFatCategoryCalculator;
import com.myfiziq.sdk.helpers.BodyFatCategoryFormatter;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.vo.BodyFatCategory;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.cardview.widget.CardView;

/**
 * @hide
 */
public class ItemViewAvatar extends FrameLayout implements BaseModelViewInterface<ModelAvatar>
{
    CardView parentContainer;
    View cardProgressBar;
    LinearLayout errorLayout;
    Button cardRetryButton;
    Button cardCancelButton;
    LinearLayout dataContainer;
    TextView date;
    AvatarDataItem layoutChest;
    AvatarDataItem layoutWaist;
    AvatarDataItem layoutHips;
    AvatarDataItem layoutThighs;
    TextView layoutHeading;
    TextView layoutHeadingIndicator;
    TextView layoutSubHeading;
    TextView cardState;
    TextView cardErrorText;

    // TODO Get order of day and month and vary depending on which comes first in the user's locale
    DateFormat dateFormat = new SimpleDateFormat("dd MMM");

    public ItemViewAvatar(Context context)
    {
        super(context);
        init(context);
    }

    public ItemViewAvatar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ItemViewAvatar(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        View view = LayoutInflater.from(context).inflate(getLayout(), this, true);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        parentContainer = view.findViewById(R.id.parentContainer);

        cardProgressBar = view.findViewById(R.id.cardProgressBar);
        cardState = view.findViewById(R.id.cardState);

        errorLayout = view.findViewById(R.id.errorLayout);
        cardErrorText = view.findViewById(R.id.cardErrorText);
        cardCancelButton = view.findViewById(R.id.cardCancelButton);
        cardRetryButton = view.findViewById(R.id.cardRetryButton);

        dataContainer = view.findViewById(R.id.dataContainer);
        date = view.findViewById(R.id.date);
        layoutChest = view.findViewById(R.id.layout_chest);
        layoutWaist = view.findViewById(R.id.layout_waist);
        layoutHips = view.findViewById(R.id.layout_hips);
        layoutThighs = view.findViewById(R.id.layout_thighs);

        layoutHeading = view.findViewById(R.id.layout_heading);
        layoutHeadingIndicator = view.findViewById(R.id.layout_heading_indicator);
        layoutSubHeading = view.findViewById(R.id.layout_subheading);
    }

    public int getLayout()
    {
        return R.layout.view_avatar_item;
    }

    @Override
    public void bind(CursorHolder holder, FragmentInterface fragment, ModelAvatar model)
    {
        if (model != null)
        {
            switch (model.getStatus())
            {
                case Completed:
                    dataContainer.setVisibility(VISIBLE);
                    cardProgressBar.setVisibility(GONE);
                    errorLayout.setVisibility(GONE);
                    populateData(model);
                    break;
                case Captured:
                case Pending:
                case Processing:
                case Uploading:
                    cardProgressBar.setVisibility(VISIBLE);
                    cardState.setText(model.getStatus().getDescription(getResources()));
                    dataContainer.setVisibility(INVISIBLE);
                    errorLayout.setVisibility(GONE);
                    break;
                case FailedGeneral:
                case FailedNoInternet:
                case FailedServerErr:
                case FailedTimeout:
                    cardErrorText.setText(model.getStatus().getDescription(getResources()));
                    errorLayout.setVisibility(VISIBLE);
                    cardProgressBar.setVisibility(GONE);
                    dataContainer.setVisibility(INVISIBLE);
                    break;
            }
        }
    }



    @Override
    public void setViewOnClickListeners(List<OnClickListener> listeners)
    {
        if (null != listeners)
        {
            if (listeners.size() >= 1)
            {
                setOnClickListener(listeners.get(0));
            }
            if (listeners.size() >= 2)
            {
                cardCancelButton.setOnClickListener(listeners.get(1));
            }
            if (listeners.size() >= 3)
            {
                cardRetryButton.setOnClickListener(listeners.get(2));
            }
        }
    }

    @Override
    public void setModelTag(ModelAvatar model)
    {
        setTag(R.id.TAG_MODEL, model);
        cardCancelButton.setTag(R.id.TAG_MODEL, model);
        cardRetryButton.setTag(R.id.TAG_MODEL, model);
    }

    @Override
    public void setHolderTag(CursorHolder holder)
    {
        setTag(R.id.TAG_HOLDER, holder);
    }

    /**
     * Populates the view with data contained in a ModelAvatar.
     *
     * @param model The ModelAvatar to populate the view with.
     */
    private void populateData(ModelAvatar model)
    {
        Date requestDate = model.getRequestDate();
        String formattedRequestDate = dateFormat.format(requestDate);

        // Remove trailing dot
        formattedRequestDate = formattedRequestDate.replace(".", "");

        date.setText(formattedRequestDate);


        layoutChest.setValueText(model.getAdjustedChest().getFormatted());
        layoutWaist.setValueText(model.getAdjustedWaist().getFormatted());
        layoutHips.setValueText(model.getAdjustedHip().getFormatted());
        layoutThighs.setValueText(model.getAdjustedThigh().getFormatted());

        if (model.getAdjustedPercentBodyFat() > 0)
        {
            // Render the total body fat in the headers if we have a body fat percent
            renderTotalBodyFatInHeaders(model);
        }
        else
        {
            // Render the height and weight in the headers if there's no body fat information
            renderHeightAndWeightInHeaders(model);
        }
    }

    /**
     * Renders the Total Body Fat in the headers of the view with data contained in a ModelAvatar.
     *
     * @param model The ModelAvatar to populate the view's headers with.
     */
    private void renderTotalBodyFatInHeaders(ModelAvatar model)
    {
        Resources resources = getResources();
        DecimalFormat percentFormat = new DecimalFormat("0.0'%'");

        String tbfString = resources.getString(R.string.tbf_label, percentFormat.format(model.getAdjustedPercentBodyFat()));
        String weightString = resources.getString(R.string.weight_label, model.getWeight().getFormatted());

        layoutHeading.setText(tbfString);
        layoutHeadingIndicator.setVisibility(VISIBLE);
        layoutSubHeading.setText(weightString);



        long avatarRequestUnixTimestamp = model.getRequestTime();

        Date dob = DateOfBirthCoordinator.getDateOfBirth();

        if (null == dob || avatarRequestUnixTimestamp == 0)
        {
            // Not enough data to render the body fat category, so hide it
            layoutHeadingIndicator.setVisibility(GONE);
            return;
        }

        Date avatarRequestedDate = new Date(avatarRequestUnixTimestamp);

        BodyFatCategory bodyFatCategory = BodyFatCategoryCalculator.determineBodyFatCategory(
                model.getGender(),
                dob,
                avatarRequestedDate,
                model.getAdjustedPercentBodyFat()
        );

        String indicatorText = BodyFatCategoryFormatter.getIndicatorText(resources, bodyFatCategory);
        int indicatorColour = BodyFatCategoryFormatter.getIndicatorColour(resources, bodyFatCategory);

        if (null == indicatorText || indicatorColour == 0)
        {
            // Error occurred when rendering the body fat category, so hide it
            layoutHeadingIndicator.setVisibility(GONE);
            return;
        }

        layoutHeadingIndicator.setText(indicatorText);
        layoutHeadingIndicator.setBackgroundColor(indicatorColour);
    }


    /**
     * Renders the Height and Weight in the headers of the view with data contained in a ModelAvatar.
     *
     * @param model The ModelAvatar to populate the view's headers with.
     */
    private void renderHeightAndWeightInHeaders(ModelAvatar model)
    {
        layoutHeading.setText(model.getWeight().getFormatted());
        layoutHeadingIndicator.setVisibility(GONE);
        Length height = model.getHeight();

        if (height instanceof Centimeters)
        {
            height.setFormat(Centimeters.heightFormat);
        }

        layoutSubHeading.setText(getContext().getString(R.string.height_label, height.getFormatted()));
    }
}
