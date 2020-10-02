package com.myfiziq.sdk.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myfiziq.sdk.MyFiziq;
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
import com.myfiziq.sdk.db.ModelLog;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Persistent;
import com.myfiziq.sdk.db.PoseSide;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.FLAG;
import com.myfiziq.sdk.util.BaseUtils;
import com.myfiziq.sdk.util.GlobalContext;
import com.myfiziq.sdk.util.MiscUtils;
import com.myfiziq.sdk.util.Stopwatch;
import com.myfiziq.sdk.views.ItemViewDebugHarness;
import com.myfiziq.sdk.views.ItemViewLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @hide
 */
public class DebugHarnessActivity extends BaseActivity implements RecyclerManagerInterface, CursorHolder.CursorChangedListener, SearchView.OnQueryTextListener, LifecycleOwner, ViewModelStoreOwner
{
    private static final String TAG = DebugHarnessActivity.class.getSimpleName();

    private LoaderManager mLoaderManager;
    private RecyclerManager mManager;
    private Parameter mParameterLog;
    private Parameter mParameterItems;

    private Toolbar toolbar;
    private RecyclerView recyclerTop;
    private RecyclerView recyclerBottom;
    private RecyclerView recyclerDrawer;
    private DrawerLayout drawerLayout;
    private SearchView searchView;
    private View statusContainer;
    private TextView statusText;

    private ExecutorService mThreadQueue = Executors.newSingleThreadExecutor();

    private ArrayList<String> mAttempts = new ArrayList<>();

    File mTempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        setContentView(R.layout.activity_debug_harness);

        toolbar = findViewById(R.id.toolbar);
        recyclerTop = findViewById(R.id.recyclerTop);
        recyclerBottom = findViewById(R.id.recyclerBottom);
        recyclerDrawer = findViewById(R.id.recyclerDrawer);
        drawerLayout = findViewById(R.id.drawer_layout);
        statusContainer = findViewById(R.id.statusContainer);
        statusText = findViewById(R.id.statusText);

        searchView = findViewById(R.id.search);

        searchView.setOnQueryTextListener(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Debug Harness");
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
                        DebugCursor debugCursor = new DebugCursor(DebugHarnessActivity.this);
                        debugCursor.forceLoad();
                        return debugCursor;
                    }

                    return super.onCreateLoader(id, bundle);
                }

                return null;
            }
        };

        mParameterLog = new Parameter.Builder()
                .setLoaderId(0)
                .setModel(ModelLog.class)
                .setView(ItemViewLog.class)
                .setOrder("pk DESC")
                .build();

        mManager.setupRecycler(
                null,
                recyclerBottom,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(mParameterLog)
                        .build()
        );

        mParameterItems = new Parameter.Builder()
                        .setLoaderId(1)
                        .setModel(DebugModel.class)
                        .setView(ItemViewDebugHarness.class)
                        .build();
        mParameterItems.getHolder().addListener(this);
        mParameterItems.getHolder().setFilterQueryProvider(constraint -> DebugCursor.loadCursor(constraint));

        mManager.setupRecycler(
                null,
                recyclerTop,
                new ParameterSet.Builder()
                        .setLayout(LayoutStyle.VERTICAL)
                        .addParam(mParameterItems)
                        .build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_debug_harness, menu);
        menu.findItem(R.id.menu_item_indevice).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false));
        menu.findItem(R.id.menu_item_runjoints).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false));
        menu.findItem(R.id.menu_item_debugpayload).setChecked(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false));
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
        else if (itemId == R.id.menu_item_set_server)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Set Server\n(must be http url)");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(ModelSetting.getSetting(ModelSetting.Setting.DEBUG_HARNESS, ""));
            builder.setView(input);

            builder.setPositiveButton(android.R.string.ok, (dialog, which) ->
            {
                String url = input.getText().toString();
                ModelSetting.putSetting(ModelSetting.Setting.DEBUG_HARNESS, url);
                getAttempts();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

            builder.show();
            return true;
        }
        else if (itemId == R.id.menu_item_process_all)
        {
            runModels();
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
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    private void runModels()
    {

        int count = mParameterItems.getHolder().getItemCount();
        for( int ix =0; ix < count; ix++)
        {
            Model m = mParameterItems.getHolder().getItem(ix);
            if (m instanceof DebugModel)
            {
                mThreadQueue.submit(() -> ((DebugModel)m).generateAvatar(DebugHarnessActivity.this));
            }
        }
    }

    private void getAttempts()
    {
        mAttempts.clear();
        AsyncHelper.run(() ->
        {
            return getApiSync("/attempts");
        }, (result) ->
        {
            if (!TextUtils.isEmpty(result))
            {
                JsonElement rootElement = JsonParser.parseString(result);
                JsonObject object = rootElement.getAsJsonObject();
                if (object.has("attemptIDs"))
                {
                    JsonArray array = object.getAsJsonArray("attemptIDs");
                    Iterator<JsonElement> elementIterator = array.iterator();
                    while (elementIterator.hasNext())
                    {
                        JsonElement element = elementIterator.next();
                        mAttempts.add(element.getAsString());
                    }
                }
            }
            mManager.reloadCursor(mParameterItems.getHolder());
        }, true);
    }

    public static String getApiSync(String endpoint)
    {
        String url = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_HARNESS, "");
        if (!TextUtils.isEmpty(url))
        {
            @SuppressLint("UseValueOf")
            Integer responseCode = new Integer(0); // NOSONAR
            return MyFiziq.getInstance().apiGet("", url+endpoint, responseCode, 0, 0, FLAG.getFlags(FLAG.FLAG_NO_EXTRA_HEADERS, FLAG.FLAG_NOBASE, FLAG.FLAG_RESPONSE));
        }

        return "";
    }

    public ArrayList<String> getAttemptList()
    {
        return mAttempts;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
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
                    AsyncHelper.run(()->{
                        model.generateAvatar(this);
                    });
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }
            }
        });
        return list;
    }

    @Override
    public void onCursorChanged(CursorHolder cursorHolder)
    {
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        if (null != mParameterItems && null != mParameterItems.getHolder())
        {
            mParameterItems.getHolder().getFilter().filter(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query)
    {
        if (null != mParameterItems && null != mParameterItems.getHolder())
        {
            mParameterItems.getHolder().getFilter().filter(query);
        }
        return false;
    }


    private void setStatus(String title, boolean bVisible)
    {
        statusText.setText(title);
        statusContainer.setVisibility((bVisible ? View.VISIBLE : View.INVISIBLE));
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
            return loadCursor("");
        }

        public static Cursor loadCursor(CharSequence filter)
        {
            //IndexedCollection<Model> ids = new ConcurrentIndexedCollection<>();
            //ids.addIndex(NavigableIndex.onAttribute(Model.));
            //SQLParser<String> parser = SQLParser.forPojoWithAttributes()

            ArrayList<Model> list = new ArrayList<>();
            String result = DebugHarnessActivity.getApiSync("/attempts");
            if (!TextUtils.isEmpty(result))
            {
                boolean bFilter = !TextUtils.isEmpty(filter);
                JsonElement rootElement = JsonParser.parseString(result);
                JsonObject object = rootElement.getAsJsonObject();
                if (object.has("attemptIDs"))
                {
                    JsonArray array = object.getAsJsonArray("attemptIDs");
                    Iterator<JsonElement> elementIterator = array.iterator();
                    while (elementIterator.hasNext())
                    {
                        JsonElement element = elementIterator.next();
                        String id = element.getAsString();
                        if (!bFilter || id.contains(filter))
                        {
                            list.add(new DebugModel(id));
                        }
                    }
                }
            }

            return CursorHolder.createCursor(DebugModel.class, list);
        }
    }

    /**
     * @hide
     */
    public static class DebugModel extends Model
    {
        public DebugModel()
        {
            super();
        }

        public DebugModel(String item)
        {
            id = item;
        }

        @Nullable
        public void generateAvatar(@NonNull Activity activity)
        {
            Stopwatch testTime = new Stopwatch(id);
            boolean bInDevice = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_INDEVICE, false);
            boolean bRunJoints = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_RUNJOINTS, false);
            boolean bDebugPayload = ModelSetting.getSetting(ModelSetting.Setting.DEBUG_PAYLOAD, false);
            int nFrames = ModelAvatar.getCaptureFrames();
            ModelAvatar avatar = Orm.newModel(ModelAvatar.class);

            // Get User (height/weight/gender)...
            UserModel user = new UserModel();
            user.deserialize(DebugHarnessActivity.getApiSync("/meta/"+id+"/0/user"));

            avatar.set(user.gender, new Centimeters(user.height), new Kilograms(user.weight), nFrames);
            avatar.set(user.gender, new Centimeters(user.height), new Kilograms(user.weight));
            avatar.id = getId();
            avatar.setAttemptId(getId());

            // Get Front and Side images...
            File destFile;
            File filesDir = GlobalContext.getContext().getFilesDir();
            Base64Model imageModel = new Base64Model();
            SensorModel sensorModel = new SensorModel();
            List<String> frontImages = new LinkedList<>();
            List<String> sideImages = new LinkedList<>();
            for (int i=0; i<nFrames; i++)
            {
                // Get front image for index
                imageModel.deserialize(DebugHarnessActivity.getApiSync("/b64/"+id+"/"+i+"/front/bmp"));
                destFile = new File(filesDir, "front" + avatar.getAttemptId() + "_" + i + ".bmp");
                imageModel.writeBinary(destFile);
                frontImages.add(destFile.getAbsolutePath());

                // Get side image for index
                imageModel.deserialize(DebugHarnessActivity.getApiSync("/b64/"+id+"/"+i+"/side/bmp"));
                destFile = new File(filesDir, "side" + avatar.getAttemptId() + "_" + i + ".bmp");
                imageModel.writeBinary(destFile);
                sideImages.add(destFile.getAbsolutePath());
            }

            sensorModel.deserialize(DebugHarnessActivity.getApiSync("/meta/"+id+"/"+0+"/front"));
            avatar.setSensorValues(sensorModel.yaw, sensorModel.pitch, sensorModel.roll, sensorModel.GravityX, sensorModel.GravityY, sensorModel.GravityZ);

            inspectAvatar(activity, avatar, PoseSide.front, frontImages);
            inspectAvatar(activity, avatar, PoseSide.side, sideImages);

            avatar.save();

            MyFiziq.getInstance().uploadAvatar(avatar.getId(), GlobalContext.getContext().getFilesDir().getAbsolutePath(), null, bInDevice, bRunJoints, bDebugPayload, false);

            // Cleanup image files...
            for (int i=0; i<nFrames; i++)
            {
                destFile = new File(filesDir, "front" + avatar.getAttemptId() + "_" + i + ".bmp");
                if (destFile.exists())
                {
                    destFile.delete();
                }

                // Get side image for index
                destFile = new File(filesDir, "side" + avatar.getAttemptId() + "_" + i + ".bmp");
                if (destFile.exists())
                {
                    destFile.delete();
                }
            }

            testTime.print();
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
                    false);

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

    public static class UserModel extends Model
    {
        @Persistent
        public float weight = 0;

        @Persistent
        public float height = 0;

        @Persistent
        public Gender gender = Gender.M;
    }

    public static class Base64Model extends Model
    {
        @Persistent
        public String base64 = "";

        public void writeBinary(File destFile)
        {
            try (OutputStream out = new FileOutputStream(destFile))
            {
                // Decode base64 and write to file.
                byte[] buf = Base64.decode(base64, Base64.DEFAULT);
                out.write(buf, 0, buf.length);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    public static class Base91Model extends Model
    {
        @Persistent
        public String base91 = "";

        public void writeBinary(File destFile)
        {
            try (OutputStream out = new FileOutputStream(destFile))
            {
                // Decode base64 and write to file.
                byte[] buf = BaseUtils.decode(base91, BaseUtils.Format.string);
                out.write(buf, 0, buf.length);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    public static class SensorModel extends Model
    {
        @Persistent
        float pitch = 0.0f;
        @Persistent
        float GravityZ = 0.0f;
        @Persistent
        float roll = 0.0f;
        @Persistent
        float GravityY = 0.0f;
        @Persistent
        float yaw = 0.0f;
        @Persistent
        float GravityX = 0.0f;
    }
}
