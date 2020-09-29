package com.myfiziq.sdk.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.myfiziq.sdk.MyFiziq;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.Gender;
import com.myfiziq.sdk.db.Length;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ModelSetting;
import com.myfiziq.sdk.db.ModelSisterStyle;
import com.myfiziq.sdk.db.ORMTable;
import com.myfiziq.sdk.db.Orm;
import com.myfiziq.sdk.db.Weight;
import com.myfiziq.sdk.gles.Shader;
import com.myfiziq.sdk.gles.ShaderPhong;
import com.myfiziq.sdk.gles.Vector3D;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.SisterAppStyleDownloader;
import com.myfiziq.sdk.helpers.SisterColors;
import com.myfiziq.sdk.lifecycle.StyleInterceptor;
import com.myfiziq.sdk.util.UiUtils;
import com.myfiziq.sdk.views.AvatarLayout;
import com.myfiziq.sdk.views.AvatarViewSpinner;
import com.myfiziq.sdk.views.DrawableView;

import java.util.Date;

import androidx.annotation.Nullable;
import io.github.inflationx.viewpump.ViewPump;

/**
 * @hide
 */
public class DebugStyleActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener
{
    private LinearLayout container;
    private EditText styleText;
    private CheckBox enabled;
    private Button apply;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_style);
        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView()
    {
        container = findViewById(R.id.container);
        styleText = findViewById(R.id.styleText);
        enabled = findViewById(R.id.enabled);
        enabled.setChecked(SisterColors.getInstance().isSisterMode());
        enabled.setOnCheckedChangeListener(this);
        apply = findViewById(R.id.apply);
        apply.setOnClickListener(this);

        String json = MyFiziq.getInstance().getKey(ModelSetting.Setting.STYLE);
        if (TextUtils.isEmpty(json))
        {
            SisterAppStyleDownloader.getStyling((responseCode1, result1, payload) ->
            {
                styleText.setText(MyFiziq.getInstance().getKey(ModelSetting.Setting.STYLE));
            });
        }
        else
        {
            styleText.setText(json);
        }

        ModelSisterStyle model = Orm.newModel(ModelSisterStyle.class);
        model.deserialize(json);

    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();

        if (R.id.apply == id)
        {
            MyFiziq.getInstance().setKey(ModelSetting.Setting.STYLE, styleText.getText().toString());
            applyStyle();
        }
    }

    private void applyStyle()
    {
        if (enabled.isChecked())
        {
            SisterColors sisterColors = SisterColors.getInstance();
            sisterColors.init(styleText.getText().toString());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_STYLING, true);
            applyStyle();
        }
        else
        {
            ModelSetting.putSetting(ModelSetting.Setting.DEBUG_STYLING, false);

            SisterColors.getInstance().setSisterMode(false);
        }
    }
}
