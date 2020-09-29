package com.myfiziq.sdk.components;

import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryVO;
import com.myfiziq.sdk.vo.MyMeasurementDialogCategoryValueVO;

public interface OnDoneListener
{
    public void onDone(MyMeasurementDialogCategoryVO chosenCategory, MyMeasurementDialogCategoryValueVO chosenValue,
                       MyMeasurementDialogCategoryValueVO chosenFractionValue);
}
