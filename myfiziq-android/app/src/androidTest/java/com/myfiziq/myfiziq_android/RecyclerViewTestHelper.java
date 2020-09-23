package com.myfiziq.myfiziq_android;

import android.content.res.Resources;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class RecyclerViewTestHelper
{
    /**
     * This method is for checking textContent in TextView within the RecyclerView item.
     *
     *
     * @param recyclerViewId        : Id of Recycler View.
     * @param position              : Item Position in Recycler View.
     * @param desiredText           : The text that want to be checked with textView inside the Recycler View Item.
     * @param textViewId            : The Id of textView inside the Recyler View Item, can be more than 1 if the textView is nested and not unique.
     */
    public static void checkItemTextAtPosition(int recyclerViewId, int position, String desiredText, int... textViewId)
    {
        TestUtils.checkerWithTimeout(getItemTextViewAtPosition(recyclerViewId, position, textViewId), withText(desiredText), 40000);
    }

    /**
     * This method is for checking textContent in TextView within the RecyclerView item.
     *
     *
     * @param recyclerViewId        : Id of Recycler View.
     * @param position              : Item Position in Recycler View.
     * @param textViewId            : The Id of textView inside the Recyler View Item, can be more than 1 if the textView is nested and not unique.
     */
    public static void checkItemTextAtPosition(int recyclerViewId, int position, int... textViewId)
    {
        TestUtils.checkerWithTimeout(getItemTextViewAtPosition(recyclerViewId, position, textViewId), isDisplayed(), 60000);
    }

    public static void clickItemAtPosition(int recyclerViewId, int position)
    {
        onView(getItemAtPosition(recyclerViewId, position)).perform(click());
    }

    public static Matcher<View> getItemTextViewAtPosition(int recyclerViewId, final int position, final int... targetViewIds)
    {

        return new TypeSafeMatcher<View>()
        {
            Resources resources = null;
            View childView;

            public void describeTo(Description description)
            {
                String idDescription = Integer.toString(recyclerViewId);
                if (this.resources != null)
                {
                    try
                    {
                        idDescription = this.resources.getResourceName(recyclerViewId);
                    } catch (Resources.NotFoundException var4)
                    {
                        idDescription = String.format("%s (resource name not found)",
                                new Object[]{Integer.valueOf(recyclerViewId)});
                    }
                }

                description.appendText("with id: " + idDescription);
            }

            public boolean matchesSafely(View view)
            {

                this.resources = view.getResources();

                if (childView == null)
                {
                    RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView != null && recyclerView.getId() == recyclerViewId)
                    {
                        childView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(position)).itemView;
                    }
                    else
                    {
                        return false;
                    }
                }

                if (targetViewIds.length == 0)
                {
                    return view == childView;
                }
                else
                {
                    View targetView = childView.findViewById(targetViewIds[0]);

                    for(int i = 1; i < targetViewIds.length; i++)
                    {
                        targetView = targetView.findViewById(targetViewIds[i]);
                    }

                    return view == targetView;
                }
            }
        };
    }

    public static Matcher<View> getItemAtPosition(int recyclerViewId, final int position)
    {

        return new TypeSafeMatcher<View>()
        {
            Resources resources = null;
            View childView;

            public void describeTo(Description description)
            {
                String idDescription = Integer.toString(recyclerViewId);
                if (this.resources != null)
                {
                    try
                    {
                        idDescription = this.resources.getResourceName(recyclerViewId);
                    } catch (Resources.NotFoundException var4)
                    {
                        idDescription = String.format("%s (resource name not found)",
                                new Object[]{Integer.valueOf(recyclerViewId)});
                    }
                }

                description.appendText("with id: " + idDescription);
            }

            public boolean matchesSafely(View view)
            {

                this.resources = view.getResources();

                if (childView == null)
                {
                    RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView != null && recyclerView.getId() == recyclerViewId)
                    {
                        childView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(position)).itemView;
                    }
                    else
                    {
                        return false;
                    }
                }

                return view == childView;
            }
        };
    }
}
