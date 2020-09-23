package com.myfiziq.sdk.db;

import java.util.ArrayList;

public class ModelBillingEventContainer extends Model
{
    @Persistent
    public ArrayList<ModelBillingEvent> Events;


    public ModelBillingEventContainer()
    {

    }

    public ModelBillingEventContainer(ArrayList<ModelBillingEvent> events)
    {
        Events = events;
    }
}
