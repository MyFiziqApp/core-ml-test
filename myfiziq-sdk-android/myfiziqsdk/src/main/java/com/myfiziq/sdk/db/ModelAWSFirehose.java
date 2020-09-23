package com.myfiziq.sdk.db;

public class ModelAWSFirehose extends Model
{
    @Persistent
    public String DeliveryStreamName;

    @Persistent
    public ModelAWSFirehosePayload Record;

    public ModelAWSFirehose()
    {}

    public ModelAWSFirehose(String deliveryStreamName, ModelAWSFirehosePayload record)
    {
        DeliveryStreamName = deliveryStreamName;
        Record = record;
    }
}
