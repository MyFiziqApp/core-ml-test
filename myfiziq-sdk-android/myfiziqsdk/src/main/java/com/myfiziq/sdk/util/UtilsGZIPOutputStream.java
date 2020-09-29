package com.myfiziq.sdk.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @hide
 */

public class UtilsGZIPOutputStream extends GZIPOutputStream
{
    public UtilsGZIPOutputStream(OutputStream os) throws IOException
    {
        super(os);
    }

    public UtilsGZIPOutputStream(OutputStream os, int bufferSize) throws IOException
    {
        super(os, bufferSize);
    }

    public void setLevel(int level)
    {
        def.setLevel(level);
    }
}
