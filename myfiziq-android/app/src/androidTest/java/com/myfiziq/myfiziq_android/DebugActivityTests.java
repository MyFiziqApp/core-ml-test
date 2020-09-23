package com.myfiziq.myfiziq_android;

import com.microsoft.appcenter.espresso.Factory;
import com.microsoft.appcenter.espresso.ReportHelper;
import com.myfiziq.sdk.activities.DebugActivity;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ORMTable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import timber.log.Timber;

import static com.google.common.truth.Truth.assertThat;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DebugActivityTests
{
    // Needed for AppCenter
    @Rule
    public ReportHelper reportHelper = Factory.getReportHelper();


    private Pattern stopwatchTimingsRegex = Pattern.compile(".*?STOPWATCH: (.*?): ([0-9]+) ms");


    @After
    public void TearDown(){
        reportHelper.label("Stopping App");
    }

    @Test
    public void canRunTestTensor() throws InterruptedException
    {
        Long startTime = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);

        ActivityScenario<DebugActivity> scenario = launchDebugActivity();


        scenario.onActivity(activity ->
        {
            Timber.plant(new LoggingTree());

            // Code in here will be run on the UI thread of the activity

            assertThat(activity.getActivity()).isNotNull();

            MyqEspressoUtils.prepareSdk(activity, TestCredentials.USERNAME, TestCredentials.PASSWORD, (responseCode) ->
            {
                if (!responseCode.isOk())
                {
                    Assert.fail("Response code from prepareSdk() was " + responseCode);
                    latch.countDown();
                    return;
                }

                DebugActivity.DebugModel.DebugItem testTensorPassInitTest = DebugActivity.DebugModel.DebugItem.TEST_INIT_TENSOR_PASS;
                DebugActivity.DebugModel testTensorPassInitModel = new DebugActivity.DebugModel(testTensorPassInitTest);

                DebugActivity.DebugModel.DebugItem testTensorPassTest = DebugActivity.DebugModel.DebugItem.TEST_TENSOR_PASS;
                DebugActivity.DebugModel testTensorPassModel = new DebugActivity.DebugModel(testTensorPassTest);

                testTensorPassInitModel.run(testTensorPassInitTest, activity, () ->
                {
                    testTensorPassModel.run(testTensorPassTest, activity, () ->
                    {
                        analyseLogs(startTime);

                        latch.countDown();
                    });
                });
            });
        });

        // Await outside of the UI thread
        latch.await();
    }

    private ActivityScenario<DebugActivity> launchDebugActivity()
    {
        return ActivityScenario.launch(DebugActivity.class);
    }

    private void analyseLogs(Long startTime)
    {
        ArrayList<ModelLog> modelLog = ORMTable.getModelList(ModelLog.class, "timestamp >= " + startTime, "pk DESC");

        if (modelLog == null)
        {
            Assert.fail("Log files are empty. Test did not run!");
            return;
        }

        
        boolean foundTestTensorPassInitTook = false;
        boolean foundTestTensorPassTook = false;
        boolean foundPassed = false;


        for (ModelLog item : modelLog)
        {
            if (item.value.contains("Inspection Error"))
            {
                Assert.fail("Inspection Error received");
                return;
            }
            else if (item.value.contains("Error in Test Tensor"))
            {
                Assert.fail("'Error in Test Tensor' found. Java exception likely occurred. Test failed.");
                return;
            }
            else if (item.value.contains("STOPWATCH"))
            {
                StopwatchData timings = extractTimings(item.value);

                if (item.value.contains("Test Init Tensor Pass took"))
                {
                    foundTestTensorPassInitTook = true;
                }

                if (item.value.contains("Test Tensor Pass took"))
                {
                    foundTestTensorPassTook = true;
                }
            }
            else if (item.value.contains("Failed:"))
            {
                Assert.fail(item.value);
            }
            else if (item.value.contains("Failed Result:"))
            {
                Assert.fail(item.value);
            }
            else if (item.value.contains("Passed:"))
            {
                foundPassed = true;
            }
        }

        Assert.assertTrue("Did not receive a pass result", foundPassed);
        Assert.assertTrue("'Test Init Tensor Pass took' was not found in the log files. Test execution probably failed", foundTestTensorPassInitTook);
        Assert.assertTrue("'Test Tensor Pass took' was not found in the log files. Test execution probably failed", foundTestTensorPassTook);

        Timber.d("All done!");
    }

    private StopwatchData extractTimings(String logEntry)
    {
        Matcher matcher = stopwatchTimingsRegex.matcher(logEntry);

        boolean matches = matcher.matches();

        if (!matches)
        {
            return null;
        }

        String title = matcher.group(1);
        String timeInMsString = matcher.group(2);

        long timeInMs = Long.parseLong(timeInMsString);

        return new StopwatchData(title, timeInMs);
    }

    private class StopwatchData
    {
        public String title;
        public long timeInMs;

        public StopwatchData(String title, long timeInMs)
        {
            this.title = title;
            this.timeInMs = timeInMs;
        }
    }
}
