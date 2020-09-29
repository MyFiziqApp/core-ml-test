package com.myfiziq.sdk.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class YuvToRgb
{
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

    public YuvToRgb(Context context)
    {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public Bitmap nv21ToBitmap(byte[] nv21Data, int dataLength, int prevSizeW, int prevSizeH)
    {
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(dataLength);
        Allocation renderScriptInput = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(prevSizeW).setY(prevSizeH);
        Allocation renderScriptOutput = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        renderScriptInput.copyFrom(nv21Data);

        yuvToRgbIntrinsic.setInput(renderScriptInput);
        yuvToRgbIntrinsic.forEach(renderScriptOutput);

        Bitmap bitmap = Bitmap.createBitmap(prevSizeW, prevSizeH, Bitmap.Config.ARGB_8888);
        renderScriptOutput.copyTo(bitmap);

        renderScriptInput.destroy();
        renderScriptOutput.destroy();

        return bitmap;
    }

    public void destroy()
    {
        if (yuvToRgbIntrinsic != null)
        {
            yuvToRgbIntrinsic.destroy();
            yuvToRgbIntrinsic = null;
        }

        if (rs != null)
        {
            rs.destroy();
            rs = null;
        }
    }
}
