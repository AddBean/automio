// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.largeimg;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

public class DismissFrameLayout extends FrameLayout implements PhotoView.OnDisableEventListener {
    private static final String                               TAG           = "DismissFrameLayout";
    private              SwipeGestureDetector                 swipeGestureDetector;
    private              OnDismissListener                    dismissListener;
    private              int                                  initHeight; //child view's original height;
    private              int                                  initWidth;
    private              int                                  initLeft      = 0;
    private              int                                  initTop       = 0;
    private              ValueAnimator                        mResetAnimator;
    private              ValueAnimator                        mExitAnimator;
    private              ValueAnimator.AnimatorUpdateListener mExitAnimatorUpdateListener;
    private              ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener;
    private              boolean                              mIsDestroy    = false;
    private              int                                  mMinimumFlingVelocity;
    private              int                                  mScreenHeight = 0;
    private boolean isEnableDismissTouch = true;

    public DismissFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public DismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void onDisableEvent(boolean disable) {
        if (swipeGestureDetector != null) {
            swipeGestureDetector.onDisableEvent(disable);
        }
    }

    public void attachView(PhotoView imageView) {
        if (null != imageView) {
            imageView.setOnDisableEventListener(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(!isEnableDismissTouch)return false;
        try {
            return swipeGestureDetector.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            //uncomment if you really want to see these errors
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnableDismissTouch)return super.onTouchEvent(event);
        return swipeGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDestroy = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsDestroy = false;
    }

    public void onDestroy() {
        mIsDestroy = true;
        if (mResetAnimator != null && mResetAnimator.isRunning()) {
            mResetAnimator.removeAllListeners();
            mResetAnimator.cancel();
            mResetAnimator = null;
        }
    }

    public void setDismissListener(OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    private void init() {
        mIsDestroy = false;
        swipeGestureDetector = new SwipeGestureDetector(getContext(), new SwipeGestureDetector.OnSwipeGestureListener() {
            @Override
            public void onSwipeLeft(float deltaX, float deltaY) {

            }

            @Override
            public void onSwipeRight(float deltaX, float deltaY) {

            }

            @Override
            public void onSwipeTop(float deltaX, float deltaY, float moveY) {
                getParent().requestDisallowInterceptTouchEvent(true);
                dragChildView(deltaX, deltaY, moveY, SwipeGestureDetector.DIRECTION_TOP);
            }

            @Override
            public void onSwipeBottom(float deltaX, float deltaY, float moveY) {
                getParent().requestDisallowInterceptTouchEvent(true);
                dragChildView(deltaX, deltaY, moveY, SwipeGestureDetector.DIRECTION_BOTTOM);
            }

            @Override
            public void onFinish(@SwipeGestureDetector.Direction int direction, float distanceX, float distanceY) {
                if (dismissListener != null && direction == SwipeGestureDetector.DIRECTION_BOTTOM || direction == SwipeGestureDetector.DIRECTION_TOP) {
                    if (mScreenHeight <= 0) {
                        mScreenHeight = getHeight();
                    }
                    if (Math.abs(distanceY) > mScreenHeight / 5) {
                        int by = (mScreenHeight + initHeight) / 2 - (int) Math.abs(distanceY);
                        exit(by, direction);
                    } else {
                        reset();
                        if (dismissListener != null) {
                            dismissListener.onCancel();
                        }
                    }
                }
                getParent().requestDisallowInterceptTouchEvent(false);
            }

            @Override
            public void onFling(MotionEvent currentDownEvent, MotionEvent ev, float velocityX, float velocityY) {
                if (dismissListener != null && Math.abs(velocityY) > Math.abs(velocityX)) {
                    dismissListener.onViewDismiss(true, true);
                }
            }
        });
    }

    /**
     * @param deltaX
     * @param deltaY
     */
    private void dragChildView(float deltaX, float deltaY, float moveY, @SwipeGestureDetector.Direction int direction) {
        int count = getChildCount();
        if (count > 0) {
            View view = getChildAt(0);
            scaleAndMove(view, deltaY, moveY, direction);
        }
    }

    /**
     * 最小缩放到1/2
     *
     * @param view
     * @param deltaY
     */
    private void scaleAndMove(View view, float deltaY, float moveY, @SwipeGestureDetector.Direction int direction) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        if (params == null) {
            params = new MarginLayoutParams(view.getWidth(), view.getHeight());
        }
        if (params.width <= 0 && params.height <= 0) {
            params.width = view.getWidth();
            params.height = view.getHeight();
        }
        if (initHeight <= 0) {
            initHeight = view.getHeight();
            initWidth = view.getWidth();
            initLeft = params.leftMargin;
            initTop = params.topMargin;
        }
        float percent = moveY / (getHeight());
        params.topMargin += deltaY;
        view.setLayoutParams(params);
        if (dismissListener != null) {
            dismissListener.onScaleProgress(percent);
        }
    }


    private void reset() {
        int count = getChildCount();
        if (count > 0) {
            final View view = getChildAt(0);
            final MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            if (initHeight <= 0) {
                initHeight = view.getHeight();
                initWidth = view.getWidth();
                initLeft = params.leftMargin;
                initTop = params.topMargin;
            }
            params.width = initWidth;
            params.height = initHeight;
            params.leftMargin = initLeft;
            if (mResetAnimator != null) {
                mResetAnimator.removeAllListeners();
                mResetAnimator.cancel();
            }
            if (mResetAnimator == null) {
                mResetAnimator = ValueAnimator.ofInt(params.topMargin, initTop);
                mResetAnimator.setDuration(200);
            }
            mResetAnimator.setIntValues(params.topMargin, initTop);

            if (mAnimatorUpdateListener == null) {
                mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (!mIsDestroy) {
                            params.topMargin = (int) animation.getAnimatedValue();
                            view.setLayoutParams(params);
                        }
                    }
                };
            }
            mResetAnimator.addUpdateListener(mAnimatorUpdateListener);
            mResetAnimator.start();
        }
    }

    public void setEnableDismissTouch(boolean enableDismissTouch) {
        isEnableDismissTouch = enableDismissTouch;
    }

    public void exit(int moveY, int direction) {
        int count = getChildCount();
        if (count > 0) {
            final View view = getChildAt(0);
            if (view == null) {
                return;
            }
            final MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.width = initWidth;
            params.height = initHeight;
            params.leftMargin = initLeft;
            if (mExitAnimator != null) {
                mExitAnimator.removeAllListeners();
                mExitAnimator.cancel();
            }
            int top = initTop;
            if (direction == SwipeGestureDetector.DIRECTION_TOP) {
                top = params.topMargin - moveY;
            } else {
                top = params.topMargin + moveY;
            }
            if (mExitAnimator == null) {
                mExitAnimator = ValueAnimator.ofInt(params.topMargin, top);
                mExitAnimator.setDuration(500);
            }
            mExitAnimator.setIntValues(params.topMargin, top);

            if (mExitAnimatorUpdateListener == null) {
                mExitAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (!mIsDestroy) {
                            params.topMargin = (int) animation.getAnimatedValue();
                            view.setLayoutParams(params);
                            int moveY = Math.abs(params.topMargin - initTop);
                            float percent = (moveY * 1.f) / (getHeight());
                            if (dismissListener != null) {
                                dismissListener.onScaleProgress(percent);
                            }
                        }
                    }
                };
            }

            view.animate().alpha(0).setDuration(300).start();

            mExitAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (dismissListener != null && !mIsDestroy) {
                        dismissListener.onScaleProgress(100f);
                        dismissListener.onViewDismiss(false, false);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mExitAnimator.addUpdateListener(mExitAnimatorUpdateListener);
            mExitAnimator.start();
        }
    }

    public interface OnDismissListener {
        void onScaleProgress(float scale);

        void onViewDismiss(boolean anim, boolean isFling);

        void onCancel();

        void onDoubleClick();

        void onPhotoTap(boolean isInPhoto);
    }

    public interface OnDisableEventListener {
        void onDisableEvent(boolean var1);
    }
}
