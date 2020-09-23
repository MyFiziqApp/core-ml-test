package com.myfiziq.sdk.util;

import android.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myfiziq.sdk.db.JwtIdToken;
import com.myfiziq.sdk.db.Orm;

import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */
public class JwtUtils
{
    private JwtUtils()
    {
        // Empty hidden constructor for the utility class
    }

    @Nullable
    public static JwtIdToken getJwtToken(String jwt)
    {
        JwtIdToken result = Orm.newModel(JwtIdToken.class);
        String[] jwsParts = jwt.split("\\.");

        if (jwsParts.length > 2 && result != null)
        {
            String body = new String(Base64.decode(jwsParts[1], Base64.DEFAULT), StandardCharsets.UTF_8);

            result.deserialize(body);
        }

        return result;
    }

    @Nullable
    public static String getTokenItem(String jwt, String item)
    {
        String result = null;

        try
        {
            String[] jwsParts = jwt.split("\\.");

            if (jwsParts.length > 2)
            {
                String body = new String(Base64.decode(jwsParts[1], Base64.DEFAULT), StandardCharsets.UTF_8);
                JsonElement rootElement = JsonParser.parseString(body);

                if (rootElement.isJsonObject())
                {
                    JsonObject object = rootElement.getAsJsonObject();
                    if (object.has(item))
                    {
                        result = object.get(item).getAsString();
                    }
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception occurred when deserialising JSON");
        }

        return result;
    }
}
