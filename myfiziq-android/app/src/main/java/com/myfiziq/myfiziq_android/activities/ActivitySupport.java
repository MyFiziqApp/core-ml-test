package com.myfiziq.myfiziq_android.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.myfiziq.myfiziq_android.R;
import com.myfiziq.sdk.db.ModelAvatar;
import com.myfiziq.sdk.db.ORMDbQueries;
import com.myfiziq.sdk.enums.IntentResponses;
import com.myfiziq.sdk.enums.SupportType;
import com.myfiziq.sdk.fragments.BaseFragment;
import com.myfiziq.sdk.helpers.ActionBarHelper;
import com.myfiziq.sdk.helpers.AsyncHelper;
import com.myfiziq.sdk.helpers.RegexHelpers;
import com.myfiziq.sdk.intents.IntentManagerService;
import com.myfiziq.sdk.lifecycle.ParameterSet;
import com.myfiziq.sdk.manager.MyFiziqSdkManager;
import com.myfiziq.sdk.views.ScrollableEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import timber.log.Timber;

public class ActivitySupport extends BaseFragment
{
    private ScrollableEditText messageText;
    private CheckBox termsCheckbox;
    private Button continueButton;
    private ProgressBar buttonProgressBar;
    private AppCompatSpinner inputSpinner;
    private EditText nameField;
    private EditText emailField;
    private Snackbar snackbar;
    private String supportType;

    @Override
    protected int getFragmentLayout()
    {
        return R.layout.activity_support;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        super.onCreate(savedInstanceState);

        Bundle extras = getArguments();
        if (extras == null)
        {
            Timber.e("No extras passed to support activity");
            return view;
        }
        ParameterSet parameterSet = extras.getParcelable(BaseFragment.BUNDLE_PARAMETERS);
        if (parameterSet == null)
        {
            Timber.e("no BUNDLE_PARAMETERS in support activity extras");
            return view;
        }
        if (parameterSet.hasParam(R.id.TAG_ARG_VIEW))
        {
            supportType = parameterSet.getParam(R.id.TAG_ARG_VIEW).getValue();
        }
        else
        {
            Timber.e("No Support Type parameter in parameter set");
            return view;
        }

        ModelAvatar modelAvatar = null;
        if (parameterSet.hasParam(R.id.TAG_ARG_MODEL_AVATAR))
        {
            modelAvatar = (ModelAvatar) parameterSet.getParam(R.id.TAG_ARG_MODEL_AVATAR).getParcelableValue();
        }
        else
        {
            ArrayList<ModelAvatar> avatars = ORMDbQueries.getLastXCompletedAvatars(1);
            if (!avatars.isEmpty())
            {
                modelAvatar = avatars.get(0);
            }
        }

        // Bind View
        messageText = view.findViewById(R.id.messageText);
        termsCheckbox = view.findViewById(R.id.checkboxTerms);
        continueButton = view.findViewById(R.id.btnContinue);
        buttonProgressBar = view.findViewById(R.id.btnProgressBar);
        inputSpinner = view.findViewById(R.id.spinner);
        nameField = view.findViewById(R.id.nameField);
        emailField = view.findViewById(R.id.emailField);


        if (supportType.equals(SupportType.VIEW_SUPPORT))
        {
            setupSpinner();
            nameField.setVisibility(View.GONE);
            view.findViewById(R.id.full_name_title).setVisibility(View.GONE);
            view.findViewById(R.id.troubleCaptureTitle).setVisibility(View.GONE);
            emailField.setVisibility(View.GONE);
        }
        else if (supportType.equals(SupportType.FEEDBACK_SUPPORT))
        {
            inputSpinner.setVisibility(View.GONE);
            view.findViewById(R.id.spinner_frame).setVisibility(View.GONE);
            view.findViewById(R.id.reason_title).setVisibility(View.GONE);
            view.findViewById(R.id.troubleCaptureTitle).setVisibility(View.GONE);
        }
        else if (supportType.equals(SupportType.ERROR_SUPPORT))
        {
            view.findViewById(R.id.spinner_frame).setVisibility(View.GONE);
            view.findViewById(R.id.full_name_title).setVisibility(View.GONE);
            inputSpinner.setVisibility(View.GONE);
            nameField.setVisibility(View.GONE);
            emailField.setVisibility(View.GONE);
        }

        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> continueButton.setEnabled(isChecked));

        final ModelAvatar finalModelAvatar = modelAvatar;

        continueButton.setOnClickListener(v ->
        {
            messageText.setError(null);
            nameField.setError(null);
            emailField.setError(null);

            Editable ed = messageText.getText();
            if (ed == null)
            {
                Timber.e("Support Edit text is null");
                return;
            }
            if (messageText.getText().toString().isEmpty())
            {
                messageText.setError("Message cannot be blank");
                messageText.requestFocus();
                return;
            }

            String reason = "";
            switch (supportType)
            {
                case SupportType.VIEW_SUPPORT:
                    reason = inputSpinner.getSelectedItem().toString();
                    break;
                case SupportType.ERROR_SUPPORT:
                    reason = getString(R.string.trouble_capturing_my_body_measurements);
                    break;
                case SupportType.FEEDBACK_SUPPORT:
                    if (emailField.getText().toString().isEmpty())
                    {
                        emailField.setError("Email cannot be blank");
                        emailField.requestFocus();
                        return;
                    }
                    if (!Pattern.compile(RegexHelpers.emailPattern).matcher(emailField.getText()).matches())
                    {
                        emailField.setError("Email must be valid");
                        emailField.requestFocus();
                        return;
                    }
                    if (nameField.getText().toString().isEmpty())
                    {
                        nameField.setError("Name cannot be blank");
                        nameField.requestFocus();
                        return;
                    }
                    reason = "General feedback from " + nameField.getText().toString() + " at " + emailField.getText().toString();
                    break;
            }

            continueButton.setEnabled(false);
            termsCheckbox.setEnabled(false);
            showLoadingBar();

            final MyFiziqSdkManager mgr = MyFiziqSdkManager.getInstance();
            final String message = ed.toString();

            AsyncHelper.Callback<Boolean> onComplete = aBoolean ->
            {
                getActivity().runOnUiThread(() ->
                {
                    hideLoadingBar();
                    if (Boolean.TRUE.equals(aBoolean))
                    {
                        snackbar = Snackbar.make(view.findViewById(R.id.btnContinue), getString(R.string.success_sending_support), Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(getString(com.myfiziq.sdk.R.string.thanks), v1 -> onBackPressed());
                        snackbar.show();
                    }
                    else
                    {
                        Toast.makeText(getActivity(), getString(com.myfiziq.sdk.R.string.error_sending_support), Toast.LENGTH_LONG).show();
                    }
                });
            };
            mgr.uploadSupportData(reason, message, finalModelAvatar, onComplete);
        });
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        ActionBarHelper.showBackButton(getActivity());
        if (supportType.equals(SupportType.FEEDBACK_SUPPORT))
        {
            ActionBarHelper.setActionBarTitle(getActivity(), R.string.feedback);
        }
        else
        {
            ActionBarHelper.setActionBarTitle(getActivity(), R.string.contact_support);
        }
        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<ParameterSet>(getActivity());
        intentManagerService.respond(IntentResponses.HIDE_BOTTOM_NAVIGATION_BAR, null);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (snackbar != null) snackbar.dismiss();

        IntentManagerService<ParameterSet> intentManagerService = new IntentManagerService<ParameterSet>(getActivity());
        intentManagerService.respond(IntentResponses.SHOW_BOTTOM_NAVIGATION_BAR, null);
    }

    /**
     * Setup spinner with list.
     */
    private void setupSpinner()
    {
        //Define Spinner List
        List<String> list = new ArrayList<>();
        list.add(getString(R.string.results_query));
        list.add(getString(R.string.other));

        //Set adapter to spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputSpinner.setAdapter(dataAdapter);
    }

    /**
     * Show a loading bar on the top of the button, along with disappearing text button.
     */
    private void showLoadingBar()
    {
        continueButton.setClickable(false);
        continueButton.setText("");
        buttonProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide a loading bar on the top of the button, along with appearing text button.
     */
    private void hideLoadingBar()
    {
        continueButton.setClickable(true);
        continueButton.setText(R.string.continueText);
        buttonProgressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
