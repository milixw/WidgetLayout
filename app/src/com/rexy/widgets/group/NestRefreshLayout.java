package com.rexy.widgets.group;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO:功能说明
 * 每次下接刷新仅仅会影响二次或四次measure 和 layout (状态变法设置visible 引起)
 *
 * @author: renzheng
 * @date: 2017-06-19 16:23
 */
public class NestRefreshLayout extends BaseViewGroup implements NestedScrollingParent {
    View mHeaderView;//固定头部。
    View mFooterView;//固定尾部。
    View mRefreshHeader;//刷新指示头。
    View mRefreshFooter;//刷新指示尾。
    View mContentView;//中间内容，仅支持一个。
    View mMaskView;

    View mScrollChild;

    boolean isHeaderViewFloat;
    boolean isFooterViewFloat;
    boolean isMaskContent = true;
    boolean isRefreshPullEnable = true;
    boolean isRefreshPushEnable = true;
    boolean isRefreshNestEnable = true;

    int mMaskViewVisible = -1;
    int mMaxPullDistance = -1;
    int mMaxPushDistance = -1;

    OnRefreshListener mRefreshListener = null;

    private int mTouchSlop;
    private float mRefreshMoveFactor = 0.33f;
    private float mRefreshReadyFactor = 1.3f;
    private boolean isRefreshing = false;
    private boolean isOptHeader;
    private boolean mIsBeingDragged = false;
    private boolean mCancelDragged = false;
    private PointF mPointDown = new PointF();
    private PointF mPointLast = new PointF();
    private boolean mNeedCheckAddInLayout = false;
    private int[] mMeasureResult = new int[3];

    private boolean mRefreshOffsetWay = false;

    private int mRefreshState = OnRefreshListener.STATE_IDLE;
    private int mLastRefreshDistance = 0;
    private int mInnerMaxPullDistance = 0;
    private int mInnerMaxPushDistance = 0;
    private int mInnerPullReadyDistance = 0;
    private int mInnerPushReadyDistance = 0;

    int mDurationMin = 0, mDurationMax = 0;
    Interpolator mInterpolator;
    private final RefreshAnimation mRefreshAnimation = new RefreshAnimation();

    public NestRefreshLayout(Context context) {
        super(context);
        init(context, null);
    }

    public NestRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NestRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public NestRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attr) {
        setLogTag("rexy_refresh");
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, checkContentView(child, index), params);
    }

    private int checkContentView(View child, int index) {
        if (child != mRefreshHeader && child != mRefreshFooter && child != mHeaderView && child != mFooterView) {
            if (mContentView != null && mContentView.getParent() == this) {
                throw new IllegalStateException("RefreshLayout can host only one content child");
            }
            mContentView = child;
            int optIndex = 0;
            if (mRefreshFooter != null && mRefreshFooter.getParent() == this) {
                optIndex++;
            }
            if (mRefreshHeader != null && mRefreshHeader.getParent() == this) {
                optIndex++;
            }
            return optIndex;
        }
        return index;
    }

    public boolean setHeaderView(View child) {
        return setHeaderView(child, isHeaderViewFloat);
    }

    public boolean setHeaderView(View child, boolean headerViewFloat) {
        if (mHeaderView != child) {
            View oldView = mHeaderView;
            mHeaderView = child;
            boolean floatChanged = this.isHeaderViewFloat != headerViewFloat;
            if (floatChanged) {
                this.isHeaderViewFloat = headerViewFloat;
            }
            if (!updateBuildInView(child, oldView) && floatChanged) {
                requestLayout();
            }
            return true;
        }
        return false;
    }

    public void setHeaderViewFloat(boolean floatView) {
        if (isHeaderViewFloat != floatView) {
            isHeaderViewFloat = floatView;
            if (!skipChild(mHeaderView)) {
                requestLayout();
            }
        }
    }

    public boolean setFooterView(View child) {
        return setFooterView(child, isFooterViewFloat);
    }

    public boolean setFooterView(View child, boolean footerViewFloat) {
        if (mFooterView != child) {
            View oldView = mFooterView;
            mFooterView = child;
            boolean floatChanged = this.isFooterViewFloat != footerViewFloat;
            if (floatChanged) {
                this.isFooterViewFloat = footerViewFloat;
            }
            if (!updateBuildInView(child, oldView) && floatChanged) {
                requestLayout();
            }
            return true;
        }
        return false;
    }

    public void setFooterViewFloat(boolean floatView) {
        if (isFooterViewFloat != floatView) {
            isFooterViewFloat = floatView;
            if (!skipChild(mFooterView)) {
                requestLayout();
            }
        }
    }

    public boolean setMaskView(View child, boolean justMaskContent) {
        if (mMaskView != child) {
            View oldView = mMaskView;
            mMaskView = child;
            boolean maskChanged = this.isMaskContent != justMaskContent;
            if (maskChanged) {
                this.isMaskContent = justMaskContent;
            }
            if (!updateBuildInView(child, oldView) && maskChanged) {
                requestLayout();
            }
        }
        return false;
    }

    public void setContentView(View child) {
        if (mContentView != child) {
            View optView = mContentView;
            mContentView = child;
            if (optView != null && optView.getParent() == this) {
                removeView(optView);
            }
            if (child != null) {
                addView(child);
            }
        }
    }

    public void setMaskViewVisibility(int visibility) {
        if (mMaskViewVisible != visibility) {
            mMaskViewVisible = visibility;
            if (!skipChild(mMaskView)) {
                mMaskView.setVisibility(VISIBLE);
                mMaskViewVisible = -1;
            }
        }
    }

    public void setMaskViewJustOverContent(boolean justOverContent) {
        if (isMaskContent != justOverContent) {
            isMaskContent = justOverContent;
            if (!skipChild(mMaskView) && (!isMaskContent || skipChild(mContentView))) {
                requestLayout();
            }
        }
    }

    public boolean setRefreshPullIndicator(View child) {
        if (mRefreshHeader != child) {
            View oldView = mRefreshHeader;
            mRefreshHeader = child;
            updateBuildInView(child, oldView);
        }
        return child != null;
    }

    public boolean setRefreshPushIndicator(View child) {
        if (mRefreshFooter != child) {
            View oldView = mRefreshFooter;
            mRefreshFooter = child;
            updateBuildInView(child, oldView);
        }
        return child != null;
    }

    public void setRefreshPullEnable(boolean enable) {
        if (isRefreshPullEnable != enable) {
            isRefreshPullEnable = enable;
        }
    }

    public void setRefreshPushEnable(boolean enable) {
        if (isRefreshPushEnable != enable) {
            isRefreshPushEnable = enable;
        }
    }

    public void setRefresNestEnable(boolean enable) {
        isRefreshNestEnable = enable;
    }

    public void setMaxPullDistance(int maxPullDistance) {
        mMaxPullDistance = maxPullDistance;
    }

    public void setMaxPushDistance(int maxPUshDistance) {
        mMaxPushDistance = maxPUshDistance;
    }

    /**
     * @param moveFactor value in [0.25f,1f]
     */
    public void setRefreshMoveFactor(float moveFactor) {
        if (moveFactor > 0.25f && moveFactor <= 1) {
            mRefreshMoveFactor = moveFactor;
        }
    }

    /**
     * @param refreshReadyFactor value in (1,..)
     */
    public void setRefreshReadyFactor(float refreshReadyFactor) {
        if (refreshReadyFactor > 1) {
            mRefreshReadyFactor = refreshReadyFactor;
        }
    }

    public void setOnRefreshListener(OnRefreshListener l) {
        mRefreshListener = l;
    }

    public void setRefreshing(boolean refreshHeader) {
        int criticalDistance = 0;
        if (refreshHeader && isRefreshPullEnable()) {
            criticalDistance = (int) (getPullReadyDistance() / mRefreshReadyFactor);
        }
        if (!refreshHeader && isRefreshPushEnable()) {
            criticalDistance = (int) (getPushReadyDistance() / mRefreshReadyFactor);
        }
        if (criticalDistance > 0) {
            setRefreshComplete(false);
            isOptHeader = refreshHeader;
            animateRefresh(0, criticalDistance, -1);
        }
    }

    public void setRefreshComplete() {
        setRefreshComplete(true);
    }

    public void setRefreshComplete(boolean animation) {
        if (mRefreshState != OnRefreshListener.STATE_IDLE || mLastRefreshDistance != 0) {
            animateRefresh(mLastRefreshDistance, 0, animation ? -1 : 0);
        }
    }

    public void setAnimationInterpolator(Interpolator l) {
        mInterpolator = l;
    }

    public void setAnimationDuration(int minDuration, int maxDuration) {
        mDurationMin = minDuration;
        mDurationMax = maxDuration;
    }

    public int getRefreshState() {
        return mRefreshState;
    }

    public View getMaskView() {
        return mMaskView;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public View getFooterView() {
        return mFooterView;
    }

    public View getContentView() {
        return mContentView;
    }

    public void setScrollChild(View child) {
        mScrollChild = child;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public boolean isRefreshPullEnable() {
        return isRefreshPullEnable && mRefreshHeader != null && hasRefreshListener(mRefreshHeader);
    }

    public boolean isRefreshPushEnable() {
        return isRefreshPushEnable && mRefreshFooter != null && hasRefreshListener(mRefreshFooter);
    }

    private boolean hasRefreshListener(View indicator) {
        return mRefreshListener != null || (indicator instanceof OnRefreshListener);
    }

    public boolean canScrollToChildTop(View child) {
        if (child == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (child instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) child;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(child, -1) || child.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(child, -1);
        }
    }

    public boolean canScrollToChildBottom(View child) {
        if (child == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (child instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) child;
                if (absListView.getChildCount() > 0) {
                    int lastChildBottom = absListView.getChildAt(absListView.getChildCount() - 1)
                            .getBottom();
                    return absListView.getLastVisiblePosition() == absListView.getAdapter().getCount() - 1
                            && lastChildBottom <= absListView.getMeasuredHeight();
                } else {
                    return false;
                }
            } else {
                return ViewCompat.canScrollVertically(child, 1) || child.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(child, 1);
        }
    }

    protected boolean isScrollAbleView(View view) {
        return (view instanceof RecyclerView)
                || (view instanceof AbsListView)
                || (view instanceof ScrollView)
                || (view instanceof WebView)
                || (view instanceof NestedScrollView)
                || (view instanceof PageScrollView)
                || (view instanceof NestFloatLayout);
    }

    protected View getScrollAbleView() {
        if (mScrollChild == null && mContentView != null) {
            if (isScrollAbleView(mContentView)) {
                mScrollChild = mContentView;
            } else {
                Queue<ViewGroup> queue = new LinkedList<>();
                queue.offer((ViewGroup) mContentView);
                ViewGroup parrent;
                while ((parrent = queue.poll()) != null) {
                    int size = parrent == null ? 0 : parrent.getChildCount();
                    for (int i = 0; i < size; i++) {
                        View v = parrent.getChildAt(i);
                        if ((v instanceof RecyclerView) || (v instanceof AbsListView) || (v instanceof ScrollView) || (v instanceof WebView)) {
                            mScrollChild = v;
                            queue.clear();
                            break;
                        } else if (v instanceof ViewGroup) {
                            queue.offer((ViewGroup) v);
                        }
                    }
                }
            }
        }
        return mScrollChild;
    }

    private boolean updateBuildInView(View view, View oldView) {
        boolean requestLayout = false;
        if (oldView != null && oldView.getParent() == this) {
            removeView(oldView);
            requestLayout = true;
        }
        if (view != null) {
            mNeedCheckAddInLayout = true;
            requestLayout();
            requestLayout = true;
        }
        return requestLayout;
    }

    private void addBuildInView(View child, int index, int defaultWidth) {
        if (child == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
            params.width = defaultWidth;
        } else if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        addViewInLayout(child, index, params, true);
    }

    private int addBuildInViewIfNeed(View view, int addIndex, int defaultWidth) {
        if (view != null) {
            if (view.getParent() != this) {
                addBuildInView(view, addIndex, defaultWidth);
            }
            addIndex++;
        }
        return addIndex;
    }

    private void makeSureBuildInViewAdded() {
        if (mRefreshState == OnRefreshListener.STATE_IDLE) {
            checkRefreshVisible(false, false);
        }
        int optIndex = addBuildInViewIfNeed(mRefreshFooter, 0, -1);
        optIndex = addBuildInViewIfNeed(mRefreshHeader, optIndex, -1);
        if (mContentView != null && mContentView.getParent() == this) {
            optIndex++;
        }
        optIndex = addBuildInViewIfNeed(mFooterView, optIndex, -1);
        optIndex = addBuildInViewIfNeed(mHeaderView, optIndex, -1);
        boolean resetMaskVisible = mMaskViewVisible != -1 && mMaskView != null && mMaskView.getParent() != this;
        addBuildInViewIfNeed(mMaskView, optIndex, -1);
        if (resetMaskVisible) {
            mMaskView.setVisibility(mMaskViewVisible);
            mMaskViewVisible = -1;
        }
    }

    @Override
    protected boolean skipChild(View child) {
        return super.skipChild(child) || child.getParent() != this;
    }

    private void measureChild(View child, int parentSpecWidth, int parentSpecHeight, int heightUsed) {
        Arrays.fill(mMeasureResult, 0);
        NestRefreshLayout.LayoutParams params = (LayoutParams) child.getLayoutParams();
        int marginHorizontal = params.getMarginHorizontal(), marginVertical = params.getMarginVertical();
        params.measure(child, parentSpecWidth, parentSpecHeight, marginHorizontal, marginVertical + heightUsed);
        mMeasureResult[0] = child.getMeasuredWidth() + marginHorizontal;
        mMeasureResult[1] = child.getMeasuredHeight() + marginVertical;
        mMeasureResult[2] = child.getMeasuredState();
    }

    @Override
    protected void dispatchMeasure(int widthMeasureSpecNoPadding, int heightMeasureSpecNoPadding, int maxSelfWidthNoPadding, int maxSelfHeightNoPadding) {
        if (mNeedCheckAddInLayout) {
            makeSureBuildInViewAdded();
            mNeedCheckAddInLayout = false;
        }
        int contentHeight = 0, contentWidth = 0, childState = 0;
        int floatHeight = 0, refreshHeight = 0;
        boolean contentVisible = !skipChild(mContentView);
        if (!skipChild(mHeaderView)) {
            measureChild(mHeaderView, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, contentHeight);
            contentWidth = Math.max(contentWidth, mMeasureResult[0]);
            childState |= mMeasureResult[2];
            if (isHeaderViewFloat) {
                floatHeight += mMeasureResult[1];
            } else {
                contentHeight += mMeasureResult[1];
            }
        }
        if (!skipChild(mFooterView)) {
            measureChild(mFooterView, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, contentHeight);
            contentWidth = Math.max(contentWidth, mMeasureResult[0]);
            childState |= mMeasureResult[2];
            if (isFooterViewFloat) {
                floatHeight += mMeasureResult[1];
            } else {
                contentHeight += mMeasureResult[1];
            }
        }
        int usedHeight = contentHeight;
        if (!skipChild(mRefreshHeader)) {
            measureChild(mRefreshHeader, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, usedHeight);
            contentWidth = Math.max(contentWidth, mMeasureResult[0]);
            childState |= mMeasureResult[2];
            refreshHeight = Math.max(refreshHeight, mMeasureResult[1]);
        }
        if (contentVisible && !skipChild(mRefreshFooter)) {
            measureChild(mRefreshFooter, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, usedHeight);
            contentWidth = Math.max(contentWidth, mMeasureResult[0]);
            childState |= mMeasureResult[2];
            refreshHeight = Math.max(refreshHeight, mMeasureResult[1]);
        }

        if (contentVisible) {
            measureChild(mContentView, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, usedHeight);
            contentWidth = Math.max(contentWidth, mMeasureResult[0]);
            childState |= mMeasureResult[2];
            contentHeight += mMeasureResult[1];
        }
        int maxContentHeight = Math.max(contentHeight, Math.max(refreshHeight, floatHeight));
        if (!skipChild(mMaskView) && (contentVisible || !isMaskContent)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mMaskView.getLayoutParams();
            int marginHorizontal = params.getMarginHorizontal(), marginVertical = params.getMarginVertical();
            if (isMaskContent) {
                NestRefreshLayout.LayoutParams cp = (LayoutParams) mContentView.getLayoutParams();
                int cWidth = mContentView.getMeasuredWidth() + cp.getMarginHorizontal();
                int cHeight = mContentView.getMeasuredHeight() + cp.getMarginVertical();
                int widthMode = MeasureSpec.getMode(widthMeasureSpecNoPadding);
                int heightMode = MeasureSpec.getMode(heightMeasureSpecNoPadding);
                params.measure(mMaskView, MeasureSpec.makeMeasureSpec(cWidth, widthMode), MeasureSpec.makeMeasureSpec(cHeight, heightMode), marginHorizontal, marginVertical);
            } else {
                params.measure(mMaskView, widthMeasureSpecNoPadding, heightMeasureSpecNoPadding, 0, 0);
                maxContentHeight = Math.max(maxContentHeight, mMaskView.getMeasuredHeight() + marginVertical);
            }
        }
        setContentSize(contentWidth, maxContentHeight);
        setMeasureState(childState);
    }

    @Override
    protected void dispatchLayout(int contentLeft, int contentTop, int paddingLeft, int paddingTop, int selfWidthNoPadding, int selfHeightNoPadding) {
        int contentRight = contentLeft + getContentWidth();
        int childLeft, childTop = contentTop, childRight, childBottom;
        boolean contentVisible = !skipChild(mContentView);
        final int offsetTopAndBottom = (mRefreshOffsetWay && mRefreshState != OnRefreshListener.STATE_IDLE) ? (isOptHeader ? mLastRefreshDistance : -mLastRefreshDistance) : 0;
        if (!skipChild(mHeaderView)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
            int childWidth = mHeaderView.getMeasuredWidth(), childHeight = mHeaderView.getMeasuredHeight();
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            childTop += params.topMargin;
            childBottom = childTop + childHeight;
            if (!isOptHeader && offsetTopAndBottom != 0) {
                mHeaderView.layout(childLeft, childTop + offsetTopAndBottom, childRight, childBottom + offsetTopAndBottom);
            } else {
                mHeaderView.layout(childLeft, childTop, childRight, childBottom);
            }
            if (isHeaderViewFloat) {
                childTop = contentTop;
            } else {
                childTop = childBottom + params.bottomMargin;
            }
        }
        int biggestBottom = childTop, tempTop = contentTop;
        if (contentVisible) {
            tempTop = childTop;
            NestRefreshLayout.LayoutParams params = (LayoutParams) mContentView.getLayoutParams();
            int childWidth = mContentView.getMeasuredWidth(), childHeight = mContentView.getMeasuredHeight();
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            childTop += params.topMargin;
            childBottom = childTop + childHeight;
            mContentView.layout(childLeft, childTop + offsetTopAndBottom, childRight, childBottom + offsetTopAndBottom);
            childTop = biggestBottom;
            biggestBottom = childBottom + params.bottomMargin;
        }
        if (!skipChild(mRefreshHeader)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mRefreshHeader.getLayoutParams();
            int childWidth = mRefreshHeader.getMeasuredWidth(), childHeight = mRefreshHeader.getMeasuredHeight();
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            childTop += params.topMargin;
            childBottom = childTop + childHeight;
            mRefreshHeader.layout(childLeft, childTop, childRight, childBottom);
            biggestBottom = Math.max(biggestBottom, childBottom + params.bottomMargin);
        }

        if (contentVisible && !skipChild(mRefreshFooter)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mRefreshFooter.getLayoutParams();
            int childWidth = mRefreshFooter.getMeasuredWidth(), childHeight = mRefreshFooter.getMeasuredHeight();
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            childBottom = biggestBottom - params.bottomMargin;
            childTop = childBottom - childHeight;
            mRefreshFooter.layout(childLeft, childTop, childRight, childBottom);
        }

        if (!skipChild(mFooterView)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mFooterView.getLayoutParams();
            int childWidth = mFooterView.getMeasuredWidth(), childHeight = mFooterView.getMeasuredHeight();
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            if (isFooterViewFloat) {
                childBottom = biggestBottom - params.bottomMargin;
                childTop = childBottom - childHeight;
            } else {
                childTop = biggestBottom + params.topMargin;
                childBottom = childTop + childHeight;
            }
            if (isOptHeader && offsetTopAndBottom != 0) {
                mFooterView.layout(childLeft, childTop + offsetTopAndBottom, childRight, childBottom + offsetTopAndBottom);
            } else {
                mFooterView.layout(childLeft, childTop, childRight, childBottom);
            }
        }

        if (!skipChild(mMaskView) && (contentVisible || !isMaskContent)) {
            NestRefreshLayout.LayoutParams params = (LayoutParams) mMaskView.getLayoutParams();
            int childWidth = mMaskView.getMeasuredWidth(), childHeight = mMaskView.getMeasuredHeight();
            childTop = (isMaskContent ? tempTop : contentTop) + params.topMargin;
            childBottom = childTop + childHeight;
            childLeft = getContentStartH(contentLeft, contentRight, childWidth, params.leftMargin, params.rightMargin, params.gravity);
            childRight = childLeft + childWidth;
            mMaskView.layout(childLeft, childTop + offsetTopAndBottom, childRight, childBottom + offsetTopAndBottom);
        }
    }

    private boolean ifNeedInterceptTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mCancelDragged = mContentView == null || mContentView.getParent() != this || mRefreshState != OnRefreshListener.STATE_IDLE;
            if (!mCancelDragged) {
                boolean refreshHeaderEnable = isRefreshPullEnable();
                boolean refreshFooterEnable = isRefreshPushEnable();
                mCancelDragged = !refreshHeaderEnable && !refreshFooterEnable;
            }
        }
        return mCancelDragged;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ifNeedInterceptTouch(ev) || mCancelDragged) {
            mIsBeingDragged = false;
        } else {
            final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
            if (action == MotionEvent.ACTION_MOVE) {
                handleTouchActionMove(ev, true);
            } else {
                if (action == MotionEvent.ACTION_DOWN) {
                    handleTouchActionDown(ev);
                }
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    handleTouchActionUp(ev);
                }
            }
        }
        return mIsBeingDragged||(isRefreshing&&skipChild(mHeaderView)&&skipChild(mFooterView));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ifNeedInterceptTouch(event) || mCancelDragged) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }
        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
        if (action == MotionEvent.ACTION_MOVE) {
            handleTouchActionMove(event, false);
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                handleTouchActionDown(event);
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                handleTouchActionUp(event);
            }
        }
        return true;
    }

    private void handleTouchActionDown(MotionEvent ev) {
        mPointDown.set(ev.getX(), ev.getY());
        mPointLast.set(mPointDown);
        mIsBeingDragged = false;
        if (mLastRefreshDistance != 0) {
            setRefreshComplete(false);
        }
    }

    private void handleTouchActionMove(MotionEvent ev, boolean fromIntercept) {
        float x = ev.getX(), y = ev.getY();
        if (mIsBeingDragged) {
            mPointLast.set(x, y);
            updateRefreshByMoveDistance(mPointLast.y - mPointDown.y, false);
        } else {
            int dx = (int) (mPointDown.x - x), dy = (int) (mPointDown.y - y);
            int dxAbs = Math.abs(dx), dyAbs = Math.abs(dy);
            boolean dragged;
            if (dragged = (dyAbs > mTouchSlop && (dyAbs * 0.6f) > dxAbs)) {
                dy = (dy > 0 ? mTouchSlop : -mTouchSlop >> 1);
                dx = 0;
            }
            if (!dragged) {
                dx = (int) (mPointLast.x - x);
                dy = (int) (mPointLast.y - y);
                dxAbs = Math.abs(dx);
                dyAbs = Math.abs(dy);
                if (dragged = (dyAbs > mTouchSlop && (dyAbs * 0.6f) > dxAbs)) {
                    dy = (dy > 0 ? mTouchSlop : -mTouchSlop) >> 1;
                    dx = 0;
                }
            }
            mPointLast.set(x, y);
            if (dragged) {
                int refreshState = mRefreshState;
                if ((dy < 0 && mRefreshHeader != null && !canScrollToChildTop(getScrollAbleView()))) {
                    refreshState = OnRefreshListener.STATE_PULL_TO_READY;
                    isOptHeader = true;
                }
                if ((dy > 0 && mRefreshFooter != null && !canScrollToChildBottom(getScrollAbleView()))) {
                    refreshState = OnRefreshListener.STATE_PUSH_TO_READY;
                    isOptHeader = false;
                }
                if (refreshState != mRefreshState) {
                    mIsBeingDragged = true;
                    mInnerMaxPullDistance = mInnerMaxPushDistance = 0;
                    mInnerPullReadyDistance = mInnerPushReadyDistance = 0;
                    mPointDown.set(mPointLast);
                    mPointDown.offset(dx, dy);
                    updateRefreshState(refreshState, (int) (dy * mRefreshMoveFactor));
                    updateRefreshByMoveDistance(mPointLast.y - mPointDown.y, false);
                }
            }
        }
    }

    private void handleTouchActionUp(MotionEvent ev) {
        if (mIsBeingDragged) {
            mIsBeingDragged = false;
            mPointLast.set(ev.getX(), ev.getY());
            updateRefreshByMoveDistance(mPointLast.y - mPointDown.y, true);
        }
    }

    private void checkRefreshVisible(boolean headerVisible, boolean footerVisible) {
        if (mRefreshHeader != null) {
            int visible = headerVisible ? View.VISIBLE : View.GONE;
            if (mRefreshHeader.getVisibility() != visible) {
                mRefreshHeader.setVisibility(visible);
            }
        }
        if (mRefreshFooter != null) {
            int visible = footerVisible ? View.VISIBLE : View.GONE;
            if (mRefreshFooter.getVisibility() != visible) {
                mRefreshFooter.setVisibility(visible);
            }
        }
    }

    private void updateRefreshState(int refreshState, int formatDistance) {
        if (mRefreshState != refreshState) {
            if (formatDistance < 0) {
                formatDistance = -formatDistance;
            }
            int oldRefreshState = mRefreshState;
            mRefreshState = refreshState;
            if (refreshState == OnRefreshListener.STATE_IDLE) {
                isRefreshing = false;
                checkRefreshVisible(false, false);
            } else {
                checkRefreshVisible(isOptHeader, !isOptHeader);
            }
            notify(refreshState, oldRefreshState, formatDistance);
        }
    }

    protected int getMaxPullDistance() {
        if (mInnerMaxPullDistance == 0) {
            int maxDistance = (int) (getHeight() * mRefreshMoveFactor);
            mInnerMaxPullDistance = mMaxPullDistance <= 0 ? maxDistance : Math.min(mMaxPullDistance, maxDistance);
        }
        return mInnerMaxPullDistance;
    }

    protected int getMaxPushDistance() {
        if (mInnerMaxPushDistance == 0) {
            int maxDistance = (int) (getHeight() * mRefreshMoveFactor);
            mInnerMaxPushDistance = mMaxPushDistance <= 0 ? maxDistance : Math.min(mMaxPushDistance, maxDistance);
        }
        return mInnerMaxPushDistance;
    }

    protected int getPullReadyDistance() {
        if (mInnerPullReadyDistance == 0 && !skipChild(mRefreshHeader) && mRefreshHeader.getMeasuredHeight() > 0) {
            mInnerPullReadyDistance = (int) (mRefreshHeader.getMeasuredHeight() * mRefreshReadyFactor);
        }
        return mInnerPullReadyDistance;
    }

    protected int getPushReadyDistance() {
        if (mInnerPushReadyDistance == 0 && !skipChild(mRefreshFooter) && mRefreshFooter.getMeasuredHeight() > 0) {
            mInnerPushReadyDistance = (int) (mRefreshFooter.getMeasuredHeight() * mRefreshReadyFactor);
        }
        return mInnerPushReadyDistance;
    }

    private int calculateRefreshState(int formatDistance, int viewHeight, int criticalDistance, boolean optRefreshHeader) {
        if (criticalDistance == 0) {
            return mRefreshState;
        } else {
            if (formatDistance < viewHeight) {
                return optRefreshHeader ? OnRefreshListener.STATE_PULL_TO_READY : OnRefreshListener.STATE_PUSH_TO_READY;
            }
            if (formatDistance < criticalDistance) {
                return optRefreshHeader ? OnRefreshListener.STATE_PULL_READY : OnRefreshListener.STATE_PUSH_READY;
            }
            return optRefreshHeader ? OnRefreshListener.STATE_PULL_BEYOND_READY : OnRefreshListener.STATE_PUSH_BEYOND_READY;
        }
    }

    protected boolean interceptRefreshDirectionChanged(boolean willOptHeader, float distance, boolean cancelUp) {
        return true;
    }

    private void animateRefresh(int from, int to, int duration) {
        mRefreshAnimation.reset(from, to, true, duration < 0 ? calculateDuration(Math.abs(to - from), isOptHeader) : duration);
        clearAnimation();
        if (mInterpolator != null) {
            mRefreshAnimation.setInterpolator(mInterpolator);
        }
        startAnimation(mRefreshAnimation);
    }

    private int calculateDuration(int distance, boolean optHeader) {
        if (distance == 0) return 0;
        int maxDistance = optHeader ? getMaxPullDistance() : getMaxPushDistance();
        if (maxDistance <= 0) {
            maxDistance = getHeight() / 2;
        }
        int defaultMin = 100, defaultMax = 250;
        int minDuration = mDurationMin <= 0 ? defaultMin : mDurationMin;
        int maxDuration = mDurationMax <= 0 ? defaultMax : mDurationMax;
        if (minDuration == maxDuration || maxDistance == 0) {
            return minDuration;
        } else {
            defaultMin = Math.min(minDuration, maxDuration);
            defaultMax = Math.max(minDuration, maxDuration);
            if (distance > maxDistance) {
                return defaultMax;
            }
            return (int) (defaultMin + distance * (defaultMax - defaultMin) / (float) maxDistance);
        }
    }

    private void updateRefreshByMoveDistance(float moveDistance, boolean cancelUp) {
        int formatDistance = Math.round(moveDistance * mRefreshMoveFactor);
        if (formatDistance == 0 && mRefreshState == OnRefreshListener.STATE_IDLE) {
            return;
        }
        boolean willOptHeader = formatDistance == 0 ? (mScrollState % 2 == 1) : formatDistance > 0;
        if (willOptHeader != isOptHeader) {
            if (interceptRefreshDirectionChanged(willOptHeader, moveDistance, cancelUp)) {
                if (cancelUp) {
                    formatDistance = 0;
                } else {
                    return;
                }
            }
        }
        if (formatDistance < 0) {
            formatDistance = -formatDistance;
        }
        updateRefreshDistance(formatDistance, true);
        if (cancelUp && mRefreshState != OnRefreshListener.STATE_IDLE) {
            View optView = isOptHeader ? mRefreshHeader : mRefreshFooter;
            int criticalDistance = optView == null ? formatDistance : optView.getHeight();
            isRefreshing = formatDistance > criticalDistance;
            if (isRefreshing && mRefreshListener != null) {
                animateRefresh(formatDistance, criticalDistance, -1);
                notifyRefresh(isOptHeader);
            } else {
                isRefreshing = false;
                animateRefresh(formatDistance, 0, -1);
            }
        }
    }

    private void updateRefreshDistance(int refreshAbsDistance, boolean fromUserTouch) {
        int oldState = mRefreshState, newState = OnRefreshListener.STATE_IDLE;
        int viewHeight, criticalDistance, finalPosition = refreshAbsDistance;
        if (refreshAbsDistance > 0) {
            if (isOptHeader) {
                finalPosition = Math.min(finalPosition, getMaxPullDistance());
                criticalDistance = getPullReadyDistance();
                viewHeight = mRefreshHeader == null ? 0 : mRefreshHeader.getHeight();
            } else {
                finalPosition = Math.min(finalPosition, getMaxPushDistance());
                criticalDistance = getPushReadyDistance();
                viewHeight = mRefreshFooter == null ? 0 : mRefreshFooter.getHeight();
            }
            mPointDown.y += (isOptHeader ? (refreshAbsDistance - finalPosition) : (finalPosition - refreshAbsDistance));
            newState = calculateRefreshState(finalPosition, viewHeight, criticalDistance, isOptHeader);
        }
        if (oldState != newState) {
            updateRefreshState(newState, refreshAbsDistance);
        }
        if (mLastRefreshDistance != finalPosition) {
            updateRefreshView(finalPosition, isOptHeader);
            notify(mScrollState, mScrollState, finalPosition);
        }
    }

    private void updateRefreshView(int absOffset, boolean refreshHeader) {
        int offset = refreshHeader ? (absOffset - mLastRefreshDistance) : (mLastRefreshDistance - absOffset);
        mLastRefreshDistance = absOffset;
        if (mRefreshOffsetWay) {
            mContentView.offsetTopAndBottom(offset);
            if (mMaskView != null && isMaskContent && mMaskView.getVisibility() == View.VISIBLE) {
                mMaskView.offsetTopAndBottom(offset);
            }
            if (refreshHeader) {
                if (mFooterView != null && !isFooterViewFloat) {
                    mFooterView.offsetTopAndBottom(offset);
                }
            } else {
                if (mHeaderView != null && !isHeaderViewFloat) {
                    mHeaderView.offsetTopAndBottom(offset);
                }
            }
        } else {
            offset = refreshHeader ? absOffset : -absOffset;
            mContentView.setTranslationY(offset);
            if (mMaskView != null && isMaskContent && mMaskView.getVisibility() == View.VISIBLE) {
                mMaskView.setTranslationY(offset);
            }
            if (refreshHeader) {
                if (mFooterView != null && !isFooterViewFloat) {
                    mFooterView.setTranslationY(offset);
                }
            } else {
                if (mHeaderView != null && !isHeaderViewFloat) {
                    mHeaderView.setTranslationY(offset);
                }
            }
        }
    }

    protected void notify(int state, int oldState, int absDistance) {
        if (mRefreshListener != null) {
            mRefreshListener.onRefreshStateChanged(NestRefreshLayout.this, state, oldState, absDistance);
        }
        View optView = isOptHeader ? mRefreshHeader : mRefreshFooter;
        if (optView != null && (optView instanceof OnRefreshListener)) {
            ((OnRefreshListener) optView).onRefreshStateChanged(NestRefreshLayout.this, state, oldState, absDistance);
        }
    }

    protected void notifyRefresh(boolean isOptHeader) {
        mRefreshListener.onRefresh(NestRefreshLayout.this, isOptHeader);
        View optView = isOptHeader ? mRefreshHeader : mRefreshFooter;
        if (optView != null && (optView instanceof OnRefreshListener)) {
            ((OnRefreshListener) optView).onRefresh(NestRefreshLayout.this, isOptHeader);
        }
    }

    public interface OnRefreshListener {
        int STATE_IDLE = 0;
        int STATE_PULL_TO_READY = 1;
        int STATE_PULL_READY = 3;
        int STATE_PULL_BEYOND_READY = 5;

        int STATE_PUSH_TO_READY = 2;
        int STATE_PUSH_READY = 4;
        int STATE_PUSH_BEYOND_READY = 6;

        void onRefreshStateChanged(NestRefreshLayout parent, int state, int preState, int moveAbsDistance);

        void onRefresh(NestRefreshLayout parent, boolean refresh);
    }

    class RefreshAnimation extends Animation implements Animation.AnimationListener {
        private int mValueFrom = 0, mValueTo = 0;

        @Override
        public void applyTransformation(float t, Transformation trans) {
            if (t < 1) {
                int current = mValueFrom + (int) ((mValueTo - mValueFrom) * t);
                updateRefreshDistance(current, false);
            }
        }

        void reset(int from, int to, boolean listener, int duration) {
            reset();
            mValueFrom = from;
            mValueTo = to;
            setDuration(duration);
            setAnimationListener(listener ? this : null);
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            updateRefreshDistance(mValueTo, false);
            setAnimationListener(null);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }


    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean acceptedVertical = isRefreshNestEnable && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        if (acceptedVertical) {
            acceptedVertical = isRefreshPullEnable() || isRefreshPushEnable();
        }
        return acceptedVertical;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mNestInitDistance = 0;
    }

    @Override
    public void onStopNestedScroll(View target) {
        if (mNestInitDistance != 0 && mRefreshState != OnRefreshListener.STATE_IDLE) {
            View optView = isOptHeader ? mRefreshHeader : mRefreshFooter;
            int criticalDistance = optView == null ? mNestInitDistance : optView.getHeight();
            isRefreshing = mNestInitDistance > criticalDistance;
            if (isRefreshing && mRefreshListener != null) {
                animateRefresh(mNestInitDistance, criticalDistance, -1);
                notifyRefresh(isOptHeader);
            } else {
                isRefreshing = false;
                animateRefresh(mNestInitDistance, 0, -1);
            }
        }
    }

    private int mNestInitDistance = 0;

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyUnconsumed == 0) {
            if (mNestInitDistance != 0) {
                mNestInitDistance = 0;
                setRefreshComplete(false);
            }
        } else {
            if (mNestInitDistance == 0) {
                int refreshState = mRefreshState;
                if (dyUnconsumed > 0 && mRefreshFooter != null && !canScrollToChildBottom(getScrollAbleView())) {
                    refreshState = OnRefreshListener.STATE_PUSH_TO_READY;
                    isOptHeader = false;
                    mNestInitDistance = dyUnconsumed;
                }
                if (dyUnconsumed < 0 && mRefreshHeader != null && !canScrollToChildTop(getScrollAbleView())) {
                    refreshState = OnRefreshListener.STATE_PULL_TO_READY;
                    isOptHeader = true;
                    mNestInitDistance = -dyUnconsumed;
                }
                updateRefreshState(refreshState, mNestInitDistance);
            } else {
                if (dyUnconsumed > 0) {
                    mNestInitDistance += dyUnconsumed;
                }
                if (dyUnconsumed < 0) {
                    mNestInitDistance -= dyUnconsumed;
                }
            }
            if (mNestInitDistance > 0) {
                updateRefreshDistance(mNestInitDistance, true);
            }
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }

}
