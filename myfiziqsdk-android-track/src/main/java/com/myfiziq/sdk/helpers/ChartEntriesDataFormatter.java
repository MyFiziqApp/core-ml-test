package com.myfiziq.sdk.helpers;

import com.github.mikephil.charting.data.Entry;
import com.myfiziq.sdk.enums.ProgressTimeSpan;
import com.myfiziq.sdk.vo.EntryDataVO;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides helper methods for rolling up raw data so that they can be displayed on a graph for a
 * given time span and and time interval.
 */
public class ChartEntriesDataFormatter
{
    private ChartEntriesDataFormatter()
    {
        // Empty hidden constructor for the utility class
    }

    /**
     * Generates a continuous list of entries.
     * <p>
     * A continuous time span will not have a gap in the graph for missing entries.
     * (i.e. January 1 will be right next to July 15 on the graph).
     *
     * @param originalEntries    The entries to use as the source data.
     * @param dataPointFormatter The formatter used to create a label immediately next to a data point on the graph.
     * @return The continuous list of entries.
     */
    public static List<Entry> generateContinuousListOfEntries(List<Entry> originalEntries,
                                                                   AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatter,
                                                                   AsyncHelper.CallbackOperation<Float, String> dataPointFormatter)
    {
        // Copy the original list of entries to make sure we don't modify the list directly
        ArrayList<Entry> entryBurnDown = new ArrayList<>(originalEntries);

        ArrayList<Entry> rolledUpValues = new ArrayList<>();

        // Keep looping until we have a time span for all entries
        for (int i = 0; i < entryBurnDown.size(); i++)
        {
            float xValue = entryBurnDown.get(i).getX();
            float yValue = entryBurnDown.get(i).getY();

            String xAxisLabel = xAxisLabelFormatter.execute((long) xValue);
            String dataPointLabel = dataPointFormatter.execute(yValue);
            EntryDataVO data = new EntryDataVO(xAxisLabel, dataPointLabel);

            // And save it to the list of values
            Entry newEntry = new Entry(i, yValue, data);
            rolledUpValues.add(newEntry);
        }

        return rolledUpValues;
    }

    public static List<Entry> getLatestValuesForEachTimePeriod(List<Entry> originalEntries,
                                                                    ProgressTimeSpan timeSpan,
                                                                    AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatter,
                                                                    AsyncHelper.CallbackOperation<Float, String> dataPointFormatter) throws ParseException
    {
        // Copy the original list of entries to make sure we don't modify the list directly
        LinkedList<Entry> entryBurnDown = new LinkedList<>(originalEntries);

        LinkedList<Entry> rolledUpValues = new LinkedList<>();

        int i = 0;

        // Keep looping until we have a time span for all entries
        while (!entryBurnDown.isEmpty())
        {
            // Remove the first item from the burn down list now that it's been processed
            Entry dateToFilterBy = entryBurnDown.remove(0);

            long dateToFilterByUnixTimestamp = (long) dateToFilterBy.getX();

            // Gets the LAST value in the timespan we're currently processing and removes all other entries in that time span.
            float lastValueInThisTimeSpan = filterOutTimespanInBurndown(entryBurnDown, dateToFilterBy, timeSpan);


            String timeSpanComparator = getTimestampComparator(dateToFilterByUnixTimestamp, timeSpan);
            Date startOfTimeSpan = timeSpan.getComparator().parse(timeSpanComparator);

            // iOS uses the first day of the week rather than the last day for the weekly graph. So let's copy iOS!
            //Date timeSpanEndPoint = ChartEntriesDataFormatter.getEndPointInTimeSpan(timeSpan, startOfTimeSpan);
            //String xAxisLabel = xAxisLabelFormatter.execute((float) startOfTimeSpan.getTime());

            String xAxisLabel = xAxisLabelFormatter.execute(startOfTimeSpan.getTime());

            // Get the LAST value in the timespan but show the FIRST value's label
            // e.g. for the "weekly" view we show the 31st of December label but use the value
            // of the 6th of January as the "latest" value on the graph
            String dataPointLabel = dataPointFormatter.execute(lastValueInThisTimeSpan);
            EntryDataVO data = new EntryDataVO(xAxisLabel, dataPointLabel);

            // And save it to the list of values
            Entry newEntry = new Entry(i, lastValueInThisTimeSpan, data);
            rolledUpValues.add(newEntry);

            i++;
        }

        return rolledUpValues;
    }

    /**
     * Rolls up entries for a particular time span.
     * <p>
     * For example, if we have 3 measurements:
     * - 1st of Jan - Value: 10
     * - 2nd of Jan - Value: 20
     * - 1st of Feb - Value: 10
     * <p>
     * And we rollup by month, the output will be:
     * - Jan - Value 15
     * - Feb - Value 10
     *
     * @param originalEntries A list of entries to rollup by.
     * @param rollupType      The time span that we should rollup entries against (e.g. rollup and average by day, week, month, etc).
     * @return A list of rolled up values.
     */
    public static List<Entry> rollupValues(List<Entry> originalEntries,
                                                ProgressTimeSpan rollupType,
                                                AsyncHelper.CallbackOperation<Long, String> xAxisLabelFormatter,
                                                AsyncHelper.CallbackOperation<Float, String> dataPointFormatter) throws ParseException
    {
        // Copy the original list of entries to make sure we don't modify the list directly
        ArrayList<Entry> entryBurnDown = new ArrayList<>(originalEntries);

        ArrayList<Entry> rolledUpValues = new ArrayList<>();

        // Keep looping until we have a time span for all entries
        for (int i = 0; i < entryBurnDown.size(); i++)
        {
            long timeSpanUnixTimestamp = (long) entryBurnDown.get(0).getX();
            float averageValuesForThisTimeSpan = calculateAverageValuesForTimeSpan(entryBurnDown, rollupType);

            String timeSpanComparator = getTimestampComparator(timeSpanUnixTimestamp, rollupType);

            Date startOfTimeSpan = rollupType.getComparator().parse(timeSpanComparator);
            Date timeSpanMiddlePoint = ChartEntriesDataFormatter.getMiddlePointInTimeSpan(rollupType, startOfTimeSpan);


            String xAxisLabel = xAxisLabelFormatter.execute(timeSpanMiddlePoint.getTime());
            String dataPointLabel = dataPointFormatter.execute(averageValuesForThisTimeSpan);
            EntryDataVO data = new EntryDataVO(xAxisLabel, dataPointLabel);

            // And save it to the list of rolled up (or "averaged") values
            Entry newEntry = new Entry(i, averageValuesForThisTimeSpan, data);
            rolledUpValues.add(newEntry);
        }

        return rolledUpValues;
    }

    private static float calculateAverageValuesForTimeSpan(ArrayList<Entry> entryBurnDown, ProgressTimeSpan rollupType)
    {
        long timeSpanUnixTimestamp = (long) entryBurnDown.get(0).getX();
        String timeSpanComparator = getTimestampComparator(timeSpanUnixTimestamp, rollupType);

        // Create a list of values for the current time span (e.g. this week or this month)
        List<Float> valuesForThisTimeSpan = new LinkedList<>();

        // Add the current value to the list of values for our current time span
        valuesForThisTimeSpan.add(entryBurnDown.get(0).getY());
        entryBurnDown.remove(0);


        // Loop through all values to find items that are in the same time span (e.g. same week or same month)
        ListIterator<Entry> iterator = entryBurnDown.listIterator();
        while (iterator.hasNext())
        {
            Entry thisElement = iterator.next();
            long thisUnixTimestamp = (long) thisElement.getX();
            Date thisDate = new Date(thisUnixTimestamp);
            String thisComparator = rollupType.getComparator().format(thisDate);

            // If the value we're looking at is in the same time span as to what we're trying to build a list for
            if (timeSpanComparator.equals(thisComparator))
            {
                // Then add it to the list of values for that time span (e.g. that week or that month)
                valuesForThisTimeSpan.add(thisElement.getY());

                // And remove it from the list of values we're yet to process
                iterator.remove();
            }
        }

        // Now that we have a list of values for this time span... (e.g. this week or this month)


        // Get the average value for this time span
        return ChartEntriesDataFormatter.calculateListAverage(valuesForThisTimeSpan);

        //String timeSpanInternalRepresentation = rollupType.getComparator().format(timeSpanMiddlePoint);
    }

    /**
     * Filters out all entries in the burndown list for the given entry's date and timespan.
     *
     * The LATEST value that was filtered out will be returned.
     *
     * @param entryBurnDown The list of entries to filter out.
     * @param entryToFilterBy The date that will be filtered against in this entry.
     * @param timespan The timespan to filter out values for (e.g. week, month, etc).
     * @return The LATEST value that was filtered out in the given timespan.
     */
    private static float filterOutTimespanInBurndown(List<Entry> entryBurnDown, Entry entryToFilterBy, ProgressTimeSpan timespan)
    {
        long latestValueTimestamp = (long) entryToFilterBy.getX();
        float latestValue = entryToFilterBy.getY();

        // Get a comparator for the first item in the remaining burn down list
        String timeSpanComparator = getTimestampComparator(latestValueTimestamp, timespan);

        // Loop through all values to find items that are in the same time span (e.g. same week or same month)
        ListIterator<Entry> iterator = entryBurnDown.listIterator();
        while (iterator.hasNext())
        {
            Entry thisElement = iterator.next();
            long thisUnixTimestamp = (long) thisElement.getX();
            Date thisDate = new Date(thisUnixTimestamp);
            String thisComparator = timespan.getComparator().format(thisDate);

            // If the value we're looking at is in the same time span as to what we're trying to build a list for
            if (timeSpanComparator.equals(thisComparator))
            {
                // Check to see if it's the latest value in that time span
                if (thisUnixTimestamp > latestValueTimestamp)
                {
                    latestValue = thisElement.getY();
                }

                // Remove it from the list of values we're yet to process
                iterator.remove();
            }
        }

        return latestValue;
    }

    private static String getTimestampComparator(long timeSpanUnixTimestamp, ProgressTimeSpan rollupType)
    {
        Date timeSpan = new Date(timeSpanUnixTimestamp);
        return rollupType.getComparator().format(timeSpan);
    }

    /**
     * Gets a middle point in a time span.
     * <p>
     * For example, if we have the 1st of January and want to get the middle point for that month,
     * the middle point will be the 15th of January.
     *
     * @param type              The type of time span to get a middle point for.
     * @param startDateOfPeriod The START of the period that we want to get a middle point for.
     * @return The middle point in that time span based on the starting date of the period.
     */
    private static Date getMiddlePointInTimeSpan(ProgressTimeSpan type, Date startDateOfPeriod)
    {
        long startingTime = startDateOfPeriod.getTime();
        long extraMilliseconds = (type.getDaysInTimeSpan() * 24 * 60 * 60 * 1000) / 2;

        return new Date(startingTime + extraMilliseconds);
    }

    /**
     * Gets the end point in a time span.
     * <p>
     * For example, if we have the 1st of January and want to get the end point for that month,
     * the middle point will be the 31th of January.
     *
     * @param type              The type of time span to get a end point for.
     * @param startDateOfPeriod The START of the period that we want to get a end point for.
     * @return The end point in that time span based on the starting date of the period.
     */
    private static Date getEndPointInTimeSpan(ProgressTimeSpan type, Date startDateOfPeriod)
    {
        long startingTime = startDateOfPeriod.getTime();
        long extraMilliseconds = (type.getDaysInTimeSpan() * 24 * 60 * 60 * 1000);

        return new Date(startingTime + extraMilliseconds);
    }

    /**
     * Calculates the average in a list of values.
     */
    private static float calculateListAverage(List<Float> input)
    {
        if (input == null || input.isEmpty())
        {
            return 0f;
        }

        float sum = 0f;

        for (Float item : input)
        {
            sum += item;
        }

        return sum / input.size();
    }
}
