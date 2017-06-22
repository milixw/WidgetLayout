package com.rexy.widgetlayout.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.rexy.common.BaseFragment;
import com.rexy.widgetlayout.R;
import com.rexy.widgetlayout.model.DecorationOffsetLinear;
import com.rexy.widgetlayout.model.RefreshIndicator;
import com.rexy.widgetlayout.model.TestRecyclerAdapter;
import com.rexy.widgets.group.NestRefreshLayout;
import com.rexy.widgets.tools.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-06-05 15:03
 */
public class FragmentRefreshLayout extends BaseFragment implements NestRefreshLayout.OnRefreshListener {
    NestRefreshLayout mRefreshLayout;
    RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRefreshLayout = (NestRefreshLayout) inflater.inflate(R.layout.fragment_refreshlayout, container, false);
        mRecyclerView = ViewUtils.view(mRefreshLayout, R.id.recycleView);
        initRecyclerView(mRecyclerView, 15);
        initRefreshLayout(mRefreshLayout);
        return mRefreshLayout;
    }

    private void initRefreshLayout(NestRefreshLayout refreshLayout) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setRefreshPullIndicator(new RefreshIndicator(inflater.getContext()));
        refreshLayout.setRefreshPushIndicator(new RefreshIndicator(inflater.getContext()));
        refreshLayout.setBackgroundColor(0xFFEEEEEE);
    }

    private void initRecyclerView(RecyclerView recyclerView, int initCount) {
        recyclerView.setAdapter(new TestRecyclerAdapter(getActivity(), createData("item", initCount)));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new DecorationOffsetLinear(false, 20));
    }

    private List<String> createData(String prefix, int count) {
        List<String> list = new ArrayList(count + 1);
        for (int i = 0; i < count; i++) {
            list.add(prefix + " " + (i + 1));
        }
        return list;
    }

    @Override
    public void onRefresh(final NestRefreshLayout parent, final boolean refresh) {
        Toast.makeText(getActivity(), refresh ? "pull refresh" : "push load more", Toast.LENGTH_SHORT).show();
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                TestRecyclerAdapter adapter = (TestRecyclerAdapter) mRecyclerView.getAdapter();
                if (refresh) {
                    adapter.setItem(createData("refresh", 15));
                } else {
                    adapter.addAll(createData("loadmore", 5));
                }
                parent.setRefreshComplete();
            }
        }, 1200);
    }

    @Override
    public void onRefreshStateChanged(NestRefreshLayout parent, int state, int preState, int moveAbsDistance) {
    }
}
