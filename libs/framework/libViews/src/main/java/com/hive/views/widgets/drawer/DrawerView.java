// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hive.utils.utils.DensityUtil;
import com.hive.views.R;

/**
 * Created by Admin on 2016/4/28.
 */
public class DrawerView extends FrameLayout {
    protected Context mContext;
    protected float mHiddenY = -1;
    protected float mHiddenX = -1;
    protected STATE mStateDef = STATE.DOWN;
    public STATE mState;
    public int mAnimTime = 300;
    protected boolean mIsInit = true;
    protected int mPaddingSide = 0;
    protected int DP = 1;
    private boolean mMeasureCompleted = false;
    public boolean mAnimRuning = false;

    DrawerAnimation anim;

    public enum STATE {
        UP(0), DOWN(1), LEFT(2), RIGHT(3);
        private int mIndex;

        STATE(int index) {
            this.mIndex = index;

        }

        public int getmIndex() {
            return mIndex;
        }

        public static STATE getStateByIndex(int index) {
            switch (index) {
                case 0:
                    return STATE.UP;
                case 1:
                    return STATE.DOWN;
                case 2:
                    return STATE.LEFT;
                case 3:
                    return STATE.RIGHT;
                default:
                    return UP;
            }
        }
    }

    public class ViewHolder {

    }

    public DrawerView(Context context) {
        super(context);
        DP = DensityUtil.dip2px(1);
        this.mContext = context;
    }

    public DrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DP = DensityUtil.dip2px(1);
        this.mContext = context;
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.DrawerView, 0, 0);
        int indexDef = a.getInt(R.styleable.DrawerView_gravity, 0);
        int index = a.getInt(R.styleable.DrawerView_current_gravity, 1);
        mPaddingSide = (int) a.getDimension(R.styleable.DrawerView_paddingSide, 0);
        setStateDef(STATE.getStateByIndex(indexDef));
        mState = STATE.getStateByIndex(index);
        a.recycle();
    }

    public void setState(STATE mState) {
        this.mState = mState;
    }

    public void setAnimTime(int mAnimTime) {
        this.mAnimTime = mAnimTime;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mHiddenY == -1) {
            mHiddenY = getY() + getMeasuredHeight();
        }
        if (mHiddenX == -1) {
            mHiddenX = getX() + getMeasuredWidth();
        }
        if (mIsInit) {
            initState();
            setStateInstant(mState);
            mIsInit = false;
        }
        mOnMeasureCompleteListener = null;//保证mOnMeasureCompleteListener只会调用一次；
    }

    /**
     * 如果视图发生变化，调用该方法，重新计算坐标；
     */
    public void requestRelayout() {
        mHiddenY = -1;
        mHiddenX = -1;
        mIsInit = true;
        setX(0);
        setY(0);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mOnMeasureCompleteListener != null) {
            mOnMeasureCompleteListener.onComplete();
            // ULog.e("onMeasure");
            mMeasureCompleted = true;
        }
    }

    public STATE getStateDef() {
        return mStateDef;
    }

    public void setStateDef(STATE mStateDef) {
        this.mStateDef = mStateDef;
    }

    protected void initState() {
        switch (mStateDef) {
            case LEFT:
                this.setX(-getMeasuredWidth() + mPaddingSide);
                break;
            case RIGHT:
                this.setX(getParentWidth() - mPaddingSide);
                break;
            case UP:
                this.setY(-getMeasuredHeight() + mPaddingSide);
                break;
            case DOWN:
                this.setY(mHiddenY - mPaddingSide);
                break;
        }
    }

    public void setPaddingSide(int mPaddingSide) {
        this.mPaddingSide = mPaddingSide;
    }

    public void setStateInstant(STATE state) {
        mState = state;
        switch (state) {
            case LEFT:
                this.setX(getX() - getMeasuredWidth() + mPaddingSide);
                break;
            case RIGHT:
                this.setX(getX() + getMeasuredWidth() - mPaddingSide);
                break;
            case UP:
                this.setY(getY() - getMeasuredHeight() + mPaddingSide);
                break;
            case DOWN:
                this.setY((int) getY() + getMeasuredHeight() - mPaddingSide);
                break;
        }
    }


    public void drawMenuUp(final DrawerListener onAnimOverListener) {
        if (mStateDef != STATE.UP)
            initState();
        getBaseView().setVisibility(View.VISIBLE);
        anim = new DrawerAnimation(this, (int) getY(), (int) getY() - getMeasuredHeight(), true, STATE.UP);
        anim.setDuration(mAnimTime);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    public void drawMenuDown(final DrawerListener onAnimOverListener) {
        if (mStateDef != STATE.DOWN)
            initState();
        getBaseView().setVisibility(View.VISIBLE);
        anim = new DrawerAnimation(this, (int) getY(), (int) getY() + getMeasuredHeight(), true, STATE.DOWN);
        anim.setDuration(mAnimTime);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    public void drawMenuLeft(final DrawerListener onAnimOverListener) {
        if (mStateDef != STATE.LEFT)
            initState();
        getBaseView().setVisibility(View.VISIBLE);
        anim = new DrawerAnimation(this, (int) getX(), (int) getX() - getMeasuredWidth() + mPaddingSide, false, STATE.LEFT);
        anim.setDuration(mAnimTime);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    public void drawMenuRight(final DrawerListener onAnimOverListener) {
        if (mStateDef != STATE.RIGHT)
            initState();
        getBaseView().setVisibility(View.VISIBLE);
        anim = new DrawerAnimation(this, (int) getX(), (int) getX() + getMeasuredWidth() - mPaddingSide, false, STATE.RIGHT);
        anim.setDuration(mAnimTime);
        anim.setOnAnimOverListener(onAnimOverListener);
        anim.startAnim();
    }

    public void cancelAnim() {
        if (anim != null) anim.cancel();
    }

    public int getParentWidth() {
        ViewGroup vp = (ViewGroup) getParent();
        if(vp==null)return 0;
        return vp.getMeasuredWidth();
    }

    public View getBaseView() {
        return this;
    }

    public STATE getState() {
        return mState;
    }

    public OnMeasureCompleteListener mOnMeasureCompleteListener;

    public void setOnMeasureCompleteListener(OnMeasureCompleteListener mOnMeasureCompleteListener) {
        this.mOnMeasureCompleteListener = mOnMeasureCompleteListener;
    }

    public interface OnMeasureCompleteListener {
        void onComplete();
    }

    public boolean isMeasureCompleted() {
        return mMeasureCompleted;
    }

    public boolean isAnimRuning() {
        return mAnimRuning;
    }
}