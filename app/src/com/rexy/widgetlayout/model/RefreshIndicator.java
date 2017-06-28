package com.rexy.widgetlayout.model;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rexy.widgetlayout.R;
import com.rexy.widgets.group.NestRefreshLayout;
import com.rexy.widgets.group.WrapLayout;
import com.rexy.widgets.tools.ViewUtils;


public class RefreshIndicator extends WrapLayout implements NestRefreshLayout.OnRefreshListener {
    ProgressBar mProgressBar;
    ImageView mImageView; //当是刷新时不为null,加载更多时为null .
    TextView mTextView;
    int mLastRotateType = 0;
    boolean isRefreshViewAdded;
    boolean isRefreshPullType;
    String[] mIndicatorTexts = new String[]{
            "获取数据中",
            "下拉刷新",
            "上拉加载更多",
            "松开刷新",
            "松开加载更多",
            "请放手刷新",
            "请放手加载更多",
    };

    public RefreshIndicator(Context context) {
        super(context);
        init(context, null);
    }

    public RefreshIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RefreshIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setGravity(Gravity.CENTER);
        setEachLineMinItemCount(1);
        setEachLineMaxItemCount(2);
        setEachLineCenterHorizontal(true);
        setEachLineCenterVertical(true);
        setMinimumHeight((int) (context.getResources().getDisplayMetrics().density*50));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        removeRefreshViewInner();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeRefreshViewInner();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isRefreshViewAdded) {
            isRefreshViewAdded = buildRefreshViewInnerIfNeed();
        }
    }

    private void removeRefreshViewInner() {
        removeAllViewsInLayout();
        isRefreshViewAdded = false;
        mProgressBar = null;
        mTextView = null;
        mImageView = null;
    }

    private boolean buildRefreshViewInnerIfNeed() {
        if (!isRefreshViewAdded && getParent() instanceof NestRefreshLayout) {
            NestRefreshLayout parent = (NestRefreshLayout) getParent();
            if (parent.getRefreshPullIndicator() == this) {
                isRefreshPullType = true;
                isRefreshViewAdded = true;
            }
            if (parent.getRefreshPushIndicator() == this) {
                isRefreshPullType = false;
                isRefreshViewAdded = true;
            }
            if (isRefreshViewAdded) {
                removeRefreshViewInner();
                buildRefreshViewInner(isRefreshPullType);
                isRefreshViewAdded = true;
            }
        }
        return isRefreshViewAdded;
    }

    private void buildRefreshViewInner(boolean header) {
        Context context = getContext();
        float density = context.getResources().getDisplayMetrics().density;
        mTextView = new TextView(context);
        mProgressBar = new ProgressBar(context);
        mImageView = new ImageView(context);
        mImageView.setImageResource(R.drawable.icon_refresh_down);
        mTextView.setTextColor(getResources().getColor(R.color.optionBackground));
        mTextView.setTextSize(16);
        mTextView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        RefreshIndicator.LayoutParams lpLeft = new RefreshIndicator.LayoutParams(-2, -2);
        lpLeft.gravity = Gravity.CENTER;
        lpLeft.maxHeight = lpLeft.maxWidth = (int) (density * 35);
        addView(mImageView, lpLeft);
        addView(mProgressBar, lpLeft);
        addView(mTextView);
    }

    private void rotateArrow(View view, boolean reversed, boolean optHeader) {
        int rotateType = reversed ? 1 : -1;
        if (rotateType != mLastRotateType) {
            int from = reversed ? 0 : 180;
            int to = reversed ? 180 : 360;
            RotateAnimation rotate = new RotateAnimation(from, to, RotateAnimation.RELATIVE_TO_SELF,
                    0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(150);
            rotate.setFillAfter(true);
            view.clearAnimation();
            view.startAnimation(rotate);
            mLastRotateType = rotateType;
        }
    }

    @Override
    public void onRefreshStateChanged(NestRefreshLayout parent, int state, int preState, int moveDistance) {
        if (isRefreshViewAdded) {
            if (state != preState && !parent.isRefreshing()) {
                if (state == NestRefreshLayout.OnRefreshListener.STATE_IDLE) {
                    mLastRotateType = 0;
                    if (mImageView != null) {
                        mImageView.clearAnimation();
                    }
                } else {
                    mTextView.setText(mIndicatorTexts[state]);
                }
                ViewUtils.setVisibility(mProgressBar, View.GONE);
                if (mImageView != null) {
                    ViewUtils.setVisibility(mImageView, View.VISIBLE);
                    if (preState == NestRefreshLayout.OnRefreshListener.STATE_PULL_READY) {
                        if (state == NestRefreshLayout.OnRefreshListener.STATE_PULL_BEYOND_READY) {
                            rotateArrow(mImageView, true, true);
                        } else if (state == NestRefreshLayout.OnRefreshListener.STATE_PULL_TO_READY) {
                            rotateArrow(mImageView, false, true);
                        }
                    }
                    if (preState == NestRefreshLayout.OnRefreshListener.STATE_PUSH_READY) {
                        if (state == NestRefreshLayout.OnRefreshListener.STATE_PUSH_BEYOND_READY) {
                            rotateArrow(mImageView, true, false);
                        } else if (state == NestRefreshLayout.OnRefreshListener.STATE_PUSH_TO_READY) {
                            rotateArrow(mImageView, false, false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRefresh(NestRefreshLayout parent, boolean refresh) {
        if (isRefreshViewAdded) {
            mTextView.setText(mIndicatorTexts[0]);
            if (mImageView != null) {
                ViewUtils.setVisibility(mImageView, View.GONE);
                mImageView.clearAnimation();
            }
            ViewUtils.setVisibility(mProgressBar, View.VISIBLE);
        }
    }
}
