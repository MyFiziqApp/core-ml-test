package com.myfiziq.sdk.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @hide
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Persistent
{
    boolean pk() default false;

    // map the value to the id field?
    boolean idMap() default false;

    // generate child id based on parent id + this field value.
    String childIdMap() default "";

    // if explicit - only get the value if the parent exists.
    boolean explicit() default false;

    // field is stored in the database
    boolean inDb() default true;

    // field is stored in the database
    boolean serialize() default true;

    // field is stored in the database
    boolean serializeIfEmpty() default true;

    // delete items not received in response?
    boolean delNotInSet() default false;

    // assign field from parent id.
    boolean fromParent() default false;

    // get field from root node (no node name - API workaround).
    boolean fromParentValue() default false;

    // if a joined field - don't store in the table, data is retrieved with a join.
    boolean joinedField() default false;

    // JSON item is an escaped string value - unescape and parse.
    // mutually exclusive with isJwt.
    // only applies to model objects.
    boolean escaped() default false;

    // For HashMap only - flatten to object rather than an array of pairs.
    boolean flatten() default false;

    // only applies to model objects.
    boolean isJwt() default false;

    // only applies to model objects.
    // take the value as a reference only...
    // JSON value is a reference to a separate (reference) table.
    boolean asReference() default false;

    // specify column order.
    int order() default 0;

    String appDb() default "";

    // parser path mapping in parent.child... notation.
    String jsonMap() default "";

    // SQL query to populate model array value + with %field% replacement.
    String query() default "";

    boolean mask() default false;

    String maskChar() default "*";

    // Field is a db path
    // When enabled the field is used to obtain the correct database for the model.
    boolean dbPath() default false;
}
