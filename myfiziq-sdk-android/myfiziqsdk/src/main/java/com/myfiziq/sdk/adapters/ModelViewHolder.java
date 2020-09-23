package com.myfiziq.sdk.adapters;

import android.view.View;

import com.myfiziq.sdk.R;
import com.myfiziq.sdk.db.Model;
import com.myfiziq.sdk.fragments.FragmentInterface;

import java.util.List;

/**
 * <code>Holder</code> wrapper for <code>Model</code> types.
 */
public class ModelViewHolder<M extends Model> extends Holder
{
    private FragmentInterface mFragment;

    ModelViewHolder(FragmentInterface fragment, View view, List<View.OnClickListener> listeners)
    {
        super(Type.MODEL, view);

        mFragment = fragment;

        if (view instanceof BaseModelViewInterface)
        {
            ((BaseModelViewInterface) view).setViewOnClickListeners(listeners);
        }
        else
        {
            if (null != listeners && listeners.size() > 0)
                view.setOnClickListener(listeners.get(0));
        }
    }

    public void bind(M model, CursorHolder holder)
    {
        if (itemView instanceof BaseModelViewInterface)
        {
            BaseModelViewInterface<M> modelViewInterface = (BaseModelViewInterface<M>) itemView;
            modelViewInterface.setModelTag(model);
            modelViewInterface.setHolderTag(holder);
            modelViewInterface.bind(holder, mFragment, model);
        }
        else
        {
            itemView.setTag(R.id.TAG_MODEL, model);
            itemView.setTag(R.id.TAG_HOLDER, holder);
        }
    }
}
