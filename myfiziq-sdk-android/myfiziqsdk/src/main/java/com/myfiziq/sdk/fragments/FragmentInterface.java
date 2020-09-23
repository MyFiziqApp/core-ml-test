package com.myfiziq.sdk.fragments;

import android.view.View;

import com.myfiziq.sdk.activities.ActivityInterface;

import androidx.annotation.Nullable;

/**
 * @hide
 */
public interface FragmentInterface
{
    boolean onBackPressed();

    @Nullable
    ActivityInterface getMyActivity();

    boolean checkPermissions(String... permissions);

    boolean allPermissionsGranted(int[] grantResults);

    void askPermissions(int requestCode, String reason, String... permissions);

    void fragmentPopSelf();

    void fragmentPopAll();

    void applyParameters(View rootView);

    int getParamMapAsInt(int paramId);

    String getParamMapAsString(int paramId);
}
