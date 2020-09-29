package com.myfiziq.sdk.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.danielnilsson9.colorpickerview.view.ColorPickerView;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.myfiziq.sdk.R;
import com.myfiziq.sdk.helpers.AppWideUnitSystemHelper;
import com.myfiziq.sdk.helpers.SisterColors;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

/**
 * @hide
 */
public class UiUtils
{
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    public static Handler getHandler()
    {
        return mHandler;
    }

    public static int getThemePrimaryColor(Context context)
    {
        int colorAttr;
        colorAttr = android.R.attr.colorPrimary;
        TypedValue outValue = new TypedValue();
        context
                .getTheme()
                .resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int getColorFromStyle(Context context, @StyleRes int style, @AttrRes int attribute)
    {
        int[] attributes = {attribute};
        TypedArray styledAttributes = context.obtainStyledAttributes(style, attributes);
        int color = styledAttributes.getInt(
                0,
                context
                        .getResources()
                        .getColor(R.color.myfiziqsdk_colorPrimary)
        );
        styledAttributes.recycle();
        return color;
    }

    public enum MessageDuration
    {
        LONG,
        SHORT,
        INDEFINITE
    }

    public static void hideSoftKeyboard(Activity activity)
    {
        if (activity != null)
        {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

            View view = activity.getCurrentFocus();

            if (inputManager != null && view != null)
            {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static void showSoftKeyboard(Activity activity)
    {
        if (activity != null)
        {
            getHandler().postDelayed(() ->
            {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                // check if no view has focus:
                View view = activity.getCurrentFocus();

                inputManager.showSoftInput(view, 0);
            }, 100);
        }
    }

    public static float getDensity(Context context)
    {
        return context.getResources().getDisplayMetrics().density;
    }

    public static float convertDpToPixel(Context context, float dp)
    {
        Resources resources = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }

    public static float convertSpToPixel(Context context, float sp)
    {
        Resources resources = context.getResources();
        return (sp * resources.getDisplayMetrics().scaledDensity);
    }

    public static float convertPixelToDp(Context context, float pixel)
    {
        Resources resources = context.getResources();
        return (pixel / resources.getDisplayMetrics().density);
    }

    public static float convertPixelToSp(Context context, float pixel)
    {
        Resources resources = context.getResources();
        return (pixel / resources.getDisplayMetrics().scaledDensity);
    }

    public static int getToolbarHeight(Context context)
    {
        int marginTop = 0;

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            marginTop = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        return marginTop;
    }

    public static int getScreenWidth(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int getScreenHeight(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public static float getScreenAspect(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return ((float) size.y) / ((float) size.x);
    }

    public static int get16x9Height(int width)
    {
        int height = width * 16 / 9;
        return height;
    }

    public static int get16x9Width(int height)
    {
        int width = height * 9 / 16;
        return width;
    }

    public static int get5x2Height(int width)
    {
        int height = width * 2 / 5;
        return height;
    }

    public static int get5x2Width(int height)
    {
        int width = height * 5 / 2;
        return width;
    }

    /**
     * @param activity
     * @param title
     * @param message
     * @param positiveButtonText can be null. Default is "Ok"
     * @param negativeButtonText can be null. Default is "Cancel"
     * @param positiveListener   cannot be null
     * @param negativeListener   can be null. Default action is to dismiss the dialog
     */
    public static void showAlertDialog(@NonNull Activity activity,
                                       @Nullable String title,
                                       @Nullable String message,
                                       @Nullable String positiveButtonText,
                                       @Nullable String negativeButtonText,
                                       @Nullable DialogInterface.OnClickListener positiveListener,
                                       @Nullable DialogInterface.OnClickListener negativeListener,
                                       boolean isCancelable)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonText != null ? positiveButtonText : activity.getString(android.R.string.ok), positiveListener);
        builder.setCancelable(isCancelable);
        builder.setOnCancelListener(dialog ->
        {
            if (negativeListener != null)
            {
                negativeListener.onClick(dialog, 1);
            }
        });


        if (!TextUtils.isEmpty(negativeButtonText))
        {
            builder.setNegativeButton(negativeButtonText != null ? negativeButtonText : activity.getString(android.R.string.cancel), negativeListener);
        }

        AlertDialog dialog = builder.show();

        setAlertDialogColours(activity, dialog);
    }

    public static void showAlertDialog(@NonNull Activity activity,
                                       @Nullable String title,
                                       @Nullable String message,
                                       @Nullable String positiveButtonText,
                                       @Nullable String negativeButtonText,
                                       @Nullable DialogInterface.OnClickListener positiveListener,
                                       @Nullable DialogInterface.OnClickListener negativeListener)
    {
        showAlertDialog(activity, title, message,
                positiveButtonText,
                negativeButtonText, positiveListener,
                negativeListener, true);
    }

    public static void showConfirmDialog(@NonNull Activity activity,
                                         @Nullable String message,
                                         @Nullable DialogInterface.OnClickListener positiveListener)
    {
        showAlertDialog(activity, activity.getString(R.string.myfiziqsdk_confirm), message,
                activity.getString(android.R.string.ok),
                null, positiveListener,
                null,
                true);
    }

    public static void showMsgDialog(Activity activity,
                                     String message,
                                     DialogInterface.OnClickListener positiveListener)
    {
        showAlertDialog(activity, activity.getString(R.string.myfiziqsdk_confirm), message,
                activity.getString(android.R.string.ok),
                null, positiveListener,
                null);
    }

    public static void showImgDialog(final Activity activity,
                                     final Bitmap bitmap)
    {
        getHandler().post(() ->
        {
            CustomAlertDialog builder = new CustomAlertDialog(activity, bitmap);
            builder.show().getWindow().setLayout(getScreenWidth(activity), getScreenHeight(activity));
        });
    }

    public static void setViewVisibility(final View v, final int visibility)
    {
        if (v == null)
        {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper())
        {
            getHandler().post(() ->
            {
                if (v.getVisibility() != visibility)
                {
                    v.setVisibility(visibility);
                }
            });
        }
        else
        {
            if (v.getVisibility() != visibility)
            {
                v.setVisibility(visibility);
            }
        }
    }

    /**
     * Sets the visibility of the scrollbar in a {@link ScrollView}.
     *
     * This also invalidates the ScrollView to ensure that the change takes place IMMEDIATELY,
     * as opposed to when the user next scrolls the view.
     *
     * @param scrollView The {@link ScrollView} to set scrollbar visibility for.
     * @param visible Whether the scrollbars should be visible or not.
     */
    public static void setScrollBarVisibility(ScrollView scrollView, boolean visible)
    {
        // Hide the scrollbar to stop it from being visible in the transition animation when we navigate away from the fragment
        scrollView.setVerticalScrollBarEnabled(visible);

        // Invalidate the view which "setVerticalScrollBarEnabled()" doesn't do for some reason.
        // This makes the scrollbar disappear immediately instead of when the user next scrolls the view.
        scrollView.invalidate();
    }
	
    public static void disablePasswordToggleButtonRipple(@NonNull TextInputLayout container)
    {
        CheckableImageButton button = container.findViewById(com.google.android.material.R.id.text_input_password_toggle);

        if (button != null)
        {
            int transparentColor = container.getContext().getResources().getColor(android.R.color.transparent);
            button.setBackgroundColor(transparentColor);
        }
    }

    public static void setAlertDialogColours(@NonNull Activity activity, @NonNull AlertDialog dialog)
    {
        if (SisterColors.getInstance().getAlertBackgroundColor() != null)
        {
            Window window = ((AlertDialog) dialog).getWindow();
            if (window == null)
            {
                return;
            }
            window.getDecorView().setBackgroundColor(SisterColors.getInstance().getAlertBackgroundColor());
        }

        if (SisterColors.getInstance().getChartLineColor() != null)
        {
            ProgressBar progressbar = (ProgressBar) dialog.findViewById(android.R.id.progress);

            if (progressbar == null)
            {
                return;
            }

            progressbar.getIndeterminateDrawable().setColorFilter(SisterColors.getInstance().getChartLineColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        else
        {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            if (positiveButton != null)
            {
                positiveButton.setTextColor(activity.getResources().getColor(R.color.myfiziqsdk_dialog_positive_button));
            }

            if (neutralButton != null)
            {
                neutralButton.setTextColor(activity.getResources().getColor(R.color.myfiziqsdk_dialog_neutral_button));
            }

            if (negativeButton != null)
            {
                negativeButton.setTextColor(activity.getResources().getColor(R.color.myfiziqsdk_dialog_negative_button));
            }
        }
    }

    /**
     * Will Create a SnackBar with margin (Floating Toast) in the bottom of the coordinator layout.
     *
     * @param coordinatorLayout : Parent layout for the floating toast
     * @param message           : Message in flying toast
     * @param actionMessage     : Text for clickable button in floating toast
     * @param actionColor       : Text color for clickable button in floating toast
     * @param actionListener    : Listener/Action for clickable button in floating toast.
     *                          Assigning null value will create an action for dismissing the toast.
     */
    public static void createFloatingToast(CoordinatorLayout coordinatorLayout, String message,
                                           String actionMessage, @ColorInt int actionColor,
                                           View.OnClickListener actionListener)
    {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE);

        //Set Action For Floating Style Toast, if null it will only dismiss the toast
        snackbar.setActionTextColor(actionColor);
        if (null == actionListener)
        {
            snackbar.setAction(actionMessage, v -> snackbar.dismiss());
        }
        else
        {
            snackbar.setAction(actionMessage, actionListener);
        }

        //Set Param For Floating Style Toast
        View snackBarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(72, 72, 72, 72);
        params.anchorGravity = Gravity.BOTTOM;
        params.gravity = Gravity.BOTTOM;
        snackBarView.setLayoutParams(params);

        snackbar.show();
    }

    public static void showColorDialog(final Activity activity,
                                       String message,
                                       final PickerColor color,
                                       final DialogInterface.OnClickListener positiveListener)
    {
        getHandler().post(() ->
        {
            CustomAlertDialog builder = new CustomAlertDialog(activity, R.layout.dialog_color)
            {
                ColorPickerView pickerView;

                @Override
                public void initViewExtras(View rootView)
                {
                    pickerView = rootView.findViewById(R.id.colorPicker);
                    pickerView.setColor(color.color);
                }

                @Override
                public void updateValues()
                {
                    color.color = pickerView.getColor();
                }
            };
            builder.setPositiveButton("", positiveListener);
            builder.show();
        });
    }

    public static void showToast(Context context, CharSequence text, int duration)
    {
        getHandler().post(() -> Toast.makeText(context, text, duration).show());
    }

    public static void showToast(Context context, @StringRes int resId, int duration)
    {
        getHandler().post(() -> Toast.makeText(context, resId, duration).show());
    }

    public static class PickerColor
    {
        public int color;

        public PickerColor(int c)
        {
            color = c;
        }
    }

    static class CustomAlertDialog
    {
        View rootView;
        TextView textViewTitle;
        TextView textViewDetail;
        Button buttonCancel;
        Button buttonConfirm;
        AlertDialog.Builder builder;

        public CustomAlertDialog(Context context)
        {
            builder = new AlertDialog.Builder(context);
            rootView = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null);
            builder.setView(rootView);
            initView(rootView);
        }

        public CustomAlertDialog(Context context, @LayoutRes int layout)
        {
            builder = new AlertDialog.Builder(context);
            rootView = LayoutInflater.from(context).inflate(layout, null);
            builder.setView(rootView);
            initView(rootView);
        }

        public CustomAlertDialog(Context context, Bitmap bitmap)
        {
            builder = new AlertDialog.Builder(context);
            rootView = LayoutInflater.from(context).inflate(R.layout.dialog_image, null);
            ImageView image = rootView.findViewById(R.id.imageView);
            image.setImageBitmap(bitmap);
            builder.setView(rootView);
            initView(rootView);
        }

        public Dialog show()
        {
            Dialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            if (null != buttonCancel)
                buttonCancel.setTag(dialog);

            if (null != buttonConfirm)
                buttonConfirm.setTag(dialog);

            dialog.show();
            return dialog;
        }

        private void initView(View rootView)
        {
            textViewTitle = rootView.findViewById(R.id.textViewTitle);
            textViewDetail = rootView.findViewById(R.id.textViewDetail);
            buttonCancel = rootView.findViewById(R.id.buttonCancel);
            buttonConfirm = rootView.findViewById(R.id.buttonConfirm);
            initViewExtras(rootView);
        }

        public void initViewExtras(View rootView)
        {

        }

        public void updateValues()
        {

        }

        public void setTitle(CharSequence title)
        {
            textViewTitle.setText(title);
        }

        public void setMessage(CharSequence message)
        {
            textViewDetail.setText(message);
        }

        public void setPositiveButton(CharSequence message, final DialogInterface.OnClickListener listener)
        {
            buttonConfirm.setText(message);
            if (null != listener)
            {
                buttonConfirm.setOnClickListener(v ->
                {
                    Dialog dlg = (Dialog) buttonConfirm.getTag();
                    dlg.dismiss();
                    updateValues();
                    listener.onClick(dlg, DialogInterface.BUTTON_POSITIVE);
                });
            }
            else
            {
                buttonConfirm.setOnClickListener(v -> ((Dialog) buttonConfirm.getTag()).dismiss());
            }
        }

        public void setNegativeButton(CharSequence message, final DialogInterface.OnClickListener listener)
        {
            buttonCancel.setText(message);
            if (TextUtils.isEmpty(message))
            {
                buttonCancel.setVisibility(View.GONE);
            }
            else
            {
                buttonCancel.setVisibility(View.VISIBLE);
            }

            if (null != listener)
            {
                buttonCancel.setOnClickListener(v ->
                {
                    Dialog dlg = (Dialog) buttonCancel.getTag();
                    dlg.dismiss();
                    listener.onClick(dlg, DialogInterface.BUTTON_NEGATIVE);
                });
            }
            else
            {
                buttonCancel.setOnClickListener(v -> ((Dialog) buttonCancel.getTag()).dismiss());
            }
        }

        public void setCancelable(boolean isCancelable)
        {
            builder.setCancelable(isCancelable);
        }
    }

    public static void setOnScreenNavigationVisibility(Activity activity, boolean isVisible)
    {
        if (isVisible)
        {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        else
        {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }
}
