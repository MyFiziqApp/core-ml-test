package com.myfiziq.myfiziq_android;

import android.view.View;
import android.widget.NumberPicker;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class TestUtils
{
    public static void checkerWithTimeout(int viewId, final Matcher<? super View> viewMatcher)
    {
        checkerWithTimeout(viewId, viewMatcher, 20000);
    }

    public static void checkerWithTimeout(final Matcher<View> viewMatcher1, final Matcher<? super View> viewMatcher2)
    {
        checkerWithTimeout(viewMatcher1, viewMatcher2, 20000);
    }

    public static void checkerWithTimeout(int viewId, final Matcher<? super View> viewMatcher, int timeoutInMs)
    {
        checkerWithTimeout(withId(viewId), viewMatcher, timeoutInMs);
    }

    /**
     * This method is for wait, substitution to IdleRegistry. So we can wait for certain element to appear and then
     * assert the UI content after the wait ends. Basically to handle wait of AsyncTask
     *
     * @param viewMatcher1 : UIView to check.
     * @param viewMatcher2 : Condition for the matches() after the wait ends.
     * @param timeoutInMs  : The timeout for wait.
     */
    public static void checkerWithTimeout(final Matcher<View> viewMatcher1, final Matcher<? super View> viewMatcher2, int timeoutInMs)
    {
        long startTime = System.currentTimeMillis();

        //Checking View Display Status. Will wait until the view is diplayed / the timeout is end.
        while ((startTime + timeoutInMs) > System.currentTimeMillis())
        {
            try
            {
                onView(viewMatcher1).check(matches(isDisplayed()));
                break;
                //view is displayed logic
            }
            catch (NoMatchingViewException e)
            {
                e.fillInStackTrace();
            }
            catch (AssertionError assertionError)
            {
                assertionError.fillInStackTrace();
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        //Assert the view after the wait is end.
        onView(viewMatcher1).check(matches(viewMatcher2));
    }

    /**
     * This method is for wait, substitution to IdleRegistry. So we can wait for certain element
     * until it's ready to perform an Action
     *
     * @param viewId      : View ID.
     * @param viewActions : Varg of actions, Can be more than 1 view action
     * @param timeoutInMs : The timeout for wait.
     */
    public static void performActionWithTimeout(int viewId, int timeoutInMs, final ViewAction... viewActions)
    {
        long startTime = System.currentTimeMillis();

        //Try to perform action on view. Will wait until the view is ready to do an action.
        while ((startTime + timeoutInMs) > System.currentTimeMillis())
        {
            try
            {
                onView(withId(viewId)).perform(viewActions);
                break;
                //view is displayed logic
            }
            catch (PerformException e)
            {
                e.fillInStackTrace();
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean isUserLoggedIn()
    {
        long startTime = System.currentTimeMillis();
        long timeoutInMs = 40000;

        boolean isLoggedIn = false;

        // Will try to check until the configuration is not null.
        // It means the AWSMobileClient is already initialized.
        while ((startTime + timeoutInMs) > System.currentTimeMillis())
        {
            if (AWSMobileClient.getInstance().getConfiguration() != null)
            {
                isLoggedIn = AWSMobileClient.getInstance().isSignedIn();
                break;
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        return isLoggedIn;
    }

    /**
     * This method is for testing the spinner, clicking desired spinner item text
     *
     * @param spinnerId         : Id of Spinner.
     * @param spinnerTextViewId : Id of the spinner item text view. We will check the content in this textview
     * @param clickStringItem   : The text that want to be checked with spinner item textView.
     */
    public static void clickAndCheckSpinner(int spinnerId, int spinnerTextViewId, String clickStringItem)
    {
        TestUtils.checkerWithTimeout(spinnerId, isDisplayed());
        onView(withId(spinnerId)).perform(click());
        onView(allOf(withId(spinnerTextViewId), withText(clickStringItem))).inRoot(isPlatformPopup()).perform(click());
    }

    /**
     * This method is for selecting item in a number picker.
     *
     * @param pickerId         : Id of Number Picker.
     * @param desiredValueText : Desired Value Text.
     */
    public static void selectNumberPickerValue(int pickerId, String desiredValueText)
    {
        ViewInteraction numPicker = onView(withId(pickerId));
        numPicker.perform(new ViewAction()
        {
            @Override
            public void perform(UiController uiController, View view)
            {
                NumberPicker np = (NumberPicker) view;

                String[] displayedValues = np.getDisplayedValues();

                for (int i = 0; i < displayedValues.length; i++)
                {
                    if (displayedValues[i].equals(desiredValueText))
                    {
                        np.setValue(i + np.getMinValue());
                        break;
                    }
                }
            }

            @Override
            public String getDescription()
            {
                return "Set the passed number into the NumberPicker";
            }

            @Override
            public Matcher<View> getConstraints()
            {
                return ViewMatchers.isAssignableFrom(NumberPicker.class);
            }
        });
    }

    /**
     * This method is for performing action on a View that is not always shown in the UI.
     * It will skip the action if the view is not displayed.
     *
     * @param viewActions : Varg of action. Can perform action simultaneously.
     */
    public static void performActionIfViewExist(int viewId, final ViewAction... viewActions)
    {
        try
        {
            onView(withId(viewId)).check(matches(isDisplayed())).perform(viewActions);
            //view is displayed logic
        }
        catch (NoMatchingViewException e)
        {
            e.fillInStackTrace();
        }
    }


    public static void videoOnboardingBubbleChecker()
    {
        videoOnboardingBubbleChecker(R.string.a_clean_background, R.string.wearing_form_fitting_clothing, R.string._4m_of_clear_space);
        videoOnboardingBubbleChecker(R.string.tie_back_hair, R.string.remove_shoes);
        videoOnboardingBubbleChecker(R.string.place_phone_at_hips_height);
        videoOnboardingBubbleChecker(R.string.align_circles);
        videoOnboardingBubbleChecker(R.string.fit_inside_outline);
        videoOnboardingBubbleChecker(R.string.get_ready_3_2_1);
        videoOnboardingBubbleChecker(R.string.now_your_side_picture);
        videoOnboardingBubbleChecker(R.string.feet_together);
        videoOnboardingBubbleChecker(R.string.no_twisting, R.string.arms_inside_body);
    }

    /**
     * This method is special for video onboarding page.
     * Since there are some bubbles in there, so we need to click some bubble with text to make the bubble disappear.
     * The bubble id is not unique, so we can check it with the string that contained in the bubble.
     *
     * @param bubbleStrings : Varg of bubble string, since every page can have more than 1 bubbles.
     */
    public static void videoOnboardingBubbleChecker(final int... bubbleStrings)
    {
        long startTime = System.currentTimeMillis();
        long timeout = 40000;

        while ((startTime + timeout) > System.currentTimeMillis())
        {
            try
            {
                for (int bubbleString : bubbleStrings)
                {
                    onView(withText(bubbleString)).perform(click());
                }
                break;
                //view is displayed logic
            }
            catch (PerformException e)
            {
                e.fillInStackTrace();
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
