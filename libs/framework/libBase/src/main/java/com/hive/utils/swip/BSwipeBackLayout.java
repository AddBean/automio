// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.swip;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ScrollView;


import com.hive.base.R;
import com.hive.utils.debug.DLog;
import com.hive.utils.system.UIUtils;

import java.util.ArrayList;
import java.util.List;


public final class BSwipeBackLayout extends FrameLayout {

    //Minimum velocity that will be detected as a fling
    private static final int MIN_FLING_VELOCITY = 400; // dips per second
    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private static final int FULL_ALPHA = 255;
    //Default threshold of scroll
    private static final float DEFAULT_SCROLL_THRESHOLD = 0.3f;
    private static final int OVERSCROLL_DISTANCE = 10;
    Drawable mShadowLeft;
    /**
     * Threshold of scroll, we will close the activity, when scrollPercent over
     * this value;
     */
    private float mScrollThreshold = DEFAULT_SCROLL_THRESHOLD;
    private Activity mActivity;
    private boolean mEnable = true;
    private boolean mDisallowIntercept = false;
    private View mContentView;
    private BViewDragHelper mDragHelper;
    private float mScrollPercent;
    private int mContentLeft;
    private int mContentTop;
    //The set of listeners to be sent events through.
    private List<BSwipeListener> mListeners;
    private float mScrimOpacity;
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private boolean mInLayout;
    private Rect mTmpRect = new Rect();
    //Edge being dragged
    private int mTrackingEdge;

    private int mEdgeFlag;
    private InputMethodManager mInputMethodManager;
    private BSwipeBackPage.ISwipe mSwipeViewPager;
    //是否已经调用隐藏输入法，防止重复调用¬
    private boolean mIsHideInputMethod = false;

    public BSwipeBackLayout(Context context) {
        this(context, null);
    }

    public BSwipeBackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BSwipeBackLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mDragHelper = BViewDragHelper.create(this, new ViewDragCallback());

        setShadow(R.drawable.swipe_shadow_left);

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;
        setEdgeSize(UIUtils.dipToPx(context, 20));
        mDragHelper.setMinVelocity(minVel);
        mDragHelper.setMaxVelocity(minVel * 2f);
        mDragHelper.setSensitivity(context, 0.5f);
    }

    /**
     * Sets the sensitivity of the NavigationLayout.
     *
     * @param context     The application context.
     * @param sensitivity value between 0 and 1, the final value for touchSlop =
     *                    ViewConfiguration.getScaledTouchSlop * (1 / s);
     */
    public void setSensitivity(Context context, float sensitivity) {
        mDragHelper.setSensitivity(context, sensitivity);
    }

    public void setEnableGesture(boolean enable) {
        mEnable = enable;
    }

    /**
     * Enable edge tracking for the selected edges of the parent view. The
     * callback's methods will only be invoked for edges for which edge tracking has been
     * enabled.
     *
     * @param edgeFlags Combination of edge flags describing the edges to watch
     */
    public void setEdgeTrackingEnabled(int edgeFlags) {
        mEdgeFlag = edgeFlags;
        mTrackingEdge = edgeFlags;
        mDragHelper.setEdgeTrackingEnabled(mEdgeFlag);
    }

    /**
     * Set a color to use for the scrim that obscures primary content while a
     * drawer is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     */
    public void setScrimColor(int color) {
        mScrimColor = color;
        invalidate();
    }

    public int getScrimColor() {
        return mScrimColor;
    }

    /**
     * Set the size of an edge. This is the range in pixels along the edges of
     * this view that will actively detect edge touches or drags if edge
     * tracking is enabled.
     *
     * @param size The size of an edge in pixels
     */
    public void setEdgeSize(int size) {
//        mTrackingEdge = size;
        mDragHelper.setEdgeSize(size);
    }

    public void setEdgeSizePercent(float size) {
        mDragHelper.setEdgeSize((int) size);
    }

    /**
     * Add a callback to be invoked when a swipe event is sent to this view.
     *
     * @param listener the swipe listener to attach to this view
     */
    public void addSwipeListener(BSwipeListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    /**
     * Removes a listener from the set of listeners
     *
     * @param listener
     */
    public void removeSwipeListener(BSwipeListener listener) {
        if (mListeners == null) {
            return;
        }
        mListeners.remove(listener);
    }

    /**
     * Set scroll threshold, we will close the activity, when scrollPercent over
     * this value
     *
     * @param threshold
     */
    public void setScrollThreshold(float threshold) {
        if (threshold >= 1.0f || threshold <= 0) {
            throw new IllegalArgumentException("Threshold value should be between 0 and 1.0");
        }
        mScrollThreshold = threshold;
    }

    public void setShadow(Drawable shadow) {
        mShadowLeft = shadow;
        invalidate();
    }

    public void setShadow(int resId) {
        setShadow(getResources().getDrawable(resId));
    }

    /**
     * Scroll out contentView and finish the activity
     */
    public void scrollToFinishActivity() {
        final int childWidth = mContentView.getWidth();
        final int childHeight = mContentView.getHeight();

        int left = 0, top = 0;

        if ((mEdgeFlag & BViewDragHelper.EDGE_LEFT) != 0) {
            left = childWidth + mShadowLeft.getIntrinsicWidth() + OVERSCROLL_DISTANCE;
            mTrackingEdge = BViewDragHelper.EDGE_LEFT;
        } else if ((mEdgeFlag & BViewDragHelper.EDGE_TOP) != 0) {
            top = childHeight + OVERSCROLL_DISTANCE;//+ mShadowRight.getIntrinsicWidth()
            mTrackingEdge = BViewDragHelper.EDGE_TOP;
        }

        mDragHelper.smoothSlideViewTo(mContentView, left, top);
        invalidate();
    }

    private void scrollToInitPosition() {
        if (mContentView != null) {
            mContentView.post(new Runnable() {
                @Override
                public void run() {
                    if (mContentView != null) {
                        mDragHelper.smoothSlideViewTo(mContentView, 0, 0);
                    }
                }
            });
        }
    }

    public void setDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
    }

    /**
     * attach view to Activity
     *
     * @param activity
     */
    public void attachToActivity(@NonNull Activity activity) {
        if (getParent() != null) {
            return;
        }

        mActivity = activity;

        Resources.Theme theme = activity.getTheme();
        int background = -1;
        if (theme != null) {
            TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
            background = a.getResourceId(0, -1);
            a.recycle();
        }

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();

        View decorChild = decor.findViewById(android.R.id.content);
        while (decorChild.getParent() != decor) {
            decorChild = (View) decorChild.getParent();
        }

        if (background != -1) {
            decorChild.setBackgroundResource(background);
        }

        decor.removeView(decorChild);

        addView(decorChild);

        setContentView(decorChild);

        setBackgroundColor(Color.TRANSPARENT);

        decor.addView(this);
    }

    /**
     * @param activity
     */
    public void removeFromActivity(@NonNull Activity activity) {
        if (getParent() == null) return;
        ViewGroup decorChild = (ViewGroup) getChildAt(0);
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        decor.removeView(this);
        removeView(decorChild);
        decor.addView(decorChild);
    }

    public void setSwipeViewPager(BSwipeBackPage.ISwipe swipeViewPager) {
        mSwipeViewPager = swipeViewPager;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //定义事件过渡，如果mSwipeViewPager不消费事件时，可以由SwipeView接手motionevent
        if (mSwipeViewPager != null && mSwipeViewPager.shouldDispatchTouchEvent(ev) && mDragHelper != null && mDragHelper.getViewDragState() != BViewDragHelper.STATE_SETTLING) {
            if (mDragHelper.getActivePointerId() == BViewDragHelper.INVALID_POINTER) {
                mDragHelper.recordInitialMotion(ev);
            } else {
                try {
                    mDragHelper.processTouchEvent(ev);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            if (mDragHelper.getViewDragState() == BViewDragHelper.STATE_DRAGGING || mDragHelper.getViewDragState() == BViewDragHelper.STATE_SETTLING) {
                return true;
            }
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mEnable || mDisallowIntercept) {
            return false;
        }

        ensureTarget();

        try {
            if (mSwipeViewPager != null) {
                if (mSwipeViewPager.shouldInterceptTouchEvent(event) && mDragHelper.getViewDragState() != BViewDragHelper.STATE_SETTLING) {
                    return mDragHelper.shouldInterceptTouchEvent(event);
                }
            } else {
                //默认仅仅处理了左右滑动事件
                if ((!isInViewArea(mScrollChild, event.getX(), event.getY()) || !canChildScrollRight()) && mDragHelper.getViewDragState() != BViewDragHelper.STATE_SETTLING) {
                    return mDragHelper.shouldInterceptTouchEvent(event);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnable) {
            return false;
        }
        hideCurrentViewInputMethodView(event);
        try {
            if (mDragHelper.getViewDragState() != BViewDragHelper.STATE_SETTLING) {
                mDragHelper.processTouchEvent(event);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mInLayout = true;
        if (mContentView != null) {
            mContentView.layout(mContentLeft, mContentTop, mContentLeft + mContentView.getMeasuredWidth(), mContentTop + mContentView.getMeasuredHeight());
        }
        mInLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final boolean drawContent = child == mContentView;

        boolean ret = super.drawChild(canvas, child, drawingTime);
        if (mEdgeFlag == BViewDragHelper.EDGE_TOP) {
            //目前只有评论里面使用下滑推送，评论是半屏，不要绘制背景

        } else if (mScrimOpacity > 0 && drawContent && mDragHelper.getViewDragState() != BViewDragHelper.STATE_IDLE) {
            drawShadow(canvas, child);
            drawScrim(canvas, child);
        }
        return ret;
    }

    @Override
    public void computeScroll() {
        mScrimOpacity = 1 - mScrollPercent;
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private final void hideCurrentViewInputMethodView(MotionEvent event) {
        if (event == null || mActivity == null || (mActivity != null && mActivity.isFinishing())) {
            return;
        }
        if (!mIsHideInputMethod && MotionEvent.ACTION_MOVE == MotionEventCompat.getActionMasked(event)) {
            try {
                View view = mActivity.getCurrentFocus();
                if (view != null) {
                    if (mInputMethodManager == null) {
                        mInputMethodManager = (InputMethodManager) mActivity.getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
                    }
                    if (mInputMethodManager.isActive()) {
                        mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        mIsHideInputMethod = true;
                    }
                }
            } catch (Throwable t) {

            }
        }
    }

    /**
     * Set up contentView which will be moved by user gesture
     *
     * @param view
     */
    public void setContentView(View view) {
        mContentView = view;
    }

    private void drawScrim(Canvas canvas, View child) {
        final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
        final int alpha = (int) (baseAlpha * mScrimOpacity);
        final int color = alpha << 24 | (mScrimColor & 0xffffff);
        if ((mTrackingEdge & BViewDragHelper.EDGE_LEFT) != 0) {
            canvas.clipRect(0, 0, child.getLeft(), getHeight());
        } else if ((mTrackingEdge & BViewDragHelper.EDGE_TOP) != 0) {
            canvas.clipRect(0, 0, getRight(), child.getTop());
        }
        canvas.drawColor(color);
    }

    private void drawShadow(Canvas canvas, View child) {
        final Rect childRect = mTmpRect;
        child.getHitRect(childRect);

        if ((mEdgeFlag & BViewDragHelper.EDGE_LEFT) != 0) {
            mShadowLeft.setBounds(childRect.left - mShadowLeft.getIntrinsicWidth(), childRect.top, childRect.left, childRect.bottom);
            mShadowLeft.setAlpha((int) (mScrimOpacity * FULL_ALPHA));
            mShadowLeft.draw(canvas);
        }
    }


    /**
     *
     */
    private View mScrollChild;
    private View mSwipetTarget;

    private void ensureTarget() {
        if (mSwipetTarget == null) {
            if (getChildCount() > 1) {
                if (DLog.isDebug()) {
                    throw new IllegalStateException("SwipeBackLayout must contains only one direct child");
                }
            } else {
                mSwipetTarget = getChildAt(0);
                if (mScrollChild == null && mSwipetTarget != null) {
                    if (mSwipetTarget instanceof ViewGroup) {
                        findScrollView((ViewGroup) mSwipetTarget);
                    } else {
                        mScrollChild = mSwipetTarget;
                    }

                }
            }
        }
    }

    /**
     * Find out the scrollable child view from a ViewGroup.
     *
     * @param viewGroup
     */
    private void findScrollView(ViewGroup viewGroup) {
//        mScrollChild = viewGroup;
        if (viewGroup.getChildCount() > 0) {
            int count = viewGroup.getChildCount();
            View child;
            for (int i = 0; i < count; i++) {
                child = viewGroup.getChildAt(i);
                if (child instanceof AbsListView || child instanceof ScrollView || child instanceof ViewPager || child instanceof WebView) {
                    mScrollChild = child;
                    return;
                }

                if (child instanceof ViewGroup) {
                    findScrollView((ViewGroup) child);
                }
            }
        }
    }

    private boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mScrollChild, -1);
    }

    private boolean canChildScrollDown() {
        return ViewCompat.canScrollVertically(mScrollChild, 1);
    }

    private boolean canChildScrollRight() {
        return ViewCompat.canScrollHorizontally(mScrollChild, -1);
    }

    private boolean canChildScrollLeft() {
        return ViewCompat.canScrollHorizontally(mScrollChild, 1);
    }

    private Rect mChildViewPos;

    private boolean isInViewArea(View view, float x, float y) {
        if (view != null) {
            if (mChildViewPos == null) {
                mChildViewPos = new Rect();
            }
            view.getGlobalVisibleRect(mChildViewPos);
            return mChildViewPos.left < mChildViewPos.right && mChildViewPos.top < mChildViewPos.bottom  // check for empty first
                    && x >= mChildViewPos.left && x < mChildViewPos.right && y >= mChildViewPos.top && y < mChildViewPos.bottom;
        }
        return false;
    }


    private class ViewDragCallback extends BViewDragHelper.Callback {
        private boolean mIsScrollOverValid;

        @Override
        public boolean tryCaptureView(View view, int i) {
//            boolean ret = mDragHelper.isEdgeTouched(mEdgeFlag, i);
//
//            if (ret) {
//                if (mDragHelper.isEdgeTouched(BViewDragHelper.EDGE_LEFT, i)) {
//                    mTrackingEdge = BViewDragHelper.EDGE_LEFT;
//                } else if (mDragHelper.isEdgeTouched(BViewDragHelper.EDGE_TOP, i)) {
//                    mTrackingEdge = BViewDragHelper.EDGE_TOP;
//                }

            if (mListeners != null && !mListeners.isEmpty()) {
                for (BSwipeListener listener : mListeners) {
                    listener.onEdgeTouch();
                }
            }
            mIsScrollOverValid = true;
//            }
            return true;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mEdgeFlag & (BViewDragHelper.EDGE_LEFT | BViewDragHelper.EDGE_RIGHT);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mEdgeFlag & BViewDragHelper.EDGE_TOP;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            if ((mTrackingEdge & BViewDragHelper.EDGE_LEFT) != 0) {
                mScrollPercent = Math.abs((float) left / (mContentView.getWidth() /*+ mShadowLeft.getIntrinsicWidth()*/));
            } else if ((mTrackingEdge & BViewDragHelper.EDGE_TOP) != 0) {
                mScrollPercent = Math.abs((float) top / mContentView.getHeight());
            }

            mContentLeft = left;
            mContentTop = top;
            invalidate();

            if (mScrollPercent < mScrollThreshold && !mIsScrollOverValid) {
                mIsScrollOverValid = true;
            }

            if (mListeners != null && !mListeners.isEmpty()) {
                for (BSwipeListener listener : mListeners) {
                    listener.onScroll(mScrollPercent, mContentLeft);
                }
            }

            if (mScrollPercent >= 1) {
                if (mScrollPercent >= mScrollThreshold && mIsScrollOverValid) {
                    mIsScrollOverValid = false;

                    if (mListeners != null && !mListeners.isEmpty()) {
                        for (BSwipeListener listener : mListeners) {
                            listener.onScrollToClose();
                        }
                    }

//                    if (mActivity != null && !mActivity.isFinishing()) {
//                        mActivity.finish();
//                        mActivity.overridePendingTransition(0, 0);
//                    }

                    if (mActivity == null) {
                        scrollToInitPosition();
                    }
                }
            }


        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            mIsHideInputMethod = false;
            final int childWidth = releasedChild.getWidth();
            final int childHeight = releasedChild.getHeight();

            int left = 0, top = 0;
            if ((mTrackingEdge & BViewDragHelper.EDGE_LEFT) != 0) {
                //判断释放以后是应该滑到最右边(关闭)，还是最左边（还原）
                left = xvel > 0 || xvel == 0 && mScrollPercent > mScrollThreshold ? childWidth + mShadowLeft.getIntrinsicWidth() + OVERSCROLL_DISTANCE : 0;
            } else if ((mTrackingEdge & BViewDragHelper.EDGE_TOP) != 0) {
                top = yvel > 0 || yvel == 0 && mScrollPercent > mScrollThreshold ? childHeight + OVERSCROLL_DISTANCE : 0;//+ mShadowLeft.getIntrinsicWidth()
            }

            mDragHelper.settleCapturedViewAt(left, top);
            invalidate();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int ret = 0;
            if ((mTrackingEdge & BViewDragHelper.EDGE_LEFT) != 0) {
                ret = Math.min(child.getWidth(), Math.max(left, 0));
            } else if ((mTrackingEdge & BViewDragHelper.EDGE_RIGHT) != 0) {
                ret = Math.min(0, Math.max(left, -child.getWidth()));
            }

            return ret;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int ret = 0;
            if ((mTrackingEdge & BViewDragHelper.EDGE_TOP) != 0) {
                ret = Math.min(child.getHeight(), Math.max(top, 0));
            }
            return ret;
        }
    }
}
