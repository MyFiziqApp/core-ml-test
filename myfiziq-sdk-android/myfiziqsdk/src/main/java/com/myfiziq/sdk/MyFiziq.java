package com.myfiziq.sdk;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import com.myfiziq.sdk.activities.DebugActivity;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelAvatarRes;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ORMContentProvider;
import com.myfiziq.sdk.db.ORMDbFactory;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.enums.Broadcast;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.manager.FLAG;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.MiscUtils;
import com.myfiziq.sdk.util.NetworkUtils;
import com.myfiziq.sdk.vo.MYQNativeHandle;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.BoolRes;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

/**
 * This class allows you to call low-level components of the MyFiziq SDK.
 *
 * Note that calling these methods directly is not recommended.
 * They do not provide safety to ensure that the SDK is in a valid state for the specific method
 * call being made at that point in time or that the operation is not being performed on the UI thread.
 *
 * In almost all cases, you should use {@link MyFiziqSdkManager}.
 */
public class MyFiziq extends MyFiziqSdk
{

    private final static String FULLTAG = MyFiziq.class.getName();

    private static MyFiziq singleton;

    private static boolean mSchemaInitDone = false;

    private ExecutorService mExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private LocalBroadcastManager mLocalBroadcastManager;
    private int mOnDeviceFlags = 0;

    // NOTE: this must match the enum in MFZJni.hpp
    @Keep
    public enum NativeOpType
    {
        NativeOpInit,
        NativeOpCancel,
        NativeOpConfirm,
        NativeOpPoseFront,
        NativeOpPoseSide
    };

    @Keep
    public enum UpdateEvents
    {
        UpdateEventsPlay,
        UpdateEventsPause,
        UpdateEventsTrack,
        PlayerEventsMax;

        public static UpdateEvents fromInt(int i)
        {
            switch (i)
            {
                default:
                case 0:
                    return UpdateEventsPlay;
                case 1:
                    return UpdateEventsPause;
                case 2:
                    return UpdateEventsTrack;
            }
        }
    }

    @Keep
    // These flags need to match the NDK layer flags.
    public enum OnDevice
    {
        OnDeviceNone(0),
        OnDeviceClassify(1),
        OnDeviceUploadPayload(2),
        OnDeviceUploadResults(4),
        OnDeviceRunJoints(8),
        OnDeviceDontFilter(16),
        OnDeviceDebug(32);

        int mValue;

        OnDevice(int value)
        {
            mValue = value;
        }

        public static int getFlags(OnDevice... flags)
        {
            int result = 0;

            for (OnDevice f : flags)
            {
                result |= f.mValue;
            }

            return result;
        }

        public static int addFlags(int currentFlags, OnDevice... flags)
        {
            int result = currentFlags;

            for (OnDevice f : flags)
            {
                result |= f.mValue;
            }

            return result;
        }

        public static int addFlags(int currentFlags, Context context, OnDevice flag, ModelSetting.Setting setting, @BoolRes int resId)
        {
            int result = currentFlags;

            // Use debug value or resource value (as default - if debug value not set).
            if (ModelSetting.getSetting(setting, context.getResources().getBoolean(resId)))
            {
                result |= flag.mValue;
            }

            return result;
        }

        public boolean hasFlag(int value, OnDevice flag)
        {
            return (0 != (value & flag.ordinal()));
        }
    }

    @Keep
    public interface ContourEvents
    {
        void onContourCreated(Bitmap bitmap, String id);
    }

    @Keep
    public interface SdkEvents
    {
        void initComplete();
    }

    SdkEvents mEventListener = null;

    boolean mInitDone = false;

    @Keep
    public MyFiziq()
    {
        super();

        Context context = GlobalContext.getContext();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());

        ORMDbFactory.getInstance().openAllDatabases(context);

        initSchemas();

        nativePostInit();

        //TODO: Change with desired expired day
        AsyncHelper.run(() -> MiscUtils.deleteOldCapture(1));
    }

    @Keep
    public synchronized static MyFiziq getInstance()
    {
        if (null == singleton)
        {
            singleton = new MyFiziq();
        }

        return singleton;
    }

    @Keep
    public boolean isSdkSetup()
    {
        return nativeIsSdkSetup();
    }

    @Keep
    public void sdkSetup()
    {
        // Run SDK setup on UI thread...
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postAtFrontOfQueue(()->{
            nativeSdkSetup();
            // Get Avatar handling flags...
            // Override with debug settings if present.
            Context context = GlobalContext.getContext();
            mOnDeviceFlags = OnDevice.addFlags(mOnDeviceFlags, context, OnDevice.OnDeviceClassify, ModelSetting.Setting.DEBUG_INDEVICE, R.bool.ondevice_classify);
            mOnDeviceFlags = OnDevice.addFlags(mOnDeviceFlags, context, OnDevice.OnDeviceUploadPayload, ModelSetting.Setting.DEBUG_PAYLOAD, R.bool.ondevice_upload_payload);
            mOnDeviceFlags = OnDevice.addFlags(mOnDeviceFlags, context, OnDevice.OnDeviceUploadResults, ModelSetting.Setting.DEBUG_UPLOAD_RESULTS, R.bool.ondevice_upload_results);
            mOnDeviceFlags = OnDevice.addFlags(mOnDeviceFlags, context, OnDevice.OnDeviceRunJoints, ModelSetting.Setting.DEBUG_RUNJOINTS, R.bool.ondevice_run_joints);
        });
    }

    @Keep
    public void setKey(ModelSetting.Setting key, String value)
    {
        nativeSetKey(key.name(), value);
    }

    @Keep
    public void setKey(String key, String value)
    {
        nativeSetKey(key, value);
    }

    @Keep
    public String getKey(ModelSetting.Setting key)
    {
        return nativeGetKey(key.name());
    }

    @Keep
    public String getKey(String key)
    {
        return nativeGetKey(key);
    }

    @Keep
    public void setSecret(String secret)
    {
        nativeSetSecret(secret);
    }

    @Keep
    public void setKeySecretEnv(String key, String secret, String sdkenv)
    {
        nativeSetKeySecretEnv(key, secret, sdkenv);
    }

    @Keep
    public void setToken(String token)
    {
        nativeSetToken(token);
    }

    @Keep
    public boolean hasTokens()
    {
        return nativeHasTokens();
    }

    @Keep
    public String getTokenAid()
    {
        return nativeGetTokenAid();
    }

    @Keep
    public String getTokenCid()
    {
        return nativeGetTokenCid();
    }

    @Keep
    public String getTokenCreated()
    {
        return nativeGetTokenCreated();
    }

    @Keep
    public String getTokenEnv()
    {
        return nativeGetTokenEnv();
    }

    @Keep
    public String getTokenVid()
    {
        return nativeGetTokenVid();
    }

    @Keep
    public synchronized void restartSdk()
    {
        mSchemaInitDone = false;
        singleton = new MyFiziq();
    }

    @Keep
    private void initSchemas()
    {
        if (!mSchemaInitDone)
        {
            for (ORMTable table : ORMTable.values())
            {
                Class<? extends Model> clazz = table.getTableClass();
                nativeAddSchema(
                        Orm.getModelName(clazz),
                        Model.getColumns(clazz),
                        Model.getColumnTypes(clazz),
                        Model.getColumnAnnotations(clazz),
                        table.getTableType().getName());
            }

            mSchemaInitDone = true;
        }
    }

    @Keep
    public void setGuestUser(String selectedGuestName)
    {
        nativeSetGuestUser(selectedGuestName);
    }

    @Keep
    public String getGuestUser()
    {
        return nativeGetGuestUser();
    }

    @Keep
    public void updateAllAvatarAdjustedValues()
    {

    }

    @SuppressLint("DefaultLocale")
    @Keep
    public String getContourId(int onColor, int offColor, int height, int width, double H, double W, Gender gender, PoseSide side, float theta, int fill)
    {
        return ContourId(onColor, offColor, height, width, H, W, gender.ordinal(), side.ordinal(), theta, fill);
    }

    @Keep
    @Nullable
    public Bitmap getContour(int onColor, int offColor, int height, int width, double H, double W, Gender gender, PoseSide side, float theta, int fill, String id)
    {
        return (Bitmap) ContourPredict(Bitmap.Config.ARGB_8888, onColor, offColor, height, width, H, W, gender.ordinal(), side.ordinal(), theta, fill, id);
    }

    @Keep
    public void getContour(final int onColor, final int offColor, final int height, final int width, final double H, final double W, final Gender gender, final PoseSide side, float theta, final int fill, final ContourEvents onComplete)
    {
        mExecutorService.execute(() ->
        {
            final String id = getContourId(onColor, offColor, height, width, H, W, gender, side, theta, fill);
            final Bitmap bmp = getContour(onColor, offColor, height, width, H, W, gender, side, theta, fill, id);

            if (null != bmp)
            {
                mHandler.post(() ->
                {
                    if (null != onComplete)
                    {
                        onComplete.onContourCreated(bmp, id);
                    }
                });
            }
        });
    }

    public String[] testInspect2(PoseSide side, String fileid, String[] filename)
    {
        ModelLog.d("Run test inspect: " + Arrays.toString(filename));
        return PoseInspect(side.ordinal(), fileid, filename, "");

            /*
            if (null != results && results.length > 0)
            {
                for (String result : results)
                {
                    ModelLog.d("Inspect result: " + result);
                }
            }
            */
    }

    public void initInspect(boolean bWait)
    {
        PoseInit(bWait);
    }

    public void releaseInspect()
    {
        PoseRelease();
    }

    public String[] inspect(PoseSide side, String contourId, String[] filename, String imageName)
    {
        return PoseInspect(side.ordinal(), contourId, filename, imageName);
    }

    public String[] inspect(PoseSide side, String contourId, int imageWidth, int imageHeight, int rotation, byte[] imageData)
    {
        return PoseDirect(side.ordinal(), contourId, imageWidth, imageHeight, rotation, imageData);
    }

    @Keep
    public String ransac(ModelAvatar avatar, List<ModelAvatarRes> results)
    {
        float[] chests = new float[results.size()];
        float[] waists = new float[results.size()];
        float[] hips = new float[results.size()];
        float[] inseams = new float[results.size()];
        float[] fits = new float[results.size()];
        float[] thighs = new float[results.size()];
        float[] PercentBodyFat = new float[results.size()];

        for (int i = 0; i < results.size(); i++)
        {
            ModelAvatarRes res = results.get(i);
            chests[i] = (float) res.chest;
            waists[i] = (float) res.waist;
            hips[i] = (float) res.hip;
            inseams[i] = (float) res.inseam;
            fits[i] = (float) res.fitness;
            thighs[i] = (float) res.thigh;
            PercentBodyFat[i] = (float) res.PercentBodyFat;
        }

        return Ransac(
                avatar.getHeight().getValueInCm(),
                avatar.getWeight().getValueInKg(),
                avatar.getGender().ordinal(),
                chests,
                waists,
                hips,
                inseams,
                fits,
                thighs,
                PercentBodyFat);
    }

    @Keep
    @Nullable
    public ByteBuffer getMeshVerts(MYQNativeHandle handle)
    {
        try
        {
            long memoryAddress = handle.getMemoryAddress();

            ByteBuffer bb = (ByteBuffer) GetMeshVerts(memoryAddress);

            if (bb == null)
            {
                return null;
            }

            bb.order(ByteOrder.nativeOrder());

            return bb;
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception when executing getMeshVerts()");
        }

        return null;
    }

    @Keep
    @Nullable
    public ByteBuffer getMeshFaces(MYQNativeHandle handle)
    {
        try
        {
            long memoryAddress = handle.getMemoryAddress();

            ByteBuffer bb = (ByteBuffer) GetMeshFaces(memoryAddress);

            if (bb == null)
            {
                return null;
            }

            bb.order(ByteOrder.nativeOrder());

            return bb;
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception when executing getMeshFaces()");
        }

        return null;
    }

    @Keep
    public void registerListener(SdkEvents listener)
    {
        mEventListener = listener;
    }

    @Keep
    public void initComplete()
    {
        mInitDone = true;

        //Timber.e("Java - initComplete");
        if (null != mEventListener)
            mEventListener.initComplete();
    }

    @Keep
    public boolean isInitDone()
    {
        return mInitDone;
    }

    @Keep
    public boolean isCaptureEnabled()
    {
        return nativeIsCaptureEnabled();
    }

    @Keep
    public boolean isFileOpen(MYQNativeHandle handle)
    {
        return handle.isInitialised();
    }

    public void setPassword(String database, String password)
    {
        if (!BuildConfig.DEBUG)
            nativeSetPassword(database, password);
    }

    public void changePassword(String password)
    {
        if (!BuildConfig.DEBUG)
            nativeChangePassword(password);
    }

    public boolean checkPassword()
    {
        return nativeCheckPassword();
    }

    @Keep
    public void checkpoint(char type)
    {
        nativeCheckpoint(type);
    }

    @Keep
    public void closeDatabase(String path)
    {
        nativeCloseDatabase(path);
    }

    @Keep
    public void apiSetEnabled(boolean enabled)
    {
        if (!enabled)
        {
            MyFiziqSdkManager.stopWorkers();
        }
        nativeApiSetEnabled(enabled);
    }

    @Keep
    public String apiGet(String modelName, String endPoint, Integer responseCode, Integer hasMore, int pageNo, int flags, Object... data)
    {
        return nativeApiGet(0, modelName, endPoint, responseCode, hasMore, pageNo, flags, data);
    }

    @Keep
    public String apiPost(String modelName, String endPoint, Integer responseCode, int flags, Object... data)
    {
        return nativeApiPost(0, modelName, endPoint, responseCode, flags, data);
    }

    @Keep
    public String apiPut(String modelName, String endPoint, Integer responseCode, int flags, Object... data)
    {
        return nativeApiPut(0, modelName, endPoint, responseCode, flags, data);
    }

    @Keep
    public String apiDelete(String modelName, String endPoint, Integer responseCode, int flags, Object... data)
    {
        return nativeApiDelete(0, modelName, endPoint, responseCode, flags, data);
    }

    @Keep
    public String computeBillingSignature(String proofOfWorkKey, String vendorId, String appId, String eventId, String eventSource, String eventMisc, String userIdHash, String clientMisc, String isoDate)
    {
        return nativeComputeBillingSignature(proofOfWorkKey, vendorId, appId, eventId, eventSource, eventMisc, userIdHash, clientMisc, isoDate);
    }

    @Keep
    public String performHmac(String key, String data)
    {
        return nativePerformHmac(key, data);
    }

    @Keep
    public String computeEvoltPass(String evoltUserId, String evoltDateCreated)
    {
        return nativeComputeEvoltPass(evoltUserId, evoltDateCreated);
    }

    @Keep
    public String signUrl(String url)
    {
        return nativeSignUrl(url);
    }

    @Keep
    public void testSegfault()
    {
        nativeTestSegfault();
    }

    @Keep
    public void execPending()
    {
        nativeExecPending();
    }

    @Keep
    public void notifyChange(String table, String id)
    {
        notifyChangeFromNative(table, id);
    }

    @Keep
    public void registerReceiver(BroadcastReceiver receiver)
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(FULLTAG);
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    @Keep
    public void unregisterReceiver(BroadcastReceiver receiver)
    {
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    @Keep
    public void pollAvatars()
    {
        if (isLoaded())
            nativePollAvatars(mOnDeviceFlags);
    }

    @Keep
    public long getMesh()
    {
        return GetCubeMesh();
    }

    @Keep
    public MYQNativeHandle getMesh(double Height, double Weight, int gender, double chest, double waist, double hip, double inseam, double fit, double thigh)
    {
        long memoryAddress = GetMesh(Height, Weight, gender, (float) chest, (float) waist, (float) hip, (float) inseam, (float) fit, (float) thigh);
        return new MYQNativeHandle(memoryAddress, this::releaseMesh);
    }

    @Keep
    public boolean segment(String avatarId, int viewside, String baseDirectory, String extraData, boolean runJoints)
    {
        return nativeSegment(avatarId, viewside, baseDirectory, extraData, runJoints);
    }

    @Keep
    public String uploadAvatar(String avatarId, String baseDirectory, String extraData)
    {
        return nativeUploadAvatar(avatarId, baseDirectory, extraData, mOnDeviceFlags);
    }

    @Keep
    public String uploadAvatar(String avatarId, String baseDirectory, String extraData, boolean bIndevice, boolean runJoints, boolean bDebugPayload, boolean bFilter)
    {
        int flags = 0;

        if (bIndevice)
        {
            flags = OnDevice.addFlags(flags, OnDevice.OnDeviceClassify);
        }

        if (runJoints)
        {
            flags = OnDevice.addFlags(flags, OnDevice.OnDeviceRunJoints);
        }

        if (bDebugPayload)
        {
            flags = OnDevice.addFlags(flags, OnDevice.OnDeviceDebug);
        }

        if (!bFilter)
        {
            flags = OnDevice.addFlags(flags, OnDevice.OnDeviceDontFilter);
        }

        return nativeUploadAvatar(avatarId, baseDirectory, extraData, flags);
    }

    @Keep
    public void post(NativeOpType opType)
    {
        nativePost(opType.ordinal());
    }

    /*
     * Callback method from C++ side -> notify when table data has changed.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public void notifyChangeFromNative(String table, String items)
    {
        if (!TextUtils.isEmpty(table))
        {
            Context context = GlobalContext.getContext();

            if (null != context)
            {
                Uri uri = ORMContentProvider.uri(table);

                if (!TextUtils.isEmpty(items))
                {
                    String[] tokens = items.split(Character.toString((char) 1));

                    //Timber.d(String.format("NDK->notifyChangeFromNative (%d):%s", tokens.length, table));

                    for (String idStr : tokens)
                    {
                        if (!TextUtils.isEmpty(idStr))
                        {
                            //Timber.d(String.format("NDK->notifyChangeFromNative %s", idStr));

                            Uri notifyUri = Uri.withAppendedPath(uri, idStr);

                            context.getContentResolver().notifyChange(notifyUri, null, false);
                        }
                    }
                }
                else
                {
                    //Timber.d("NDK->notifyChangeFromNative (0):" + table);
                    context.getContentResolver().notifyChange(uri, null, false);
                }
            }
        }
        else
        {
            if (!TextUtils.isEmpty(items))
            {
                mHandler.post(() ->
                {
                    String[] logs = items.split("\\n");
                    for (String log : logs)
                    {
                        if (!log.contains("\\n"))
                        {
                            Intent intent = new Intent();
                            intent.setAction(FULLTAG);
                            intent.putExtra("LOG", log);
                            mLocalBroadcastManager.sendBroadcast(intent);
                            Timber.d(log);
                        }
                    }
                });
            }
        }
    }

    /*
     * Callback method from C++ side -> get network available.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public boolean isNetworkAvailable()
    {
        return NetworkUtils.isNetworkAvailable(GlobalContext.getContext());
    }

    /*
     * Callback method from C++ side -> re-login required.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public void loginRequired(int flags)
    {
        //Timber.e("[loginRequired]");
        //TODO: .sendLoginBroadcast(flags);
    }

    /*
     * Callback method from C++ side -> re-login required.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public void setEnv(String... values)
    {
        Timber.e("[setEnv]" + values.toString());
    }

    /*
     * Callback method from C++ side -> token refreshed.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public void tokenRefreshed()
    {
        //Timber.e("[tokenRefreshed]");
    }

    /*
     * Callback method from C++ side -> get device id.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public String getId()
    {
        return Settings.Secure.getString(GlobalContext.getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    /*
     * Callback method from C++ side -> get common name for csr.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public String getCN()
    {
        StringBuilder cn = new StringBuilder();

        cn.append(BuildConfig.FLAVOR.toUpperCase().charAt(0));

        return cn.toString();
    }

    /*
     * Callback method from C++ side -> get device id.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public String getUuid()
    {
        return UUID.randomUUID().toString();
    }


    /*
     * Callback method from C++ side.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    public void visualizeMat(String name, long id, Bitmap bitmap)
    {
        if (BuildConfig.DEBUG)
        {
            AsyncHelper.run(() ->
            {
                try
                {
                    String destName = String.format("%d%s.jpg", id, name);
                    FileOutputStream fo = GlobalContext.getContext().openFileOutput(destName, Context.MODE_PRIVATE);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fo);
                    fo.close();
                    mHandler.post(() ->
                    {
                        Intent visActivity = new Intent(GlobalContext.getContext(), DebugActivity.class);
                        visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        visActivity.putExtra(DebugActivity.EXTRA_VISUALIZE, destName);
                        GlobalContext.getContext().startActivity(visActivity);
                    });
                }
                catch (Exception e)
                {
                    Timber.e(e, "Error when visualising mat");
                }
            });
        }
    }


    // This is private to ensure that we don't corrupt the lifecycle of MYQNativeHandle
    // by accidentally destroying a C++ object but retaining a reference to it's memory in Java.
    @Keep
    private void releaseMesh(long memoryAddress)
    {
        try
        {
            ReleaseMesh(memoryAddress);
        }
        catch (Exception e)
        {
            Timber.e(e, "Exception when executing releaseMesh()");
        }
    }

    /*
     * Callback method from C++ side.
     * This method isn't used from the Java side but is used from C++ land.
     */
    @Keep
    void apiProgress(String model, int dltotal, int dlnow, int ultotal, int ulnow)
    {
        Timber.d("[apiProgress]:%s %d %d %d %d", model, dltotal, dlnow, ultotal, ulnow);
        Broadcast.send(Broadcast.ACTION_RESOURCE_DL_PROGRESS, model, String.valueOf(dltotal), String.valueOf(dlnow), String.valueOf(ultotal), String.valueOf(ulnow));
    }
	
	@Keep
    boolean getAsset(String name)
    {
        MyFiziqAsset asset = new MyFiziqAsset(MyFiziqAsset.AssetType.MYQASSET_FILE, name);
        asset.fetch();
        return asset.isReady();
    }
}
