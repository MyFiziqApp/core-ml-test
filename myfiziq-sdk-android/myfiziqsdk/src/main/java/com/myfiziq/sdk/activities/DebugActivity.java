package com.myfiziq.sdk.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.LayoutStyle;
import com.myfiziq.sdk.adapters.RecyclerManager;
import com.myfiziq.sdk.adapters.RecyclerManagerInterface;
import com.myfiziq.sdk.db.Centimeters;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Kilograms;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelInspect;
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Persistent;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.db.Status;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.util.BmpUtil;
import com.myfiziq.sdk.util.FileProviderLogs;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.MiscUtils;
import com.myfiziq.sdk.util.TimeFormatUtils;
import com.myfiziq.sdk.views.ItemViewDebug;
import com.myfiziq.sdk.views.ItemViewLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

/**
 * @hide
 */
public class DebugActivity extends BaseActivity implements RecyclerManagerInterface, CursorHolder.CursorChangedListener, AdapterView.OnItemSelectedListener, SearchView.OnQueryTextListener, LifecycleOwner, ViewModelStoreOwner
{
    private static final String TAG = DebugActivity.class.getSimpleName();
    public static final String EXTRA_VISUALIZE = "VISUALIZE";
    private static HashMap<String, String> mPoseMap;

    private LoaderManager mLoaderManager;
    private RecyclerManager mManager;
    private Parameter mParameter;

    private Toolbar toolbar;
    private RecyclerView recycler;
    private RecyclerView recyclerDrawer;
    private DrawerLayout drawerLayout;
    private SearchView searchView;
    private AppCompatSpinner spinner;
    private View statusContainer;
    private TextView statusText;
    private boolean bFirstScroll = true;

    File mTempFile;

    static final int SELECT_IMAGE_FRONT = 0;
    static final int SELECT_IMAGE_SIDE = 1;
    static final int SELECT_MODEL = 2;

    static
    {
        mPoseMap = new HashMap<>();
        mPoseMap.put(".*front_armsup.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_arms_down.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_legleftkick.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":0,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_legrightkick.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":1,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        //mPoseMap.put(".*front_multiple.*", "{\"result\":{\"Face\":2,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_noarmleft.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":1,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_noarmright.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":0,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_noarms.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_noarmsnolegs.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_nohead.*", "{\"result\":{\"Face\":0,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_nolegleft.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":0,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_nolegright.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":1,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_nolegs.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_pass.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*front_barrywhite_pass.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":1,\"RA\":1,\"LL\":1,\"RL\":1,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*side_armsup.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":1}}");
        //mPoseMap.put(".*side_noarms.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":1}}");
        mPoseMap.put(".*side_nohead.*", "{\"result\":{\"Face\":0,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":0,\"LB\":0}}");
        mPoseMap.put(".*side_nolegs.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":1,\"LB\":0}}");
        mPoseMap.put(".*side_pass.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":1,\"LB\":1}}");
        mPoseMap.put(".*side_barrywhite_pass.*", "{\"result\":{\"Face\":1,\"GE\":0,\"LA\":0,\"RA\":0,\"LL\":0,\"RL\":0,\"BG\":1,\"DP\":1,\"UB\":1,\"LB\":1}}");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_VISUALIZE))
        {
            handleVisualize(intent);
        }
        else
        {
            setContentView(R.layout.activity_debug);

            toolbar = findViewById(R.id.toolbar);
            recycler = findViewById(R.id.recycler);
            recyclerDrawer = findViewById(R.id.recyclerDrawer);
            drawerLayout = findViewById(R.id.drawer_layout);
            statusContainer = findViewById(R.id.statusContainer);
            statusText = findViewById(R.id.statusText);

            searchView = findViewById(R.id.search);
            spinner = findViewById(R.id.spinner);

            searchView.setOnQueryTextListener(this);
            spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ModelLog.Type.values()));
            spinner.setOnItemSelectedListener(this);

            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle("Debug");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    getActivity(),
                    drawerLayout,
                    toolbar,
                    0,
                    0);

            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            mLoaderManager = LoaderManager.getInstance(this);
            mManager = new RecyclerManager(getActivity(), mLoaderManager, this)
            {
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
                {
                    CursorHolder holder = getHolder(id);
                    if (null != holder)
                    {
                        if (holder.getModelClass() == DebugModel.class)
                        {
                            DebugCursor debugCursor = new DebugCursor(DebugActivity.this);
                            debugCursor.forceLoad();
                            return debugCursor;
                        }

                        return super.onCreateLoader(id, bundle);
                    }

                    return null;
                }
            };

            mParameter = new Parameter.Builder()
                    .setLoaderId(0)
                    .setModel(ModelLog.class)
                    .setView(ItemViewLog.class)
                    .setOrder("pk DESC")
                    .build();
            mParameter.getHolder().addListener(this);
            mParameter.getHolder().setFilterQueryProvider(new FilterQueryProvider()
            {
                @Override
                public Cursor runQuery(CharSequence constraint)
                {
                    if (constraint.length() > 0)
                    {
                        mParameter.getHolder().addWhere("value", String.format("REGEXP \"%s\"", constraint));
                    }
                    else
                    {
                        mParameter.getHolder().remWhere("value");
                    }

                    return mParameter.getHolder().runQuery((selfChange) ->
                    {
                        if (!selfChange)
                        {
                            mManager.reloadCursor(mParameter.getHolder());
                        }
                    });
                    //ContentResolver resolver = GlobalContext.getContext().getContentResolver();
                    //cursor.setNotificationUri(resolver, ORMContentProvider.uri(ModelLog.class));
                }
            });

            mManager.setupRecycler(
                    null,
                    recycler,
                    new ParameterSet.Builder()
                            .setLayout(LayoutStyle.VERTICAL)
                            .addParam(mParameter)
                            .build()
            );

            mManager.setupRecycler(
                    null,
                    recyclerDrawer,
                    new ParameterSet.Builder()
                            .setLayout(LayoutStyle.VERTICAL)
                            .addParam(new Parameter.Builder()
                                    .setLoaderId(1)
                                    .setModel(DebugModel.class)
                                    .setView(ItemViewDebug.class)
                                    .build())
                            .build());
        }
    }

    void handleVisualize(Intent intent)
    {
        setContentView(R.layout.activity_debug_vis);

        String bitmapName = intent.getStringExtra(EXTRA_VISUALIZE);

        ImageView iv = findViewById(R.id.image);
        TextView tv = findViewById(R.id.text);

        tv.setText(bitmapName);

        AsyncHelper.run(() ->
        {
            Bitmap bitmap = null;
            try
            {
                FileInputStream is = openFileInput(bitmapName);
                bitmap = BitmapFactory.decodeStream(is);
                is.close();
                return bitmap;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return bitmap;
        }, iv::setImageBitmap, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_debug, menu);
        menu.findItem(R.id.menu_item_video_onboarding).setChecked(ModelSetting.getSetting(ModelSetting.Setting.FEATURE_VIDEO_ONBOARDING, false));
        menu.findItem(R.id.menu_item_disable_alignment).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, false));
        menu.findItem(R.id.menu_item_inspect_pass).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, false));
        menu.findItem(R.id.menu_item_inspect_fail).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INSPECT_FAIL, false));
        menu.findItem(R.id.menu_item_practise_mode).setChecked(ModelSetting.getSetting(ModelSetting.Setting.FEATURE_PRACTISE_MODE, false));
        menu.findItem(R.id.menu_item_visualize).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false));
        menu.findItem(R.id.menu_item_visualize_pose).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE_POSE, false));
        menu.findItem(R.id.menu_item_indevice).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false));
        menu.findItem(R.id.menu_item_runjoints).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false));
        menu.findItem(R.id.menu_item_debugpayload).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false));
        menu.findItem(R.id.menu_item_upload_results).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_UPLOAD_RESULTS, false));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_item_clear)
        {
            ModelLog.prune(System.currentTimeMillis());
            return true;
        }
        else if (itemId == R.id.menu_item_style)
        {
            Intent intent = new Intent(this, DebugStyleActivity.class);
            startActivity(intent);
            return true;
        }
        else if (itemId == R.id.menu_item_export)
        {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");

            String dateString = TimeFormatUtils.formatDate(new Date(), TimeZone.getDefault(), TimeFormatUtils.PATTERN_ISO8601_2, TimeZone.getTimeZone("UTC"));
            intent.putExtra(Intent.EXTRA_SUBJECT, dateString);

            intent.putExtra(Intent.EXTRA_TEXT, getEmailText(intent));
            intent.putExtra(Intent.EXTRA_STREAM, getEmailAttachment());

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent mailer = intent.createChooser(intent, "Send Log");
            startActivity(mailer);
            return true;
        }
        else if (itemId == R.id.menu_item_video_onboarding)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.FEATURE_VIDEO_ONBOARDING, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_disable_alignment)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_inspect_pass)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_INSPECT_PASS, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_inspect_fail)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_INSPECT_FAIL, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_practise_mode)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.FEATURE_PRACTISE_MODE, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_visualize)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_VISUALIZE, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_visualize_pose)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_VISUALIZE_POSE, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_indevice)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_INDEVICE, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_runjoints)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_debugpayload)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_PAYLOAD, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_upload_results)
        {
            item.setChecked(!item.isChecked());
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_UPLOAD_RESULTS, item.isChecked());
            return true;
        }
        else if (itemId == R.id.menu_item_poseframes)
        {
            int minimumFrames = 2;
            int maximumFrames = 8;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Inspection Frame count\n(must be " + maximumFrames + "> and >=" + minimumFrames + ")");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_POSEFRAMES, ""));
            builder.setView(input);

            builder.setPositiveButton(android.R.string.ok, (dialog, which) ->
            {
                int frames = minimumFrames;

                try
                {
                    frames = Integer.decode(input.getText().toString());
                }
                catch (NumberFormatException e)
                {
                    Timber.e(e, "Cannot parse input %s. Default to %s", input.getText().toString(), minimumFrames);
                }

                if (frames < minimumFrames)
                    frames = minimumFrames;
                if (frames > maximumFrames)
                    frames = maximumFrames;

                ModelSetting.putSetting(ModelSetting.Setting.DEBUG_POSEFRAMES, frames);
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

            builder.show();
            return true;
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (null != mTempFile)
        {
            try
            {
                mTempFile.delete();
                mTempFile = null;
            }
            catch (Exception e)
            {

            }
        }
    }

    protected String getEmailText(Intent intent)
    {
        StringBuilder sb = new StringBuilder();

        sb
                .append("Device: ")
                .append(Build.MANUFACTURER)
                .append(" ")
                .append(Build.MODEL)
                .append(" ")
                .append(android.os.Build.PRODUCT)
                .append(" ")
                .append(Build.VERSION.RELEASE)
                .append(" ")
                .append(Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName())
                .append("\n");
        sb.append("\n");

        return sb.toString();
    }

    public Uri getEmailAttachment()
    {
        StringBuilder sb_zip = new StringBuilder();

        if (null != mParameter && null != mParameter.getHolder() && mParameter.getHolder().isCursorFilled())
        {
            CursorHolder holder = mParameter.getHolder();
            int count = holder.getItemCount();

            for (int i = 0; i < count; i++)
            {
                ModelLog eventLog = (ModelLog) holder.getItem(i);
                sb_zip.append(eventLog.timestamp).append(" ");
                sb_zip.append(eventLog.type).append(" ");
                sb_zip.append(eventLog.tag).append(" ");
                sb_zip.append(eventLog.value).append("\n");
                //sb.append("\n");
            }

            try
            {
                File tempPath = new File(getFilesDir(), "logs");
                if (!tempPath.exists())
                {
                    tempPath.mkdirs();
                }

                mTempFile = File.createTempFile("EventLog", ".zip", tempPath);
                FileOutputStream fout = new FileOutputStream(mTempFile);
                ZipOutputStream zout = new ZipOutputStream(fout);

                zout.putNextEntry(new ZipEntry("EventLog.txt"));
                byte[] data = sb_zip.toString().getBytes();
                zout.write(data);
                zout.closeEntry();
                zout.close();
            }
            catch (Exception e)
            {
                Timber.e(e, "Failed to get email attachment");
            }

            return FileProviderLogs.getUriForFile(this, getPackageName() + ".fileproviderlogs", mTempFile);
        }

        return null;
    }

    @Override
    public List<View.OnClickListener> getItemSelectListeners()
    {
        ArrayList<View.OnClickListener> list = new ArrayList<>();
        list.add(v ->
        {
            Model m = (Model) v.getTag(R.id.TAG_MODEL);
            if (m instanceof DebugModel)
            {
                DebugModel model = (DebugModel) v.getTag(R.id.TAG_MODEL);
                if (null != model)
                {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                    model.run(model.mItem, this, () ->
                    {
                    });
                }
            }
        });
        return list;
    }

    @Override
    public void onCursorChanged(CursorHolder cursorHolder)
    {
        /*
        if (null != recycler && null != cursorHolder && null != cursorHolder.getCursor())
        {
            if (!bFirstScroll)
            {
                recycler.smoothScrollToPosition(cursorHolder.getItemCount());
            }
            else
            {
                recycler.scrollToPosition(cursorHolder.getItemCount());
            }

            bFirstScroll = false;
        }
        */
    }

    private static void copyImagesToStorage(Activity activity)
    {
        String[] assets = getAllAssets(activity);

        for (String file : assets)
        {
            if (file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".bmp"))
            {
                String bmpFilename = file;
                bmpFilename = bmpFilename.replace(".png", ".bmp");
                bmpFilename = bmpFilename.replace(".jpg", ".bmp");

                File bmpFile = new File(activity.getFilesDir(), bmpFilename);

                if (!bmpFile.exists())
                {
                    Log.d(TAG, "writing:"+bmpFilename);
                    Bitmap bitmap = getBitmapFromAsset(activity, file);
                    BmpUtil.save(bitmap, bmpFile.getAbsolutePath());
                }
            }
        }
    }

    static File copyFileToStorage(Activity activity, String filename)
    {
        try
        {
            File front = new File(activity.getFilesDir(), filename);
            if (!front.exists())
            {
                AssetManager mgr = activity.getAssets();
                InputStream stream = mgr.open(filename);
                MiscUtils.CopyToSDCard(stream, front.getAbsolutePath());
            }

            return front;
        }
        catch (Throwable t)
        {
            Timber.e(t, "Failed to copy file to storage");
        }

        return null;
    }

    static String[] getAllAssets(Activity activity)
    {
        try
        {
            AssetManager mgr = activity.getAssets();
            return mgr.list("");
        }
        catch (Throwable t)
        {
            Timber.e(t, "Failed to get assets");
        }

        return null;
    }

    static Bitmap getBitmapFromAsset(Activity activity, String filePath)
    {
        AssetManager assetManager = activity.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try
        {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        }
        catch (IOException e)
        {
            Timber.e(e, "Failed to get bitmap");
        }

        return bitmap;
    }

    static void copyAllFilesToStorage(Activity activity, String[] filenames)
    {
        if (null != filenames && filenames.length > 0)
        {
            for (String file : filenames)
            {
                copyFileToStorage(activity, file);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (null != mParameter && null != mParameter.getHolder())
        {
            if (position > 0)
            {
                mParameter.getHolder().addWhere("type", "=" + position);
            }
            else
            {
                mParameter.getHolder().remWhere("type");
            }
            mManager.reloadCursor(mParameter.getHolder());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
        mParameter.getHolder().remWhere("type");
        mManager.reloadCursor(mParameter.getHolder());
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        if (null != mParameter && null != mParameter.getHolder())
        {
            mParameter.getHolder().getFilter().filter(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query)
    {
        if (null != mParameter && null != mParameter.getHolder())
        {
            mParameter.getHolder().getFilter().filter(query);
        }
        return false;
    }


    private void setStatus(String title, boolean bVisible)
    {
        statusText.setText(title);
        statusContainer.setVisibility((bVisible ? View.VISIBLE : View.INVISIBLE));
    }

    private static boolean isFrontSource(String filename, String filter)
    {
        return (filename.matches(String.format(".*front%s.*\\.bmp", filter)));
    }

    private static boolean isSideSource(String filename, String filter)
    {
        return (filename.matches(String.format(".*side%s.*\\.bmp", filter)));
    }

    private static boolean isSourceMatch(String filename1, String filename2, int parts)
    {
        String[] parts1 = filename1.split("_");
        String[] parts2 = filename2.split("_");
        if ((parts1.length > parts) && (parts2.length > parts))
        {
            for (int i = 0; i < parts; i++)
            {
                if (!parts1[i].contentEquals(parts2[i]))
                    return false;
            }

            return true;
        }
        return false;
    }

    private static boolean validatePose(String filename, String[] results)
    {
        if (null != results)
        {
            for (String key : mPoseMap.keySet())
            {
                if (filename.matches(key))
                {
                    String val = mPoseMap.get(key);

                    for (String res : results)
                    {
                        ModelInspect inspect1 = Orm.newModel(ModelInspect.class);
                        ModelInspect inspect2 = Orm.newModel(ModelInspect.class);
                        inspect1.deserialize(res);
                        inspect2.deserialize(val);
                        if (!inspect1.matches(inspect2))
                        {
                            ModelLog.e("Failed:" + filename);
                            ModelLog.e("Failed Result:" + res);
                            return false;
                        }
                    }

                    ModelLog.e("Passed:" + filename);
                    return true;
                }
            }
        }
        else
        {
            ModelLog.e("Failed:" + filename);
            return false;
        }
        return false;
    }

    private static class DebugCursor extends AsyncTaskLoader<Cursor>
    {
        WeakReference<Activity> mActivity;

        public DebugCursor(Activity activity)
        {
            super(activity);
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public Cursor loadInBackground()
        {
            ArrayList<Model> list = new ArrayList<>();
            for (DebugModel.DebugItem item : DebugModel.DebugItem.values())
            {
                list.add(new DebugModel(item));
            }

            return CursorHolder.createCursor(DebugModel.class, list);
        }
    }

    /**
     * @hide
     */
    public interface DebugRunnable
    {
        void run(DebugModel.DebugItem debugItem, Activity activity, AsyncHelper.CallbackVoid callback);
    }

    /**
     * @hide
     */
    public static class DebugModel extends Model
    {
        /**
         * Here are the debug items to show in the nav menu...
         *
         * @hide
         */
        public enum DebugItem
        {
            TEST_HARNESS("Test Harness", (debugItem, activity, callback) ->
            {
                Intent visActivity = new Intent(GlobalContext.getContext(), DebugHarnessActivity.class);
                visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                GlobalContext.getContext().startActivity(visActivity);
            }),
            TEST_INSPECT_POSE("Test Inspect Pose", (debugItem, activity, callback) ->
            {
                Intent visActivity = new Intent(GlobalContext.getContext(), InspectPoseActivity.class);
                visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                GlobalContext.getContext().startActivity(visActivity);
            }),
            TEST_SEGMENT("Test Segment", (debugItem, activity, callback) ->
            {
                Intent visActivity = new Intent(GlobalContext.getContext(), SegmentActivity.class);
                visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                GlobalContext.getContext().startActivity(visActivity);
            }),
            TEST_JOINT("Test Joint", (debugItem, activity, callback) ->
            {
                Intent visActivity = new Intent(GlobalContext.getContext(), JointActivity.class);
                visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                GlobalContext.getContext().startActivity(visActivity);
            });
//            TEST_AVATAR("Test Avatar Upload Pass", (debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                boolean bInDevice = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false);
//                                boolean bRunJoints = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false);
//                                boolean bDebugPayload = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false);
//
//                                MyFiziq.getInstance().initInspect(true);
//                                ModelAvatar avatar = generateAvatar(activity, debugItem, -1, true, "", "", "");
//                                MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, true);
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in %s", debugItem.mTitle);
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            TEST_AVATAR_WEIGHT("Test Avatar Upload Weight", (debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//                builder.setTitle("Avatar weight");
//                final EditText input = new EditText(activity);
//                input.setInputType(InputType.TYPE_CLASS_NUMBER);
//                builder.setView(input);
//
//                builder.setPositiveButton(android.R.string.ok, (dialog, which) ->
//                {
//                    try
//                    {
//                        double weight = Double.parseDouble(input.getText().toString());
//                        AsyncHelper.run(() ->
//                                {
//                                    try
//                                    {
//                                        boolean bInDevice = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false);
//                                        boolean bRunJoints = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false);
//                                        boolean bDebugPayload = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false);
//                                        MyFiziq.getInstance().initInspect(true);
//                                        ModelAvatar avatar = generateAvatar(activity, debugItem, weight, true, "", "", "");
//                                        MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, true);
//                                    }
//                                    catch (Throwable t)
//                                    {
//                                        Timber.e(t, "Error in %s", debugItem.mTitle);
//                                    }
//                                },
//                                () ->
//                                {
//                                    setStatus(activity, debugItem.mTitle, false);
//
//                                    if (callback != null)
//                                    {
//                                        callback.execute();
//                                    }
//                                },
//                                true);
//                    }
//                    catch (NumberFormatException e)
//                    {
//                        Timber.e(e, "Cannot parse input %s", input.getText().toString());
//                    }
//                });
//                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
//                builder.show();
//            }),
//            TEST_AVATAR_FAILED("Test Avatar Upload Failure", (debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                ModelAvatar avatar = generateAvatar(activity, debugItem, -1, false, "", "", "");
//                                avatar.setStatus(Status.FailedGeneral);
//                                avatar.save();
//
//                                AvatarUploadWorker.announceProcessingError(activity.getApplicationContext(), avatar);
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in %s", debugItem.mTitle);
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            DEL_CAPTURES("Del Captures", (debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            String[] files = MiscUtils.getFiles(activity.getFilesDir(), ".*\\.bmp");
//                            for (String file : files)
//                            {
//                                File video = new File(activity.getFilesDir(), file);
//                                try
//                                {
//                                    video.delete();
//                                }
//                                catch (Throwable t)
//                                {
//                                }
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            DEL_AVATARS("Del Local Avatars", (debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            ArrayList<ModelAvatar> list = ORMTable.getModelList(ModelAvatar.class, null, null);
//                            for (ModelAvatar avatar : list)
//                            {
//                                avatar.delete();
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            TEST_JAVA_CRASH("Test Java Crash", ((debugItem, activity, callback) ->
//            {
//                // For testing Crashlytics
//                throw new RuntimeException("Test crash");
//            })),
//            TEST_GET_ASSETS("Test Get Assets List", ((debugItem, activity, callback) ->
//            {
//                MyFiziqAsset.getAssetList();
//            })),
//            TEST_GET_ASSET("Test Get Asset", ((debugItem, activity, callback) ->
//            {
//                new MyFiziqAsset(MyFiziqAsset.AssetType.MYQASSET_FILE, "test.png").fetchAsync((asset)->{});
//            })),
//            TEST_POLL_AVATARS("Test Poll Avatars", ((debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            MyFiziq.getInstance().pollAvatars();
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            })),
//            TEST_SEGFAULT("Test Segfault", ((debugItem, activity, callback) ->
//            {
//                // For testing Crashlytics
//                MyFiziq.getInstance().testSegfault();
//            })),
//            TEST_CAMERA_HIGH("Test Avatar Inspect High Camera", ((debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                boolean bInDevice = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false);
//                                boolean bRunJoints = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false);
//                                boolean bDebugPayload = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false);
//                                MyFiziq.getInstance().initInspect(true);
//                                ModelAvatar avatar = generateAvatar(activity, debugItem, -1, true, "_high_camera", "", "");
//                                MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, true);
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in %s", debugItem.mTitle);
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            })),
//            TEST_CAMERA_LOW("Test Avatar Inspect Low Camera", ((debugItem, activity, callback) ->
//            {
//                ModelLog.w(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                boolean bInDevice = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false);
//                                boolean bRunJoints = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false);
//                                boolean bDebugPayload = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false);
//                                MyFiziq.getInstance().initInspect(true);
//                                ModelAvatar avatar = generateAvatar(activity, debugItem, -1, true, "_low_camera", "", "");
//                                MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, true);
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in %s", debugItem.mTitle);
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            })),
//            TEST_APISIG("Test Api Sig", ((debugItem, activity, callback) ->
//            {
//                AsyncHelper.run(
//                        () ->
//                        {
//                            MyFiziq mfz = MyFiziq.getInstance();
//
//                            @SuppressLint("UseValueOf")
//                            Integer responseCode = new Integer(0); // NOSONAR
//                            mfz.apiGet(
//                                    "",
//                                    "https://resources.myfiziq.io/index.html",
//                                    responseCode,
//                                    0,
//                                    0,
//                                    FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS, FLAG.FLAG_NOBASE, FLAG.FLAG_SIGN_URL)
//                            );
//                        },
//                        () ->
//                        {
//                        }, true);
//            })),
//            TEST_AVATAR_VIS("Test Avatar Vis", ((debugItem, activity, callback) ->
//            {
//                Intent visActivity = new Intent(GlobalContext.getContext(), DebugAvatarActivity.class);
//                visActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                GlobalContext.getContext().startActivity(visActivity);
//            })),
//            TEST_TENSOR("Test Tensor", (debugItem, activity, callback) ->
//            {
//                ModelLog.e(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                copyImagesToStorage(activity);
//
//                                MyFiziq.SetFlag(
//                                        "visualize",
//                                        String.valueOf(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false)));
//
//                                MyFiziq.getInstance().initInspect(true);
//
//                                Stopwatch stopwatch = new Stopwatch(debugItem.mTitle);
//
//                                String[] filenames = MiscUtils.getFiles(activity.getFilesDir(), "[MF]_.*\\.bmp");
//
//                                Arrays.sort(filenames, String::compareTo);
//
//                                if (null != filenames && filenames.length > 0)
//                                {
//                                    for (String file : filenames)
//                                    {
//                                        File image = new File(activity.getFilesDir(), file);
//                                        String[] parts = file.split("_");
//                                        Gender gender = Gender.valueOf(parts[0]);
//                                        float height = Float.valueOf(parts[1]);
//                                        float weight = Float.valueOf(parts[2]);
//                                        PoseSide side = PoseSide.valueOf(parts[3]);
//
//                                        MyFiziq myFiziq = MyFiziq.getInstance();
//
//                                        String id = myFiziq.getContourId(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1);
//
//                                        myFiziq.getContour(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1, id);
//
//                                        validatePose(file, myFiziq.testInspect2(side, id, new String[]{image.getAbsolutePath()}));
//                                    }
//                                }
//
//                                stopwatch.print();
//
//                                MyFiziq.getInstance().releaseInspect();
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in Test Tensor");
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            TEST_INIT_TENSOR_PASS("Test Init Tensor Pass", (debugItem, activity, callback) ->
//            {
//                ModelLog.e(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                Stopwatch stopwatch = new Stopwatch(debugItem.mTitle);
//
//                                copyImagesToStorage(activity);
//
//                                MyFiziq.SetFlag(
//                                        "visualize",
//                                        String.valueOf(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false)));
//
//                                MyFiziq.getInstance().initInspect(true);
//
//                                String[] filenames = MiscUtils.getFiles(activity.getFilesDir(), "[MF]_.*\\.bmp");
//
//                                if (null != filenames && filenames.length > 0)
//                                {
//                                    for (String file : filenames)
//                                    {
//                                        File image = new File(activity.getFilesDir(), file);
//                                        if (image.getName().contains("_pass"))
//                                        {
//                                            String[] parts = file.split("_");
//                                            if (parts.length > 4)
//                                            {
//                                                Gender gender = Gender.valueOf(parts[0]);
//                                                float height = Float.valueOf(parts[1]);
//                                                float weight = Float.valueOf(parts[2]);
//                                                PoseSide side = PoseSide.valueOf(parts[3]);
//
//                                                MyFiziq myFiziq = MyFiziq.getInstance();
//
//                                                String id = myFiziq.getContourId(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1);
//                                                myFiziq.getContour(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1, id);
//                                            }
//                                        }
//                                    }
//                                }
//
//                                stopwatch.print();
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in Test Tensor");
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            }),
//            TEST_TENSOR_PASS("Test Tensor Pass", (debugItem, activity, callback) ->
//            {
//                ModelLog.e(TAG, debugItem.mTitle);
//                setStatus(activity, debugItem.mTitle, true);
//                AsyncHelper.run(() ->
//                        {
//                            try
//                            {
//                                copyImagesToStorage(activity);
//
//                                MyFiziq.SetFlag(
//                                        "visualize",
//                                        String.valueOf(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false)));
//
//                                MyFiziq.getInstance().initInspect(true);
//
//                                Stopwatch stopwatch = new Stopwatch(debugItem.mTitle);
//
//                                String[] filenames = MiscUtils.getFiles(activity.getFilesDir(), "[MF]_.*\\.bmp");
//
//                                if (null != filenames && filenames.length > 0)
//                                {
//                                    for (String file : filenames)
//                                    {
//                                        File image = new File(activity.getFilesDir(), file);
//                                        if (image.getName().contains("_pass"))
//                                        {
//                                            String[] parts = file.split("_");
//                                            if (parts.length > 4)
//                                            {
//                                                Gender gender = Gender.valueOf(parts[0]);
//                                                float height = Float.valueOf(parts[1]);
//                                                float weight = Float.valueOf(parts[2]);
//                                                PoseSide side = PoseSide.valueOf(parts[3]);
//
//                                                MyFiziq myFiziq = MyFiziq.getInstance();
//
//                                                String id = myFiziq.getContourId(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1);
//
//                                                myFiziq.getContour(255, 255, 1280, 720, height, weight, gender, side, 0.0f, 1, id);
//
//                                                validatePose(file, myFiziq.testInspect2(side, id, new String[]{image.getAbsolutePath()}));
//                                            }
//                                        }
//                                    }
//                                }
//
//                                stopwatch.print();
//                            }
//                            catch (Throwable t)
//                            {
//                                Timber.e(t, "Error in Test Tensor");
//                            }
//                        },
//                        () ->
//                        {
//                            setStatus(activity, debugItem.mTitle, false);
//
//                            if (callback != null)
//                            {
//                                callback.execute();
//                            }
//                        },
//                        true);
//            });

            String mTitle;
            DebugRunnable mRunnable;

            DebugItem(String title, DebugRunnable runnable)
            {
                mTitle = title;
                mRunnable = runnable;
            }
        }

        @Persistent
        DebugItem mItem;

        public DebugModel()
        {
            super();
        }

        public DebugModel(DebugItem item)
        {
            id = item.mTitle;
            mItem = item;
        }

        public void run(DebugItem debugItem, Activity activity, AsyncHelper.CallbackVoid callback)
        {
            mItem.mRunnable.run(debugItem, activity, callback);
        }

        static void setStatus(Activity activity, String title, boolean bVisible)
        {
            ((DebugActivity) activity).setStatus(title, bVisible);
        }

        @Nullable
        private static ModelAvatar generateAvatar(@NonNull Activity activity, @NonNull DebugItem debugItem, double weight, boolean bInspect, String cameraHeightSourceFilter, String frontSourceIn, String sideSourceIn) throws IOException
        {
            copyImagesToStorage(activity);

            MyFiziq.SetFlag(
                    "visualize",
                    String.valueOf(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false)));

            long testStart = System.currentTimeMillis();

            String frontSource = frontSourceIn;
            String sideSource = sideSourceIn;

            // find two matching .*front_pass, .*side_pass
            String[] filenames = MiscUtils.getFiles(activity.getFilesDir(), "[MF]_.*\\.bmp");

            if (null != filenames && filenames.length > 0)
            {
                for (String file : filenames)
                {
                    if (TextUtils.isEmpty(frontSource))
                    {
                        if(cameraHeightSourceFilter.isEmpty() && isFrontSource(file, "_pass"))
                        {
                            // if no cameraHeightSourceFilter specified, match *_pass files for source images
                            if (TextUtils.isEmpty(sideSource))
                            {
                                frontSource = file;
                            }
                            else if (isSourceMatch(file, sideSource, 3))
                            {
                                frontSource = file;
                            }
                        }
                        else if (isFrontSource(file, cameraHeightSourceFilter))
                        {
                            // if cameraHeightSourceFilter specified, match that for the source image instead
                            if (TextUtils.isEmpty(sideSource))
                            {
                                frontSource = file;
                            }
                            else if (isSourceMatch(file, sideSource, 3))
                            {
                                frontSource = file;
                            }
                        }
                    }
                    else if (TextUtils.isEmpty(sideSource) && isSideSource(file, "_pass"))
                    {
                        if (TextUtils.isEmpty(frontSource))
                        {
                            sideSource = file;
                        }
                        else if (isSourceMatch(file, frontSource, 3))
                        {
                            sideSource = file;
                        }
                    }

                    if (!TextUtils.isEmpty(frontSource) && !TextUtils.isEmpty(sideSource))
                        break;
                }
            }

            if (!TextUtils.isEmpty(frontSource) && !TextUtils.isEmpty(sideSource))
            {
                String[] parts = frontSource.split("_");
                if (parts.length > 4)
                {
                    ModelAvatar avatar = Orm.newModel(ModelAvatar.class);
                    Gender gender = Gender.valueOf(parts[0]);
                    float height = Float.valueOf(parts[1]);

                    PoseSide side = PoseSide.valueOf(parts[3]);
                    int nFrames = ModelAvatar.getCaptureFrames();

                    if (weight < 0)
                        weight = Float.valueOf(parts[2]);

                    avatar.set(gender, new Centimeters(height), new Kilograms(weight), nFrames);
                    avatar.set(gender, new Centimeters(height), new Kilograms(weight));

                    avatar.setSensorValues(0.9612565815448761, -0.9410139858722687, 15.830237483978271, 0, 9.8, 0);

                    List<String> frontImages = new LinkedList<>();
                    List<String> sideImages = new LinkedList<>();

                    for (int i = 0; i < nFrames; i++)
                    {
                        frontImages.add(createAvatarFrame(activity, avatar, PoseSide.front, frontSource, i));
                        sideImages.add(createAvatarFrame(activity, avatar, PoseSide.side, sideSource, i));
                    }

                    if (bInspect)
                    {
                        inspectAvatar(activity, avatar, PoseSide.front, frontImages);
                        inspectAvatar(activity, avatar, PoseSide.side, sideImages);
                    }

                    avatar.save();
                    avatar.setStatus(Status.Pending);

                    long testTookTime = System.currentTimeMillis() - testStart;
                    ModelLog.d("STOPWATCH: " + debugItem.mTitle + " took: " + testTookTime + " ms");

                    return avatar;
                }
            }

            return null;
        }

        static ModelAvatar generateAvatar(@NonNull Activity activity, @NonNull DebugItem debugItem, double weight, double height, Gender gender, boolean bInspect, Uri frontSourceUri, String frontSourceName, Uri sideSourceUri, String sideSourceName) throws IOException
        {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), frontSourceUri);
            BmpUtil.save(bitmap,activity.getFilesDir() + "/" + frontSourceName);

            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), sideSourceUri);
            BmpUtil.save(bitmap,activity.getFilesDir() + "/" + sideSourceName);

            MyFiziq.SetFlag(
                    "visualize",
                    String.valueOf(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_VISUALIZE, false)));

            long testStart = System.currentTimeMillis();


            if (!TextUtils.isEmpty(frontSourceName) && !TextUtils.isEmpty(sideSourceName))
            {
                ModelAvatar avatar = Orm.newModel(ModelAvatar.class);

                int nFrames = ModelAvatar.getCaptureFrames();

                avatar.set(gender, new Centimeters(height), new Kilograms(weight), nFrames);
                avatar.set(gender, new Centimeters(height), new Kilograms(weight));

                avatar.setSensorValues(0.9612565815448761, -0.9410139858722687, 15.830237483978271, 0, 9.8, 0);

                List<String> frontImages = new LinkedList<>();
                List<String> sideImages = new LinkedList<>();

                for (int i = 0; i < nFrames; i++)
                {
                    frontImages.add(createAvatarFrame(activity, avatar, PoseSide.front, frontSourceName, i));
                    sideImages.add(createAvatarFrame(activity, avatar, PoseSide.side, sideSourceName, i));
                }

                if (bInspect)
                {
                    inspectAvatar(activity, avatar, PoseSide.front, frontImages);
                    inspectAvatar(activity, avatar, PoseSide.side, sideImages);
                }

                avatar.save();
                avatar.setStatus(Status.Pending);

                long testTookTime = System.currentTimeMillis() - testStart;
                ModelLog.d("STOPWATCH: " + debugItem.mTitle + " took: " + testTookTime + " ms");

                return avatar;
            }
            return null;
        }

        /**
         * Clones test data to create the files for a specific attempt ID.
         */
        private static String createAvatarFrame(Activity activity, ModelAvatar avatar, PoseSide side, String source, int frame)
        {
            File sourceFile = new File(activity.getFilesDir(), source);
            File destFile = new File(sourceFile.getParent(), side.name() + avatar.getAttemptId() + "_" + frame + ".bmp");

            try
            {
                copy(sourceFile, destFile);
                return destFile.getAbsolutePath();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return "";
        }

        private static void inspectAvatar(Activity activity, ModelAvatar avatar, PoseSide side, List<String> images)
        {
            // Upload requires joints...
            MyFiziq myFiziqSdk = MyFiziq.getInstance();
            int nFrames = ModelAvatar.getCaptureFrames();

            String[] sourceImageArray = new String[nFrames];
            sourceImageArray = images.toArray(sourceImageArray);

            String imageBaseName = images.get(0).replace(".bmp", "");
            String id = myFiziqSdk.getContourId(255, 255, 1280, 720, avatar.getHeight().getValueInCm(), avatar.getWeight().getValueInKg(), avatar.getGender(), side, 0.0f, 0);

            String[] results = myFiziqSdk.inspect(
                    side,
                    id,
                    sourceImageArray,
                    imageBaseName,
                    ModelSetting.getSetting(ModelSetting.Setting.DEBUG_DISABLE_ALIGNMENT, false));

            String result = MiscUtils.join(Character.toString((char) 1), results);
            switch (side)
            {
                case front:
                    avatar.setFrontInspectResult(result);
                    break;

                case side:
                    avatar.setSideInspectResult(result);
                    break;
            }
        }

        public static void segment(Activity activity, ModelAvatar avatar, double height, double weight, String gender, boolean writeOutput, Uri frontImageUri, String frontImageName, Uri sideImageUri, String sideImageName) throws IOException
        {

            //need to copy out files
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), frontImageUri);
            BmpUtil.save(bitmap,activity.getFilesDir() + "/" + frontImageName);

            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), sideImageUri);
            BmpUtil.save(bitmap,activity.getFilesDir() + "/" + sideImageName);

            //then make 4x captures
            int nFrames = ModelAvatar.getCaptureFrames();
            for (int i = 0; i < nFrames; i++)
            {
                createAvatarFrame(activity, avatar, PoseSide.front, frontImageName, i);
                createAvatarFrame(activity, avatar, PoseSide.side, sideImageName, i);
            }

            MyFiziq.getInstance().testSegment(PoseSide.front.ordinal(), height, weight, gender, avatar.getId(), writeOutput);
            MyFiziq.getInstance().testSegment(PoseSide.side.ordinal(), height, weight, gender, avatar.getId(), writeOutput);
        }

        public static void copy(File src, File dst) throws IOException
        {
            try (InputStream in = new FileInputStream(src))
            {
                try (OutputStream out = new FileOutputStream(dst))
                {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }

}
