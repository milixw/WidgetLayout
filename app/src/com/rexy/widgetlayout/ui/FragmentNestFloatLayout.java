package com.rexy.widgetlayout.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rexy.common.BaseFragment;
import com.rexy.widgetlayout.R;
import com.rexy.widgetlayout.model.DecorationOffsetLinear;
import com.rexy.widgetlayout.model.TestRecyclerAdapter;
import com.rexy.widgets.tools.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-06-05 15:03
 */
public class FragmentNestFloatLayout extends BaseFragment {
    RecyclerView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_nestfloatlayout, container, false);
        mListView = ViewUtils.view(root, R.id.listView);
        initRecyclerView(mListView, 25);
        return root;
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
}
