package com.myfiziq.sdk.components;

import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;

public interface OnCategoryChanged
{
    void onChanged(MyMeasurementDialogCategoryVO newCategory, MyMeasurementDialogCategoryVO previousCategory,
                   int oldMeasurementValue, int oldMeasurementFractionValues);
}
