package com.myfiziq.sdk.db;

import org.sqlite.database.sqlite.SQLiteCursor;
import org.sqlite.database.sqlite.SQLiteCursorDriver;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteQuery;

public class CachedCursor extends SQLiteCursor
{
    public CachedCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
    {
        super(db, driver, editTable, query);
    }

    public CachedCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
    {
        super(driver, editTable, query);
    }

    @Override
    public int getColumnIndex(String columnName)
    {
        return super.getColumnIndex(columnName);
    }
}
