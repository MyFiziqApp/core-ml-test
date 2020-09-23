package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.MyFiziqLoaderManager;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.adapters.RecyclerManagerInterface;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.views.ItemViewAvatar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import timber.log.Timber;

public class FragmentAvatarSelector extends BaseFragment implements
        FragmentInterface,
        View.OnClickListener,
        RecyclerManagerInterface,
        SwipeRefreshLayout.OnRefreshListener
{
    private RecyclerView recycler;
    private RecyclerManager mManager;
    private IntentResponses mSelectionResponse;

    private MyFiziqLoaderManager userProfileCacheLoader;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_track_select));

        ParameterSet parameterSet = getParameterSet();


        String whereClause = String.format("Status='%s'", Status.Completed);

        if (null != parameterSet)
        {
            mSelectionResponse = parameterSet.getEnumParamValue(IntentResponses.class, R.id.TAG_ARG_SELECTION_RESPONSE);

            if (parameterSet.hasParam(R.id.TAG_ARG_EXCLUDE_MODEL))
            {
                Parcelable parcelable = parameterSet.getParam(R.id.TAG_ARG_EXCLUDE_MODEL).getParcelableValue();

                if (parcelable instanceof ModelAvatar)
                {
                    ModelAvatar avatar = (ModelAvatar) parcelable;
                    whereClause = String.format("%s AND attemptId <> '%s'", whereClause, avatar.getAttemptId());
                }
            }
        }

        recycler = view.findViewById(R.id.recycler);

        mManager = new RecyclerManager(getActivity(), getLoaderManager(), this);

        mManager.setupRecycler(this, recycler,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(new Parameter.Builder()
                                .setModel(ModelAvatar.class)
                                .setView(ItemViewAvatar.class)
                                .setWhere(ModelAvatar.getWhere(whereClause))
                                .setOrder(ModelAvatar.getOrderBy(0))
                                .build())
                        .build()
        );
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_avatar_selector;
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
    }

    @Override
    public List<View.OnClickListener> getItemSelectListeners()
    {
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();

        listeners.add(v ->
        {
            ModelAvatar model = (ModelAvatar) v.getTag(R.id.TAG_MODEL);

            if (null == model)
            {
                Timber.e("Cannot get model from RecyclerView item");
            }
            else
            {
                Timber.d("User selected model: %s", model.getId());
            }

            if (mSelectionResponse != null)
            {
                IntentManagerService<ModelAvatar> intentManagerService = new IntentManagerService<>(getActivity());
                intentManagerService.respond(mSelectionResponse, model, true);
            }

            getActivity().onBackPressed();
        });

        return listeners;
    }

    @Override
    public void onRefresh()
    {
    }
}

