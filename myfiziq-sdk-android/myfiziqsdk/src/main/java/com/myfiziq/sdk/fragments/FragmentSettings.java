package com.myfiziq.sdk.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.activities.AsyncProgressDialog;
import com.myfiziq.sdk.db.Imperial;
import com.myfiziq.sdk.db.Metric;
import com.myfiziq.sdk.db.ModelUserProfile;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.SystemOfMeasurement;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.views.SettingsRowView;

import java.util.ArrayList;

import timber.log.Timber;

public class FragmentSettings extends BaseFragment
{
    private LinearLayout settingRows;
    private TextView versionNumberLabel;
    private IntentManagerService<ParameterSet> intentManagerService;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        ActionBarHelper.setActionBarTitle(getActivity(), R.string.myfiziqsdk_settings);

        settingRows = view.findViewById(R.id.settingRows);
        versionNumberLabel = view.findViewById(R.id.versionNumber);

        populateVersionNumber();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Get a fresh settings route each time we view this page in case the user changes an
        // Advanced Setting or Guest which causes the list of available settings to become invalid.
        intentManagerService = new IntentManagerService<>(getActivity());
        intentManagerService.requestAndListenForResponse(
                IntentPairs.SETTINGS_ROUTE,
                this::generateSettingRows
        );
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (intentManagerService != null)
        {
            intentManagerService.unbindAll();
        }
    }

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.fragment_settings;
    }

    /**
     * Populates the label at the bottom of the screen indicating which version the app is currently at.
     */
    private void populateVersionNumber()
    {
        try
        {
            String packageName = getActivity().getPackageName();
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(packageName, 0);

            // Don't use getLongVersionCode(). It was added in API 28 and will crash on phones running versions below API 28.
            long versionNumber = packageInfo.versionCode;

            String versionName = packageInfo.versionName;

            String formattedVersionNumber = "v" + versionName + " (" + versionNumber + ")";
            versionNumberLabel.setText(formattedVersionNumber);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Timber.e(e, "Cannot populate version number label");
            versionNumberLabel.setVisibility(View.GONE);
        }
    }

    private void generateSettingRows(ParameterSet parameterSet)
    {
        ArrayList<ParameterSet> nextSets = parameterSet.getNextSets();

        // Clear any existing rows if we're adding them after resuming the app
        settingRows.removeAllViews();


        for (ParameterSet set: nextSets)
        {
            int iconDrawableId = set.getIntParamValue(R.id.TAG_ARG_SETTINGS_ICON, 0);
            int labelId = set.getIntParamValue(R.id.TAG_ARG_SETTINGS_LABEL, 0);
            String label = set.getStringParamValue(R.id.TAG_ARG_SETTINGS_LABEL, "");
            boolean isActionNeedConfirmation = set.getBooleanParamValue(R.id.TAG_ARG_SETTINGS_IS_NEED_CONFIRMATION, false);
            String intentPairCallback = set.getStringParamValue(R.id.TAG_ARG_SETTINGS_INTENT_PAIR_CALLBACK, "");

            SettingsRowView newRow = new SettingsRowView(getActivity());

            if (labelId != 0)
            {
                newRow.setLabel(labelId);
            }
            else
            {
                newRow.setStringLabel(label);
            }

            newRow.setIcon(iconDrawableId);
            newRow.setDestination(getMyActivity(), set);

            if (!TextUtils.isEmpty(intentPairCallback))
            {
                IntentPairs intentPairs = IntentPairs.valueOf(intentPairCallback);
                newRow.setIntentPairCallback(intentPairs);
            }

            settingRows.addView(newRow);
        }

        SettingsRowView measurementUnitsRow = generateMeasurementUnitsRow();
        settingRows.addView(measurementUnitsRow);
    }

    private SettingsRowView generateMeasurementUnitsRow()
    {
        int iconDrawableId = R.drawable.ic_format_align_center_black_24dp;
        SettingsRowView newRow = new SettingsRowView(getActivity());
        newRow.setIcon(iconDrawableId);
        //newRow.setActionNeedConfirmation(false);

        generateSystemOfMeasurementLabel(newRow);

        newRow.setOnClickListener(view ->
        {
            Activity activity = getActivity();

            if (activity == null)
            {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(getString(R.string.set_measurement_units));
            builder.setMessage(getString(R.string.select_units));

            builder.setPositiveButton(getString(R.string.metric), (dialog, which) ->
                    setSystemOfMeasurement(Metric.class, newRow)
            );

            builder.setNegativeButton(getString(R.string.imperial), (dialog, which) ->
                    setSystemOfMeasurement(Imperial.class, newRow)
            );

            builder.setNeutralButton(getString(R.string.myfiziqsdk_cancel), null);

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.getResources().getColor(R.color.myfiziqsdk_black)));
            dialog.show();
        });

        return newRow;
    }

    private void setSystemOfMeasurement(Class<? extends SystemOfMeasurement> systemOfMeasurement, SettingsRowView row)
    {
        ModelUserProfile userProfile = ORMTable.getModel(ModelUserProfile.class, null);

        if (userProfile == null)
        {
            Toast.makeText(getContext(), "Cannot update user profile. Please try again in a few moments", Toast.LENGTH_LONG).show();
            return;
        }

        Class<? extends SystemOfMeasurement> originalSystemOfMeasurement = userProfile.getSystemOfMeasurement();

        userProfile.setSystemOfMeasurement(systemOfMeasurement);
        userProfile.save();


        final AsyncProgressDialog dialog = AsyncProgressDialog.showProgress(getActivity(), "Please Wait...", true, false, null);

        MyFiziqSdkManager.updateUserProfile(userProfile, (resultCode, result) ->
        {
            dialog.dismiss();

            if (resultCode.isOk())
            {
                Timber.i("Successfully updated user profile");
            }
            else
            {
                Timber.e("Cannot update user profile. Result: %s. Message: %s", resultCode, result);
                Toast.makeText(getContext(), "Cannot update user profile", Toast.LENGTH_LONG).show();

                // Rollback to the original system of measurement if we couldn't save the data (e.g. internet down).
                userProfile.setSystemOfMeasurement(originalSystemOfMeasurement);
                userProfile.save();
            }

            generateSystemOfMeasurementLabel(row);
        });
    }

    private void generateSystemOfMeasurementLabel(SettingsRowView row)
    {
        if (AppWideUnitSystemHelper.getAppWideUnitSystemSync() == Imperial.class)
        {
            row.setLabel(R.string.current_imperial);
        }
        else
        {
            row.setLabel(R.string.current_metric);
        }
    }
}
