package com.rexy.widgetlayout.model;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rexy.widgetlayout.R;
import com.rexy.widgets.group.NestRefreshLayout;
import com.rexy.widgets.tools.ViewUtils;


public class RefreshIndicator extends RelativeLayout implements NestRefreshLayout.OnRefreshListener {
    ProgressBar mProgressBar;
    ImageView mImageView;
    TextView mIndicatorText;
    int mLastRotateType = 0;
    String[] mIndicatorTexts = new String[]{
            "获取数据中...",
            "下拉刷新",
            "上拉加载更多",
            "松开刷新",
            "松开加载更多",
            "请放手刷新",
            "请放手加载更多",
    };

    public RefreshIndicator(Context context) {
        super(context);
    }

    public RefreshIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RefreshIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mProgressBar = (ProgressBar) findViewById(R.id.pull_to_refresh_progress);
        mImageView = (ImageView) findViewById(R.id.pull_to_refresh_image);
        mIndicatorText = (TextView) findViewById(R.id.pull_to_refresh_text);
    }

    private void rotateArrow(View view, boolean reversed,boolean optHeader) {
        int roateType = reversed ? 1 : -1;
        if (roateType != mLastRotateType) {
            int from = reversed ? 0 : 180;
            int to = reversed ? 180 : 360;
            RotateAnimation rotate = new RotateAnimation(from, to, RotateAnimation.RELATIVE_TO_SELF,
                    0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(150);
            rotate.setFillAfter(true);
            view.clearAnimation();
            view.startAnimation(rotate);
            mLastRotateType = roateType;
        }
    }

    @Override
    public void onRefreshStateChanged(NestRefreshLayout parent, int state, int preState, int moveDistance) {
        if (parent.isRefreshing()) {
            mIndicatorText.setText(mIndicatorTexts[0]);
        }
        if (state != preState && !parent.isRefreshing()) {
            if (state == NestRefreshLayout.OnRefreshListener.STATE_IDLE) {
                mLastRotateType = 0;
                mImageView.clearAnimation();
            } else {
                mIndicatorText.setText(mIndicatorTexts[state]);
            }
            ViewUtils.setVisibility(mImageView, View.VISIBLE);
            ViewUtils.setVisibility(mProgressBar, View.GONE);
            if (preState == NestRefreshLayout.OnRefreshListener.STATE_PULL_READY) {
                if (state == NestRefreshLayout.OnRefreshListener.STATE_PULL_BEYOND_READY) {
                    rotateArrow(mImageView, true,true);
                } else if (state == NestRefreshLayout.OnRefreshListener.STATE_PULL_TO_READY) {
                    rotateArrow(mImageView, false,true);
                }
            }
            if (preState == NestRefreshLayout.OnRefreshListener.STATE_PUSH_READY) {
                if (state == NestRefreshLayout.OnRefreshListener.STATE_PUSH_BEYOND_READY) {
                    rotateArrow(mImageView, true,false);
                } else if (state == NestRefreshLayout.OnRefreshListener.STATE_PUSH_TO_READY) {
                    rotateArrow(mImageView, false,false);
                }
            }
        }
    }

    @Override
    public void onRefresh(NestRefreshLayout parent, boolean refresh) {
        ViewUtils.setVisibility(mImageView, View.GONE);
        mImageView.clearAnimation();
        ViewUtils.setVisibility(mProgressBar, View.VISIBLE);
    }
}
