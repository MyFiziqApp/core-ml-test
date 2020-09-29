package com.myfiziq.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.myfiziq.sdk.db.ModelRemoteAsset;
import com.myfiziq.sdk.db.ModelRemoteAssets;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.enums.Broadcast;
import com.myfiziq.sdk.enums.SdkResultCode;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.FilesystemHelpers;
import com.myfiziq.sdk.manager.FLAG;
import com.myfiziq.sdk.util.GlobalContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * This class defines a mechanism for getting a remote asset with versioning.
 */
public class MyFiziqAsset
{
    public enum AssetType
    {
        MYQASSET_UNDEF,
        MYQASSET_LIB,
        MYQASSET_FILE
    }

    private enum AssetState
    {
        MISSING,
        FG_DOWNLOAD,
        BG_DOWNLOAD,
        READY,
        FAILED
    }

    private static ThreadPoolExecutor mThreadPool = new ThreadPoolExecutor(
            2, 2, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
            new LinkedBlockingQueue<>(),
            new MyFiziqThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    private final AtomicBoolean mDownloading = new AtomicBoolean(false);

    AssetState mState = AssetState.MISSING;
    String mFilename;
    AssetType mType;
    ModelRemoteAsset mRemoteAsset = null;

    public MyFiziqAsset(AssetType type, String filename)
    {
        mType = type;
        mFilename = filename;
    }

    public boolean isReady()
    {
        return (AssetState.READY == mState);
    }

    public void fetchAsync(AsyncHelper.Callback<MyFiziqAsset> ready)
    {
        AsyncHelper.run(this::fetch, ready, true);
    }

    public MyFiziqAsset fetch()
    {
        synchronized (this)
        {
            // Asset check already completed?
            if (AssetState.READY == mState)
            {
                return this;
            }

            // Check state...
            // Download may be completed by another thread.
            mState = getAssetState();
            if (AssetState.READY == mState)
            {
                return this;
            }

            // Set downloading to true.
            // If downloading was already true... wait.
            if (!mDownloading.compareAndSet(false, true))
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    Timber.i(e);
                }
            }

            switch (mState)
            {
                // Download in foreground
                case FAILED:
                case MISSING:
                case FG_DOWNLOAD:
                    getAssetSync();
                    notifyAll();
                    break;

                // Download in background
                case BG_DOWNLOAD:
                    getAsset();
                    notifyAll();
                    break;

                // Download not required
                case READY:
                    notifyAll();
                    break;
            }
        }
        return this;
    }

    public AssetState getAssetState()
    {
        boolean bUpgrade = GlobalContext.getContext().getResources().getBoolean(R.bool.remote_resources_upgrade);
        boolean bDestExists = assetDestExists();

        if (bDestExists && !bUpgrade)
            return AssetState.READY;

        ModelRemoteAssets remoteAssets = getAssetTable();
        ModelRemoteAssets localAssets = getLocalAssetTable();

        mRemoteAsset = remoteAssets.findLatestVersion(mFilename);

        if (null != mRemoteAsset)
        {
            ModelRemoteAsset localAsset = localAssets.findAsset(mFilename);
            if (null == localAsset || !bDestExists)
            {
                return AssetState.FG_DOWNLOAD;
            }

            if (localAsset.id.contentEquals(mRemoteAsset.id) && bDestExists)
            {
                // should we force a recalculation of the localAsset.etag here?
                if (mRemoteAsset.etag != localAsset.etag)
                {
                    return AssetState.FAILED;
                }

                return AssetState.READY;
            }

            if (mRemoteAsset.force)
            {
                return AssetState.FG_DOWNLOAD;
            }

            return AssetState.BG_DOWNLOAD;
        }

        if (bDestExists)
            return AssetState.READY;

        return AssetState.MISSING;
    }

    public long getVersion()
    {
        if (null != mRemoteAsset)
        {
            return mRemoteAsset.getVersion();
        }

        return 0;
    }

    private synchronized ModelRemoteAssets getLocalAssetTable()
    {
        ModelRemoteAssets assets = ORMTable.getModel(ModelRemoteAssets.class, "local");
        if (null == assets)
        {
            assets = Orm.newModel(ModelRemoteAssets.class);
            assets.id = "local";
            assets.save();
        }

        return assets;
    }

    private ModelRemoteAssets getAssetTable()
    {
        getAssetList();
        ModelRemoteAssets assets = ORMTable.getModel(ModelRemoteAssets.class, "remote");
        if (null == assets || assets.files.size() < 1)
        {
            assets = Orm.newModel(ModelRemoteAssets.class);
            assets.id = "REMOTE";
        }

        return assets;
    }

    public static void getAssetList()
    {
        MyFiziq mfz = MyFiziq.getInstance();
        String baseUrl = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RESOURCE_SVR, "");

        if (TextUtils.isEmpty(baseUrl))
            baseUrl = GlobalContext.getContext().getResources().getString(R.string.remote_resources_base_url);

        String url = baseUrl + "/android/files.json";

        @SuppressLint("UseValueOf")
        Integer responseCode = new Integer(0); // NOSONAR
        mfz.apiGet(
                "ModelRemoteAssets",
                url,
                responseCode,
                0,
                0,
                FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS, FLAG.FLAG_ETAG, FLAG.FLAG_NOBASE, FLAG.FLAG_SIGN_URL)
        );
    }

    public boolean assetDestExists()
    {
        switch (mType)
        {
            case MYQASSET_LIB:
            {
                // Search included native libs...
                final File nativeLibraryDir = new File(GlobalContext.getContext().getApplicationInfo().nativeLibraryDir);
                final String[] primaryNativeLibraries = nativeLibraryDir.list();
                for (String lib : primaryNativeLibraries)
                {
                    if (lib.contentEquals(mFilename))
                        return true;
                }

                // Search local lib folder...
                File dest = getNativeStoragePath(GlobalContext.getContext());
                return dest.exists();
            }

            default:
            {
                File dest = new File(getNativeStoragePath(GlobalContext.getContext()), mFilename);
                if (!dest.exists())
                {
                    return false;

                    // TODO: handle files bundled in APK...
                    /*
                    try
                    {
                        GlobalContext.getContext().getAssets().open(mFilename);
                        return true;
                    }
                    catch (Exception e)
                    {
                        // Failed to find in assets... not really an error in all cases.
                        // no logging required.
                    }
                    */
                }

                return false;
            }
        }
    }

    private void getAsset()
    {
        mThreadPool.execute(this::getAssetSync);
    }

    private void getAssetSync()
    {
        if (null != mRemoteAsset)
        {
            mDownloading.set(true);

            //TODO: if remote file size > remote_resources_wifi_size_mb then wait for WiFi...

            MyFiziq mfz = MyFiziq.getInstance();
            String baseUrl = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RESOURCE_SVR, "");

            if (TextUtils.isEmpty(baseUrl))
                baseUrl = GlobalContext.getContext().getResources().getString(R.string.remote_resources_base_url);

            String url = baseUrl + "/" + mRemoteAsset.id;

            File dest = new File(GlobalContext.getContext().getExternalFilesDir(null), mFilename);

            Broadcast.send(Broadcast.ACTION_RESOURCE_DL_START, mFilename);

            @SuppressLint("UseValueOf")
            Integer responseCode = new Integer(0); // NOSONAR
            String md5 = mfz.apiGet(
                    dest.getAbsolutePath(),
                    url,
                    responseCode,
                    0,
                    0,
                    FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS, FLAG.FLAG_NOBASE, FLAG.FLAG_SIGN_URL, FLAG.FLAG_FILE, FLAG.FLAG_PROGRESS)
            );

            SdkResultCode resultCode = SdkResultCode.valueOfHttpCode(responseCode);
            if (resultCode.isOk() && mRemoteAsset.etag.contentEquals(md5))
            {
                Context context = GlobalContext.getContext();

                // Source directory where files are downloaded to in the EXTERNAL storage
                File sourcePath = context.getExternalFilesDir(null);

                // Target directory where files are persisted in the INTERNAL storage
                File targetPath = getNativeStoragePath(context);

                String destinationFilename = getVersionedFilenameFromOriginal(mFilename);
                try
                {
                    FilesystemHelpers.moveFile(sourcePath, targetPath, mFilename, destinationFilename);
                    updateLocalAssetState();
                    Broadcast.send(Broadcast.ACTION_RESOURCE_DL_SUCCESS, mFilename);
                }
                catch (IOException e)
                {
                    Timber.e(e);
                }
            }
            else
            {
                Broadcast.send(Broadcast.ACTION_RESOURCE_DL_FAILED, mFilename);
            }

            mDownloading.set(false);
        }
    }

    private void updateLocalAssetState()
    {
        ModelRemoteAssets localAssets = getLocalAssetTable();
        localAssets.replaceAsset(mRemoteAsset);
        mState = AssetState.READY;
    }

    /**
     * Gets the path where native code will be stored and loaded from in the app.
     */
    private File getNativeStoragePath(Context context)
    {
        switch (mType)
        {
            case MYQASSET_LIB:
                // "lib" MUST match the string in "ReLinkerInstance.LIB_DIR"
                return context.getDir("lib", Context.MODE_PRIVATE);

            default:
                //TODO:
                return context.getDir("files", Context.MODE_PRIVATE);
        }
    }

    /**
     * Converts an original filename (e.g. "libMFZJni.so") to a versioned filename
     * (e.g. "libMFZJni.so.12345") that is recognised by Relinker.
     *
     * @param originalFilename The original filename.
     */
    private String getVersionedFilenameFromOriginal(String originalFilename)
    {
        switch (mType)
        {
            case MYQASSET_LIB:
                String versionCode = BuildConfig.SDK_VERSION + "." + mRemoteAsset.getVersion();
                return originalFilename + "." + versionCode;

            default:
                return originalFilename;
        }
    }
}
