package com.myfiziq.sdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.jobs.MoveViewJob;
import com.myfiziq.myfiziqsdk_android_track.R;
import com.myfiziq.sdk.enums.ProgressTimeSpan;
import com.myfiziq.sdk.models.MyFiziqChartAttributes;
import com.myfiziq.sdk.models.MyFiziqChartData;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.vo.EntryDataVO;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import timber.log.Timber;

public class MyFiziqChart extends com.github.mikephil.charting.charts.LineChart
{
    private MyFiziqChartAttributes attributes;


    public MyFiziqChart(Context context)
    {
        super(context);
    }

    public MyFiziqChart(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        parseAttributes(attrs, 0);
    }

    public MyFiziqChart(Context context, @Nullable AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        parseAttributes(attrs, defStyle);
    }


    public void handlePageStateChange()
    {
        //animateX(300);
    }

    /**
     * Renders the chart of the screen.
     *
     * @param chartData The data to render.
     */
    public void render(MyFiziqChartData chartData)
    {
        if (attributes == null)
        {
            Timber.e("No styling attributes were specified for the MyFiziqChart");
            return;
        }

        if (chartData == null || chartData.getPrimaryDataSetEntries() == null)
        {
            Timber.e("No chart data was specified for MyFiziqChart");
            return;
        }

        List<Entry> primaryDataSetEntries = chartData.getPrimaryDataSetEntries();
        List<Entry> secondaryDataSetEntries = chartData.getSecondaryDataSetEntries();


        if (primaryDataSetEntries == null)
        {
            Timber.e("You must specify at least the primary data set");
            return;
        }


        LineDataSet primaryDataSet = new LineDataSet(primaryDataSetEntries, "");
        LineDataSet secondaryDataSet = null;

        LineData lineData = new LineData();

        if (secondaryDataSetEntries != null)
        {
            secondaryDataSet = new LineDataSet(secondaryDataSetEntries, "");
            lineData.addDataSet(secondaryDataSet);
        }

        // Add the primary data set last to make it appear on top of the secondary data set
        lineData.addDataSet(primaryDataSet);


        configureChartStyling(chartData, lineData, primaryDataSet, secondaryDataSet);

        setData(lineData);

        invalidate();


        // This MUST be called after invalidate()
        long visibleXRange = attributes.getMaxVisibleValues();
        setVisibleXRangeMaximum(visibleXRange);


        // Set the focus of the graph to the latest X value
        long lastXValue = (long) primaryDataSetEntries.get(primaryDataSetEntries.size() - 1).getX();
        moveViewToX(lastXValue);
    }

    public void destroy()
    {
        mRenderer = null;
        mHighlighter = null;
        mData = null;
        mChartTouchListener = null;

        // Destroys the MoveViewJob singleton which holds references to our Fragment and Activity
        // This prevents a memory leak
        // See: https://github.com/PhilJay/MPAndroidChart/issues/2238
        MoveViewJob.getInstance(null, 0, 0, null, null);
    }


    /**
     * Configures the chart's styling based on the AttributeSet that has been specified with the view.
     *
     * @param lineData The LineData that will be shown to the user.
     * @param primaryDataSet  The primary LineDataSet that will be shown to the user.
     * @param secondaryDataSet  The secondary LineDataSet that will be shown to the user, if specified.
     */
    // TODO Split up this method into smaller ones
    private void configureChartStyling(MyFiziqChartData chartData,
                                       @NonNull LineData lineData,
                                       @NonNull LineDataSet primaryDataSet,
                                       @Nullable LineDataSet secondaryDataSet)
    {
        XAxis xAxis = getXAxis();
        YAxis leftAxis = getAxisLeft();
        YAxis rightAxis = getAxisRight();
        Legend legend = getLegend();


        setExtraLeftOffset(25);
        setExtraRightOffset(25);
        setExtraBottomOffset(20);
        setExtraTopOffset(10);


        Description chartDescription = new Description();
        chartDescription.setText("");
        setDescription(chartDescription);


        setTouchEnabled(true);               // Allows to enable/disable all possible touch-interactions with the chart.
        setDragEnabled(true);                // Enables/disables dragging (panning) for the chart.
        setScaleEnabled(false);              // Enables/disables scaling for the chart on both axes.
        setScaleXEnabled(false);             // Enables/disables scaling on the x-axis.
        setScaleYEnabled(false);             // Enables/disables scaling on the y-axis.
        setPinchZoom(false);                 // If set to true, pinch-zooming is enabled. If disabled, x- and y-axis can be zoomed separately.
        setDoubleTapToZoomEnabled(false);    // Set this to false to disallow zooming the chart via double-tap on it.


        String[] xAxisLabels = new String[primaryDataSet.getEntryCount()];

        for (int i = 0; i < primaryDataSet.getEntryCount(); i++)
        {
            Entry entry = primaryDataSet.getEntryForIndex(i);
            EntryDataVO data = (EntryDataVO) entry.getData();

            xAxisLabels[i] = data.getXAxisLabel();
        }

        IndexAxisValueFormatter formatter = new IndexAxisValueFormatter(xAxisLabels);
        xAxis.setValueFormatter(formatter);

        // X Axis is sequential, so make the interval 1
        xAxis.setGranularity(1);

        // Make the X axis on the bottom, not on the top
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);



        // Set the line and data point colour
        primaryDataSet.setColor(attributes.getPrimaryLineColour());

        primaryDataSet.setCircleColor(attributes.getPrimaryLineCircleColour());
        primaryDataSet.setCircleHoleColor(attributes.getPrimaryLineCircleColour());

        if (attributes.getPrimaryLineDashLength() > 0)
        {
            primaryDataSet.enableDashedLine(attributes.getPrimaryLineLength(), attributes.getPrimaryLineDashLength(), 0);
        }

        primaryDataSet.setLineWidth(attributes.getPrimaryLineWidth());
        primaryDataSet.setCircleRadius(attributes.getPrimaryLineCircleRadius());



        if (secondaryDataSet != null)
        {
            secondaryDataSet.setColor(attributes.getSecondaryLineColour());

            secondaryDataSet.setCircleColor(attributes.getSecondaryLineCircleColour());
            secondaryDataSet.setCircleHoleColor(attributes.getSecondaryLineCircleColour());

            if (attributes.getSecondaryLineDashLength() > 0)
            {
                secondaryDataSet.enableDashedLine(attributes.getSecondaryLineLength(), attributes.getSecondaryLineDashLength(), 0);
            }

            secondaryDataSet.setLineWidth(attributes.getSecondaryLineWidth());
            secondaryDataSet.setCircleRadius(attributes.getSecondaryLineCircleRadius());
        }




        // Disable and hide the right Y axis
        rightAxis.setEnabled(false);


        // Enable the left axis but hide it. This is so we can draw the grid lines for it.
        leftAxis.setEnabled(true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false);


        // Show gridlines
        leftAxis.setDrawGridLines(attributes.isLeftAxisDrawGridLines());
        leftAxis.setGridColor(attributes.getLeftAxisGridColor());
        leftAxis.setDrawGridLinesBehindData(true);


        xAxis.setEnabled(true);
        xAxis.setAxisLineWidth(attributes.getxAxisLineWidth());
        xAxis.setAxisLineColor(attributes.getxAxisLineColor());
        xAxis.setTextSize(attributes.getXAxisTextSize());
        xAxis.setTextColor(attributes.getXAxisTextColor());
        xAxis.setDrawGridLines(false);

        if (attributes.getXAxisTextFontFamily() > 0)
        {
            int fontResourceId = attributes.getXAxisTextFontFamily();
            Typeface font = ResourcesCompat.getFont(getContext(), fontResourceId);

            xAxis.setTypeface(font);
        }


        xAxis.setAxisMinimum(chartData.getXAxisMin());
        xAxis.setAxisMaximum(chartData.getXAxisMax());
        leftAxis.setAxisMinimum(chartData.getLeftAxisMin());
        leftAxis.setAxisMaximum(chartData.getLeftAxisMax());

        // No legend
        legend.setEnabled(false);


        // Remove cross hairs when clicking on the chart
        lineData.setHighlightEnabled(false);


        // Format the text
        lineData.setValueTextSize(attributes.getLineDataValueTextSize());

        if (attributes.getLineDataValueTextFontFamily() > 0)
        {
            int fontResourceId = attributes.getLineDataValueTextFontFamily();
            Typeface font = ResourcesCompat.getFont(getContext(), fontResourceId);

            lineData.setValueTypeface(font);
        }
        else if (attributes.isLineDataValueTypefaceIsBold())
        {
            // No custom font specified, so see if we're trying to make the default font bold
            lineData.setValueTypeface(Typeface.DEFAULT_BOLD);
        }


        if (!TextUtils.isEmpty(attributes.getLineDataValueFormat()))
        {
            DecimalFormat defaultLineDataValueFormatter = new DecimalFormat(attributes.getLineDataValueFormat());

            int lineDataValueTextColor = attributes.getLineDataValueTextColor();
            lineData.setValueTextColor(lineDataValueTextColor);
            lineData.setValueFormatter(new ValueFormatter()
            {
                @Override
                public String getFormattedValue(float value)
                {
                    return defaultLineDataValueFormatter.format(value);
                }

                @Override
                public String getPointLabel(Entry entry)
                {
                    Object entryData = entry.getData();

                    if (entryData == null)
                    {
                        Timber.w("Entry data is empty");
                        return super.getPointLabel(entry);
                    }
                    else if (entryData instanceof EntryDataVO)
                    {
                        EntryDataVO data = (EntryDataVO) entryData;
                        return data.getDataPointLabel();
                    }
                    else
                    {
                        Timber.w("Unknown value in entry data variable. Class: %s", entryData.getClass().getSimpleName());
                        return super.getPointLabel(entry);
                    }
                }
            });
        }
    }

    /**
     * Calculates the number of X Axis labels to show for a given time span between 2 days.
     * <p>
     * If there are more labels than the maximum configured allowed, then return "-1".
     *
     * @param timeSpanType The type of time span to calculate the amount of X Axis labels for.
     *                     For example, if weekly has been specified, get an X Label for every 1 week.
     * @param firstXValue  The first X Value in the range.
     * @param lastXValue   The last X Value in the range.
     */
    private long calculateXAxisLabelCount(ProgressTimeSpan timeSpanType, long firstXValue, long lastXValue)
    {
        long labelCount;

        Date firstDate = new Date(firstXValue);
        Date lastDate = new Date(lastXValue);

        switch (timeSpanType)
        {
            case DAYS:
                labelCount = TimeFormatUtils.calculateDaysBetween(firstDate, lastDate) + 1;
                break;
            case WEEKS:
                labelCount = TimeFormatUtils.calculateWeeksBetween(firstDate, lastDate) + 1;
                break;
            case MONTHS:
                labelCount = TimeFormatUtils.calculateMonthsBetween(firstDate, lastDate) + 1;
                break;
            default:
                throw new UnsupportedOperationException("Data rollup type not implemented: " + timeSpanType);
        }

        if (labelCount > attributes.getDesiredMaxXAxisLabels())
        {
            // If there's too many labels, let the chart library manage the labels for us
            return -1;
        }
        else
        {
            // Show a label for each data point
            return labelCount;
        }
    }

    /**
     * Parses attributes that the user has specified in the layout XML or styles XML file.
     *
     * @param attrs    The attributes of the XML tag that is inflating the view
     * @param defStyle The default style.
     */
    private void parseAttributes(@Nullable AttributeSet attrs, int defStyle)
    {
        if (null != attrs)
        {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MyFiziqChart, 0, defStyle);
            attributes = new MyFiziqChartAttributes().fromTypedArray(typedArray);

            typedArray.recycle();
        }
    }

    /**
     * Sets the content padding. Content padding value is equal to xAxisLabelDistance * paddingMultiplier
     * xAxisLabelDistanceValue may vary, according to timeSpan
     *
     * @param paddingMultiplier The variable for multiplying xAxisLabelDistance.
     *                          This should be between 0 < x < 1
     * @param timeSpanType      The time span.
     * @param xAxis             The xAxis from given chart.
     * @param firstXValue       The minimum value of xAxis
     * @param lastXValue        The maximum value of xAxis
     */
    private void setChartContentPadding(float paddingMultiplier, ProgressTimeSpan timeSpanType,
                                        AxisBase xAxis, float firstXValue, float lastXValue)
    {
        float contentPadding;
        float dayInTimeStamp = 24 * 60 * 60 * 1000;

        switch (timeSpanType)
        {
            case DAYS:
                contentPadding = dayInTimeStamp * paddingMultiplier;
                break;
            case WEEKS:
                contentPadding = 2 * dayInTimeStamp * paddingMultiplier;
                break;
            case MONTHS:
                contentPadding = 3 * dayInTimeStamp * paddingMultiplier;
                break;
            default:
                throw new UnsupportedOperationException("Unknown time span: " + timeSpanType);
        }

        //Set axis minimum and maximum
        xAxis.setAxisMinimum(firstXValue - contentPadding);
        xAxis.setAxisMaximum(lastXValue + contentPadding);
    }
}
