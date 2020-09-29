package com.myfiziq.sdk.util;

import android.text.TextUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Provides utilities for managing zip files.
 */
public class ZipUtils
{
    /**
     * Creates a zip file from a map of input files.
     *
     * @param zipOutputPath The path where the zip file should be created.
     * @param inputFiles    A map of input files to place in the zip file.
     *                      The key is filename to use for the entry in the zip file and the value
     *                      is the contents of the zip file entry (i.e. the file to place in
     *                      the zip file).
     * @return A {@link File} object pointing to the zip file that was created.
     * Will return null if an error occurred creating the zip file.
     */
    @Nullable
    public static File createZipFiles(String zipOutputPath, HashMap<String, File> inputFiles)
    {
        if (inputFiles == null || inputFiles.size() == 0)
        {
            Timber.e("No files specified to upload");
            return null;
        }

        File zipFile = new File(zipOutputPath);

        try (OutputStream outputStream = new FileOutputStream(zipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        )
        {
            for (Map.Entry<String, File> source : inputFiles.entrySet())
            {
                if (source == null
                        || TextUtils.isEmpty(source.getKey())
                        || source.getValue() == null
                        || !source.getValue().exists())
                {
                    continue;
                }

                String name = source.getKey();

                zipOutputStream.putNextEntry(new ZipEntry(name));
                FileUtils.copyFile(source.getValue(), zipOutputStream);

                zipOutputStream.closeEntry();
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Cannot create zip file for uploading avatar diagnostic data");

            if (zipFile.exists())
            {
                zipFile.delete();
            }

            zipFile = null;
        }

        return zipFile;
    }
}
