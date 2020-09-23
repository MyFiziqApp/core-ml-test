package com.myfiziq.sdk.components;

import java.util.Observable;

/**
 * Provides a mechanism to announce when a contour has been generated.
 */
// The simplest possible (dummy) implementation of a working Observable
public class ContourObservable extends Observable
{
    @Override
    public void notifyObservers()
    {
        setChanged();
        super.notifyObservers();
    }

    @Override
    public void notifyObservers(Object arg)
    {
        setChanged();
        super.notifyObservers(arg);
    }
}
