package com.myfiziq.sdk.helpers;

import android.content.Context;

import com.myfiziq.sdk.util.GlobalContext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

public class FilesystemHelpers
{
    private static final String[] MYFIZIQ_FILE_REGEXES = {
            "front([\\d]+)_([\\d])\\.([a-z]{1,5})$",              // e.g. front00000002221569290268_1.bmp
            "side([\\d]+)_([\\d])\\.([a-z]{1,5})$",               // e.g. side00000002221569290268_0.bmp
            "([\\d]+)\\.vert$",                                   // e.g. 00000002221569226433.vert
            "([\\d]+)\\.face$"                                    // e.g. 00000002221569226433.face
    };


    private FilesystemHelpers()
    {

    }

    /**
     * Clears the application's cache that resides on the filesystem.
     */
    public static void clearFilesystemCache(Context context)
    {
        File cacheDirectory = context.getCacheDir();

        File[] files = cacheDirectory.listFiles();

        if (files != null)
        {
            for (File file : files)
            {
                file.delete();
            }
        }
    }

    /**
     * Clears the application's internal storage.
     */
    public static void clearInternalStorage(Context context)
    {
        File internalStorageFolder = context.getFilesDir();

        File[] files = internalStorageFolder.listFiles();

        if (files != null)
        {
            for (File file : files)
            {
                file.delete();
            }
        }
    }

    /**
     * Remove MyFiziq files from the filesystem.
     */
    public static void clearMyFiziqFiles(Context context)
    {
        File internalStorageFolder = context.getFilesDir();
        File[] files = internalStorageFolder.listFiles();

        if (files == null)
        {
            Timber.w("No files found to delete when logging out");
            return;
        }

        Pattern[] patterns = compileRegexes(MYFIZIQ_FILE_REGEXES);

        for (File file : files)
        {
            for (Pattern pattern : patterns)
            {
                String filename = file.getName();

                if (pattern.matcher(filename).matches())
                {
                    file.delete();

                    // Pattern matches. File has been deleted.
                    // Stop looking for patterns that might match it and move on to the next file.
                    break;
                }
            }
        }
    }

    /**
     * Unzips a list of zip files and deletes them afterwards.
     *
     * @param zipNames A list of zip filenames to extract.
     */
    public static void unzip(File folderPath, List<String> zipNames) throws IOException
    {
        for (String zipName : zipNames)
        {
            long startTime = System.currentTimeMillis();

            File zipFile = new File(folderPath, zipName);

            try (FileInputStream inputStream = new FileInputStream(zipFile);
                 ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream))
            )
            {
                String filename;
                ZipEntry zipEntry;
                byte[] buffer = new byte[64 * 1024];
                int count;

                while ((zipEntry = zipInputStream.getNextEntry()) != null)
                {
                    filename = zipEntry.getName();

                    if (zipEntry.isDirectory())
                    {
                        // Skip directories. Unsupported
                        Timber.w("Directories are unsupported: %s", zipEntry.getName());
                        continue;
                    }

                    File outputFile = new File(folderPath, filename);

                    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile))
                    {
                        while ((count = zipInputStream.read(buffer)) != -1)
                        {
                            fileOutputStream.write(buffer, 0, count);
                        }
                    }
                    catch (Exception e)
                    {
                        Timber.e(e, "Exception when writing output file");
                        throw e;
                    }

                    zipInputStream.closeEntry();

                    long endTime = System.currentTimeMillis();
                    Timber.i("Took %sms to extract %s", (endTime - startTime), filename);
                }
            }
            catch (Exception e)
            {
                Timber.e("Cannot unzip file");
                throw e;
            }

            zipFile.delete();
        }
    }


    /**
     * Moves a list of files from the one folder to another.
     *
     * @param sourceFolder    The source folder.
     * @param targetFolder    The target folder.
     * @param sourceFilenames A list of filenames in the source folder to move.
     */
    public static void moveFiles(File sourceFolder, File targetFolder, List<String> sourceFilenames, List<String> destinationFilenames) throws IOException
    {
        for (int i = 0; i < sourceFilenames.size(); i++)
        {
            moveFile(sourceFolder, targetFolder, sourceFilenames.get(i), destinationFilenames.get(i));
        }
    }

    /**
     * Moves a file from the one folder to another.
     *
     * @param sourceFolder    The source folder.
     * @param targetFolder    The target folder.
     * @param sourceFilename A filename in the source folder to move.
     */
    public static void moveFile(File sourceFolder, File targetFolder, String sourceFilename, String destinationFilename) throws IOException
    {
        long startTime = System.currentTimeMillis();

        Context context = GlobalContext.getContext();
        File externalPath = context.getExternalFilesDir(null);
        File internalPath = context.getFilesDir();

        File srcFile = new File(sourceFolder, sourceFilename);
        File dstFile = new File(targetFolder, destinationFilename);

        boolean bSameVol = srcFile.getAbsolutePath().contains(externalPath.getAbsolutePath()) && dstFile.getAbsolutePath().contains(externalPath.getAbsolutePath());

        if (!bSameVol)
            bSameVol = srcFile.getAbsolutePath().contains(internalPath.getAbsolutePath()) && dstFile.getAbsolutePath().contains(internalPath.getAbsolutePath());

        if (bSameVol)
        {
            // Both files are in the same volume... should be able to do a rename.
            dstFile.getParentFile().mkdirs();
            bSameVol = srcFile.renameTo(dstFile);
        }

        // If different volume or rename failed... fallback to copy.
        if (!bSameVol)
        {
            try (InputStream in = new FileInputStream(srcFile);
                 OutputStream out = new FileOutputStream(dstFile))
            {
                // Transfer bytes from in to out
                byte[] buf = new byte[64 * 1024];
                int len;

                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
            }
            catch (Exception e)
            {
                Timber.e(e, "Cannot move file");
                throw e;
            }

            // Delete the downloaded file after we've copied it
            srcFile.delete();
        }

        long endTime = System.currentTimeMillis();
        Timber.i("Took %sms to move %s", (endTime - startTime), sourceFilename);
    }

    private static Pattern[] compileRegexes(String[] regexPatterns)
    {
        Pattern[] patterns = new Pattern[regexPatterns.length];

        for (int i = 0; i < regexPatterns.length; i++)
        {
            patterns[i] = Pattern.compile(regexPatterns[i]);
        }

        return patterns;
    }
}
