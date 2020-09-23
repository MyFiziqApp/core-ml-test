package com.myfiziq.sdk.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>Model</code> classes that are annotated with <code>Cached</code> will be kept in memory once
 * loaded from the database.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cached
{
    boolean cached() default true;
}
