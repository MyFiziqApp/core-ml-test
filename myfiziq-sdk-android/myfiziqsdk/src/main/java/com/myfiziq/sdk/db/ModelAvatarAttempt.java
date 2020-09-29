package com.myfiziq.sdk.db;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.util.AwsUtils;
import com.myfiziq.sdk.util.GlobalContext;

import java.io.File;

/**
 * @hide
 */
public class ModelAvatarAttempt extends Model
{
    @Persistent(serialize = false)
    public Synctime sync = new Synctime();

    /**
     * Returns a path with the App ID (AID), username and attempt ID as folders and
     * a custom filename at the end.
     *
     * @param filename The filename to use.
     */
    public String getName(String filename)
    {
        String result = "";
        if (MyFiziq.getInstance().hasTokens())
        {
            String usernameNumber = AwsUtils.getCognitoUsernameNumber();
            result = String.format("%s/%s/%s/%s", MyFiziq.getInstance().getTokenAid(), usernameNumber, getAttemptId(), filename);
        }

        return result;
    }

    /**
     * Returns a path to the avatar with the App ID and username as folders and the attempt ID
     * represented as a zip file.
     */
    public String getNameForZip()
    {
        String result = "";
        if (MyFiziq.getInstance().hasTokens())
        {
            String usernameNumber = AwsUtils.getCognitoUsernameNumber();
            result = String.format("%s/%s/%s.zip", MyFiziq.getInstance().getTokenAid(), usernameNumber, getAttemptId());
        }

        return result;
    }

    public String getAttemptId()
    {
        return "";
    }

    public String getOutputsBaseName()
    {
        return "outputs.json";
    }

    public String getOutputsName()
    {
        return getName(getOutputsBaseName());
    }

    public File getOutputsFile()
    {
        String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
        return new File(baseDir, getAttemptId()+getOutputsBaseName());
    }

    public String getZipFilename(int ix)
    {
        return getAttemptId() + String.valueOf(ix) + ".zip";
    }

    public String getZipFilePath(int ix)
    {
        String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
        return new File(baseDir, getZipFilename(ix)).getAbsolutePath();
    }

    public File getZipFile(int ix)
    {
        String baseDir = GlobalContext.getContext().getFilesDir().getAbsolutePath();
        return new File(baseDir, getZipFilename(ix));
    }

    public void downloadOutputs(TransferListener transferListener)
    {
        ModelAppConf conf = ModelAppConf.getInstance();
        if (null != conf)
        {
            try
            {
                File f = new File(GlobalContext.getContext().getFilesDir(), getAttemptId()+".json");
                AwsUtils.getTransferUtility().download(conf.results, getOutputsName(), f, transferListener);
            }
            catch (Exception e)
            {
            }
        }
    }
}
