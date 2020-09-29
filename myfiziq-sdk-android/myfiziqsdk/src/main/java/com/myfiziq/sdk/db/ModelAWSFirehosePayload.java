package com.myfiziq.sdk.db;

import android.util.Base64;

public class ModelAWSFirehosePayload extends Model
{
    @Persistent
    public String Data;


    public ModelAWSFirehosePayload(String base64Data)
    {
        Data = base64Data;
    }

    public ModelAWSFirehosePayload()
    {

    }

    public ModelAWSFirehosePayload(Model model)
    {
        setData(model);
    }

    public void setData(Model model)
    {
        String dataJson = model.serialize();

        Data =  Base64.encodeToString(dataJson.getBytes(), Base64.NO_WRAP);
    }
}
