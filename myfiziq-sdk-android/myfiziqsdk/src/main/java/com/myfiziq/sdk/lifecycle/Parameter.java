package com.myfiziq.sdk.lifecycle;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.myfiziq.sdk.adapters.CursorHolder;
import com.myfiziq.sdk.adapters.HolderViewBinder;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.db.ORMContentProvider;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * A Parameter is an item within a ParameterSet that is passed to an Activity, Fragment or
 * StyledRecyclerView.
 * It may contain a key/value pair (key being a unique int value).
 * It may contain a set of values for a StyledRecyclerView Model/ItemView.
 */
public class Parameter implements Parcelable
{
    public enum ParameterType
    {
        UNKNOWN,
        LAYOUT,
        ATTRIBUTE,
        MODELVIEW
    }

    private ParameterType mType = ParameterType.UNKNOWN;
    private int mViewId = 0;
    private int mParamId = 0;
    private String mValue = "";
    private Parcelable mParcelableValue;

    private CursorHolder mHolder = null;
    private ArrayList<Integer> mHeaderList;

    private boolean bInit = false;

    public Parameter()
    {

    }

    public Parameter(int paramId, int value)
    {
        mViewId = 0;
        mParamId = paramId;
        mValue = String.valueOf(value);
    }

    public Parameter(int viewId, int paramId, int value)
    {
        mViewId = viewId;
        mParamId = paramId;
        mValue = String.valueOf(value);
    }

    public Parameter(int paramId, String value)
    {
        mViewId = 0;
        mParamId = paramId;
        mValue = value;
    }

    public Parameter(int paramId, Parcelable value)
    {
        mViewId = 0;
        mParamId = paramId;
        mParcelableValue = value;
    }

    public Parameter(int viewId, int paramId, String value)
    {
        mViewId = viewId;
        mParamId = paramId;
        mValue = value;
    }

    private Parameter(Parcel in)
    {
        mType = ParameterType.values()[in.readInt()];
        mViewId = in.readInt();
        mParamId = in.readInt();
        mValue = in.readString();
        if (0 != in.readByte())
        {
            String parcelType = in.readString();
            if (!TextUtils.isEmpty(parcelType))
            {
                try
                {
                    mParcelableValue = in.readParcelable(Class.forName(parcelType).getClassLoader());
                }
                catch (ClassNotFoundException e)
                {
                    Timber.e(e);
                }
            }
        }
        if (0 != in.readByte())
        {
            mHolder = in.readParcelable(CursorHolder.class.getClassLoader());
        }
        int listSize = in.readInt();
        for (int i = 0; i < listSize; i++)
        {
            addHeader(in.readInt());
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags)
    {
        dest.writeInt(mType.ordinal());
        dest.writeInt(mViewId);
        dest.writeInt(mParamId);
        dest.writeString(mValue);

        if (null == mParcelableValue)
        {
            dest.writeByte((byte)0);
        }
        else
        {
            dest.writeByte((byte)1);
            dest.writeString(mParcelableValue.getClass().getName());
            dest.writeParcelable(this.mParcelableValue, flags);
        }

        if (null == mHolder)
        {
            dest.writeByte((byte)0);
        }
        else
        {
            dest.writeByte((byte)1);
            dest.writeParcelable(mHolder, flags);
        }

        if (null != mHeaderList)
        {
            dest.writeInt(mHeaderList.size());
            for (int id : mHeaderList)
            {
                dest.writeInt(id);
            }
        }
        else
        {
            dest.writeInt(0);
        }
    }

    public static final Creator<Parameter> CREATOR = new Creator<Parameter>()
    {
        @Override
        public Parameter createFromParcel(Parcel in)
        {
            try
            {
                return new Parameter(in);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return null;
        }

        @Override
        public Parameter[] newArray(int size)
        {
            return new Parameter[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    public void addHeader(int id)
    {
        if (null == mHeaderList)
        {
            mHeaderList = new ArrayList<>();
        }

        mHeaderList.add(id);
    }

    public void initHolder(HolderViewBinder binder)
    {
        if (null != mHolder && null != mHeaderList && !bInit)
        {
            for (int id : mHeaderList)
            {
                mHolder.addHeader(id, binder);
            }

            bInit = true;
        }
    }

    public CursorHolder getHolder()
    {
        return mHolder;
    }

    public int getColor(Resources resources)
    {
        return resources.getColor(Integer.valueOf(mValue));
    }

    public String getValue()
    {
        return mValue;
    }

    public Parcelable getParcelableValue()
    {
        return mParcelableValue;
    }

    public int getViewId()
    {
        return mViewId;
    }

    public int getParamId()
    {
        return mParamId;
    }

    public static class Builder
    {
        Parameter mParameter = new Parameter();

        public Builder()
        {

        }

        public Parameter build()
        {
            return mParameter;
        }

        private void createHolder()
        {
            if (null == mParameter.mHolder)
            {
                mParameter.mHolder = new CursorHolder(0, null, null, null, Model.DEFAULT_DEPTH, null, null, null);
            }
        }

        public Builder createAsLayout(int layoutResId)
        {
            mParameter.mType = ParameterType.LAYOUT;
            return setValue(layoutResId);
        }

        public Builder createAsAttribute(int viewId, int value)
        {
            mParameter.mType = ParameterType.ATTRIBUTE;
            setViewId(viewId);
            return setValue(value);
        }

        public Builder createAsModelView(Class<? extends Model> clazzModel, Class<? extends ViewGroup> clazzView)
        {
            mParameter.mType = ParameterType.MODELVIEW;
            setModel(clazzModel);
            return setView(clazzView);
        }

        public Builder addHeader(int id)
        {
            mParameter.addHeader(id);
            return this;
        }

        /**
         * Set a unique id if required. Only needed if there are multiple Loaders in an
         * Activity or Fragment.
         * @param id
         * @return
         */
        public Builder setLoaderId(int id)
        {
            createHolder();
            mParameter.mHolder.setLoaderId(id);
            return this;
        }

        public Builder setViewId(int id)
        {
            mParameter.mViewId = id;
            return this;
        }

        public Builder setParamId(int id)
        {
            mParameter.mParamId = id;
            return this;
        }

        /**
         * Set a String value for this Parameter.
         * @param value
         * @return
         */
        public Builder setValue(String value)
        {
            mParameter.mValue = value;
            return this;
        }

        /**
         * Set an integer value for this parameter.
         * @param value
         * @return
         */
        public Builder setValue(int value)
        {
            mParameter.mValue = String.valueOf(value);
            return this;
        }

        /**
         * Set a parcelable value for this parameter.
         * @param value
         * @return
         */
        public Builder setParcelableValue(Parcelable value)
        {
            mParameter.mParcelableValue = value;
            return this;
        }

        /**
         * Set the StyledRecyclerView Model for this parameter
         * @param clazz
         * @return
         */
        public Builder setModel(Class<? extends Model> clazz)
        {
            createHolder();
            mParameter.mHolder.setModelClass(clazz);
            mParameter.mHolder.setUri(ORMContentProvider.uri(clazz));
            return this;
        }

        /**
         * Set the StyledRecyclerView item View for this parameter
         * @param clazz
         * @return
         */
        public Builder setView(Class<? extends ViewGroup> clazz)
        {
            createHolder();
            mParameter.mHolder.setViewClass(clazz);
            return this;
        }

        /**
         * Set the StyledRecyclerView content URI for this parameter
         * @param uri
         * @return
         */
        public Builder setUri(Uri uri)
        {
            createHolder();
            mParameter.mHolder.setUri(uri);
            return this;
        }

        /**
         * Set the StyledRecyclerView where clause for this parameter
         * @param where
         * @return
         */
        public Builder setWhere(String where)
        {
            createHolder();
            mParameter.mHolder.setWhere(where);
            return this;
        }

        /**
         * Set the StyledRecyclerView order clause for this parameter
         * @param order
         * @return
         */
        public Builder setOrder(String order)
        {
            createHolder();
            mParameter.mHolder.setOrder(order);
            return this;
        }
    }
}
