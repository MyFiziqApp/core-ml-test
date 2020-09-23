package com.myfiziq.sdk.db;

import java.util.ArrayList;

public class ModelSupportImages extends Model
{
    @Persistent
    public String captureProcessErrorMessage;
    @Persistent
    public int retryCount;
    @Persistent
    public ArrayList<String> frontCaptures;
    @Persistent
    public ArrayList<String> sideCaptures;
    @Persistent
    public String attemptID;
    @Persistent
    public String imageType;

    public ModelSupportImages()
    {

    }

    public ModelSupportImages(int retryCount, ArrayList<String> frontCaptures, ArrayList<String> sideCaptures, String attemptID, String imageType)
    {
        this.captureProcessErrorMessage = captureProcessErrorMessage;
        this.retryCount = retryCount;
        this.frontCaptures = frontCaptures;
        this.sideCaptures = sideCaptures;
        this.attemptID = attemptID;
        this.imageType = imageType;
    }
}
