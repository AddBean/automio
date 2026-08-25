// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.largeimg;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SwipeGestureDetector {
    public static final  int                    DIRECTION_LEFT   = 0x00;
    public static final  int                    DIRECTION_RIGHT  = 0x01;
    public static final  int                    DIRECTION_TOP    = 0x02;
    public static final  int                    DIRECTION_BOTTOM = 0x03;
    private static final String                 TAG              = "SwipeGestureDetector";
    private static final boolean                DEBUG            = false;
    private final        int                    mMinimumFlingVelocity;
    private final        int                    mMaximumFlingVelocity;
    private              OnSwipeGestureListener listener;
    private              int                    touchSlop;
    private              float                  initialMotionX, initialMotionY;
    private float lastMotionX, lastMotionY;
    private boolean isBeingDragged;
    @Direction
    private int     direction;
    private boolean mIsIgnoreEvent = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private MotionEvent     mCurrentDownEvent;

    public SwipeGestureDetector(Context context, @NonNull OnSwipeGestureListener listener) {
        this.listener = listener;
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        mMinimumFlingVelocity = (int) (1.5f * config.getScaledMinimumFlingVelocity());
        mMaximumFlingVelocity = config.getScaledMaximumFlingVelocity();
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        // two fingers
        int pointerCount = event.getPointerCount();
        if (pointerCount > 1) {
            return false;
        }

        float x = event.getRawX();
        float y = event.getRawY();
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + x + "-" + y);

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            reset(event, initialMotionX, initialMotionY);
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN) {
            if (isBeingDragged) {
                return true;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialMotionX = lastMotionX = x;
                initialMotionY = lastMotionY = y;
                mIsIgnoreEvent = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsIgnoreEvent) {
                    break;
                }
                final float xDiff = Math.abs(x - initialMotionX);
                final float yDiff = Math.abs(y - initialMotionY);
//                if (xDiff > touchSlop && xDiff > yDiff) {
//                    isBeingDragged = true;
//                    if (x - initialMotionX > 0) {
//                        direction = DIRECTION_RIGHT;
//                        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: RIGHT");
//                    } else {
//                        direction = DIRECTION_LEFT;
//                        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: LEFT");
//                    }
//                } else
                if (yDiff > touchSlop && yDiff > xDiff) {
                    isBeingDragged = true;
                    if (y - initialMotionY > 0) {
                        direction = DIRECTION_BOTTOM;
                        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: BOTTOM");
                    } else {
                        direction = DIRECTION_TOP;
                        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: TOP");
                    }
                }
                break;
        }
        return isBeingDragged;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        int pointerCount = event.getPointerCount();
        if (pointerCount > 1) {
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);


        float x = event.getRawX();
        float y = event.getRawY();
        if (DEBUG) Log.d(TAG, "onTouchEvent: " + x + "-" + y);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialMotionX = lastMotionX = x;
                initialMotionY = lastMotionY = y;
                mIsIgnoreEvent = false;
                isBeingDragged = false;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsIgnoreEvent) {
                    isBeingDragged = false;
                    break;
                }
                final float deltaX = x - lastMotionX;
                final float deltaY = y - lastMotionY;
                lastMotionX = x;
                lastMotionY = y;
                if (isBeingDragged) {
                    if (direction == DIRECTION_TOP /*|| direction == DIRECTION_RIGHT*/) {
                        if (y - initialMotionY > 0) {
                            direction = DIRECTION_BOTTOM;
                        } else {
                            direction = DIRECTION_TOP;
                        }
                    }
                    if (direction == DIRECTION_TOP) {
                        if (listener != null) {
                            listener.onSwipeTop(deltaX, deltaY, Math.abs(y - initialMotionY));
                        }
                    } else if (direction == DIRECTION_BOTTOM) {
                        if (listener != null) {
                            listener.onSwipeBottom(deltaX, deltaY, Math.abs(y - initialMotionY));
                        }
                    }
                } else {
                    final float xDiff = Math.abs(x - initialMotionX);
                    final float yDiff = Math.abs(y - initialMotionY);
                    if (yDiff > touchSlop && yDiff > xDiff) {
                        isBeingDragged = true;
                        if (y - initialMotionY > 0) {
                            direction = DIRECTION_BOTTOM;
                        } else {
                            direction = DIRECTION_TOP;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                reset(event, x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
                reset(event, x, y);
                break;
        }
        return true;
    }

    private void reset(MotionEvent ev, float x, float y) {
        mIsIgnoreEvent = false;
        if (isBeingDragged) {
            // A fling must travel the minimum tap distance
            if (mVelocityTracker != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
//                final VelocityTracker velocityTracker = mVelocityTracker;
//                final int pointerId = ev.getPointerId(0);
//                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
//                final float velocityY = velocityTracker.getYVelocity(pointerId);
//                final float velocityX = velocityTracker.getXVelocity(pointerId);
//                if (Math.abs(velocityY) > mMinimumFlingVelocity && Math.abs(velocityY) > Math.abs(velocityX) /*|| (Math.abs(velocityX) > mMinimumFlingVelocity)*/) {
//                    if (listener != null) {
//                        listener.onFling(mCurrentDownEvent, ev, velocityX, velocityY);
//                    }
//                } else {
                if (listener != null) {
                    listener.onFinish(direction, x - initialMotionX, y - initialMotionY);
                }
//                }
            } else {
                if (listener != null) {
                    listener.onFinish(direction, x - initialMotionX, y - initialMotionY);
                }
            }
        } else {
            // A fling must travel the minimum tap distance
            if (mVelocityTracker != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                final VelocityTracker velocityTracker = mVelocityTracker;
                final int pointerId = ev.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final float velocityY = velocityTracker.getYVelocity(pointerId);
                final float velocityX = velocityTracker.getXVelocity(pointerId);

                if (Math.abs(velocityY) > mMinimumFlingVelocity && Math.abs(velocityY) > Math.abs(velocityX)) {
                    if (listener != null) {
                        listener.onFling(mCurrentDownEvent, ev, velocityX, velocityY);
                    }
                }
            }
        }
        if (mVelocityTracker != null) {
            // This may have been cleared when we called out to the
            // application above.
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        isBeingDragged = false;
    }

    public void onDisableEvent(boolean disableEvent) {
        mIsIgnoreEvent = disableEvent;
        if (mIsIgnoreEvent) {
            isBeingDragged = false;
        }
    }

    @IntDef({DIRECTION_LEFT, DIRECTION_RIGHT, DIRECTION_TOP, DIRECTION_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    @interface Direction {
    }

    public interface OnSwipeGestureListener {
        void onSwipeLeft(float deltaX, float deltaY);

        void onSwipeRight(float deltaX, float deltaY);

        void onSwipeTop(float deltaX, float deltaY, float moveY);

        void onSwipeBottom(float deltaX, float deltaY, float moveY);

        void onFinish(@Direction int direction, float distanceX, float distanceY);

        void onFling(MotionEvent currentDownEvent, MotionEvent ev, float velocityX, float velocityY);
    }

}
