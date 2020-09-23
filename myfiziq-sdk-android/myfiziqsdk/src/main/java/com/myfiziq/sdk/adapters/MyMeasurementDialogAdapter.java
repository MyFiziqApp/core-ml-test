package com.myfiziq.sdk.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;

import java.util.List;

import androidx.annotation.NonNull;

public class MyMeasurementDialogAdapter extends ArrayAdapter<MyMeasurementDialogCategoryVO>
{
    public MyMeasurementDialogAdapter(Context context, int resource)
    {
        super(context, resource);
    }

    public MyMeasurementDialogAdapter(Context context, int resource, int textViewResourceId)
    {
        super(context, resource, textViewResourceId);
    }

    public MyMeasurementDialogAdapter(Context context, int resource, MyMeasurementDialogCategoryVO[] objects)
    {
        super(context, resource, objects);
    }

    public MyMeasurementDialogAdapter(Context context, int resource, int textViewResourceId, MyMeasurementDialogCategoryVO[] objects)
    {
        super(context, resource, textViewResourceId, objects);
    }

    public MyMeasurementDialogAdapter(Context context, int resource, List<MyMeasurementDialogCategoryVO> objects)
    {
        super(context, resource, objects);
    }

    public MyMeasurementDialogAdapter(Context context, int resource, int textViewResourceId, List<MyMeasurementDialogCategoryVO> objects)
    {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
    {
        return getView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View view = super.getView(position, convertView, parent);

        if (view instanceof TextView)
        {
            MyMeasurementDialogCategoryVO item = getItem(position);

            if (null != item)
            {
                TextView textView = (TextView) view;
                textView.setText(item.getLabel());
                textView.setPadding(40, 30, 100, 30);
            }
        }

        return view;
    }
}
