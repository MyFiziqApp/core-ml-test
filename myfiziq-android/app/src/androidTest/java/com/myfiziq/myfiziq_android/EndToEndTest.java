package com.myfiziq.myfiziq_android;

import android.Manifest;
import android.widget.DatePicker;

import com.microsoft.appcenter.espresso.Factory;
import com.microsoft.appcenter.espresso.ReportHelper;
import com.myfiziq.myfiziq_android.activities.ActivityEntrypoint;
import com.myfiziq.sdk.db.ModelSetting;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by Muhammad Naufal Azzaahid on 25/07/2019.
 */
@RunWith(AndroidJUnit4.class)
public class EndToEndTest
{
    //Height Categories :
    // 1. Centimeters.     Format Value = 165 cm
    // 2. Feet.            Format Value = 6' 4"
    private final String HEIGHT_CATEGORIES = "Centimeters";
    private final String HEIGHT_VALUE = "180 cm";

    //Weight Categories :
    // 1. Kilograms.       Format Value = 80           Format Fraction Value = .0 kg
    // 2. Pounds.          Format Value = 175 lbs      Format Fraction Value = none
    // 3. Stone.           Format Value = 12 st        Format Fraction Value = 8 lbs
    private final String WEIGHT_CATEGORIES = "Kilograms";
    private final String WEIGHT_VALUE = "80";
    private final String WEIGHT_FORMAT_VALUE = ".0 kg";

    //Date Input
    private final int BIRTH_DAY = 15;
    private final int BIRTH_MONTH = 7;
    private final int BIRTH_YEAR = 1994;

    //Gender Input Input
    private final boolean IS_MALE = true;

    //Skip Onboarding
    private final boolean IS_ONBOARDING_SKIPPED = false;

    @Rule
    public ActivityTestRule<ActivityEntrypoint> mActivityRule = new ActivityTestRule<>(ActivityEntrypoint.class);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    //Needed for AppCenter
    @Rule
    public ReportHelper reportHelper = Factory.getReportHelper();

    @After
    public void TearDown()
    {
        reportHelper.label("Stopping App");
    }

    private boolean isUserLoggedIn = false;

    @Test
    public void runSampleTest()
    {
        onSplashScreen();
        if (isUserLoggedIn)
        {
            //Automate Logout User First
            onMainScreenToSettings();
            onSettingsScreen();
        }
        onWelcomeScreen();
        onTermsOfServiceScreen();
        onLoginScreen();
        onMainScreenToCreateAvatar();
        onCreateAvatarScreen();
        onConsentImageScreen();
        onOnboardingScreen();
        onCaptureConfirmationScreen();
        onProfileListScreen();
        onMainScreenDeleteAvatar();
    }

    private void onSplashScreen()
    {
        //Check if the user is logged in or not.
        isUserLoggedIn = TestUtils.isUserLoggedIn();
    }

    private void onWelcomeScreen()
    {
        TestUtils.checkerWithTimeout(R.id.loginButton, isDisplayed());

        //Click Login Button
        onView(withId(R.id.loginButton)).check(matches(isDisplayed())).perform(click());
    }

    private void onTermsOfServiceScreen()
    {
        //Click Agreement Button
        TestUtils.checkerWithTimeout(R.id.iAgreeCheckbox, isDisplayed());
        TestUtils.performActionIfViewExist(R.id.iAgreeCheckbox, click());

        //Click Agreement Button
        TestUtils.checkerWithTimeout(R.id.continueButton, isDisplayed());
        TestUtils.performActionIfViewExist(R.id.continueButton, click());
    }

    private void onLoginScreen()
    {
        TestUtils.checkerWithTimeout(R.id.etxtEmailAddress, isDisplayed());

        //Enter Username
        onView(withId(R.id.etxtEmailAddress)).check(matches(isDisplayed())).perform(clearText());
        onView(withId(R.id.etxtEmailAddress)).check(matches(isDisplayed())).perform(replaceText(TestCredentials.USERNAME), closeSoftKeyboard());

        //Enter Password
        onView(withId(R.id.etxtPassword)).check(matches(isDisplayed())).perform(replaceText(TestCredentials.PASSWORD), closeSoftKeyboard());

        //Click Login Button
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed())).perform(click());
    }

    private void onMainScreenToSettings()
    {
        TestUtils.checkerWithTimeout(R.id.navigation_settings, isDisplayed(), 60000);

        onView(withId(R.id.navigation_settings)).check(matches(isDisplayed())).perform(click());
    }

    private void onSettingsScreen()
    {
        TestUtils.checkerWithTimeout(withText("Logout"), isDisplayed());

        onView(withText("Logout")).check(matches(isDisplayed())).perform(click());

        onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());
    }


    private void onMainScreenToCreateAvatar()
    {
        TestUtils.checkerWithTimeout(R.id.navigation_new, isDisplayed(), 60000);

        onView(withId(R.id.navigation_new)).check(matches(isDisplayed())).perform(click());
    }

    private void onCreateAvatarScreen()
    {
        TestUtils.checkerWithTimeout(R.id.heightEditText, isDisplayed());

        //-----Start of height measurement dialog----//
        onView(withId(R.id.heightEditText)).check(matches(isDisplayed())).perform(click());

        TestUtils.clickAndCheckSpinner(R.id.unitsOfMeasurement, android.R.id.text1, HEIGHT_CATEGORIES);
        TestUtils.selectNumberPickerValue(R.id.measurement, HEIGHT_VALUE);

        onView(withId(android.R.id.button1)).perform(click());
        //------------------------------------------//

        //-----Start of weight measurement dialog----//
        onView(withId(R.id.weightEditText)).check(matches(isDisplayed())).perform(click());

        TestUtils.clickAndCheckSpinner(R.id.unitsOfMeasurement, android.R.id.text1, WEIGHT_CATEGORIES);
        TestUtils.selectNumberPickerValue(R.id.measurement, WEIGHT_VALUE);
        TestUtils.selectNumberPickerValue(R.id.measurement_second, WEIGHT_FORMAT_VALUE);

        onView(withId(android.R.id.button1)).perform(click());
        //------------------------------------------//

        //-----Start of date dialog-------//
        onView(withId(R.id.dateOfBirth)).check(matches(isDisplayed())).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(BIRTH_YEAR, BIRTH_MONTH, BIRTH_DAY));

        onView(withId(android.R.id.button1)).perform(click());
        //--------------------------------//

        //-------Radio button check--------//
        if (IS_MALE)
        {
            onView(withId(R.id.genderMale)).check(matches(isDisplayed()));
            onView(withId(R.id.genderMale)).perform(click());
        }
        else
        {
            onView(withId(R.id.genderFemale)).check(matches(isDisplayed()));
            onView(withId(R.id.genderFemale)).perform(click());
        }
        //--------------------------------//

        //Scroll To Bottom
        onView(withId(R.id.fragmentCreateAvatarScrollView)).perform(swipeUp());

        //Click Continue Button
        onView(withId(R.id.btnCapture)).check(matches(isDisplayed())).perform(click());
    }

    private void onConsentImageScreen()
    {
        //Scroll To Bottom
        TestUtils.performActionIfViewExist(R.id.fragmentImageConsentScrollView, swipeUp());

        //Click Agreement Button
        TestUtils.performActionIfViewExist(R.id.iAgreeCheckbox, click());

        //Click Agreement Button
        TestUtils.performActionIfViewExist(R.id.continueButton, click());
    }

    private void onOnboardingScreen()
    {
        boolean showVideoOnboarding = ModelSetting.getSetting(ModelSetting.Setting.FEATURE_VIDEO_ONBOARDING, false);

        //Before showing Onboarding Screen, we should enable debug inspect pass, so we create a fake avatar for the capture screen.
        ModelSetting.putSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, true);

        // Do not check to see if the device is aligned on the capture screen
        ModelSetting.putSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, true);

        if (showVideoOnboarding)
        {
            onOnboardingVideoScreen();
        }
        else
        {
            onOnboardingImageScreen();
        }
    }

    private void onOnboardingVideoScreen()
    {
        if (IS_ONBOARDING_SKIPPED)
        {
            TestUtils.performActionIfViewExist(android.R.id.button2, click());
        }
        else
        {
            TestUtils.performActionIfViewExist(android.R.id.button1, click());
            TestUtils.videoOnboardingBubbleChecker();
        }
    }

    private void onOnboardingImageScreen()
    {
        if (IS_ONBOARDING_SKIPPED)
        {
            TestUtils.performActionIfViewExist(android.R.id.button2, click());
        }
        else
        {
            TestUtils.performActionIfViewExist(android.R.id.button1, click());

            //Go through picture onboarding page
            TestUtils.checkerWithTimeout(R.id.onboarding_page_1_next_page_button, isDisplayed());
            onView(withId(R.id.onboarding_page_1_next_page_button)).perform(click());

            for (int i = 0; i < 4; i++)
            {
                onView(withId(R.id.next)).check(matches(isDisplayed())).perform(click());
            }
        }
    }

    private void onCaptureConfirmationScreen()
    {
        //Wait Until Button Appear and Click Confirm Button Twice. Should wait here longer because
        //we will wait for the capture process (more than 20 seconds.)

        TestUtils.checkerWithTimeout(R.id.buttonConfirm, isDisplayed(), 100000);

        //fileResourcesInjection();

        TestUtils.performActionWithTimeout(R.id.buttonConfirm, 20000, click(), click());
    }

    private void onProfileListScreen()
    {
        TestUtils.checkerWithTimeout(R.id.navigation_profile, isDisplayed());

        RecyclerViewTestHelper.checkItemTextAtPosition(R.id.recycler, 0, R.id.layout_chest, R.id.avatarDataItemValue);
        RecyclerViewTestHelper.checkItemTextAtPosition(R.id.recycler, 0, R.id.layout_waist, R.id.avatarDataItemValue);
        RecyclerViewTestHelper.checkItemTextAtPosition(R.id.recycler, 0, R.id.layout_thighs, R.id.avatarDataItemValue);
        RecyclerViewTestHelper.checkItemTextAtPosition(R.id.recycler, 0, R.id.layout_hips, R.id.avatarDataItemValue);

        RecyclerViewTestHelper.clickItemAtPosition(R.id.recycler, 0);
    }

    private void onMainScreenDeleteAvatar()
    {
        /*try
        {
            Thread.sleep(40000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }*/

        //TestUtils.checkerWithTimeout(R.id.progress, isDisplayed());

        TestUtils.checkerWithTimeout(R.id.action_delete_avatar, isDisplayed());

        onView(withId(R.id.action_delete_avatar)).check(matches(isDisplayed())).perform(click());

        onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());

        TestUtils.checkerWithTimeout(R.id.recycler, isDisplayed());
    }
}
