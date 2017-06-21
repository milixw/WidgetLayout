package com.rexy.widgetlayout.model;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * This class can only be used in the RecyclerView which use a LinearLayoutManager or
 * its subclass.
 */
public class DecorationOffsetLinear extends RecyclerView.ItemDecoration {
    private final SparseArray<OffsetsCreator> mTypeOffsetsFactories = new SparseArray<>();
    private boolean isHorizontal;
    private int mContentOffset;
    private int mContentOffsetStart;
    private int mContentOffsetEnd;

    private boolean isApplyOffsetToEdge;
    private boolean isNoMoreData = true;

    public DecorationOffsetLinear(boolean horizontal, int contentOffset) {
        this(horizontal, contentOffset, 0, 0);
    }

    public DecorationOffsetLinear(boolean horizontal, int contentOffset, int contentOffsetStart, int contentOffsetEnd) {
        isApplyOffsetToEdge = false;
        isHorizontal = horizontal;
        mContentOffset = contentOffset;
        mContentOffsetStart = contentOffsetStart;
        mContentOffsetEnd = contentOffsetEnd;
    }

    public void setApplyOffsetToEdge(boolean isOffsetEdge) {
        this.isApplyOffsetToEdge = isOffsetEdge;
    }

    public void setNoMoreData(boolean noMoreData) {
        isNoMoreData = noMoreData;
    }

    public void registerTypeOffset(int itemType, OffsetsCreator offsetsCreator) {
        mTypeOffsetsFactories.put(itemType, offsetsCreator);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int adapterPosition = parent.getChildAdapterPosition(view);
        int adapterLastCountIndex = state.getItemCount() - 1;
        int itemOffset = getDividerOffsets(parent, view, adapterPosition);
        boolean contentItemFirst = adapterPosition == 0;
        boolean contentItemLast = adapterPosition == adapterLastCountIndex;
        boolean dataLastItem = contentItemLast && isNoMoreData;
        if (isHorizontal) {
            outRect.right = dataLastItem ? mContentOffsetEnd : itemOffset;
            if (isApplyOffsetToEdge) {
                outRect.top = itemOffset;
                outRect.bottom = itemOffset;
            }
            if (contentItemFirst) {
                outRect.left = mContentOffsetStart;
            }
        } else {
            outRect.bottom = dataLastItem ? mContentOffsetEnd : itemOffset;
            if (isApplyOffsetToEdge) {
                outRect.left = itemOffset;
                outRect.right = itemOffset;
            }
            if (contentItemFirst) {
                outRect.top = mContentOffsetStart;
            }
        }
    }

    private int getDividerOffsets(RecyclerView parent, View view, int adapterPosition) {
        if (mTypeOffsetsFactories.size() == 0) {
            return mContentOffset;
        }
        final int itemType = parent.getAdapter().getItemViewType(adapterPosition);
        final OffsetsCreator offsetsCreator = mTypeOffsetsFactories.get(itemType);
        if (offsetsCreator != null) {
            return offsetsCreator.create(parent, adapterPosition);
        }
        return mContentOffset;
    }

    public interface OffsetsCreator {
        int create(RecyclerView parent, int adapterPosition);
    }
}
