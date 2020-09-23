package com.myfiziq.sdk.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.models.MyFiziqChartData;
import com.myfiziq.sdk.views.MyFiziqChart;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FragmentTrackChart extends Fragment
{
    private MyFiziqChart mLineChart;

    private MyFiziqChartData chartData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_track_chart, container, false);

        parseArguments(getArguments());

        mLineChart = view.findViewById(R.id.chart);

        if (chartData != null && chartData.getPrimaryDataSetEntries() != null && !chartData.getPrimaryDataSetEntries().isEmpty())
        {
            mLineChart.render(chartData);
        }

        applySisterStyling();

        return view;
    }

    @Override
    public void setArguments(Bundle bundle)
    {
        super.setArguments(bundle);
        parseArguments(bundle);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Destroy the chart to prevent it from leaking
        // BEWARE! The MPAndroidChart library has more leaks than a sieve
        chartData = null;
        mLineChart.destroy();
    }

    public void handlePageStateChange()
    {
        mLineChart.handlePageStateChange();
    }

    /**
     * Parses any incoming arguments and saves the data for later use.
     */
    private void parseArguments(Bundle bundle)
    {
        if (null != bundle)
        {
            ParameterSet set = bundle.getParcelable(BaseFragment.BUNDLE_PARAMETERS);

            if (null != set)
            {
                this.chartData = (MyFiziqChartData) set.getParam(R.id.TAG_CHART_DATA).getParcelableValue();
            }
        }
    }

    private void applySisterStyling()
    {
        SisterColors sisterColors = SisterColors.getInstance();

        if (sisterColors.getChartLineColor() != null)
        {
            LineData lineData = mLineChart.getLineData();

            if (lineData != null && lineData.getDataSets() != null
                    && !lineData.getDataSets().isEmpty()
                    && lineData.getDataSets().get(0) instanceof LineDataSet)
            {
                LineDataSet dataSet = (LineDataSet) lineData.getDataSets().get(0);
                dataSet.setDrawCircleHole(false);
                dataSet.setValueTextColor(sisterColors.getChartLineColor());
                dataSet.setColor(sisterColors.getChartLineColor());
                dataSet.setCircleColor(sisterColors.getChartLineColor());
            }
        }
    }
}