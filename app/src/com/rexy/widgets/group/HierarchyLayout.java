package com.rexy.widgets.group;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.util.Pair;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;


import com.rexy.widgets.tools.PaintUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.Typeface.NORMAL;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.INVALID_POINTER_ID;

public class HierarchyLayout extends WrapLayout {
    private static final int TRACKING_UNKNOWN = 0;
    private static final int TRACKING_VERTICALLY = 1;
    private static final int TRACKING_HORIZONTALLY = -1;
    private static final int ROTATION_MAX = 55;
    private static final int ROTATION_MIN = -ROTATION_MAX;
    private static final int ROTATION_DEFAULT_X = 6;
    private static final int ROTATION_DEFAULT_Y = -12;
    private static final float ZOOM_DEFAULT = 0.75f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 1.5f;
    private static final int SPACING_DEFAULT = 25;
    private static final int SPACING_MIN = 10;
    private static final int SPACING_MAX = 100;

    Rect mLayoutBounds = new Rect();
    PointF mTempPointF = new PointF();
    Resources mResources;
    float mSlop, mDensity;


    private final RectF mOptionRect = new RectF();
    private final Rect mViewBounds = new Rect();
    private final Paint mViewBorderPaint = new Paint(ANTI_ALIAS_FLAG);
    private final Camera mCamera = new Camera();
    private final Matrix mMatrix = new Matrix();

    private final SparseArray<String> mIdNameArr = new SparseArray<>();

    private float mViewTextOffset = 1;
    private boolean mHierarchyViewEnable = true;
    private boolean mHierarchyNodeEnable = false;
    private boolean mHierarchySummaryEnable = true;
    private boolean mDrawViewEnable = true;

    private boolean mDrawViewIdEnable = true;
    private int mPointerOne = INVALID_POINTER_ID;
    private PointF mLastPointOne = new PointF();
    private int mPointerTwo = INVALID_POINTER_ID;
    private PointF mLastPointTwo = new PointF();

    private PointF mPointDown = new PointF();

    private int mMultiTouchTracking = TRACKING_UNKNOWN;
    private float mRotationY = ROTATION_DEFAULT_Y;
    private float mRotationX = ROTATION_DEFAULT_X;
    private float mZoom = ZOOM_DEFAULT;

    private float mSpacing = SPACING_DEFAULT;
    private int mViewColor = 0xFF888888;


    private int mViewShadowColor = 0xFF000000;
    private int mHierarchyColor = 0xAA000000;
    private int mTreeNodeColor = 0xFF0000f2;
    private int mTreeLeafColor = 0xFFFC7946;
    private int mTreeBranchColor = 0xAAFF56FF;
    private int mTreeBackground = 0;
    private int mTreeTextSize = 4;
    private int mTreeTextColor = 0xFFFF0000;
    private int mTreeSumTextSize = 15;

    private int mTreeSumTextColor = 0xFFAA2A20;
    private int mMaxTreeLeafSize = -1;

    private float mTreeWidthWeight = 0.95f;
    private float mTreeHeightWeight = 0.85f;
    private float mTreeLeafMarginWeight = 1f;
    private float mTreeLevelMarginWeight = 3.5f;
    private float mTreeOffsetX = 0;
    private float mTreeOffsetY = 10;

    float mLeafSize;
    float mLeafMargin;
    float mLevelMargin;
    boolean mHierarchyTreeHorizontal;
    ViewHierarchyTree mTree;
    StringBuilder mStringBuilder = new StringBuilder();
    Paint mTreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HierarchyLayout(Context context) {
        super(context);
        init(context, null);
    }

    public HierarchyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HierarchyLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mResources = context.getResources();
        mDensity = context.getResources().getDisplayMetrics().density;
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTreePaint.setStyle(Paint.Style.FILL);
        mTreePaint.setTextAlign(Paint.Align.CENTER);
        mTreeOffsetX *= mDensity;
        mTreeOffsetY *= mDensity;

        mViewTextOffset *= mDensity;
        mViewBorderPaint.setStyle(STROKE);
        mViewBorderPaint.setTextSize(6 * mDensity);
        if (Build.VERSION.SDK_INT >= JELLY_BEAN) {
            mViewBorderPaint.setTypeface(Typeface.create("sans-serif-condensed", NORMAL));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mHierarchyViewEnable) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                requestDisallowInterceptTouchEvent(true);
            }
        } else {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mPointDown.set(ev.getX(), ev.getY());
            }
            if (action == MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) {
                handleClickUp(ev.getX(), ev.getY());
            }
        }
        return mHierarchyViewEnable || super.onInterceptTouchEvent(ev);
    }

    private void handleClickUp(float endX, float endY) {
        float x = mPointDown.x, y = mPointDown.y;
        if (Math.abs(x - endX) < mSlop && Math.abs(y - endY) < mSlop) {
            if (x >= mOptionRect.left - mSlop && x <= mOptionRect.right + mSlop && y >= mOptionRect.top - mSlop && y <= mOptionRect.bottom + mSlop) {
                if (x > mOptionRect.centerX()) {
                    mHierarchyViewEnable = !mHierarchyViewEnable;
                } else {
                    mHierarchyNodeEnable = !mHierarchyNodeEnable;
                }
                invalidate();
            }
        }
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
        int action = event.getActionMasked();
        if (!mHierarchyViewEnable) {
            if (action == MotionEvent.ACTION_DOWN) {
                mPointDown.set(event.getX(), event.getY());
            }
            if (action == MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) {
                handleClickUp(event.getX(), event.getY());
            }
            return super.onTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPointDown.set(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = (action == ACTION_DOWN) ? 0 : event.getActionIndex();
                if (mPointerOne == INVALID_POINTER_ID) {
                    mPointerOne = event.getPointerId(index);
                    mLastPointOne.set(event.getX(index), event.getY(index));
                } else if (mPointerTwo == INVALID_POINTER_ID) {
                    mPointerTwo = event.getPointerId(index);
                    mLastPointTwo.set(event.getX(index), event.getY(index));
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mPointerTwo == INVALID_POINTER_ID) {
                    // Single pointer controlling 3D rotation.
                    for (int i = 0, count = event.getPointerCount(); i < count; i++) {
                        if (mPointerOne == event.getPointerId(i)) {
                            float eventX = event.getX(i);
                            float eventY = event.getY(i);
                            float drx = 90 * ((eventX - mLastPointOne.x) / getWidth());
                            float dry = 90 * (-(eventY - mLastPointOne.y) / getHeight()); // Invert Y-axis.
                            // An 'x' delta affects 'y' rotation and vise versa.
                            if (drx != 0 || dry != 0) {
                                mRotationY = Math.min(Math.max(mRotationY + drx, ROTATION_MIN), ROTATION_MAX);
                                mRotationX = Math.min(Math.max(mRotationX + dry, ROTATION_MIN), ROTATION_MAX);
                                mLastPointOne.set(eventX, eventY);
                                invalidate();
                            }
                        }
                    }
                } else {
                    int pointerOneIndex = event.findPointerIndex(mPointerOne);
                    int pointerTwoIndex = event.findPointerIndex(mPointerTwo);
                    float xOne = event.getX(pointerOneIndex);
                    float yOne = event.getY(pointerOneIndex);
                    float xTwo = event.getX(pointerTwoIndex);
                    float yTwo = event.getY(pointerTwoIndex);
                    float dxOne = xOne - mLastPointOne.x;
                    float dyOne = yOne - mLastPointOne.y;
                    float dxTwo = xTwo - mLastPointTwo.x;
                    float dyTwo = yTwo - mLastPointTwo.y;
                    if (mMultiTouchTracking == TRACKING_UNKNOWN) {
                        float adx = Math.abs(dxOne) + Math.abs(dxTwo);
                        float ady = Math.abs(dyOne) + Math.abs(dyTwo);
                        if (adx > mSlop * 2 || ady > mSlop * 2) {
                            if (adx > ady) {
                                // Left/right movement wins. Track horizontal.
                                mMultiTouchTracking = TRACKING_HORIZONTALLY;
                            } else {
                                // Up/down movement wins. Track vertical.
                                mMultiTouchTracking = TRACKING_VERTICALLY;
                            }
                        }
                    }
                    if (mMultiTouchTracking != TRACKING_UNKNOWN) {
                        if (dyOne != dyTwo) {
                            if (mMultiTouchTracking == TRACKING_VERTICALLY) {
                                if (yOne >= yTwo) {
                                    mZoom += dyOne / getHeight() - dyTwo / getHeight();
                                } else {
                                    mZoom += dyTwo / getHeight() - dyOne / getHeight();
                                }
                                mZoom = Math.min(Math.max(mZoom, ZOOM_MIN), ZOOM_MAX);

                            }
                            if (mMultiTouchTracking == TRACKING_HORIZONTALLY) {
                                if (xOne >= xTwo) {
                                    mSpacing += (dxOne / getWidth() * SPACING_MAX) - (dxTwo / getWidth() * SPACING_MAX);
                                } else {
                                    mSpacing += (dxTwo / getWidth() * SPACING_MAX) - (dxOne / getWidth() * SPACING_MAX);
                                }
                                mSpacing = Math.min(Math.max(mSpacing, SPACING_MIN), SPACING_MAX);
                            }
                            invalidate();
                        }
                        mLastPointOne.set(xOne, yOne);
                        mLastPointTwo.set(xTwo, yTwo);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleClickUp(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_UP: {
                int index = (action != ACTION_POINTER_UP) ? 0 : event.getActionIndex();
                int pointerId = event.getPointerId(index);
                if (mPointerOne == pointerId) {
                    // Shift pointer two (real or invalid) up to pointer one.
                    mPointerOne = mPointerTwo;
                    mLastPointOne.set(mLastPointTwo);
                    // Clear pointer two and tracking.
                    mPointerTwo = INVALID_POINTER_ID;
                    mMultiTouchTracking = TRACKING_UNKNOWN;
                } else if (mPointerTwo == pointerId) {
                    mPointerTwo = INVALID_POINTER_ID;
                    mMultiTouchTracking = TRACKING_UNKNOWN;
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void doAfterMeasure(int measuredWidth, int measuredHeight, int contentWidth, int contentHeight) {
        super.doAfterMeasure(measuredWidth, measuredHeight, contentWidth, contentHeight);
        mLayoutBounds.set(0, 0, measuredWidth, measuredHeight);
        mOptionRect.setEmpty();
    }

    @Override
    protected void doAfterLayout(boolean firstAttachLayout) {
        super.doAfterLayout(firstAttachLayout);
        if (mTree != null) {
            mTree.destroy();
        }
        mLeafSize = -1;
        mTree = ViewHierarchyTree.create(this);
        int width = mLayoutBounds.width(), height = mLayoutBounds.height();
        mHierarchyTreeHorizontal = width > height;
        int longSize = Math.max(width, height), shortSize = Math.min(width, height);
        calculateHierarchyLayoutRadius(longSize * mTreeWidthWeight, shortSize * mTreeHeightWeight, mHierarchyTreeHorizontal);
    }

    @Override
    protected void dispatchDrawAfter(Canvas canvas) {
        super.dispatchDrawAfter(canvas);
        if (mTree != null && mLeafSize > 0) {
            if ((mHierarchyNodeEnable || mHierarchyViewEnable)) {
                if (mHierarchyColor != 0) {
                    canvas.drawColor(mHierarchyColor);
                }
                if (mHierarchyViewEnable) {
                    drawHierarchyView(canvas);
                }
                if (mHierarchyNodeEnable) {
                    drawHierarchyTree(canvas);
                }
            }
            mTreePaint.setColor(0xAA00FF00);
            canvas.drawRect(0,0,mLayoutBounds.right,mOptionRect.bottom+mDensity*3,mTreePaint);
            canvas.drawRect(mOptionRect, mViewBorderPaint);

            mTreePaint.setColor(mTreeSumTextColor);
            mTreePaint.setTextSize(mTreeSumTextSize * mDensity);
            mTreePaint.setTextAlign(Paint.Align.LEFT);
            if (mHierarchySummaryEnable && mTreeSumTextColor != 0 && mTreeSumTextSize > 0) {
                RectF treeBounds = mTree.getTag();
                drawTreeSummaryInfo(canvas, treeBounds, mHierarchyTreeHorizontal);
            }
            drawOptionBar(canvas);
        }
    }

    private void drawHierarchyView(Canvas canvas) {
        boolean applyChangeVisible = mHierarchyColor != 0 && (255 == (mHierarchyColor >>> 24));
        Rect location = mTree.getWindowLocation();
        int saveCount = canvas.save();
        final float x = location.left, y = location.top;
        final float translateShowX = mSpacing * mDensity * mRotationY / ROTATION_MAX;
        final float translateShowY = mSpacing * mDensity * mRotationX / ROTATION_MAX;
        final float cx = getWidth() / 2f, cy = getHeight() / 2f;
        mCamera.save();
        mCamera.rotate(mRotationX, mRotationY, 0);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-cx, -cy);
        mMatrix.postTranslate(cx, cy);
        canvas.concat(mMatrix);
        canvas.scale(mZoom, mZoom, cx, cy);
        SparseArray<ViewHierarchyInfo> nodes = mTree.getHierarchyNodeArray();
        mViewBorderPaint.setColor(mViewColor);
        mViewBorderPaint.setShadowLayer(0, 1, -1, mViewShadowColor);
        for (int i = 1; i < nodes.size(); i++) {
            ViewHierarchyInfo node = nodes.get(i);
            View view = node.getView();
            int layer = node.getLevel();
            int viewSaveCount = canvas.save();
            float tx = layer * translateShowX, ty = layer * translateShowY;
            location = node.getWindowLocation();
            canvas.translate(tx, -ty);
            canvas.translate(location.left - x, location.top - y);
            mViewBounds.set(0, 0, view.getWidth(), view.getHeight());
            canvas.drawRect(mViewBounds, mViewBorderPaint);
            if (mDrawViewEnable) {
                if (applyChangeVisible) {
                    changeChildVisible(view, true);
                    view.draw(canvas);
                    changeChildVisible(view, false);
                } else {
                    boolean viewGroupType = view instanceof ViewGroup;
                    if (viewGroupType) {
                        if (view.getBackground() != null) {
                            view.getBackground().draw(canvas);
                        }
                    } else {
                        view.draw(canvas);
                    }
                }
            }
            if (mDrawViewIdEnable) {
                int id = view.getId();
                if (id != NO_ID) {
                    canvas.drawText(nameForId(id), mViewTextOffset, mViewBorderPaint.getTextSize(), mViewBorderPaint);
                }
            }
            canvas.restoreToCount(viewSaveCount);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawOptionBar(Canvas canvas) {
        float textHeight = PaintUtils.getTextHeight(mTreePaint);
        if (mOptionRect.isEmpty()) {
            mOptionRect.set(mLayoutBounds);
            mOptionRect.bottom = mOptionRect.top + textHeight;
            mOptionRect.left = mOptionRect.right - textHeight * 5;
            textHeight = mDensity * 3;
            mOptionRect.offset(-textHeight, textHeight);
            postInvalidate();
        }
        float midX = mOptionRect.centerX();
        canvas.drawLine(midX, mOptionRect.top, midX, mOptionRect.bottom, mViewBorderPaint);
        canvas.drawText("NODE", mOptionRect.left, textHeight, mTreePaint);
        canvas.drawText("VIEW", mOptionRect.right - PaintUtils.getTextBounds(mTreePaint, "VIEW").width() - mDensity, textHeight, mTreePaint);

        mTreePaint.setColor(mHierarchyNodeEnable ? 0x880000FF : 0x220000FF);
        canvas.drawRect(mOptionRect.left, mOptionRect.top, midX - mDensity, mOptionRect.bottom, mTreePaint);

        mTreePaint.setColor(mHierarchyViewEnable ? 0x880000FF : 0x220000FF);
        canvas.drawRect(midX + mDensity, mOptionRect.top, mOptionRect.right, mOptionRect.bottom, mTreePaint);
    }

    private void drawTreeSummaryInfo(Canvas canvas, RectF treeBounds, boolean horizontal) {
        mStringBuilder.delete(0, mStringBuilder.length());
        mStringBuilder.append("层级(").append(mTree.getHierarchyCount()).append(',').append(String.format("%.1f", mTree.getArgHierarchyCount())).append(")").append(',');
        mStringBuilder.append("结点(").append(mTree.getCountOfNode()).append(',').append(mTree.getCountOfViewGroup()).append(',').append(mTree.getCountOfView()).append(")").append(',');
        mStringBuilder.append("测绘(").append(mLastMeasureCost).append(',').append(mLastLayoutCost).append(',').append(mLastDrawCost).append(")");
        float textHeight = PaintUtils.getTextHeight(mTreePaint);
        canvas.drawText(mStringBuilder.toString(), textHeight / 2, textHeight, mTreePaint);
    }

    protected void drawTreeNode(Canvas canvas, ViewHierarchyInfo info, float radius) {
        PointF tempPoint = mTempPointF;
        ViewHierarchyInfo parent = info.getParent();
        getNodePosition((RectF) info.getTag(), tempPoint, radius);
        float x = tempPoint.x, y = tempPoint.y;
        if (parent != null && mTreeBranchColor != 0) {
            getNodePosition((RectF) parent.getTag(), tempPoint, radius);
            float px = tempPoint.x, py = tempPoint.y;
            mTreePaint.setColor(mTreeBranchColor);
            canvas.drawLine(x, y, px, py, mTreePaint);
        }
        mTreePaint.setColor(info.isLeaf() ? mTreeLeafColor : mTreeNodeColor);
        canvas.drawCircle(x, y, radius, mTreePaint);

        if (mTreeTextColor != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(info.getMarkName()).append('[').append(info.getLevel()).append(',').append(info.getLevelIndex()).append(']');
            canvas.drawText(sb.toString(), x, y - radius - radius, mTreePaint);
        }
    }

    protected void drawHierarchyTree(Canvas canvas) {
        if (mLeafSize > 0) {
            mTreePaint.setTextAlign(Paint.Align.CENTER);
            RectF treeBounds = mTree.getTag();
            if (mTreeBackground != 0) {
                mTreePaint.setColor(mTreeBackground);
                canvas.drawRect(treeBounds, mTreePaint);
            }
            if (mTreeTextColor != 0) {
                mTreePaint.setTextSize(mTreeTextSize * mDensity);
            }
            float radius = mLeafSize / 2f;
            SparseArray<ViewHierarchyInfo> infoArr = mTree.getHierarchyNodeArray();
            int size = infoArr.size();
            for (int i = size - 1; i >= 0; i--) {
                drawTreeNode(canvas, infoArr.get(i), radius);
            }
        }
    }

    private final SparseArray<View> mTempInt = new SparseArray();

    private void changeChildVisible(View view, boolean hide) {
        if (view instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) view;
            int size = p.getChildCount();
            if (size > 0) {
                if (hide) {
                    mTempInt.clear();
                    for (int i = 0; i < size; i++) {
                        View child = p.getChildAt(i);
                        if (child.getVisibility() == View.VISIBLE) {
                            mTempInt.put(i, child);
                            child.setVisibility(View.INVISIBLE);
                        }
                    }
                } else {
                    size = mTempInt.size();
                    for (int i = 0; i < size; i++) {
                        mTempInt.valueAt(i).setVisibility(View.VISIBLE);
                    }
                    mTempInt.clear();
                }
            }
        }
    }

    private void calculateHierarchyLayoutRadius(float longSize, float shortSize, boolean horizontal) {
        int leafCount = mTree.getLeafCount();
        int levelCount = mTree.getHierarchyCount();
        if (leafCount > 0 && levelCount > 0) {
            //leafCount*leafSize+(leafCount-1)*(leafSize*mLeafMarginHorizontalFactor)=w;
            mLeafSize = (longSize / (leafCount + (leafCount - 1) * mTreeLeafMarginWeight));
            if (mMaxTreeLeafSize > 0 && mLeafSize > mMaxTreeLeafSize) {
                mLeafSize = mMaxTreeLeafSize;
            }
            mLeafMargin = (mTreeLeafMarginWeight * mLeafSize);
            mLevelMargin = (mTreeLevelMarginWeight * mLeafSize);
            //leafLevel*leafSize+(leafCount-1)*maxMarginVertical=h;
            if (levelCount > 0) {
                mLevelMargin = Math.min(mLevelMargin, (shortSize - levelCount * mLeafSize) / (levelCount - 1));
            }
            float hierarchyWidth = leafCount * mLeafSize + (leafCount - 1) * mLeafMargin;
            float hierarchyHeight = levelCount * mLeafSize + (levelCount - 1) * mLevelMargin;
            calculateHierarchyLayoutPosition(mLayoutBounds, hierarchyWidth, hierarchyHeight, horizontal);
        }
    }

    private void calculateHierarchyLayoutPosition(Rect canvasBounds, float hierarchyWidth, float hierarchyHeight, boolean horizontal) {
        RectF rootBounds = new RectF();
        if (horizontal) {
            rootBounds.left = (int) (canvasBounds.left + (1 - mTreeWidthWeight) * canvasBounds.width() / 2);
            rootBounds.top = (int) (canvasBounds.top + (1 - mTreeHeightWeight) * canvasBounds.height() / 2);
            rootBounds.right = rootBounds.left + hierarchyWidth;
            rootBounds.bottom = rootBounds.top + hierarchyHeight;
        } else {
            rootBounds.left = (int) (canvasBounds.left + (1 - mTreeHeightWeight) * canvasBounds.width() / 2);
            rootBounds.top = (int) (canvasBounds.top + (1 - mTreeWidthWeight) * canvasBounds.height() / 2);
            rootBounds.right = rootBounds.left + hierarchyHeight;
            rootBounds.bottom = rootBounds.top + hierarchyWidth;
        }
        rootBounds.offset(mTreeOffsetX, mTreeOffsetY);
        SparseIntArray lines = mTree.getHierarchyCountArray();
        SparseArray<ViewHierarchyInfo> list = mTree.getHierarchyNodeArray();

        int lineCount = lines.size(), startIndex, endIndex = 0;
        float levelMargin = mLevelMargin + mLeafSize, usedWeight = 0;
        list.get(endIndex++).setTag(rootBounds);
        ViewHierarchyInfo prevParent = null, parent, child;
        for (int line = 1; line < lineCount; line++) {
            startIndex = endIndex;
            endIndex = startIndex + lines.get(line);
            for (int i = startIndex; i < endIndex; i++) {
                child = list.get(i);
                parent = child.getParent();
                if (parent != prevParent) {
                    usedWeight = 0;
                    prevParent = parent;
                }
                usedWeight += buildAndSetHierarchyBounds(child, parent, usedWeight, levelMargin, horizontal);
            }
        }
        invalidate();
    }

    private int buildAndSetHierarchyBounds(ViewHierarchyInfo child, ViewHierarchyInfo parent, float usedWeight, float levelMargin, boolean horizontal) {
        RectF bounds = new RectF((RectF) parent.getTag());
        int currentWeight = child.getLeafCount();
        float weightSum = parent.getLeafCount(), weightStart = usedWeight, weightEnd = usedWeight + currentWeight;
        float start, end, size;
        if (horizontal) {
            size = bounds.width();
            start = bounds.left + size * weightStart / weightSum;
            end = bounds.left + size * weightEnd / weightSum;
            bounds.left = start;
            bounds.right = end;
            bounds.top = bounds.top + levelMargin;
        } else {
            size = bounds.height();
            start = bounds.bottom - size * weightStart / weightSum;
            end = bounds.bottom - size * weightEnd / weightSum;
            bounds.top = end;
            bounds.bottom = start;
            bounds.left = bounds.left + levelMargin;
        }
        child.setTag(bounds);
        return currentWeight;
    }

    private void getNodePosition(RectF rect, PointF point, float radius) {
        if (mHierarchyTreeHorizontal) {
            point.set(rect.centerX(), rect.top + radius);
        } else {
            point.set(rect.left + radius, rect.centerY());
        }
    }

    private String nameForId(int id) {
        String name = mIdNameArr.get(id);
        if (name == null) {
            try {
                name = mResources.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                name = String.format("0x%8x", id);
            }
            mIdNameArr.put(id, name);
        }
        return name;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTree != null) {
            mTree.destroy();
            mTree = null;
        }
        mIdNameArr.clear();
    }

    static class ViewHierarchyInfo {
        private static int MAX_POOL_SIZE = 500;
        private static final Pools.SimplePool<ViewHierarchyInfo> mPool = new Pools.SimplePool(MAX_POOL_SIZE);
        private static int[] LOCATION = new int[]{0, 0};
        private int mLevel = -1;
        private int mLevelIndex = -1;
        private int mParentIndex = -1;
        private int mLeafCount = 0;
        private View mView;
        private Object mTag;
        private String name;
        private Rect mWindowLocation = new Rect();
        private ViewHierarchyInfo mParent;
        private LinkedList<ViewHierarchyInfo> mChildArr;

        public static ViewHierarchyInfo obtain(View view, int index, int levelIndex, ViewHierarchyInfo parent) {
            ViewHierarchyInfo data = mPool.acquire();
            if (data == null) {
                data = new ViewHierarchyInfo(view, index, levelIndex, parent);
            } else {
                data.analyzeHierarchyInfo(view, index, levelIndex, parent);
            }
            return data;
        }

        protected ViewHierarchyInfo(View view, int index, int levelIndex, ViewHierarchyInfo parent) {
            analyzeHierarchyInfo(view, index, levelIndex, parent);
        }

        private void analyzeHierarchyInfo(View view, int index, int levelIndex, ViewHierarchyInfo parent) {
            this.mView = view;
            this.name = view.getClass().getName();
            mWindowLocation.set(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
            mView.getLocationInWindow(LOCATION);
            mWindowLocation.offset(LOCATION[0], LOCATION[1]);
            if (parent == null) {
                this.mLevel = 0;
            } else {
                this.mLevel = parent.mLevel + 1;
                this.mParent = parent;
                parent.mChildArr.add(this);
            }
            this.mParentIndex = index;
            this.mLevelIndex = levelIndex;
            if (view instanceof ViewGroup) {
                mChildArr = new LinkedList();
                if (index == -1) {
                    if (view.getParent() instanceof ViewGroup) {
                        this.mParentIndex = ((ViewGroup) view.getParent()).indexOfChild(view);
                    } else {
                        this.mParentIndex = 0;
                    }
                }
            }
            computeWeightIfNeed(view, parent);
        }

        private void computeWeightIfNeed(View view, ViewHierarchyInfo parent) {
            boolean calculateWeight = !(view instanceof ViewGroup);
            if (!calculateWeight) {
                ViewGroup p = (ViewGroup) view;
                int count = p.getChildCount();
                calculateWeight = true;
                for (int i = 0; i < count; i++) {
                    if (View.GONE != p.getChildAt(i).getVisibility()) {
                        calculateWeight = false;
                        break;
                    }
                }
            }
            if (calculateWeight) {
                mLeafCount = 1;
                while (parent != null) {
                    parent.mLeafCount = parent.mLeafCount + 1;
                    parent = parent.mParent;
                }
            }
        }

        public void setTag(Object tag) {
            this.mTag = tag;
        }

        public <CAST extends Object> CAST getTag() {
            return (CAST) mTag;
        }

        public boolean isLeaf() {
            return mChildArr == null;
        }

        public boolean isRoot() {
            return mParent == null;
        }

        public String getMarkName() {
            String name = getSimpleName();
            if (name != null && name.length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < name.length(); i++) {
                    char c = name.charAt(i);
                    if (c >= 'A' && c <= 'Z') {
                        sb.append(c);
                    }
                }
                if (sb.length() == 0) {
                    sb.append(name);
                }
                return sb.toString();
            }
            return name;
        }

        public String getName() {
            return name;
        }

        public String getSimpleName() {
            String result = name;
            int point = result == null ? -1 : 0;
            if (point == 0) {
                point = result.lastIndexOf('$');
                if (point == -1) {
                    point = result.lastIndexOf('.');
                }
            }
            if (point > 0 && point < result.length()) {
                result = result.substring(point + 1);
            }
            return result;
        }

        public View getView() {
            return mView;
        }

        public Rect getWindowLocation() {
            return mWindowLocation;
        }

        public int getLevel() {
            return mLevel;
        }

        public int getLevelIndex() {
            return mLevelIndex;
        }

        public int getLeafCount() {
            return mLeafCount;
        }

        public int getLayoutIndex() {
            return mParentIndex;
        }

        public int getParentIndex() {
            int result = mParentIndex;
            if (mParent != null && mParent.mChildArr != null) {
                result = mParent.mChildArr.indexOf(this);
            }
            return result;
        }

        public ViewHierarchyInfo getParent() {
            return mParent;
        }

        public List<? extends ViewHierarchyInfo> getChildArr() {
            return mChildArr;
        }

        public int getChildCount() {
            if (mChildArr != null) {
                return mChildArr.size();
            }
            return 0;
        }

        private void recycle() {
            destroy(true);
            mLevel = -1;
            mLevelIndex = -1;
            mParentIndex = -1;
            mLeafCount = 0;
            mPool.release(this);
        }

        protected void destroy(boolean recycle) {
            mView = null;
            name = null;
            mWindowLocation.setEmpty();
            mTag = null;
            mParent = null;
            if (mChildArr != null) {
                Iterator<ViewHierarchyInfo> its = mChildArr.iterator();
                while (its.hasNext()) {
                    if (recycle) {
                        its.next().recycle();
                    } else {
                        its.next().destroy(recycle);
                    }
                    its.remove();
                }
                mChildArr = null;
            }
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(boolean fullInfo) {
            StringBuilder sb = new StringBuilder();
            if (isRoot()) {
                sb.append("[root ");
            } else if (isLeaf()) {
                sb.append("[leaf ");
            } else {
                sb.append("[node ");
            }
            sb.append(getLevel()).append(',').append(getLevelIndex()).append(']');
            sb.append(' ').append(fullInfo ? getSimpleName() : getMarkName()).append('{');
            sb.append("index=").append(getParentIndex()).append(',');
            sb.append("location=").append(getWindowLocation()).append(',');
            sb.append("count=").append(getChildCount()).append(',');
            sb.append("leaf=").append(getLeafCount());
            return sb.append('}').toString();
        }
    }

    static class ViewHierarchyTree extends ViewHierarchyInfo {
        private SparseIntArray mHierarchyCountArray = new SparseIntArray();
        private SparseArray<ViewHierarchyInfo> mHierarchyNodeArray = new SparseArray();

        public static ViewHierarchyTree create(View root) {
            return new ViewHierarchyTree(root, -1, 0, null);
        }

        protected static ViewHierarchyTree create(View root, int index, int levelIndex, ViewHierarchyInfo parent) {
            return new ViewHierarchyTree(root, index, levelIndex, parent);
        }

        private ViewHierarchyTree(View root, int index, int levelIndex, ViewHierarchyInfo parent) {
            super(root, index, levelIndex, parent);
            mHierarchyCountArray.put(getLevel(), 1);
            mHierarchyNodeArray.put(0, this);
            if (getChildArr() != null) {
                analyzeViewHierarchy(Pair.create(this, (ViewGroup) root));
            }
        }

        private void analyzeViewHierarchy(Pair<? extends ViewHierarchyInfo, ViewGroup> root) {
            Queue<Pair<? extends ViewHierarchyInfo, ViewGroup>> queue = new LinkedList();
            queue.offer(root);
            int arrayIndex = mHierarchyNodeArray.size();
            int levelIndex = 0, level = 0;
            Pair<? extends ViewHierarchyInfo, ViewGroup> pair;
            while ((pair = queue.poll()) != null) {
                ViewHierarchyInfo parent = pair.first;
                ViewGroup layout = pair.second;
                int size = layout.getChildCount();
                for (int i = 0; i < size; i++) {
                    View child = layout.getChildAt(i);
                    if (child.getVisibility() != View.GONE) {
                        int curLevel = parent == null ? 0 : (parent.mLevel + 1);
                        if (curLevel != level) {
                            level = curLevel;
                            levelIndex = 0;
                        }
                        ViewHierarchyInfo node = ViewHierarchyInfo.obtain(child, i, levelIndex++, parent);
                        mHierarchyCountArray.put(node.mLevel, mHierarchyCountArray.get(curLevel, 0) + 1);
                        mHierarchyNodeArray.put(arrayIndex++, node);
                        if (node.mChildArr != null) {
                            queue.offer(Pair.create(node, (ViewGroup) child));
                        }
                    }
                }
            }
        }

        public ViewHierarchyInfo getViewHierarchyInfo(int level, int levelIndex) {
            int sum = levelIndex;
            for (int i = 0; i < level; i++) {
                sum += mHierarchyCountArray.get(i, 0);
            }
            if (sum >= 0 && sum < mHierarchyNodeArray.size()) {
                return mHierarchyNodeArray.get(sum);
            }
            return null;
        }

        public int getCountOfViewGroup() {
            return getCountOfNode() - getCountOfView();
        }

        public int getCountOfView() {
            return getLeafCount();
        }

        public int getCountOfNode() {
            return mHierarchyNodeArray.size();
        }

        public int getCountOfNode(int level) {
            return mHierarchyCountArray.get(level);
        }

        public int getCountOfView(int level) {
            int start = 0, leafCount = 0;
            while (start < level) {
                start += mHierarchyCountArray.get(start);
            }
            for (int end = start + getCountOfNode(level); start < end; start++) {
                if (mHierarchyNodeArray.get(start).isLeaf()) {
                    leafCount++;
                }
            }
            return leafCount;
        }

        public int getCountOfViewGroup(int level) {
            return getCountOfNode(level) - getCountOfView(level);
        }

        public int getHierarchyCount() {
            return mHierarchyCountArray.size();
        }

        public float getArgHierarchyCount() {
            final float sum = getCountOfNode();
            float result = 0;
            final int hierarchyCount = getHierarchyCount();
            for (int i = 0; i < hierarchyCount; i++) {
                result += (getCountOfNode(i) * (i + 1) / sum);
            }
            return result;
        }

        public SparseIntArray getHierarchyCountArray() {
            return mHierarchyCountArray;
        }

        public SparseArray<ViewHierarchyInfo> getHierarchyNodeArray() {
            return mHierarchyNodeArray;
        }

        public StringBuilder dumpNodeWeight(StringBuilder sb) {
            sb = sb == null ? new StringBuilder() : sb;
            int size = mHierarchyCountArray.size();
            int weight = 0;
            for (int i = 0; i < size; i++) {
                int value = mHierarchyCountArray.get(i);
                weight += (value * (i + 1));
                sb.append(" | ").append(value);
            }
            sb.insert(0, weight);
            return sb;
        }

        public void destroy() {
            super.destroy(true);
            mHierarchyCountArray.clear();
            mHierarchyNodeArray.clear();
        }
    }
}