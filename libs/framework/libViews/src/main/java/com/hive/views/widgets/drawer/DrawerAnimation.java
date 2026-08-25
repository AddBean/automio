// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.drawer;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.hive.utils.debug.DLog;

/**
 * Created by Admin on 2016/4/27.
 */
public class DrawerAnimation extends Animation implements Animation.AnimationListener {
    private DrawerListener mOnAnimOverListener;
    private DrawerView mView;
    private int mStart;
    private int mEnd;
    public boolean mIsRuning = false;
    public boolean mIsVer = true;
    public DrawerView.STATE mTargetState;

    public DrawerAnimation(DrawerView view, int start, int end, boolean isVer, DrawerView.STATE targetState) {
        DLog.e("start:" + start);
        this.mView = view;
        this.mStart = start;
        this.mEnd = end;
        this.mIsVer = isVer;
        mTargetState = targetState;
        this.setAnimationListener(this);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int d = (int) (mStart + interpolatedTime * (mEnd - mStart));
        if(mEnd - mStart==0)return;
//        ALog.e("applyTransformation:"+d);
        if (mIsVer) {
            this.mView.setY(d);
        } else {
            this.mView.setX(d);
        }
    }

    public void setOnAnimOverListener(DrawerListener mOnAnimOverListener) {
        this.mOnAnimOverListener = mOnAnimOverListener;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        mIsRuning = false;
        this.mView.mState = mTargetState;
        mView.mAnimRuning=false;
        if (mOnAnimOverListener != null) {
            this.mView.clearAnimation();
            mOnAnimOverListener.onOver(mView);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public void startAnim() {
        if ((this.mView.getAnimation() != null && this.mView.getAnimation().hasStarted()) || mIsRuning)
            return;
        this.mView.clearAnimation();
        mView.mAnimRuning=true;
        if (mOnAnimOverListener != null) mOnAnimOverListener.onBegin(mView);
        mIsRuning = true;
        this.mView.startAnimation(this);
    }
}
