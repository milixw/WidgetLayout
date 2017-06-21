package com.rexy.widgetlayout.model;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rexy.widgets.view.FadeTextButton;

import java.util.List;

/**
 * Created by rexy on 17/6/21.
 */

public class TestRecyclerAdapter extends RecyclerView.Adapter {
    List<String> mListData;
    Context mContext;

    public TestRecyclerAdapter(Context context, List<String> list) {
        mContext = context;
        mListData = list;
    }

    public void setItem(List<String> data) {
        if (data != mListData) {
            mListData = data;
            notifyDataSetChanged();
        }
    }

    public void addAll(List<String> data) {
        if (data != null) {
            int start = getItemCount();
            if (mListData == null) {
                mListData = data;
            } else {
                mListData.addAll(data);
            }
            notifyItemRangeInserted(start, data.size());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        FadeTextButton textView = new FadeTextButton(mContext);
        textView.setPadding(30, 20, 30, 20);
        textView.setTextSize(18);
        textView.setClickable(true);
        textView.setTextColor(ColorStateList.valueOf(0xFFFF0000));
        textView.setBackgroundColor(0xAA00FF00);
        textView.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        return new TestRecyclerHolder(textView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder.itemView instanceof TextView) {
            ((TextView) viewHolder.itemView).setText(mListData.get(i));
        }
    }

    @Override
    public int getItemCount() {
        return mListData == null ? 0 : mListData.size();
    }

    static class TestRecyclerHolder extends RecyclerView.ViewHolder {
        public TestRecyclerHolder(View itemView) {
            super(itemView);
        }
    }
}