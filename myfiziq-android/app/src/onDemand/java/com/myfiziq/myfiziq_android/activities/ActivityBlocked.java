package com.myfiziq.myfiziq_android.activities;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;

import com.myfiziq.myfiziq_android.R;
import com.myfiziq.myfiziq_android.views.SplashScreenVideo;
import com.myfiziq.sdk.helpers.AsyncHelper;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityBlocked extends AppCompatActivity
{
    private SplashScreenVideo avatarView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        avatarView = findViewById(R.id.splashScreenVideo);

        AsyncHelper.run(() -> disableLauncher());
        AsyncHelper.run(()-> renderSplashScreenVideo());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        avatarView.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        avatarView.onResume();
    }

    private void disableLauncher()
    {
        try
        {
            PackageManager p = getPackageManager();
            ComponentName componentName = new ComponentName(this, "com.myfiziq.myfiziq_android.activities.ActivityBlocked");
            p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        catch (Throwable t)
        {
        }
    }

    private void renderSplashScreenVideo()
    {
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loading);

        avatarView.setVideoFromUri(this, uri);
        avatarView.setLooping(true);
        avatarView.start();
    }
}
