package com.myfiziq.sdk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @hide
 */
public class ItemDecorationDivider extends RecyclerView.ItemDecoration {

    protected final boolean drawFirstDivider;
    protected final boolean drawLastDivider;
    protected final boolean dividerTakesUpSize;

    protected final int paddingStart;
    protected final int paddingEnd;
    protected final int dividerSize;

    protected final Context mContext;
    protected final Paint mPaint;

    public interface DividerListener {
        boolean shouldDrawBottomDivider(int position);
    }

    protected DividerListener mDividerListener;

    public void setListener(DividerListener listener) {
        this.mDividerListener = listener;
    }

    public ItemDecorationDivider(Builder builder) {
        this.mContext = builder.context;
        this.drawFirstDivider = builder.drawFirstDivider;
        this.drawLastDivider = builder.drawLastDivider;
        this.dividerTakesUpSize = builder.dividerTakesUpSize;
        this.paddingStart = builder.paddingStart;
        this.paddingEnd = builder.paddingEnd;
        this.dividerSize = builder.dividerSize;

        mPaint = new Paint();
        mPaint.setColor(builder.dividerColor);
        mPaint.setStrokeWidth(this.dividerSize);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    private int getOrientation(RecyclerView parent)
    {
        RecyclerView.LayoutManager mgr = parent.getLayoutManager();
        if (null != mgr)
            return ((LinearLayoutManager)parent.getLayoutManager()).getOrientation();
        return LinearLayoutManager.VERTICAL;
    }

    public void drawVertical(Canvas c, RecyclerView parent) {
        final int left = parent.getPaddingLeft() + paddingStart;
        final int right = parent.getWidth() - parent.getPaddingRight() - paddingEnd;

        for (int i = 0; i < parent.getChildCount(); i++) {
            drawDecorationForViewInVertical(c, parent, i, left, right, mPaint);
        }
    }

    public void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop() + paddingStart;
        final int bottom = parent.getHeight() - parent.getPaddingBottom() - paddingEnd;

        for (int i = 0; i < parent.getChildCount(); i++) {
            drawDecorationForViewInHorizontal(c, parent, i, top, bottom, mPaint);
        }
    }

    protected void drawDecorationForViewInVertical(Canvas c, RecyclerView parent, int index, int left, int right, Paint paint) {
        final View child = parent.getChildAt(index);
        final int childPos = parent.getChildAdapterPosition(child);
        final RecyclerView.LayoutParams params =
                (RecyclerView.LayoutParams) child.getLayoutParams();

        if (!(childPos == parent.getAdapter().getItemCount()-1 && !drawLastDivider)
                && (mDividerListener == null || mDividerListener.shouldDrawBottomDivider(childPos))) {
            drawBottomLine(c, child, params, left, right, paint);
        }

        // draw first divider
        if (childPos == 0 && drawFirstDivider) {
            drawTopLine(c, child, params, left, right, paint);
        }
    }

    protected void drawDecorationForViewInHorizontal(Canvas c, RecyclerView parent, int index, int top, int bottom, Paint paint) {
        final View child = parent.getChildAt(index);
        final int childPos = parent.getChildAdapterPosition(child);
        final RecyclerView.LayoutParams params =
                (RecyclerView.LayoutParams) child.getLayoutParams();

        if (!(childPos == parent.getAdapter().getItemCount()-1 && !drawLastDivider)) {
            drawRightLine(c, child, params, top, bottom, paint);
        }
        // draw left divider
        if (childPos == 0 && drawFirstDivider) {
            drawLeftLine(c, child, params, top, bottom, paint);
        }
    }

    protected void drawTopLine(Canvas c, View viewToDraw, RecyclerView.LayoutParams lp, int left, int right, Paint paint) {
        int y = viewToDraw.getTop() - lp.topMargin;
        drawLine(c, left, y, right, y, paint);
    }

    protected void drawBottomLine(Canvas c, View viewToDraw, RecyclerView.LayoutParams lp, int left, int right, Paint paint) {

        int y = viewToDraw.getBottom() + lp.bottomMargin;
        drawLine(c, left, y, right, y, paint);
    }

    protected void drawLeftLine(Canvas c, View viewToDraw, RecyclerView.LayoutParams lp, int top, int bottom, Paint paint) {
        int x = viewToDraw.getLeft() - lp.leftMargin + Math.round(paint.getStrokeWidth() * 0.5f);
        drawLine(c, x, top, x, bottom, paint);
    }

    protected void drawRightLine(Canvas c, View viewToDraw, RecyclerView.LayoutParams lp, int top, int bottom, Paint paint) {
        int x = viewToDraw.getRight() + lp.rightMargin - Math.round(paint.getStrokeWidth() * 0.5f);
        drawLine(c, x, top, x, bottom, paint);
    }

    protected void drawLine(Canvas c, int x1, int y1, int x2, int y2, Paint paint) {
        c.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        if (!dividerTakesUpSize) {
            outRect.set(0, 0, 0, 0);
            return;
        }

        final int itemPosition = parent.getChildAdapterPosition(view);
        int first = (itemPosition == 0 && drawFirstDivider) ? dividerSize : 0;
        int last = (itemPosition == parent.getAdapter().getItemCount() - 1 && !drawLastDivider) ? 0 : dividerSize;

        if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
            outRect.set(0, first, 0, last);
        } else {
            outRect.set(first, 0, last, 0);
        }
    }

    public static class Builder {

        private Context context;
        private boolean drawFirstDivider = false;
        private boolean drawLastDivider = false;
        private boolean dividerTakesUpSize = true;
        private int paddingStart = 0;
        private int paddingEnd = 0;
        private int dividerSize = 1;

        @ColorInt
        private int dividerColor = 0xFFDDDDDD;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder setDrawFirstDivider(boolean drawFirstDivider) {
            this.drawFirstDivider = drawFirstDivider;
            return this;
        }

        public Builder setDrawLastDivider(boolean drawLastDivider) {
            this.drawLastDivider = drawLastDivider;
            return this;
        }

        public Builder setDividerTakesUpSize(boolean dividerTakesUpSize) {
            this.dividerTakesUpSize = dividerTakesUpSize;
            return this;
        }

        public Builder setPaddingStart(int paddingStart) {
            this.paddingStart = paddingStart;
            return this;
        }

        public Builder setPaddingEnd(int paddingEnd) {
            this.paddingEnd = paddingEnd;
            return this;
        }

        public Builder setDividerColor(int dividerColor) {
            this.dividerColor = dividerColor;
            return this;
        }
        public Builder setDividerSize(int dividerSize) {
            this.dividerSize = dividerSize;
            return this;
        }

        public ItemDecorationDivider build(){
            return new ItemDecorationDivider(this);
        }
    }
}
