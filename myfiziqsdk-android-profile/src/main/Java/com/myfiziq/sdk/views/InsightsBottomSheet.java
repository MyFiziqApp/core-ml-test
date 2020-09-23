package com.myfiziq.sdk.views;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.myfiziqsdk_android_profile.R;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class InsightsBottomSheet extends BottomSheetDialogFragment
{

    public int rawres;

    public InsightsBottomSheet()
    {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.insights_bottom_sheet, container, false);
        return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        try
        {
            InputStream inputStream = getResources().openRawResource(rawres);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                stringBuilder.append(line).append("\n");
            }
            ((WebView) view.findViewById(R.id.sheetWebview)).loadDataWithBaseURL(null, stringBuilder.toString(), "text/html", "UTF-8", null);
            view.findViewById(R.id.webviewprogress).setVisibility(View.GONE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
