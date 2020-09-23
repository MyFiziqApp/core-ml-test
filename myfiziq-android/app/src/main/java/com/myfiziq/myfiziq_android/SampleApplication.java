package com.myfiziq.myfiziq_android;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;


// You must implement "CameraXConfig.Provider" in your Application class
public class SampleApplication extends Application implements CameraXConfig.Provider
{
    @NonNull
    @Override
    public CameraXConfig getCameraXConfig()
    {
        // This must exist in your Application class
        return Camera2Config.defaultConfig();
    }
}
