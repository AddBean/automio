// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.drawer;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import com.hive.utils.debug.DLog;


/**
 * Created by Admin on 2016/7/5.
 */
public class DrawerViewWithTouch extends DrawerView {
    private float mStartX;
    private float mDispatchStartX;
    private int ANIM_TIME = 200;
    private boolean mFakeTouchDown = false;
    private boolean mTouchDownEnable = false;
    private VelocityTracker mVelocityTracker = null;
    private boolean isFastMoveOpen = false;
    private boolean isFastMoveClose = false;

    public DrawerViewWithTouch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownEnable = false;
                mDispatchStartX = event.getRawX();
                super.dispatchTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getRawX() - mDispatchStartX);
                if (dx > 40 * DP) {
                    mTouchDownEnable = true;
                    if (!mFakeTouchDown) {
                        event.setAction(MotionEvent.ACTION_DOWN);
                        mFakeTouchDown = true;
                    }
                    onTouchEvent(event);
                } else {
                    mTouchDownEnable = false;
                    super.dispatchTouchEvent(event);
                }
                break;
            default:
                if (mTouchDownEnable) {
                    onTouchEvent(event);
                    mFakeTouchDown = false;
                } else {
                    super.dispatchTouchEvent(event);
                    mFakeTouchDown = false;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mStartX = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
//                Log.e("menu", "Move Velocity" + mVelocityTracker.getXVelocity());
                float dx = event.getRawX() - mStartX;
                if (mStateDef == STATE.LEFT) {
                    float tragetX = this.getX() + dx;
//                    ALog.e("-getMeasuredWidth():"+getMeasuredWidth()+" tragetX:"+tragetX);
                    if (tragetX < 0 && tragetX > -getMeasuredWidth())
                        this.setX(tragetX);
                }
                if (mStateDef == STATE.RIGHT) {
                    float tragetX = this.getX() + dx;
                    if (tragetX > (getParentWidth() - getMeasuredWidth()) && tragetX < getParentWidth())
                        this.setX(tragetX);
                }
                mStartX = event.getRawX();
                if (mVelocityTracker.getXVelocity() > 400) {
                    isFastMoveOpen = true;
                } else if (mVelocityTracker.getXVelocity() < -400) {
                    isFastMoveClose = true;
                } else {
                    isFastMoveOpen = false;
                    isFastMoveClose = false;
                }


                break;
            case MotionEvent.ACTION_UP:
                if (mStateDef == STATE.LEFT) {
//                    Log.e("menu", "fast LEFT");
                    if (mVelocityTracker.getXVelocity() > 400 || isFastMoveOpen) {
                        doAnimRight(null);
//                        Log.e("menu","Move Velocity"+mVelocityTracker.getXVelocity());
//                        Log.e("menu", "fast open");
                    } else if (mVelocityTracker.getXVelocity() < -400 || isFastMoveClose) {
                        doAnimLeft(null);
//                        Log.e("menu","Move Velocity"+mVelocityTracker.getXVelocity());
//                        Log.e("menu", "fast close");
                    } else if ((this.getX() + getMeasuredWidth()) > getParentWidth() / 2) {
//                        Log.e("menu", "open");
                        doAnimRight(null);
                    } else if ((this.getX() + getMeasuredWidth()) > getParentWidth() / 2) {
//                        Log.e("menu", "open");
                        doAnimRight(null);
                    } else {
//                        Log.e("menu", "close");
//                        Log.e("menu","Move Velocity"+mVelocityTracker.getXVelocity());
                        doAnimLeft(null);
                    }
                }
                if (mStateDef == STATE.RIGHT) {
//                    Log.e("menu", "fast close");
                    if (this.getX() > getParentWidth() / 2) {
                        doAnimRight(null);
                    } else {
                        doAnimLeft(null);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
//                mVelocityTracker.recycle();
                break;
            default:
                isFastMoveClose = false;
                isFastMoveOpen = false;
                break;
        }
        return true;
    }


    private void doAnimRight(final DrawerListener onAnimOverListener) {
        getBaseView().setVisibility(View.VISIBLE);
        DLog.e("" + getX());
        DrawerAnimation anim = null;
        if (mStateDef == STATE.LEFT)
            anim = new DrawerAnimation(this, (int) getX(), 0, false, STATE.RIGHT);
        if (mStateDef == STATE.RIGHT)
            anim = new DrawerAnimation(this, (int) getX(), getParentWidth() - mPaddingSide, false, STATE.RIGHT);
        anim.setDuration(ANIM_TIME);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    private void doAnimLeft(final DrawerListener onAnimOverListener) {
        getBaseView().setVisibility(View.VISIBLE);
        DLog.e("" + getX());
        DrawerAnimation anim = null;
        if (mStateDef == STATE.LEFT)
            anim = new DrawerAnimation(this, (int) getX(), -getMeasuredWidth() + mPaddingSide, false, STATE.LEFT);
        if (mStateDef == STATE.RIGHT)
            anim = new DrawerAnimation(this, (int) getX(), getParentWidth() - getMeasuredWidth(), false, STATE.LEFT);
        anim.setDuration(ANIM_TIME);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    public int getParentWidth() {
        return ((View) getParent()).getMeasuredWidth();
    }

    public Rect getOutRect() {
        Rect r = new Rect((int) this.getX(),
                (int) this.getY(),
                (int) this.getX() + this.getMeasuredWidth(),
                (int) this.getY() + this.getMeasuredHeight());
        return r;
    }

    @Override
    public void setX(float x) {
        super.setX(x);
        if (mOnMoveListener != null) {
            float percent = 0;
            if (mStateDef == STATE.LEFT)
                percent = (x + getMeasuredWidth()) / (getMeasuredWidth() - mPaddingSide);
            if (mStateDef == STATE.RIGHT)
                percent = x / (getMeasuredWidth() - mPaddingSide);
            mOnMoveListener.onMove(percent);
        }
        ((ViewGroup) getParent()).invalidate();
    }

    public OnMoveListener mOnMoveListener;

    public void setOnMoveListener(OnMoveListener mOnMoveListener) {
        this.mOnMoveListener = mOnMoveListener;
    }

    public interface OnMoveListener {
        public void onMove(float percent);
    }
}
