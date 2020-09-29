package com.myfiziq.sdk.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

interface ActivityManagerInterface
{
    void setFragment(Fragment fragment);

    Fragment getFragment();

    void onCreate(Bundle savedInstanceState);

    void onStart();

    void onSaveInstanceState(@NonNull Bundle outState);

    boolean isActivityIsRunning();

    void addDeferredOperation(DeferredOperation operation);

    /**
     * Adds an operation to be executed once the activity is running again.
     * @param operation The operation to execute.
     */
    void postDeferredOperation(DeferredOperation operation);

    void runDeferredOperations();
}
