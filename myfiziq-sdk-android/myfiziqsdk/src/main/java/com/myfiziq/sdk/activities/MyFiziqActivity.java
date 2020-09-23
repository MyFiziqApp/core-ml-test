package com.myfiziq.sdk.activities;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.Parameter;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import java.util.Date;

import androidx.annotation.Keep;
import timber.log.Timber;

/**
 * <code>MyFiziqActivity</code> - The entry point for an Avatar capture process...
 * - The <code>Intent</code> passed to the activity must contain a <code>ParameterSet</code> as
 * <code>getParcelableExtra("args")</code>.
 * <br>
 * - The <code>ParameterSet</code> must contain the Gender, Height & Weight in the following values:
 * <br>
 * <br>
 * <code>R.id.TAG_ARG_GENDER</code> - <code>Gender.name()</code>.<br>
 * <code>R.id.TAG_ARG_HEIGHT</code> - double value for height.<br>
 * <code>R.id.TAG_ARG_WEIGHT</code> - double value for weight.<br>
 * <br>
 *
 * @see ParameterSet
 */
@Keep
public class MyFiziqActivity extends BaseActivity
{
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            Timber.e("SDK is no longer initialised. Shutting down...");
            getActivity().finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        setContentView(getLayoutId());

        // TODO Remove default parameters??
        Gender gender = Gender.M;
        float heightInCm = 150;
        float weightInKg = 50;
        String preferredHeightUnits = "";
        String preferredWeightUnits = "";

        if (null != mParameterSet)
        {
            if (
                    mParameterSet.hasParam(R.id.TAG_ARG_GENDER) &&
                            mParameterSet.hasParam(R.id.TAG_ARG_HEIGHT_IN_CM) &&
                            mParameterSet.hasParam(R.id.TAG_ARG_WEIGHT_IN_KG) &&
                            mParameterSet.hasParam(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS) &&
                            mParameterSet.hasParam(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS))

            {
                try
                {
                    gender = Gender.valueOf(mParameterSet.getParam(R.id.TAG_ARG_GENDER).getValue());
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot parse gender");
                }

                try
                {
                    heightInCm = Float.valueOf(mParameterSet.getParam(R.id.TAG_ARG_HEIGHT_IN_CM).getValue());
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot parse height");
                }

                try
                {
                    weightInKg = Float.valueOf(mParameterSet.getParam(R.id.TAG_ARG_WEIGHT_IN_KG).getValue());
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot parse weight");
                }

                try
                {
                    preferredHeightUnits = mParameterSet.getParam(R.id.TAG_ARG_PREFERRED_HEIGHT_UNITS).getValue();
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot parse height units of measurement");
                }

                try
                {
                    preferredWeightUnits = mParameterSet.getParam(R.id.TAG_ARG_PREFERRED_WEIGHT_UNITS).getValue();
                }
                catch (Exception e)
                {
                    Timber.e(e, "Cannot parse weight units of measurement");
                }

                Length heightObject = Length.fromCentimeters(preferredHeightUnits, heightInCm);
                Weight weightObject = Weight.fromKilograms(preferredWeightUnits, weightInKg);

                // Add a new ModelAvatar parameter to the next set (prob capture).
                ParameterSet set = mParameterSet.getNext();
                if (null != set)
                {
                    ModelAvatar avatar = Orm.newModel(ModelAvatar.class);
                    // TODO Do we need to put the units of measurement here?
                    avatar.set(gender, heightObject, weightObject, ModelAvatar.getCaptureFrames());
                    set.addParam(new Parameter(com.myfiziq.sdk.R.id.TAG_ARG_MODEL_AVATAR, avatar));
                }

                mParameterSet.startNext(this, true);
            }
            else
            {
                mParameterSet.startNext(this, true);
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
          Do not put this in onDestroy.
          onDestroy is to perform any final cleanup before an activity is destroyed AND it is no longer visible to the user.
          Therefore, it's not guaranteed to immediately run (if at all) and when it does run, it only runs when the activity
          is not visible to the user. This is something we do not want since the user must be taken to the right location
          after this activity finishes.
        */
        if (isFinishing())
        {
            IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<>(this);
            intentManagerService.respond(
                    IntentResponses.MYFIZIQ_ACTIVITY_FINISHING,
                    null
            );
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!MyFiziqSdkManager.isSdkInitialised())
        {
            Timber.e("SDK is no longer initialised. Shutting down...");
            finish();
        }
    }

    /**
     * @hide
     */
    protected int getLayoutId()
    {
        return R.layout.activity_myfiziq;
    }
}
