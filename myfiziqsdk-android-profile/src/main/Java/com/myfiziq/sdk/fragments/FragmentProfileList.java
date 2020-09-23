package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.RecyclerCursorAdapter;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.adapters.RecyclerManagerInterface;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.AvatarUploadWorker;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.intents.parcels.ViewAvatarRouteRequest;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.ItemViewAvatar;
import com.myfiziqsdk_android_profile.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentProfileList extends BaseFragment implements
        FragmentInterface,
        View.OnClickListener,
        RecyclerManagerInterface,
        FragmentHomeInterface,
        CursorHolder.CursorChangedListener
{
    RecyclerView recycler;
    View emptyCard;
    RecyclerManager mManager;

    private boolean hasLeftFragment = false;


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        recycler = view.findViewById(R.id.recycler);
        emptyCard = view.findViewById(R.id.profile_list_card);

        ActionBarHelper.setActionBarTitle(getActivity(), R.string.profile);

        Parameter avatarItems = new Parameter.Builder()
                .setModel(ModelAvatar.class)
                .setView(ItemViewAvatar.class)
                .setWhere(ModelAvatar.getWhere())
                .setOrder(ModelAvatar.getOrderBy(0))
                .build();

        avatarItems.getHolder().addListener(this);

        mManager = new RecyclerManager(getActivity(), getLoaderManager(), this);
        mManager.setupRecycler(
                this,
                recycler,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(avatarItems)
                        .build());

        animateRecyclerViewOnce();

        view.findViewById(R.id.newAvatar).setOnClickListener(v ->
        {
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.ONBOARDING_ROUTE,
                    result -> result.start(getMyActivity()));
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        hasLeftFragment = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Fallback.. update empty card state
        AsyncHelper.run(
                () ->
                {
                    // Make new avatars "visible"
                    ModelAvatar.makeAvatarsSeen();

                    // Check visible avatar count.
                    return ORMTable.getModelCount(ModelAvatar.class, ModelAvatar.getWhere());
                },
                avatarCount ->
                {
                    UiUtils.setViewVisibility(emptyCard, (avatarCount > 0) ? View.GONE : View.VISIBLE);
                },
                true
        );
    }

    @Override
    public void onStop()
    {
        super.onResume();

        if (hasLeftFragment)
        {
            hasLeftFragment = false;
            animateRecyclerViewOnce();

            // Force the RecyclerView to re-execute the enter animation if we are resuming the fragment
            // but aren't rendering it for the first time.
            // (e.g. when the user minimises the app or navigates to another fragment but this
            // fragment stays in memory).
            recycler.getAdapter().notifyDataSetChanged();
            recycler.scheduleLayoutAnimation();
        }
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_profile_list;
    }

    @Override
    public void onClick(View v)
    {
        // int id = v.getId();
    }

    @Override
    public List<View.OnClickListener> getItemSelectListeners()
    {
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();

        listeners.add(v ->
        {
            ModelAvatar thisModel = (ModelAvatar) v.getTag(R.id.TAG_MODEL);

            if (!thisModel.isCompleted())
            {
                // Model isn't ready to view yet. Nothing to see!
                return;
            }

            ViewAvatarRouteRequest requestParcel = new ViewAvatarRouteRequest(thisModel.getId());

            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(getActivity());
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.VIEW_AVATAR_ROUTE,
                    requestParcel,
                    result -> result.start(getMyActivity())
            );
        });

        listeners.add(v ->
                UiUtils.showAlertDialog(
                        getMyActivity().getActivity(),
                        "",
                        getString(R.string.sure_cancel_session),
                        getString(R.string.cancel_session),
                        getString(R.string.myfiziqsdk_cancel),
                        (dialog, which) ->
                        {
                            ModelAvatar model = (ModelAvatar) v.getTag(R.id.TAG_MODEL);
                            // Ensure it's deleted from the server and then remove locally.
                            MyFiziqSdkManager.deleteAvatar(model, (responseCode, result, payload) -> model.delete());
                        },
                        (dialog, which) ->
                        {
                        }
                )
        );

        listeners.add(v ->
        {
            ModelAvatar model = (ModelAvatar) v.getTag(R.id.TAG_MODEL);
            model.setStatus(Status.Pending);
            AvatarUploadWorker.createWorker(model);
        });

        return listeners;
    }

    @Override
    public int getIcon()
    {
        return R.drawable.ic_profile_circle;
    }

    @Override
    public void onCursorChanged(CursorHolder holder)
    {
        UiUtils.setViewVisibility(emptyCard, (holder.getItemCount() > 0) ? View.GONE : View.VISIBLE);
    }

    /**
     * Animate the ReyclerView once when it is next updated.
     */
    private void animateRecyclerViewOnce()
    {
        if (getActivity() == null || isDetached() || recycler == null)
        {
            // Fragment has detached from activity
            return;
        }

        RecyclerView.Adapter adapter = recycler.getAdapter();

        if (adapter instanceof RecyclerCursorAdapter)
        {
            RecyclerCursorAdapter recyclerCursorAdapter = (RecyclerCursorAdapter) adapter;

            LayoutAnimationController animationController = AnimationUtils.loadLayoutAnimation(getContext(), com.myfiziq.sdk.R.anim.layout_animation_fall_down);

            recyclerCursorAdapter.setAnimationController(animationController);

            recyclerCursorAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
            {
                @Override
                public void onChanged()
                {
                    super.onChanged();

                    // Clear the animation after we've rendered it for the first time.
                    recyclerCursorAdapter.setAnimationController(null);

                    // Stop listening for new changes
                    recyclerCursorAdapter.unregisterAdapterDataObserver(this);
                }
            });
        }
    }
}
