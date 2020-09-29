package com.myfiziq.sdk.db;

/**
 * @hide
 */
public enum ORMTableType
{
    // The order and number of items here needs to be kept in sync with the NDK layer.
    NOT_PERSISTED("0"),

    // Not persisted but ued for Table Joins - ORMContentProvider.
    NOT_PERSISTED_GLOBAL("1"),

    GLOBAL("1"),

    USER("2"),
    USER_SECURE("3"),

    // In memory only
    MEMORY_GLOBAL("4");

    String mName;

    ORMTableType(String name)
    {
        mName = name;
    }

    public String getName()
    {
        return mName;
    }

    public char getNameChar()
    {
        return mName.charAt(0);
    }

    public boolean matches(ORMTableType another)
    {
        return mName.contentEquals(another.mName);
    }
}
