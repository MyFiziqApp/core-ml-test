package com.myfiziq.sdk.helpers;

import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryValueVO;

import java.util.ArrayList;

public class MyMeasurementDialogVOGenerator
{
    public static ArrayList<MyMeasurementDialogCategoryValueVO> generateValues(int min, int max, int interval, MyMeasurementDialogFormatterInterface formatter)
    {
        int initialCapacity = (max - min) + 1;
        ArrayList<MyMeasurementDialogCategoryValueVO> values = new ArrayList<>(initialCapacity);

        for (int i = min; i <= max; i+= interval)
        {
            MyMeasurementDialogCategoryValueVO value = new MyMeasurementDialogCategoryValueVO();
            value.setValue(i);

            String label = formatter.format(i);
            value.setLabel(label);

            values.add(value);
        }

        return values;
    }
}
