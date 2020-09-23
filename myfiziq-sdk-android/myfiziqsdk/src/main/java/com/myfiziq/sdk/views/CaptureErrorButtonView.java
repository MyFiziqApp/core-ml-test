package com.myfiziq.sdk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.SupportType;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * Created by Muhammad Naufal Azzaahid on 20/08/2019.
 */
public class CaptureErrorButtonView extends ConstraintLayout
{
    private View container;
    private Button tryAgainButton;
    private Button contactSupportButton;
    private Button exitButton;

    public CaptureErrorButtonView(Context context)
    {
        super(context);
        init(context);
    }

    public CaptureErrorButtonView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public CaptureErrorButtonView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context)
    {
        container = LayoutInflater.from(context).inflate(getLayout(), this, true);

        tryAgainButton = container.findViewById(R.id.tryAgainButton);
        exitButton = container.findViewById(R.id.exitButton);
        contactSupportButton = container.findViewById(R.id.capSupportButton);
        contactSupportButton.getPaint().setUnderlineText(true);
        if(SisterColors.getInstance().isSisterMode())
        {
            contactSupportButton.setVisibility(View.GONE);
        }
    }

    public void setButtonListeners(ActivityInterface activity, ModelAvatar avatar, AsyncHelper.Callback<Void> onTryAgain, AsyncHelper.Callback<Void> onExit)
    {
        contactSupportButton.setOnClickListener(v ->
        {

            ParameterSet paramSet = new ParameterSet.Builder()
                    .addParam(new Parameter(R.id.TAG_ARG_VIEW,SupportType.ERROR_SUPPORT))
                    .addParam(new Parameter(R.id.TAG_ARG_MODEL_AVATAR,avatar))
                    .build();

            new IntentManagerService<ParameterSet>(activity.getActivity()).requestAndListenForResponse(
                    IntentPairs.SUPPORT_ROUTE,
                    paramSet,
                    result -> result.start(activity, true)
            );
        });
        tryAgainButton.setOnClickListener(v -> onTryAgain.execute(null));
        exitButton.setOnClickListener(v -> onExit.execute(null));
    }

    public int getLayout()
    {
        return R.layout.view_capture_button_error;
    }
}
