package com.myfiziq.sdk.adapters;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ORMTable;

import java.util.HashMap;

import androidx.annotation.Keep;

/**
 * Encapsulates a table query based on the <code>ORMContentProvider</code> URI, the "where" and the "order" clauses.
 *
 */
@Keep
public class CursorArgs implements Parcelable
{
    Uri mUri;
    String mWhere;
    String mOrder;
    int mDepth = Model.DEFAULT_DEPTH;
    HashMap<String, String> mWhereValues;

    /**
     * Constructs a <code>CursorArgs</code> instance based on a <code>Model</code> type.
     * @param model - The <code>Model</code>.
     */
    public CursorArgs(Model model)
    {
        this(model, null);
    }

    /**
     * Constructs a <code>CursorArgs</code> instance based on a <code>Model</code> type.
     * @param model - The <code>Model</code>.
     * @param order - The sort order for the database query.
     */
    public CursorArgs(Model model, String order)
    {
        this(ORMTable.uri(model.getClass()), model, order);
    }

    /**
     * Constructs a <code>CursorArgs</code> instance based on a <code>Uri</code>.
     * The <code>Model</code> specifies a single instance to load for the database 'where' clause.
     * @param uri - The <code>ORMContentProvider</code> URI for the table query.
     * @param model - The <code>Model</code>.
     * @param order - The sort order for the database query.
     */
    public CursorArgs(Uri uri, Model model, String order)
    {
        this(uri, String.format("%s='%s'", model.getIdFieldName(), model.getId()), order);
    }

    /**
     * Constructs a <code>CursorArgs</code> instance based on a <code>Uri</code>.
     * @param uri - The <code>ORMContentProvider</code> URI for the table query.
     * @param where - The where clause for the database query.
     * @param order - The sort order for the database query.
     */
    public CursorArgs(Uri uri, String where, String order)
    {
        mUri = uri;
        mWhere = where;
        mOrder = order;
    }

    /**
     * Constructs a <code>CursorArgs</code> instance based on a <code>Uri</code>.
     * @param uri - The <code>ORMContentProvider</code> URI for the table query.
     * @param where - The where clause for the database query.
     * @param order - The sort order for the database query.
     * @param depth - For nested <code>Model</code> instances - limit the depth to read from the
     *              database.
     */
    public CursorArgs(Uri uri, String where, String order, int depth)
    {
        mUri = uri;
        mWhere = where;
        mOrder = order;
        mDepth = depth;
    }

    /**
     * Update the where clause for the database query.
     * <br>
     * NOTE - A new query would need to be triggered externally.
     * @param where - The where clause for the database query.
     */
    public void setWhere(String where)
    {
        mWhere = where;
    }

    private CursorArgs(Parcel in)
    {
        mUri = in.readParcelable(Uri.class.getClassLoader());
        mWhere = in.readString();
        mOrder = in.readString();
        mDepth = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags)
    {
        dest.writeParcelable(mUri, flags);
        dest.writeString(mWhere);
        dest.writeString(mOrder);
        dest.writeInt(mDepth);
    }

    public static final Creator<CursorArgs> CREATOR = new Creator<CursorArgs>()
    {
        @Override
        public CursorArgs createFromParcel(Parcel in)
        {
            return new CursorArgs(in);
        }

        @Override
        public CursorArgs[] newArray(int size)
        {
            return new CursorArgs[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    private void buildWhere()
    {
        if (null != mWhereValues)
        {
            boolean bFirst = true;
            StringBuilder sb = new StringBuilder();
            for (String key : mWhereValues.keySet())
            {
                if (!bFirst)
                {
                    sb.append(" AND ");
                }
                bFirst = false;

                sb.append(key);
                sb.append(" ");
                sb.append(mWhereValues.get(key));
            }

            mWhere = sb.toString();
        }
    }

    public void addWhere(String key, String value)
    {
        if (null == mWhereValues)
        {
            mWhereValues = new HashMap<>();
        }

        mWhereValues.put(key, value);
        buildWhere();
    }

    public void remWhere(String key)
    {
        if (null != mWhereValues)
        {
            mWhereValues.remove(key);
            buildWhere();
        }
    }
}
