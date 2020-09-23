package com.myfiziq.sdk.fragments;

import android.app.ProgressDialog;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.adapters.RecyclerManagerInterface;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.GuestHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.ItemViewGuest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java9.util.concurrent.CompletableFuture;
import timber.log.Timber;

public class FragmentSelectGuest extends BaseFragment implements FragmentInterface, RecyclerManagerInterface
{
    RecyclerView recycler;
    RecyclerManager mManager;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        recycler = view.findViewById(R.id.recycler);

        ActionBarHelper.setActionBarTitle(getActivity(), getString(R.string.myfiziqsdk_title_select_guest));

        setRecyclerView();
    }

    protected void setRecyclerView()
    {
        mManager = new RecyclerManager(getActivity(), getLoaderManager(), this)
        {
            @Override
            public void bind(int holderId, int id, int position, View view)
            {
                if (view.getId() == R.id.guestHeader)
                {
                    View clearGuestContainer = view.findViewById(R.id.clearGuestContainer);
                    clearGuestContainer.setOnClickListener(v -> onClearGuestClicked());
                }
            }
        };

        mManager.setupRecycler(
                this,
                recycler,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(new Parameter.Builder()
                                .addHeader(R.layout.view_guest_item_clear_header)
                                .setModel(ModelAvatar.class)
                                .setView(ItemViewGuest.class)
                                .setWhere(
                                        ModelAvatar.getWhereWithGuestAvatars(
                                                "miscGuest IS NOT NULL AND TRIM (miscGuest, ' ') != '') GROUP BY (miscGuest")
                                )
                                .build())
                        .build()
        );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ActionBarHelper.showBackButton(getActivity());
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_select_guest;
    }

    @Override
    public List<View.OnClickListener> getItemSelectListeners()
    {
        return Arrays.asList(this::onGuestSelected, this::onGuestDeleteSelected);
    }


    public void onGuestSelected(View v)
    {
        if (getActivity() == null || getMyActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        ModelAvatar thisModel = (ModelAvatar) v.getTag(R.id.TAG_MODEL);
        String selectedGuestName = thisModel.getGuestName();

        AsyncHelper.run(
                () -> GuestHelper.persistGuestSelection(selectedGuestName),
                () ->
                {
                    // Go back to the previous screen
                    getActivity().onBackPressed();
                },
                true
        );

    }

    private void onClearGuestClicked()
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }


        if (mParameterSet.hasParam(R.id.TAG_ARG_SELECTION_RESPONSE))
        {
            IntentResponses selectionResponseId = mParameterSet.getEnumParamValue(IntentResponses.class, R.id.TAG_ARG_SELECTION_RESPONSE);

            // Announce to any fragments that are listening that we have cleared the guest
            IntentManagerService innerIntentManagerService = new IntentManagerService(getActivity());
            innerIntentManagerService.respond(selectionResponseId, null, true);
        }


        AsyncHelper.run(
                () -> GuestHelper.persistGuestSelection(""),
                () ->
                {
                    Timber.i("Guest cleared, exiting %s", FragmentSelectOrCreateGuest.class.getSimpleName());
                    getActivity().onBackPressed();
                },
                true);
    }

    private void onGuestDeleteSelected(View v)
    {
        if (getActivity() == null)
        {
            // Fragment has detached from activity
            return;
        }

        ModelAvatar thisModel = (ModelAvatar) v.getTag(R.id.TAG_MODEL);
        String selectedGuestName = thisModel.getGuestName();

        UiUtils.showAlertDialog(
                getActivity(),
                getString(R.string.dialog_delete_guest_title),
                getString(R.string.dialog_delete_guest_message, selectedGuestName),
                getString(android.R.string.yes),
                getString(android.R.string.no),
                (dialog, which) -> deleteGuest(selectedGuestName),
                (dialog, which) ->
                {
                }
        );
    }

    private void deleteGuest(String guestName)
    {
        ProgressDialog loadingDialog = new ProgressDialog(getActivity(), R.style.AlertDialogStyle);
        loadingDialog.setCancelable(false);
        loadingDialog.setIndeterminate(true);
        loadingDialog.setMessage(getString(R.string.delete_guest_progress));
        loadingDialog.show();

        AsyncHelper.run(
                () -> processGuestDeletion(guestName),
                loadingDialog::dismiss,
                true
        );

    }

    private void processGuestDeletion(String guestName)
    {
        // Never allow a blank guest name otherwise this will delete avatars for the main user.
        if (TextUtils.isEmpty(guestName) || guestName.trim().isEmpty())
        {
            Timber.e("No guest name specified to delete avatars for.");
            return;
        }

        StringBuilder escapedStringBuilder = new StringBuilder("miscGuest = ");

        // Escape the guest name to handle any quotation marks that may have been entered
        DatabaseUtils.appendEscapedSQLString(escapedStringBuilder, guestName);

        ArrayList<ModelAvatar> avatarsForThisGuest = ORMTable.getModelList(
                ModelAvatar.class,
                ModelAvatar.getWhereWithGuestAvatars(escapedStringBuilder.toString()),
                ""
        );

        if (avatarsForThisGuest == null)
        {
            Timber.e("Cannot delete guest. List of guests for that user is null.");
            return;
        }

        Timber.i("Deleting %d avatars for guest: %s", avatarsForThisGuest.size(), guestName);

        // Delete all the avatars for the guest
        for (ModelAvatar avatar : avatarsForThisGuest)
        {
            CompletableFuture<Void> future = new CompletableFuture<>();

            MyFiziqSdkManager.deleteAvatar(
                    avatar,
                    (resultCode, message, success) -> future.complete(null)
            );

            try
            {
                // Wait here until we've deleted the avatar
                future.get();
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        // Clear the selected guest
        GuestHelper.persistGuestSelection("");
    }
}
