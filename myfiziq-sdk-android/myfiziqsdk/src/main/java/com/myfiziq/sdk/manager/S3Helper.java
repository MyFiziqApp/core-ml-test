package com.myfiziq.sdk.manager;


import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.regions.Region;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.myfiziq.sdk.db.ModelAppConf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import timber.log.Timber;


// Package private. Should not be exposed to the customer app.
class S3Helper
{
    // Package private. Should not be exposed to the customer app.
    static String invoke(String lambda, String payload)
    {
        String result = null;
        ModelAppConf conf = ModelAppConf.getInstance();
        if (null != conf)
        {
            AWSLambdaClient client = new AWSLambdaClient(AWSMobileClient.getInstance());
            client.setRegion(Region.getRegion(conf.awsregion));
            try
            {
                Charset charset = StandardCharsets.UTF_8;
                ByteBuffer buf = charset.encode(payload);

                InvokeRequest invokeRequest = new InvokeRequest();
                invokeRequest.setFunctionName(String.format("%s-%s-%s", conf.vendor, conf.env, lambda));
                invokeRequest.setPayload(buf);

                InvokeResult response = client.invoke(invokeRequest);

                Timber.v("%s response size is %s bytes", lambda, response.getPayload().remaining());

                result = charset.decode(response.getPayload()).toString();
            }
            catch (Exception e)
            {
                Timber.e(e, "Failed to invoke: %s", lambda);
            }
        }

        return result;
    }

    // Package private. Should not be exposed to the customer app.
    @Nullable
    static String readS3ObjectAsString(S3Object object)
    {
        try
        {
            S3ObjectInputStream stream = object.getObjectContent();
            return IOUtils.toString(stream);
        }
        catch (IOException e)
        {
            Timber.e(e, "Cannot read S3 object");
        }

        return null;
    }
}
