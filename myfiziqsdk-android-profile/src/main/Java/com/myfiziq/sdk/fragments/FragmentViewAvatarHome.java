package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.MyFiziqLoaderManager;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziqsdk_android_profile.R;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import timber.log.Timber;

// TODO Consolidate layout.xml with FragmentViewAvatar
public class FragmentViewAvatarHome extends FragmentViewAvatar implements FragmentInterface, FragmentHomeInterface, CursorHolder.CursorChangedListener
{
    MyFiziqLoaderManager mMQYLoaderManager;
    private NestedScrollView nestedScrollView;
    private LinearLayout measurementCards;
    private View firstMeasurementCard;
    private View mNewAvatar;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        measurementCards = view.findViewById(R.id.measurementCards);
        firstMeasurementCard = view.findViewById(R.id.first_measurement_card);
        mNewAvatar = view.findViewById(R.id.newAvatar);

        mNewAvatar.setOnClickListener(v -> onNewAvatarClicked());

        mMQYLoaderManager = new MyFiziqLoaderManager(getActivity(), getLoaderManager());
        mMQYLoaderManager.loadCursor(0,
                ORMContentProvider.uri(ModelAvatar.class),
                ModelAvatar.getWhere(
                        String.format("Status='%s'", Status.Completed)
                ),
                ModelAvatar.getOrderBy(2),
                ModelAvatar.class,
                null,
                this);

        /*
        AsyncHelper.run(
                () -> ORMDbQueries.getLastXCompletedAvatars(1),
                avatarList ->
                {
                    if (avatarList == null || avatarList.isEmpty())
                    {
                        Timber.i("ViewAvatarHome created, no avatars are present so hide the loading card now");
                        firstMeasurementCard.setVisibility(View.VISIBLE);
                    }
                },
                true
        );
        */
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_profile;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // The homescreen should follow the back button style of the activity, not of FragmentViewAvatar which we extend from
        ActionBarHelper.resetBackButtonToDefaultState(getActivity());

        ActionBarHelper.setActionBarTitle(getActivity(), R.string.myfiziqsdk_home);
    }

    @Override
    protected void renderPage(@Nullable ModelAvatar model)
    {
        if (null != model)
        {
            UiUtils.setViewVisibility(nestedScrollView, View.INVISIBLE);
        }
        super.renderPage(model);
    }

    private void renderExistingModels(CursorHolder cursorHolder)
    {
        if (null == getActivity())
        {
            Timber.w("The view died before we could render any existing avatars");
            return;
        }

        //UiUtils.setViewVisibility(loadingCard, View.GONE);

        if (null == cursorHolder || cursorHolder.getItemCount() < 1)
        {
            Timber.i("Cursor holder has no items, can't render avatar");
            UiUtils.setViewVisibility(firstMeasurementCard, View.VISIBLE);
            return;
        }

        ModelAvatar thisModel = (ModelAvatar)cursorHolder.getItem(0);
        renderPage(thisModel);
    }

    private void onNewAvatarClicked()
    {
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.requestAndListenForResponse(
                IntentPairs.ONBOARDING_ROUTE,
                result -> result.start(getMyActivity())
        );
    }

    @Override
    public void onCursorChanged(CursorHolder cursorHolder)
    {
        renderExistingModels(cursorHolder);
    }

    @Override
    public int getIcon()
    {
        return R.drawable.ic_home;
    }
}
