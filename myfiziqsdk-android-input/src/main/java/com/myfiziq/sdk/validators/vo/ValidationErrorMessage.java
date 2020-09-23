package com.myfiziq.sdk.validators.vo;

import androidx.annotation.StringRes;

import com.myfiziq.sdk.db.SystemOfMeasurement;

public class ValidationErrorMessage
{
    private TypeOfValidationError typeOfValidationError;
    private @StringRes int errorStringResourceId;


    public ValidationErrorMessage(TypeOfValidationError typeOfValidationError,
                                  @StringRes int errorStringResourceId)
    {
        this.typeOfValidationError = typeOfValidationError;
        this.errorStringResourceId = errorStringResourceId;
    }

    public TypeOfValidationError getTypeOfValidationError()
    {
        return typeOfValidationError;
    }

    public int getErrorStringResourceId()
    {
        return errorStringResourceId;
    }
}
