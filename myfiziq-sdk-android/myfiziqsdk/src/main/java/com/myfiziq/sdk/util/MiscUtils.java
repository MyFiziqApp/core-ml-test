/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myfiziq.sdk.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import com.myfiziq.sdk.db.PoseSide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Some handy utilities.
 */
public class MiscUtils
{
    private MiscUtils()
    {
        // Empty hidden constructor for the utility class
    }

    public static boolean isDevBuild()
    {
        return GlobalContext.getContext().getPackageName().matches("com\\.myfiziq\\.myfiziq_android\\.myfiziq.*dev");
    }

    public static boolean isInternalBuild()
    {
        return GlobalContext.getContext().getPackageName().matches("com\\.myfiziq\\.myfiziq_android\\.myfiziq.*(dev|internal)");
    }

    public static void clearCache()
    {
        File path = GlobalContext.getContext().getFilesDir();
        for (String filename : path.list())
        {
            File file = new File(path, filename);

            if (file.isFile())
            {
                if (filename.endsWith(".vert") || filename.endsWith(".face"))
                {
                    file.delete();
                }
            }
        }
    }

    /**
     * Delete capture that older than n-days.
     * @param dayCount: n-Days.
     */
    public static void deleteOldCapture(int dayCount)
    {
        long currentUnixTimestamp = TimeFormatUtils.getUnixTimestampInUtcWithSeconds();
        long timestampDeleteThreshold = currentUnixTimestamp - (60 * 60 * 24 * dayCount);

        String attemptIdRegex = ".*([0-9]{20}).*";
        Pattern pattern = Pattern.compile(attemptIdRegex);

        String[] deleteOldCapturesWithExtension = { ".bmp" };

        File directoryPath = GlobalContext.getContext().getFilesDir();
        File[] filesInDirectory = directoryPath.listFiles();

        for (File file : filesInDirectory)
        {
            String filename = file.getName();


            boolean processFile = false;
            String lowercaseFilename = filename.toLowerCase();

            for (String extension: deleteOldCapturesWithExtension)
            {
                if (lowercaseFilename.endsWith(extension))
                {
                    processFile = true;
                }
            }

            if (!processFile)
            {
                // File does not have a recognised file extension for a capture
                continue;
            }


            Matcher matcher = pattern.matcher(filename);

            if (!matcher.matches())
            {
                Timber.v("%s is not a recognised capture file", filename);
                continue;
            }

            String attemptId = matcher.group(1);
            String unixTimestampString = attemptId.substring(attemptId.length() - 10);

            long unixTimestamp = Long.parseLong(unixTimestampString);

            if (unixTimestamp > timestampDeleteThreshold)
            {
                Timber.v("%s will be retained for another %s seconds", filename, (unixTimestamp - timestampDeleteThreshold));

                continue;
            }

            Timber.v("%s has passed its retention deadline and will now be deleted", filename);

            if (file.isFile())
            {
                file.delete();
            }
        }
    }

    /**
     * Obtains a list of files that live in the specified directory and match the glob pattern.
     */
    public static String[] getFiles(File dir, String regex)
    {
        final Pattern pattern = Pattern.compile(regex);
        String[] result = dir.list((dir1, name) ->
        {
            Matcher matcher = pattern.matcher(name);
            return matcher.matches();
        });
        Arrays.sort(result);

        return result;
    }

    /**
     * Get a files from asset to desired path & file
     */
    public static void copyFilesFromAsset(File path, String filename, String assetFileName)
    {
        AssetManager am = GlobalContext.getContext().getAssets();
        try
        {
            //Get Desired Content
            InputStream initialStream  = am.open(assetFileName);
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            //Copy Desired Content to Files
            File targetFile = new File(path, filename);
            OutputStream outStream = new FileOutputStream(targetFile);
            outStream.write(buffer);

            initialStream.close();
            outStream.close();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void swapImagesForAttemptId(String attemptId, PoseSide side, String replacementFilename, int imagesToSwap)
    {
        for (int i = 0; i < imagesToSwap; i++)
        {
            String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
            File captureOutput = new File(baseDir + "/" + side.getSideImageFilename(attemptId, i));
            MiscUtils.copyFilesFromAsset(captureOutput, replacementFilename);
        }
    }

    /**
     * Get a files from asset to desired path & file
     */
    public static void copyFilesFromAsset(File targetFile, String assetFileName)
    {
        AssetManager am = GlobalContext.getContext().getAssets();
        try
        {
            // Get Desired Content
            InputStream initialStream  = am.open(assetFileName);
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            //C opy Desired Content to Files
            OutputStream outStream = new FileOutputStream(targetFile);
            outStream.write(buffer);

            initialStream.close();
            outStream.close();

        } catch (IOException e)
        {
            Timber.e(e);
        }
    }

    /**
     * Obtains a list of files that live in the specified directory and match the glob pattern.
     */
    public static String[] globFiles(File dir, String glob)
    {
        String regex = globToRegex(glob);
        final Pattern pattern = Pattern.compile(regex);
        String[] result = dir.list((dir1, name) ->
        {
            Matcher matcher = pattern.matcher(name);
            return matcher.matches();
        });
        Arrays.sort(result);

        return result;
    }

    /**
     * Converts a filename globbing pattern to a regular expression.
     * <p>
     * The regex is suitable for use by Matcher.matches(), which matches the entire string, so
     * we don't specify leading '^' or trailing '$'.
     */
    private static String globToRegex(String glob)
    {
        // Quick, overly-simplistic implementation -- just want to handle something simple
        // like "*.mp4".
        //
        // See e.g. http://stackoverflow.com/questions/1247772/ for a more thorough treatment.
        StringBuilder regex = new StringBuilder(glob.length());
        //regex.append('^');
        for (char ch : glob.toCharArray())
        {
            switch (ch)
            {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                default:
                    regex.append(ch);
                    break;
            }
        }
        //regex.append('$');
        return regex.toString();
    }

    /**
     * Obtains the approximate refresh time, in nanoseconds, of the default display associated
     * with the activity.
     * <p>
     * The actual refresh rate can vary slightly (e.g. 58-62fps on a 60fps device).
     */
    public static long getDisplayRefreshNsec(Activity activity)
    {
        Display display = ((WindowManager)
                activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        double displayFps = display.getRefreshRate();
        long refreshNs = Math.round(1000000000L / displayFps);
        //Timber.d("refresh rate is " + displayFps + " fps --> " + refreshNs + " ns");
        return refreshNs;
    }

    public static String leftPad(final String str, final int size, final char padChar)
    {
        if (str == null)
        {
            return null;
        }
        final int pads = size - str.length();
        if (pads <= 0)
        {
            return str; // returns original String when possible
        }

        return repeat(padChar, pads).concat(str);
    }

    public static String leftPad(final String str, final int size, String padStr)
    {
        if (str == null)
        {
            return null;
        }
        if (TextUtils.isEmpty(padStr))
        {
            padStr = " ";
        }

        final int padLen = padStr.length();
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0)
        {
            return str; // returns original String when possible
        }
        if (padLen == 1)
        {
            return leftPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen)
        {
            return padStr.concat(str);
        }
        else if (pads < padLen)
        {
            return padStr.substring(0, pads).concat(str);
        }
        else
        {
            final char[] padding = new char[pads];
            final char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++)
            {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    public static String repeat(final char ch, final int repeat)
    {
        if (repeat <= 0)
        {
            return "";
        }
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--)
        {
            buf[i] = ch;
        }
        return new String(buf);
    }

    public static String repeat(final String str, final int repeat)
    {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null)
        {
            return null;
        }
        if (repeat <= 0)
        {
            return "";
        }
        final int inputLength = str.length();
        if (repeat == 1 || inputLength == 0)
        {
            return str;
        }
        if (inputLength == 1)
        {
            return repeat(str.charAt(0), repeat);
        }

        final int outputLength = inputLength * repeat;
        switch (inputLength)
        {
            case 1:
                return repeat(str.charAt(0), repeat);
            case 2:
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--)
                {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default:
                final StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++)
                {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    public static String join(CharSequence delimiter, CharSequence... elements)
    {
        StringBuilder sb = new StringBuilder();
        boolean bFirst = true;
        for (CharSequence elem : elements)
        {
            if (!bFirst)
                sb.append(delimiter);
            sb.append(elem);
            bFirst = false;
        }
        return sb.toString();
    }

    public static void CopyToSDCard(InputStream in, String path)
    {
        FileOutputStream out = null;

        try
        {
            out = new FileOutputStream(path);
            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0)
            {
                out.write(buff, 0, read);
            }
        } catch (Throwable t)
        {

        } finally
        {
            try
            {
                if (null != in)
                    in.close();
                if (null != out)
                    out.close();
            } catch (Throwable t)
            {

            }
        }
    }

    public static void CopyRAWtoSDCard(Resources resources, int id, String path)
    {
        InputStream in = null;
        FileOutputStream out = null;

        try
        {
            in = resources.openRawResource(id);
            out = new FileOutputStream(path);
            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0)
            {
                out.write(buff, 0, read);
            }
        } catch (Throwable t)
        {

        } finally
        {
            try
            {
                if (null != in)
                    in.close();
                if (null != out)
                    out.close();
            } catch (Throwable t)
            {

            }
        }
    }
}
