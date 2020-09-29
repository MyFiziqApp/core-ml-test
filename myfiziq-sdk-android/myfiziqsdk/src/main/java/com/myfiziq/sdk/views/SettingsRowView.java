package com.myfiziq.sdk.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.vo.SdkResultParcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import timber.log.Timber;

public class SettingsRowView extends LinearLayout
{
    private ImageView settingsIcon;
    private TextView settingsLabel;
    private IntentPairs intentPairCallback;

    private IntentManagerService<SdkResultParcelable> intentManagerService;

    public SettingsRowView(Context context)
    {
        super(context);
        init(context);
    }

    public SettingsRowView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public SettingsRowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        LinearLayout view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.view_settings_row, this, true);

        settingsIcon = view.findViewById(R.id.settingsIcon);
        settingsLabel = view.findViewById(R.id.settingsLabel);
    }

    public void setLabel(@StringRes int label)
    {
        String strLabel = getResources().getString(label);
        settingsLabel.setText(strLabel);
    }

    public void setStringLabel(String label)
    {
        settingsLabel.setText(label);
    }


    public void setIcon(@DrawableRes int drawableId)
    {
        Drawable drawable = getResources().getDrawable(drawableId);
        settingsIcon.setImageDrawable(drawable);
    }

    public void setIntentPairCallback(IntentPairs intentPairCallback)
    {
        this.intentPairCallback = intentPairCallback;
    }

    public void setDestination(ActivityInterface activity, ParameterSet destination)
    {
        setOnClickListener(view ->
        {
            if (intentPairCallback != null)
            {
                intentManagerService = new IntentManagerService<>(activity.getActivity());
                intentManagerService.requestAndListenForResponse(intentPairCallback, responseParcelable ->
                {
                    if (responseParcelable.getResultCode().isOk())
                    {
                        destination.start(activity, true);
                    }
                    else if (responseParcelable.getResultCode() == SdkResultCode.USER_CANCELLED)
                    {
                        Timber.e("User cancelled operation for %s settings row failed.", settingsLabel);
                    }
                    else
                    {
                        Timber.e("Callback for %s settings row failed. Code received: %s", settingsLabel, responseParcelable);
                    }

                    intentManagerService.unbindAll();
                });
            }
            else
            {
                // Don't need to listen for anything, just go to the destination activity when clicked
                destination.start(activity, true);
            }
        });
    }

    public void destroy()
    {
        if (intentManagerService != null)
        {
            intentManagerService.unbindAll();
            intentManagerService = null;
        }
    }
}

