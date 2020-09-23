package com.myfiziq.myfiziq_android.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.google.android.material.tabs.TabLayout;
import com.myfiziq.myfiziq_android.BuildConfig;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.activities.BaseActivity;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.helpers.AsyncHelper;

import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class ActivityAdvancedSettings extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setTitle(getString(R.string.advanced_settings));
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }

        ArrayList<DisplayedSetting> experimentalSettings = new ArrayList<>();

        ArrayList<DisplayedSetting> internalSettings = new ArrayList<>();
        internalSettings.add(new DisplayedSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, getString(R.string.always_pass_inspection)));
        internalSettings.add(new DisplayedSetting(ModelSetting.Setting.FEATURE_PRACTISE_MODE, getString(R.string.practise_mode)));
        internalSettings.add(new DisplayedSetting(ModelSetting.Setting.FEATURE_GUEST_USERS, getString(R.string.guest_users), checked ->
        {
            // Clear the guest when toggling on or off so we don't see the selected guest's avatars
            // when we turn off the guest functionality.
            ORMDbFactory.getInstance().setGuestUser("");
        }));
        internalSettings.add(new DisplayedSetting(ModelSetting.Setting.FEATURE_INSIGHTS_VIEW_AVATAR, getString(R.string.view_avatar_insights)));
        internalSettings.add(new DisplayedSetting(ModelSetting.Setting.FEATURE_INSIGHTS_INPUT, getString(R.string.ethnicity_selection)));

        ArrayList<DisplayedSetting> developerSettings = new ArrayList<>();
        developerSettings.add(new DisplayedSetting(ModelSetting.Setting.DEBUG_VISUALIZE_POSE, getString(R.string.visualize_pose)));

        ArrayList<DisplayedSetting> settingsList = new ArrayList<>(internalSettings);

        RecyclerView recyclerView = findViewById(R.id.experimental_settings_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        TogglesAdapter adapter = new TogglesAdapter(settingsList);
        recyclerView.setAdapter(adapter);

        TabLayout mTabs = findViewById(R.id.settings_tabs);
        mTabs.addTab(mTabs.newTab().setText(R.string.internal).setTag(R.string.internal));
        mTabs.addTab(mTabs.newTab().setText(R.string.experimental).setTag(R.string.experimental));

        if (BuildConfig.DEBUG)
        {
            mTabs.addTab(mTabs.newTab().setText(R.string.developer).setTag(R.string.developer));
        }

        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                int tagNumber = (int) tab.getTag();
                settingsList.clear();
                switch (tagNumber)
                {
                    case R.string.internal:
                        settingsList.addAll(internalSettings);
                        break;
                    case R.string.experimental:
                        settingsList.addAll(experimentalSettings);
                        break;
                    case R.string.developer:
                        settingsList.addAll(developerSettings);
                        break;
                    default:
                        Timber.e("invalid tab tag selected");
                        return;
                }

                recyclerView.setAdapter(adapter);

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    static class DisplayedSetting
    {
        ModelSetting.Setting setting;
        String displayName;
        AsyncHelper.Callback<Boolean> onChangedCallback;

        DisplayedSetting(ModelSetting.Setting setting, String displayName)
        {
            this.setting = setting;
            this.displayName = displayName;
        }

        DisplayedSetting(ModelSetting.Setting setting, String displayName, AsyncHelper.Callback<Boolean> onChangedCallback)
        {
            this.setting = setting;
            this.displayName = displayName;
            this.onChangedCallback = onChangedCallback;
        }
    }

    static class ToggleHolder extends RecyclerView.ViewHolder
    {
        Switch settingSwitch;
        DisplayedSetting displayedSetting;

        ToggleHolder(View view)
        {
            super(view);
            settingSwitch = view.findViewById(R.id.setting_toggle);
        }
    }

    static class TogglesAdapter extends RecyclerView.Adapter
    {

        ArrayList<DisplayedSetting> settingsList;

        TogglesAdapter(ArrayList<DisplayedSetting> settingsList)
        {
            this.settingsList = settingsList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View listItem = layoutInflater.inflate(R.layout.toggle_list_item, parent, false);
            return new ToggleHolder(listItem);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            ToggleHolder toggleHolder = (ToggleHolder) holder;
            toggleHolder.displayedSetting = settingsList.get(position);
            toggleHolder.settingSwitch.setText(settingsList.get(position).displayName);

            ModelSetting.getSettingAsync(
                    toggleHolder.displayedSetting.setting,
                    false,
                    result ->
                    {
                        toggleHolder.settingSwitch.setChecked(result);
                        toggleHolder.settingSwitch.setOnCheckedChangeListener((view, checked) ->
                                {
                                    AsyncHelper.run(() -> ModelSetting.putSetting(toggleHolder.displayedSetting.setting, checked));

                                    if (toggleHolder.displayedSetting.onChangedCallback != null)
                                    {
                                        toggleHolder.displayedSetting.onChangedCallback.execute(checked);
                                    }
                                }
                        );
                    }
            );
        }

        @Override
        public int getItemCount()
        {
            return settingsList.size();
        }
    }
}
