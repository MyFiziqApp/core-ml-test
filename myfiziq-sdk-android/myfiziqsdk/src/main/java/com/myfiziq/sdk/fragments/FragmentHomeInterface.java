package com.myfiziq.sdk.fragments;

import androidx.annotation.DrawableRes;

/**
 * This interface defines the resource(s) to be used for Fragments that are navigation pages.
 *
 * If this interface is implemented then previous items in the backstack will be cleared
 * when the user visits this fragment.
 */
public interface FragmentHomeInterface
{
    @DrawableRes int getIcon();
}
