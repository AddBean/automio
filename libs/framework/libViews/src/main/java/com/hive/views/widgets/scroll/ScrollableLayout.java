// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.scroll;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Scroller;


import com.hive.utils.system.UIUtils;
import com.hive.views.R;
import com.hive.views.widgets.BlurredView;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

public class ScrollableLayout extends RelativeLayout {

    private int DP = 1;
    boolean mIsMoveUpOrDown = false;
    boolean mIsAllow = false;
    private Context context;
    private Scroller mScroller;
    private float mDownX;
    private float mDownY;
    private float mLastX;
    private float mLastY;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private boolean mIsHorizontalScrolling;
    private float x_down;
    private float y_down;
    private float x_move;
    private float y_move;
    private float moveDistanceX;
    private float moveDistanceY;
    private View mHeadView;
    private BlurredView mBackgroundView;
    private ViewPager childViewPager;
    private DIRECTION mDirection;
    private int mHeadHeight;
    private int mScrollY;
    private int sysVersion;
    private boolean flag1, flag2;
    private int mLastScrollerY;
    private boolean mDisallowIntercept;
    private boolean isRefreshHeaderPullDownable = false;
    private int minY = 0;
    private int maxY = 0;
    private int mCurY;
    private boolean isClickHead;
    private int mScrollMinY = 10;
    //状态栏高度
    private int mStatusBarHeight = 0;
    private float mDefaultNavHeight = 0;
    private boolean mFixTopNav = false;
    private final boolean mEnable;
    private OnScrollListener onScrollListener;
    private ScrollableHelper mHelper;
    private boolean mIsTranslucentStatusBar = false;
    private int topHeight;
    private int scrollStatus = SCROLL_STATE_IDLE;

    public ScrollableLayout(Context context) {
        this(context, null);
    }

    public ScrollableLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DP = UIUtils.dipToPx(getContext(), 1);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScrollableLayout);
        mFixTopNav = ta.getBoolean(R.styleable.ScrollableLayout_fixTopNav, false);
        mEnable = ta.getBoolean(R.styleable.ScrollableLayout_enable, true);
        mDefaultNavHeight = getResources().getDimension(ta.getResourceId(R.styleable.ScrollableLayout_commonNavHeight, 44 * DP));

        ta.recycle();

        this.context = context;
        mHelper = new ScrollableHelper();
        mScroller = new Scroller(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        sysVersion = Build.VERSION.SDK_INT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mStatusBarHeight = getInternalDimensionSize(getResources(), "status_bar_height");
            if (mStatusBarHeight < 0) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    mStatusBarHeight = UIUtils.dipToPx(getContext(), 25);
                else mStatusBarHeight = UIUtils.dipToPx(getContext(), 24);
            }
        }

        measureScrollDimension();
    }

    private void measureScrollDimension() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                computeViewHeight();

            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeViewHeight();

    }

    public void reMeasure() {
        computeViewHeight();
    }

    private void computeViewHeight() {
        if (isEnabled()) {
            if (mHeadView != null) {

                maxY = mHeadView.getMeasuredHeight() + (mIsTranslucentStatusBar ? UIUtils.dipToPx(ScrollableLayout.this.getContext(), 0.5f) : 0);

                if (mFixTopNav) {
                    maxY = (int) (maxY - mDefaultNavHeight);
                }

                mHeadHeight = mHeadView.getMeasuredHeight();
            }

            if (null != mBackgroundView) {

                ViewGroup.LayoutParams layoutParams = mBackgroundView.getLayoutParams();
                layoutParams.height = mHeadHeight + topHeight;
                mBackgroundView.requestLayout();

                mBackgroundView.setVisibility(VISIBLE);
            }
        }
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public ScrollableHelper getHelper() {
        return mHelper;
    }

    public void setIsTranslucentStatusBar(boolean translucentStatusBar) {
        mIsTranslucentStatusBar = translucentStatusBar;
    }

    public void setupTopBackgroundView(final BlurredView backgroundView, int topHeight) {
        this.topHeight = topHeight;
        this.mBackgroundView = backgroundView;
        measureScrollDimension();
    }

    public void adjustBackgroundView(int offset) {
        if (mBackgroundView != null) {
            int scrollDistance = Math.max(offset, 0);
            mBackgroundView.scrollTo(0, scrollDistance);
            float alpha = scrollDistance * 1.0f / maxY;
            mBackgroundView.setBlurredLevel(alpha);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mEnable) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + maxY, MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onFinishInflate() {
        if (!mEnable) {
            return;
        }
        mHeadView = findViewWithTag("head");

        if (mHeadView != null && !mHeadView.isClickable()) {
            mHeadView.setClickable(true);
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt != null && childAt instanceof ViewPager) {
                childViewPager = (ViewPager) childAt;
            }
        }
        super.onFinishInflate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!mEnable) {
            return super.dispatchTouchEvent(ev);
        }
        float currentX = ev.getX();
        float currentY = ev.getY();
        float deltaY;
        int shiftX;
        int shiftY;
        shiftX = (int) Math.abs(currentX - mDownX);
        shiftY = (int) Math.abs(currentY - mDownY);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDisallowIntercept = false;
                mIsHorizontalScrolling = false;
                x_down = ev.getRawX();
                y_down = ev.getRawY();
                flag1 = true;
                flag2 = true;
                mIsMoveUpOrDown = false;
                isRefreshHeaderPullDownable = false;
                mDownX = currentX;
                mDownY = currentY;
                mLastX = currentX;
                mLastY = currentY;
                mScrollY = getScrollY();
                checkIsClickHead((int) currentY, mHeadHeight, getScrollY());
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                mScroller.forceFinished(true);
                if (childViewPager != null) {
                    childViewPager.requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDisallowIntercept) {
                    break;
                }
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(ev);
                deltaY = mLastY - currentY;
                if (flag1) {
                    if (!mIsMoveUpOrDown && shiftX > mTouchSlop && shiftX > shiftY) {
                        flag1 = false;
                        flag2 = false;
                        mIsMoveUpOrDown = false;
                    } else if (shiftY > mTouchSlop && shiftY > shiftX) {
                        flag1 = false;
                        flag2 = true;
                        mIsMoveUpOrDown = true;
                    }
                }

                onScrollStatusChanged(currentX, currentY, mLastX, mLastY);

                ScrollableHelper.ScrollableContainer container = mHelper.getCurrentScrollableContainer();

                if (container != null && "flist".equals(container.getScrollableViewId())) {
                    View view = container.getScrollableView();
                    if (view != null && view instanceof RecyclerView && view.isEnabled()) {
                        isRefreshHeaderPullDownable = mCurY <= 0 && currentY > mDownY;
                        if (view instanceof IScrollabelInteface) {
                            ((IScrollabelInteface) view).setRefreshEnabled(isRefreshHeaderPullDownable);
                        }

                    }
                }

                if (flag2 && shiftY > mTouchSlop && shiftY > shiftX && (!isSticked() || mHelper.isTop()) && container != null && container.canScroll() && !isRefreshHeaderPullDownable) {
                    if (childViewPager != null) {
                        childViewPager.requestDisallowInterceptTouchEvent(true);
                    }
                    scrollBy(0, (int) (deltaY + 0.5));
                } else {
                    if (childViewPager != null) {
                        childViewPager.requestDisallowInterceptTouchEvent(mIsMoveUpOrDown);
                    }
                }
                mLastX = currentX;
                mLastY = currentY;
                x_move = ev.getRawX();
                y_move = ev.getRawY();
                moveDistanceX = (int) (x_move - x_down);
                moveDistanceY = (int) (y_move - y_down);
                if (Math.abs(moveDistanceY) > mScrollMinY && (Math.abs(moveDistanceY) * 0.1 > Math.abs(moveDistanceX))) {
                    mIsHorizontalScrolling = false;
                } else {
                    mIsHorizontalScrolling = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (childViewPager != null) {
                    childViewPager.requestDisallowInterceptTouchEvent(false);
                }
                mIsMoveUpOrDown = false;
                if (flag2 && shiftY > shiftX && shiftY > mTouchSlop) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    float yVelocity = -mVelocityTracker.getYVelocity();
                    if (Math.abs(yVelocity) > mMinimumVelocity) {
                        mDirection = yVelocity > 0 ? DIRECTION.UP : DIRECTION.DOWN;
                        if (onScrollListener != null) {
                            onScrollListener.onScrollEndTouch(mDirection == DIRECTION.DOWN);
                        }
                        if (mDirection == DIRECTION.UP && isSticked()) {
                        } else {
                            mScroller.fling(0, getScrollY(), 0, (int) yVelocity, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
                            mScroller.computeScrollOffset();
                            mLastScrollerY = getScrollY();
                            invalidate();
                        }
                    }
                    if (isClickHead || !isSticked()) {
                        int action = ev.getAction();
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        boolean dd = super.dispatchTouchEvent(ev);
                        ev.setAction(action);
                        return dd;
                    }
                }
                onScrollStatusChanged(currentX, currentY, mLastX, mLastY);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (childViewPager != null) {
                    childViewPager.requestDisallowInterceptTouchEvent(false);
                }
                mIsMoveUpOrDown = false;
                if (flag2 && isClickHead && (shiftX > mTouchSlop || shiftY > mTouchSlop)) {
                    int action = ev.getAction();
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    boolean dd = super.dispatchTouchEvent(ev);
                    ev.setAction(action);
                    return dd;
                }
                onScrollStatusChanged(currentX, currentY, mLastX, mLastY);
                break;
            default:
                break;
        }
        super.dispatchTouchEvent(ev);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private int getScrollerVelocity(int distance, int duration) {
        if (!mEnable) {
            return 0;
        }
        if (mScroller == null) {
            return 0;
        } else if (sysVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return (int) mScroller.getCurrVelocity();
        } else {
            return distance / duration;
        }
    }

    @Override
    public void computeScroll() {
        if (!mEnable) {
            return;
        }

        if (mScroller.computeScrollOffset()) {

            final int currY = mScroller.getCurrY();

            if (mDirection == DIRECTION.UP) {
                if (isSticked()) {
                    int distance = mScroller.getFinalY() - currY;
                    int duration = calcDuration(mScroller.getDuration(), mScroller.timePassed());
                    mHelper.smoothScrollBy(getScrollerVelocity(distance, duration), distance + 3, duration);
                    mScroller.forceFinished(true);
                    return;
                } else {
                    scrollTo(0, currY);
                }
            } else {
                if (mHelper.isTop()) {
                    int deltaY = (currY - mLastScrollerY);
                    int toY = getScrollY() + deltaY;
                    scrollTo(0, toY);
                    if (mCurY <= minY) {
                        mScroller.forceFinished(true);
                        return;
                    }
                }
                invalidate();
            }
            mLastScrollerY = currY;
        }
    }

    protected void onScrollStatusChanged(float l, float t, float oldl, float oldt) {
//        if (scrollStatus == SCROLL_STATE_IDLE) {
//            if (t != oldt) {
//                scrollStatus = SCROLL_STATE_TOUCH_SCROLL;
//                if (onScrollListener != null) {
//                    onScrollListener.onScrollStart();
//                }
//            }
//        } else if (scrollStatus == SCROLL_STATE_TOUCH_SCROLL) {
//            if (t == oldt) {
//                scrollStatus = SCROLL_STATE_IDLE;
//                if (onScrollListener != null) {
//                    onScrollListener.onScrollEnd();
//                }
//            }
//        }
    }

    @Override
    public void scrollBy(int x, int y) {
        if (!mEnable) {
            return;
        }
        int scrollY = getScrollY();
        int toY = scrollY + y;
        if (toY >= maxY) {
            toY = maxY;
        } else if (toY <= minY) {
            toY = minY;
        }
        y = toY - scrollY;

        super.scrollBy(x, y);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (!mEnable) {
            return;
        }
        if (y >= maxY) {
            y = maxY;
        } else if (y <= minY) {
            y = minY;
        }
        mCurY = y;
        adjustBackgroundView(y);
        super.scrollTo(x, y);

        if (onScrollListener != null) {
            onScrollListener.onScroll(y, maxY);
        }
    }

    private void initOrResetVelocityTracker() {
        if (!mEnable) {
            return;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void checkIsClickHead(int downY, int headHeight, int scrollY) {
        isClickHead = downY + scrollY <= headHeight;
    }

    private int calcDuration(int duration, int timepass) {
        return duration - timepass;
    }

    public void requestScrollableLayoutDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        mDisallowIntercept = disallowIntercept;
    }

    public boolean isSticked() {
        return mCurY >= maxY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setScrollMinY(int y) {
        mScrollMinY = y;
    }

    public boolean isCanPullToRefresh() {
        if (getScrollY() <= 0 && mHelper.isTop() && !mIsHorizontalScrolling) {
            return true;
        }
        return false;
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    private int getInternalDimensionSize(Resources res, String key) {
        int result = -1;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            try {
                result = res.getDimensionPixelSize(resourceId);
            } catch (Resources.NotFoundException e) {

            }
        }
        return result;
    }

    enum DIRECTION {
        UP, DOWN
    }

    public interface OnScrollListener {
        void onScroll(int currentY, int maxY);

        void onScrollStart();

        void onScrollEnd();

        void onScrollEndTouch(boolean isMoveDown);
    }
}