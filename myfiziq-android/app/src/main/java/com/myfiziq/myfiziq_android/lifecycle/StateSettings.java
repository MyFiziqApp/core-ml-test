package com.myfiziq.myfiziq_android.lifecycle;

import android.content.Context;
import android.text.TextUtils;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.activities.ActivityAdvancedSettings;
import com.myfiziq.myfiziq_android.activities.ActivityEditProfile;
import com.myfiziq.myfiziq_android.activities.ActivitySupport;
import com.myfiziq.sdk.BuildConfig;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.activities.DebugActivity;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.SupportType;
import com.myfiziq.sdk.fragments.FragmentLogout;
import com.myfiziq.sdk.fragments.FragmentSettings;
import com.myfiziq.sdk.fragments.FragmentTermsOfService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.ParameterSet.Builder;
import com.myfiziq.sdk.lifecycle.StateGuest;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.MiscUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class StateSettings
{
    private StateSettings()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Create a parameter set for the settings screen indicating which settings should be present on the screen and which rows.
     */
    public static ParameterSet getSettings()
    {
        Builder builder = new Builder(FragmentSettings.class)
                .addNextSet(new Builder(ActivityEditProfile.class)
                        .setName("PROFILE")
                        .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_edit_black_24dp))
                        .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.profile_settings))
                        .build());

        if (ModelSetting.getSetting(ModelSetting.Setting.FEATURE_GUEST_USERS, false))
        {
            String selectedGuest = ORMDbFactory.getInstance().getGuestUser();

            if (TextUtils.isEmpty(selectedGuest))
            {
                selectedGuest = "None";
            }


            String guestLabel = "Guest: " + selectedGuest;

            ParameterSet guestParamSet = StateGuest.getSelectGuest();
            guestParamSet.addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_people_alt));
            guestParamSet.addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, guestLabel));

            builder.addNextSet(guestParamSet);
        }

        builder.addNextSet(new Builder(ActivitySupport.class)
                .setName("FEEDBACK")
                .addParam(new Parameter(R.id.TAG_ARG_VIEW, SupportType.FEEDBACK_SUPPORT))
                .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_settings_feedback))
                .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.feedback))
                .build());

        builder.addNextSet(new Builder(FragmentTermsOfService.class)
                .setName("TOS")
                .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_settings_tos))
                .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.myfiziqsdk_terms_of_service))
                .build());

        if (MiscUtils.isInternalBuild())
        {
            builder.addNextSet(new Builder(ActivityAdvancedSettings.class)
                    .setName("ADVANCED_SETTINGS_PAGE")
                    .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.advanced_settings_big))
                    .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.advanced_settings))
                    .build());
        }

        if (isAuthorisedToViewDebugActivity())
        {
            builder.addNextSet(new Builder(DebugActivity.class)
                    .setName("DEBUG_PAGE")
                    .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_debug_menu))
                    .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.debug_menu))
                    .build());
        }

        builder.addNextSet(new Builder(FragmentLogout.class)
            .setName("LOGOUT")
            .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_ICON, R.drawable.ic_settings_logout))
            .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_LABEL, R.string.logout))
            .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_IS_NEED_CONFIRMATION, "true"))
            .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_CONFIRMATION_TEXT, R.string.myfiziqsdk_confirm_logout))
            .addParam(new Parameter(R.id.TAG_ARG_SETTINGS_CONFIRMATION_POSITIVE_TEXT, R.string.logout))
			.addParam(new Parameter(R.id.TAG_ARG_SETTINGS_INTENT_PAIR_CALLBACK, IntentPairs.LOGOUT_CLICKED.toString()))
            .build());


        return builder.build();
    }

    private static boolean isAuthorisedToViewDebugActivity()
    {
        if (BuildConfig.DEBUG)
        {
            return true;
        }

        Context context = GlobalContext.getContext();

        if (context == null)
        {
            Timber.e("Context is null");
            return false;
        }


        String packageName = context.getPackageName();

        Pattern isInternalPattern = Pattern.compile("com\\.myfiziq\\.myfiziq_android\\.myfiziq\\.([A-Za-z0-9]+)\\.internal");
        Matcher isInternalMatcher = isInternalPattern.matcher(packageName);

        boolean isInternal = isInternalMatcher.find();
        boolean isDev = packageName.equals("com.myfiziq.myfiziq_android.myfiziq.dev");


        return (isDev || isInternal);
    }
}
