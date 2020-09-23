package com.myfiziq.sdk.db;

import java.text.DecimalFormat;

/**
 * Represents a type of measurement.
 */
public interface TypeOfMeasurement
{
    String getInternalName();
    String getFormatted();
    void setFormat(DecimalFormat format);
}
