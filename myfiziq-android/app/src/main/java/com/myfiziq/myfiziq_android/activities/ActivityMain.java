package com.myfiziq.myfiziq_android.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.helpers.NotificationHelper;
import com.myfiziq.myfiziq_android.helpers.SignInHelper;
import com.myfiziq.myfiziq_android.lifecycle.AvatarRetryReceiver;
import com.myfiziq.myfiziq_android.routes.AvatarSelectorRouteExecutor;
import com.myfiziq.myfiziq_android.routes.HomepageRouteGenerator;
import com.myfiziq.myfiziq_android.routes.LogoutRouteGenerator;
import com.myfiziq.myfiziq_android.routes.OnboardingRouteGenerator;
import com.myfiziq.myfiziq_android.routes.ReinitialiseSdkRouteGenerator;
import com.myfiziq.myfiziq_android.routes.SettingsRouteGenerator;
import com.myfiziq.myfiziq_android.routes.SupportRouteGenerator;
import com.myfiziq.myfiziq_android.routes.TrackRouteGenerator;
import com.myfiziq.myfiziq_android.routes.ViewAllRouteGenerator;
import com.myfiziq.myfiziq_android.routes.ViewAvatarRouteGenerator;
import com.myfiziq.sdk.MyFiziqAvatarDownloadManager;
import com.myfiziq.sdk.activities.ActivityInterface;
import com.myfiziq.sdk.activities.BaseActivity;
import com.myfiziq.sdk.activities.DeferredOperation;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.enums.IntentRequests;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.enums.StatusBarStyle;
import com.myfiziq.sdk.enums.SupportType;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.DateOfBirthCoordinator;
import com.myfiziq.sdk.helpers.DialogHelper;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.helpers.StatusBarHelper;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.intents.MyFiziqBroadcastReceiver;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.lifecycle.PendingMessageRepository;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.MYQBottomNavigationView;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import timber.log.Timber;

public class ActivityMain extends BaseActivity implements BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener
{
    private Toolbar mainToolbar;
    private MYQBottomNavigationView bottomNav;

    private ProgressDialog loadingDialog;
    private Menu actionBarMenu;

    private IntentManagerService<ParameterSet> intentManagerService;
    private List<MyFiziqBroadcastReceiver> registeredReceivers = new LinkedList<>();

    private boolean isPaused = false;

    private ModelAvatar mAvatar;
    private String mUserGender;
    private String mUserHeight;
    private String mUserWeight;
    private String mPreferredHeightUnits;
    private String mPreferredWeightUnits;
    private String mDisplayMode;

    private enum DisplayMode
    {
        list,
        capture,
        track
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        if (ModelSetting.getSetting(ModelSetting.Setting.DEBUG_STYLING, false))
        {
            String json = ModelSetting.getSetting(ModelSetting.Setting.STYLE, "");
            if (!TextUtils.isEmpty(json))
            {
                SisterColors.getInstance().init(json);
            }
        }
        */

        mainToolbar = findViewById(R.id.mainToolbar);
        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnNavigationItemSelectedListener(this);
        bottomNav.setOnNavigationItemReselectedListener(this);
        initToolbar();

        intentManagerService = new IntentManagerService<>(this);

        bindRouteGeneratorListeners();
        bindBottomNavigationBarListeners();
        bindLifecycleListeners();

        StatusBarHelper.setStatusBarStyle(getActivity(), StatusBarStyle.DEFAULT_LIGHT, getResources().getColor(R.color.myfiziqsdk_status_bar_white));

        mUserGender = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_GENDER, null);
        mUserHeight = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_HEIGHT_IN_CM, null);
        mUserWeight = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_WEIGHT_IN_KG, null);
        mPreferredHeightUnits = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, null);
        mPreferredWeightUnits = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, null);
        mDisplayMode = getStringParamValue(com.myfiziq.sdk.R.id.TAG_ARG_ACTIVITY_VIEW, null);

        initPageState();

        startBackgroundSignIn();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        isPaused = false;

        // Only start checking for avatars if we have finished the sign in process.
        // If we haven't finished signing in, we will start checking in the callback for
        // {@code SignInHelper} once we have signed in.
        if (MyFiziqSdkManager.isSignedIn())
        {
            MyFiziqAvatarDownloadManager.getInstance().startCheckingForAvatars();
        }

        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            Timber.e("SDK is no longer initialised. Trying to restart...");

            // If the SKD is no longer running, go to the entry point activity to reinitialize it
            intentManagerService.requestAndListenForResponse(
                    IntentPairs.REINITIALISE_SDK,
                    result ->
                    {
                        fragmentPopAll();
                        result.start(this);
                    }
            );
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        isPaused = true;

        // Ensure that we stop polling for new avatars if we go to the background
        MyFiziqAvatarDownloadManager.getInstance().stopCheckingForAvatars();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        if (! (getFragment() instanceof BaseFragment))
        {
            return;
        }

        ParameterSet parameterSet = ((BaseFragment)getFragment()).getParameterSet();

        if (parameterSet == null)
        {
            return;
        }

        // Extract the navbar button that we want selected from the ParameterSet when the user presses the back button
        int homeButtonId = parameterSet.getIntParamValue(R.id.TAG_ARG_HOME_BUTTON_SELECTION_ID, -1);

        if (homeButtonId != -1 && findViewById(homeButtonId) != null)
        {
            bottomNav.setCheckedItemId(homeButtonId);
        }
    }

    void initPageState()
    {
        MenuItem navigation_home = bottomNav.getMenu().findItem(R.id.navigation_home);
        MenuItem navigation_profile = bottomNav.getMenu().findItem(R.id.navigation_profile);
        MenuItem navigation_new = bottomNav.getMenu().findItem(R.id.navigation_new);
        MenuItem navigation_track = bottomNav.getMenu().findItem(R.id.navigation_track);
        boolean bItemSelected = false;

        if (!TextUtils.isEmpty(mDisplayMode))
        {
            try
            {
                DisplayMode mode = DisplayMode.valueOf(mDisplayMode);
                switch (mode)
                {
                    case list:
                    {
                        if (null != navigation_profile && navigation_profile.isVisible())
                        {
                            onProfileClicked();
                            bItemSelected = true;
                        }
                    }
                    break;

                    case capture:
                    {
                        if (null != navigation_new && navigation_new.isVisible())
                        {
                            onNewAvatarClicked();
                            bItemSelected = true;
                        }
                    }
                    break;

                    case track:
                    {
                        if (null != navigation_track && navigation_track.isVisible())
                        {
                            onTrackAvatarClicked();
                            bItemSelected = true;
                        }
                    }
                    break;
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        if (!bItemSelected)
        {
            if (getSupportFragmentManager().getFragments().isEmpty())
            {
                if (null != navigation_home && navigation_home.isVisible())
                    onHomeClicked();
                else if (null != navigation_profile && navigation_profile.isVisible())
                    onProfileClicked();
                else if (null != navigation_new && navigation_new.isVisible())
                    onNewAvatarClicked();
            }
        }
    }

    void initToolbar()
    {
        setSupportActionBar(mainToolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // Make sure we don't leak any intents
        intentManagerService.unbindAll();

        for (MyFiziqBroadcastReceiver receiver : registeredReceivers)
        {
            receiver.stopListening();
        }

        registeredReceivers.clear();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                // Send the user back to the previous fragment when they press the back button in the toolbar, if it is visible
                onBackPressed();
                return true;

            case R.id.menu_item_swap:
                intentManagerService.respond(IntentResponses.SWAP_AVATARS, null);
                return true;

            case R.id.action_view_support:
                onViewSupportClicked();
                return true;

            case R.id.action_delete_avatar:
                onDeleteAvatarClicked();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Start listening for when the MyFiziq SDK requests a route that the user should follow.
     * <p>
     * A route is a series of past and future pages (fragments) that the user may follow in the
     * lifecycle of the application.
     * <p>
     * A route may be requested when the user clicks on a button to go to a new page.
     * <p>
     * Customisations to which route the user may follow can be made in the Route Generator.
     */
    private void bindRouteGeneratorListeners()
    {
        HomepageRouteGenerator homepageRouteGenerator = new HomepageRouteGenerator(this, IntentPairs.HOMEPAGE_ROUTE);
        homepageRouteGenerator.startListening();
        registeredReceivers.add(homepageRouteGenerator);

        ViewAllRouteGenerator viewAllRouteGenerator = new ViewAllRouteGenerator(this, IntentPairs.VIEW_ALL_ROUTE);
        viewAllRouteGenerator.startListening();
        registeredReceivers.add(viewAllRouteGenerator);

        ViewAvatarRouteGenerator viewAvatarRouteGenerator = new ViewAvatarRouteGenerator(this, IntentPairs.VIEW_AVATAR_ROUTE);
        viewAvatarRouteGenerator.startListening();
        registeredReceivers.add(viewAvatarRouteGenerator);

        OnboardingRouteGenerator onboardingGenerator = new OnboardingRouteGenerator(this, IntentPairs.ONBOARDING_ROUTE);
        onboardingGenerator.startListening();
        registeredReceivers.add(onboardingGenerator);

        TrackRouteGenerator trackGenerator = new TrackRouteGenerator(this, IntentPairs.TRACK_ROUTE);
        trackGenerator.startListening();
        registeredReceivers.add(trackGenerator);

        SupportRouteGenerator supportGenerator = new SupportRouteGenerator(this, IntentPairs.SUPPORT_ROUTE);
        supportGenerator.startListening();
        registeredReceivers.add(supportGenerator);

        SettingsRouteGenerator settingsGenerator = new SettingsRouteGenerator(this, IntentPairs.SETTINGS_ROUTE);
        settingsGenerator.startListening();
        registeredReceivers.add(settingsGenerator);

        LogoutRouteGenerator logoutRouteGenerator = new LogoutRouteGenerator(this, IntentPairs.LOGOUT_ROUTE);
        logoutRouteGenerator.startListening();
        registeredReceivers.add(logoutRouteGenerator);

        ReinitialiseSdkRouteGenerator reinitialiseSdkRouteGenerator = new ReinitialiseSdkRouteGenerator(this, IntentPairs.REINITIALISE_SDK);
        reinitialiseSdkRouteGenerator.startListening();
        registeredReceivers.add(reinitialiseSdkRouteGenerator);

        AvatarSelectorRouteExecutor avatarSelectorRouteExecutor = new AvatarSelectorRouteExecutor(this, IntentPairs.AVATAR_SELECTOR);
        avatarSelectorRouteExecutor.startListening();
        registeredReceivers.add(avatarSelectorRouteExecutor);
    }

    /**
     * Listens for when MyFiziq sends users to a new page so we can update the Bottom Navigation Bar
     * to highlight which page the user is currently viewing.
     * <p>
     * For example, the user may press the back button during the Avatar capture process which would
     * send them to the homepage. The bottom navigation bar should then highlight the homepage
     * button to reflect this.
     */
    private void bindBottomNavigationBarListeners()
    {
        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_HOMEPAGE_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_home)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_VIEW_AVATAR_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_home)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_VIEW_ALL_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_profile)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_ONBOARDING_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_new)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_TRACK_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_track)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.NEW_SETTINGS_ROUTE,
                v -> bottomNav.setCheckedItemId(R.id.navigation_settings)
        );
    }

    /**
     * Intercepts certain lifecycle events and performs an action when triggered.
     */
    private void bindLifecycleListeners()
    {
        intentManagerService.listenIndefinitely(
                IntentResponses.MYFIZIQ_ACTIVITY_FINISHING,
                v -> onMyFiziqActivityFinishing()
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.SHOW_SWAP_MENU_BUTTON,
                v -> onSwapButtonSetVisibility(true, R.id.menu_item_swap)
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.HIDE_SWAP_MENU_BUTTON,
                v -> onSwapButtonSetVisibility(false, R.id.menu_item_swap)
        );

        intentManagerService.listenIndefinitely(
                IntentResponses.SHOW_VIEW_SUPPORT_BUTTON,
                v ->
                {
                    if (!SisterColors.getInstance().isSisterMode())
                    {
                        onSwapButtonSetVisibility(true, R.id.action_view_support);
                    }
                    onSwapButtonSetVisibility(true, R.id.action_delete_avatar);
                }
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.HIDE_VIEW_SUPPORT_BUTTON,
                v ->
                {
                    onSwapButtonSetVisibility(false, R.id.action_view_support);
                    onSwapButtonSetVisibility(false, R.id.action_delete_avatar);
                }
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.SHOW_BOTTOM_NAVIGATION_BAR,
                v -> setBottomNavigationBarVisibility(View.VISIBLE)
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.HIDE_BOTTOM_NAVIGATION_BAR,
                v -> setBottomNavigationBarVisibility(View.GONE)
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.CAPTURE_PROCESSING_ERROR,
                this::handleCaptureProcessingError
        );
        intentManagerService.listenIndefinitely(
                IntentResponses.NAVIGATE_HOME,
                v -> onHomeClicked()
        );
        intentManagerService.listenIndefinitely(
                IntentRequests.LOGOUT_CLICKED,
                v -> LogoutRouteGenerator.onLogoutClicked(this, IntentPairs.LOGOUT_CLICKED)
        );
    }

    private void onHomeClicked()
    {
        intentManagerService.requestAndListenForResponse(
                IntentPairs.HOMEPAGE_ROUTE,
                result -> {
                    injectNavBarSelectionIntoParameterSet(result, R.id.navigation_home);
                    startRouteFragment(result, this, false);
                }
        );
    }

    private void onTrackAvatarClicked()
    {
        intentManagerService.requestAndListenForResponse(
                IntentPairs.TRACK_ROUTE,
                result ->  {
                    injectNavBarSelectionIntoParameterSet(result, R.id.navigation_track);
                    startRouteFragment(result, this, false);
                }
        );
    }

    private void onViewSupportClicked()
    {
        ModelAvatar modelAvatar = (ModelAvatar) PendingMessageRepository.getPendingMessage(IntentResponses.MESSAGE_MODEL_AVATAR);
        if (modelAvatar == null)
        {
            Timber.e("ModelAvatar intended for support activity is null");
            return;
        }

        ParameterSet parameterSet = new ParameterSet.Builder()
                .addParam(new Parameter(R.id.TAG_ARG_VIEW, SupportType.VIEW_SUPPORT))
                .addParam(new Parameter(R.id.TAG_ARG_MODEL_AVATAR, modelAvatar))
                .build();

        new IntentManagerService<ParameterSet>(this).requestAndListenForResponse(
                IntentPairs.SUPPORT_ROUTE,
                parameterSet,
                result -> result.start(this)
        );
    }

    private void onNewAvatarClicked()
    {
        ParameterSet.Builder builder = new ParameterSet.Builder();
        if (!TextUtils.isEmpty(mUserGender))
            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_GENDER, mUserGender));
        if (!TextUtils.isEmpty(mUserWeight))
            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_WEIGHT_IN_KG, mUserWeight));
        if (!TextUtils.isEmpty(mUserHeight))
            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_HEIGHT_IN_CM, mUserHeight));
        if (!TextUtils.isEmpty(mPreferredHeightUnits))
            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS, mPreferredHeightUnits));
        if (!TextUtils.isEmpty(mPreferredWeightUnits))
            builder.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS, mPreferredWeightUnits));

        new IntentManagerService<ParameterSet>(this).requestAndListenForResponse(
                IntentPairs.ONBOARDING_ROUTE,
                builder.build(),
                result -> {
                    injectNavBarSelectionIntoParameterSet(result, R.id.navigation_new);
                    startRouteFragment(result, this, false);
                }
        );
    }

    private void onProfileClicked()
    {
        intentManagerService.requestAndListenForResponse(
                IntentPairs.VIEW_ALL_ROUTE,
                result -> {
                    injectNavBarSelectionIntoParameterSet(result, R.id.navigation_profile);
                    startRouteFragment(result, this, false);
                }
        );
    }

    private void onSettingsClicked()
    {
        intentManagerService.requestAndListenForResponse(
                IntentPairs.SETTINGS_ROUTE,
                result -> {
                    injectNavBarSelectionIntoParameterSet(result, R.id.navigation_settings);
                    startRouteFragment(result, this, false);
                }
        );
    }

    private void startRouteFragment(ParameterSet result, ActivityInterface activity, boolean addToBackStack)
    {
        if (isFinishing() || isPaused)
        {
            // Activity is finishing or is paused.
            // Don't navigate to any routes in case a broadcast arrives when we're sleeping.
            return;
        }


        result.start(activity, addToBackStack);
    }

    private void onSignOut()
    {
        Intent loginActivity = new Intent(this, ActivityLogin.class);
        loginActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginActivity);
    }

    private void onMyFiziqActivityFinishing()
    {
        postDeferredOperation(new DeferredOperation()
        {
            @Override
            public void execute()
            {
                intentManagerService.requestAndListenForResponse(
                        IntentPairs.VIEW_ALL_ROUTE,
                        route -> route.start(ActivityMain.this));
            }
        });
    }

    private void onSwapButtonSetVisibility(boolean visible, int item)
    {
        if (null != actionBarMenu)
        {
            MenuItem swapMenuItem = actionBarMenu.findItem(item);
            swapMenuItem.setVisible(visible);
        }
    }

    private void setBottomNavigationBarVisibility(int visibility)
    {
        if (null != bottomNav)
        {
            bottomNav.setVisibility(visibility);
        }
    }

    private void handleCaptureProcessingError(ParameterSet parameterSet)
    {
        /*PendingIntent intent = new PendingIntent();

        NotificationCompat.Action[] actions = {
            new NotificationCompat.Action(android.R.drawable.stat_notify_error, "Retry", intent)
        };*/

        int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        Intent retryAvatarIntent = new Intent(this, AvatarRetryReceiver.class);
        retryAvatarIntent.setAction(AvatarRetryReceiver.ACTION);
        retryAvatarIntent.putExtra(String.valueOf(R.id.TAG_MODEL_ID), parameterSet);
        retryAvatarIntent.putExtra(String.valueOf(R.id.TAG_NOTIFICATION_ID), notificationId);


        // I must have a unique "requestCode" and have a flag set. Otherwise the intent data won't appear when the PendingIntent is executed
        PendingIntent retryAvatarPendingIntent = PendingIntent.getBroadcast(this, notificationId, retryAvatarIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action[] actions = {
                new NotificationCompat.Action(R.drawable.ic_replay, "Retry", retryAvatarPendingIntent)
        };

        NotificationHelper.showNotification(
                getContext(),
                notificationId,
                android.R.drawable.stat_notify_error,
                getString(R.string.notification_category_errors),
                getString(R.string.error_title),
                getString(R.string.error_upload_failed),
                actions
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        actionBarMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_swap, menu);
        ActionBarHelper.applyActionBarColors(actionBarMenu,mainToolbar);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.navigation_home:
                onHomeClicked();
                break;

            case R.id.navigation_profile:
                onProfileClicked();
                break;

            case R.id.navigation_new:
                onNewAvatarClicked();
                break;

            case R.id.navigation_track:
                onTrackAvatarClicked();
                break;

            case R.id.navigation_settings:
                onSettingsClicked();
                break;
        }

        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture)
    {

    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item)
    {

    }


    private void navigateViewAll()
    {
        new IntentManagerService<ParameterSet>(this).requestAndListenForResponse(
                IntentPairs.VIEW_ALL_ROUTE,
                result -> result.start(this)
        );
    }

    public void onDeleteAvatarClicked()
    {
        UiUtils.showAlertDialog(
                this,
                getString(R.string.delete_avatar),
                getString(R.string.permanent_delete),
                getString(R.string.continueText),
                getString(R.string.cancel),
                (dialog, which) ->
                {
                    ModelAvatar modelAvatar = (ModelAvatar) PendingMessageRepository.getPendingMessage(IntentResponses.MESSAGE_MODEL_AVATAR);
                    if (modelAvatar == null)
                    {
                        onDeleteAvatarFailed();
                        return;
                    }
                    loadingDialog = new ProgressDialog(this, R.style.AlertDialogStyle);
                    loadingDialog.setCancelable(false);
                    loadingDialog.setMessage(getString(R.string.wait_delete));
                    loadingDialog.setIndeterminate(true);
                    loadingDialog.show();
                    MyFiziqSdkManager.deleteAvatar(modelAvatar, this::onDeleted);
                },
                (dialog, which) ->
                {
                }
        );
    }

    private void onDeleted(SdkResultCode responseCode, String result, Boolean success)
    {
        if (loadingDialog != null)
        {
            loadingDialog.dismiss();
        }

        if (success)
        {
            navigateViewAll();
        }
        else
        {
            onDeleteAvatarFailed();
        }
    }

    private void onDeleteAvatarFailed()
    {
        UiUtils.showMsgDialog(
                this,
                getString(R.string.error_delete_msg),
                (dialog1, which1) -> navigateViewAll()
        );
    }

    // Remember the selected navbar button as the user moves through the back stack
    private void injectNavBarSelectionIntoParameterSet(ParameterSet parameterSet, @IdRes int navBarItemId)
    {
        if (parameterSet != null && !parameterSet.hasParam(R.id.TAG_ARG_HOME_BUTTON_SELECTION_ID))
        {
            Parameter parameter = new Parameter(R.id.TAG_ARG_HOME_BUTTON_SELECTION_ID, navBarItemId);
            parameterSet.addParam(parameter);
        }
    }

    private void startBackgroundSignIn()
    {
        // Refresh the sign in state and AWS tokens when we open the app to make sure that we're still signed in
        SignInHelper.getInstance().refreshSignInState(this, (responseCode, result) ->
        {
            if (responseCode.isOk())
            {
                if (!isPaused)
                {
                    // Only start checking for avatars if the window has focus. i.e. don't start checking for avatars if we're in the background...
                    // If we don't have focus, we'll start checking in onResume()
                    MyFiziqAvatarDownloadManager.getInstance().startCheckingForAvatars();
                }
            }
            else if (responseCode.isInternetDown())
            {
                // No internet
                DialogHelper.showInternetDownDialog(this, this::finishAffinity);
            }
            else
            {
                // Session is invalid, sign out the user and send them to the welcome screen
                MyFiziqSdkManager.signOut((responseCode1, result1) ->
                {
                    Timber.e("Session invalid. Sending user to login screen.");
                    startActivityWelcome(this);
                });
            }
        });
    }

    /**
     * Starts the Welcome activity.
     *
     * Since the Welcome activity is the first screen in the workflow, we'll clear the back stack
     * to ensure that the user won't go back to the Splash screen when they press the back button.
     */
    private void startActivityWelcome(Activity activity)
    {
        Intent welcomeActivity = new Intent(activity, ActivityWelcome.class);
        welcomeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        activity.startActivity(welcomeActivity);
    }
}
