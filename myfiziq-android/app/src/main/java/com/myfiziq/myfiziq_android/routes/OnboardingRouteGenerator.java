package com.myfiziq.myfiziq_android.routes;

import android.content.Context;
import android.os.Parcelable;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbCache;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.enums.IntentPairs;

import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.StateInput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.Nullable;

public class OnboardingRouteGenerator extends MyFiziqBroadcastReceiver<ParameterSet>
{
    public OnboardingRouteGenerator(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    /*
    public ParameterSet generateParameterSet(@Nullable Parcelable parcel)
    {
        boolean showVideoOnboarding = ModelSetting.getSetting(ModelSetting.Setting.FEATURE_VIDEO_ONBOARDING, false);

        ModelAvatar avatar = getLatestUser();
        ParameterSet stateOnboardingParams = generateParameterSet(showVideoOnboarding);

        return StateInput.getInput(avatar, stateOnboardingParams);
    }
    */

    public ParameterSet generateParameterSet(@Nullable ParameterSet parcel)
    {
        boolean showVideoOnboarding = ModelSetting.getSetting(ModelSetting.Setting.FEATURE_VIDEO_ONBOARDING, false);

        ModelAvatar avatar = getLatestUser();
        ParameterSet stateOnboardingParams = generateParameterSet(showVideoOnboarding);

        ParameterSet input = StateInput.getInput(avatar, stateOnboardingParams);

        Parameter enableDobParameter = new Parameter.Builder()
                .setParamId(R.id.TAG_ARG_INPUT_ENABLE_BIRTH_DATE)
                .setValue(String.valueOf(true))
                .build();

        input.addParam(enableDobParameter);

        if (null != parcel)
        {
            ParameterSet set = null;

            if (input.getName().contentEquals("INPUT"))
            {
                set = input;
            }
            if (null == set)
            {
                set = input.getSubSet("INPUT");
            }

            if (null != set)
            {
                // Merge parameters.
                set.mergeSetParameters(parcel);
            }
        }

        return input;
    }

    /**
     * This method is for creating a set of fragment for Onboarding Page. Currently there are 2
     * type of onboarding, video and picture.
     *
     * @param isVideo : Set true for displaying video onboarding, and false for picture onboarding.
     */
    private ParameterSet generateParameterSet(boolean isVideo)
    {
        if (isVideo)
        {
            return getVideoParameterSet();
        }
        else
        {
            return getPictureParameterSet();
        }
    }

    private ParameterSet getVideoParameterSet()
    {
        if (getUserAvatarCount() == 0)
        {
            List<OnboardingVideoPageVO> pages = generateVideoSectionPages();
            return StateOnboarding.getOnboardingVideo(pages, false);
        }
        else
        {
            List<OnboardingVideoPageVO> pages = generateVideoSectionPages();
            return StateOnboarding.getOnboardingVideo(pages, true);
        }
    }

    private List<OnboardingVideoPageVO> generateVideoSectionPages()
    {
        List<OnboardingVideoPageVO> pages = new LinkedList<>();
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_1, 0, 0, true, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_2, 0, 6900, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_3, 6900, 10800, false, false, true));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_4, 10800, 13000, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_5, 13000, 27500, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_6, 27500, 30000, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_7, 30000, 33200, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_8, 33200, 35750, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_9, 35750, 39700, false, false));
        pages.add(new OnboardingVideoPageVO(FragmentOnboardingVideoPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_video_page_10, 39700, 51500, false, true));

        return pages;
    }

    private ParameterSet getPictureParameterSet()
    {
        boolean hasExistingAvatar = getUserAvatarCount() > 0;
        List<OnboardingPageVO> pages = new LinkedList<>();
        pages.add(new OnboardingPageVO(FragmentOnboardingPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_page_1, 0, com.myfiziqsdk_android_onboarding.R.id.onboarding_page_1_next_page_button, 0, true, hasExistingAvatar));
        pages.add(new OnboardingPageVO(FragmentOnboardingPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_page_2, com.myfiziqsdk_android_onboarding.R.id.onboarding_page_2_next_screen_button, 0, 0, false, hasExistingAvatar));
        pages.add(new OnboardingPageVO(FragmentOnboardingPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_page_3, com.myfiziqsdk_android_onboarding.R.id.onboarding_page_3_next_screen_button, 0, 0, false, hasExistingAvatar));
        pages.add(new OnboardingPageVO(FragmentOnboardingPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_page_4, com.myfiziqsdk_android_onboarding.R.id.onboarding_page_4_next_screen_button, 0, 0, false, hasExistingAvatar));
        pages.add(new OnboardingPageVO(FragmentOnboardingPage.class, com.myfiziqsdk_android_onboarding.R.layout.fragment_onboarding_page_5, com.myfiziqsdk_android_onboarding.R.id.onboarding_page_5_next_screen_button, 0, 0, false, hasExistingAvatar));
        return StateOnboarding.getOnboarding(pages, hasExistingAvatar);
    }

    @Nullable
    private ModelAvatar getLatestUser()
    {
        ModelAvatar avatar = getLatestAvatarFromCache();

        if (null != avatar)
        {
            return avatar;
        }
        else
        {
            return getLatestAvatarFromDatabase();
        }
    }

    @Nullable
    private ModelAvatar getLatestAvatarFromCache()
    {
        return ORMDbCache.getInstance().getLatestModel(ModelAvatar.class);

    }

    @Nullable
    private ModelAvatar getLatestAvatarFromDatabase()
    {
        ArrayList<ModelAvatar> avatarList = ORMTable.getModelList(
                ModelAvatar.class,
                ModelAvatar.getWhere(),
                ModelAvatar.getOrderBy(1)
        );

        if (null != avatarList && !avatarList.isEmpty())
        {
            return avatarList.get(0);
        }

        return null;
    }

    private int getUserAvatarCount()
    {
        int avatarCount = ORMTable.getModelCount(ModelAvatar.class, ModelAvatar.getWhere());

        if (avatarCount > 0)
        {
            return avatarCount;
        }

        return 0;
    }
}