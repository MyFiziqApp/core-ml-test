package com.myfiziq.sdk.db;

public class ModelSupportMeasurements extends Model
{
    @Persistent
    public Double weightKG;
    @Persistent
    public Double hipsCM;
    @Persistent
    public Double fitness;
    @Persistent
    public Double inseamCM;
    @Persistent
    public Double thighCM;
    @Persistent
    public Double waistCM;
    @Persistent
    public Double tbf;
    @Persistent
    public Double heightCM;
    @Persistent
    public Double chestCM;

    public ModelSupportMeasurements()
    {

    }

    public ModelSupportMeasurements(ModelAvatar avatar)
    {
        this.weightKG = avatar.getWeight().valueInKg;
        this.hipsCM = avatar.getSampleHip().valueInCm;
        this.fitness = avatar.fitness;
        this.inseamCM = avatar.getSampleInseam().valueInCm;
        this.thighCM = avatar.getSampleThigh().valueInCm;
        this.waistCM = avatar.getSampleWaist().valueInCm;
        this.tbf = avatar.getSamplePercentBodyFat();
        this.heightCM = avatar.getHeight().valueInCm;
        this.chestCM = avatar.getSampleChest().valueInCm;
    }
}
