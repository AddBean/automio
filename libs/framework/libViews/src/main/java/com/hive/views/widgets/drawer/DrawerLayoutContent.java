// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.hive.utils.utils.DensityUtil;
import com.hive.views.R;


/**
 * Created by Admin on 2016/7/5.
 */
public class DrawerLayoutContent extends RelativeLayout implements DrawerViewWithTouch.OnMoveListener {
    private DrawerViewWithTouch mBaseDrawViewWithTouch;
    private boolean isTouchMenuLeft = false;
    private boolean isTouchMenuRight = false;
    private int DP = 0;
    private final int SHADEE_WIDTH = 16;//阴影大小；
    private int mStartColor = 0x6F000000;
    private int mEndColor = 0x00000000;
    private int mShaderWidth = 0;
    private float mPercent = 0;
    private final int LAYOUT_TOP = 0;
    private final int LAYOUT_BOTTOM = 1;
    private int mLayoutPosition = LAYOUT_TOP;
    private boolean mInterceptTouchEnable = false;

    public DrawerLayoutContent(Context context) {
        super(context);
        initView(null);
    }

    public DrawerLayoutContent(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public DrawerLayoutContent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        DP = DensityUtil.dip2px( 1);
        mShaderWidth = SHADEE_WIDTH * DP;
        if (attrs == null) return;
        initParment(attrs);
        if (mBaseDrawViewWithTouch == null)
            mBaseDrawViewWithTouch = getDrawerMenu();
    }

    private void initParment(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.DrawerLayoutContent, 0, 0);
        mEndColor = a.getInt(R.styleable.DrawerLayoutContent_shaderEndColor, 0x00000000);
        mStartColor = a.getColor(R.styleable.DrawerLayoutContent_shaderStartColor, 0x6F000000);
        mShaderWidth = (int) a.getDimension(R.styleable.DrawerLayoutContent_shaderWidth, 0);
        mLayoutPosition = a.getInt(R.styleable.DrawerLayoutContent_layoutPosition, LAYOUT_TOP);
        if (mLayoutPosition == 0) {
            mPercent = 1;
        } else {
            mPercent = 0;
        }
        a.recycle();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mBaseDrawViewWithTouch == null)
            mBaseDrawViewWithTouch = getDrawerMenu();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchMenuLeft = event.getRawX() < getMeasuredWidth() / 30;
                isTouchMenuRight = event.getRawX() > getMeasuredWidth() - getMeasuredWidth() / 30;
                if (proformClick(event)) {
                    return false;
                }
                break;
        }
        if (mBaseDrawViewWithTouch.mStateDef == DrawerView.STATE.LEFT) {
            return isTouchMenuLeft ? mBaseDrawViewWithTouch.dispatchTouchEvent(event) : super.dispatchTouchEvent(event);
        } else {
            return isTouchMenuRight ? mBaseDrawViewWithTouch.dispatchTouchEvent(event) : super.dispatchTouchEvent(event);
        }
    }

    private boolean proformClick(MotionEvent event) {
        //如果点击菜单外部区域；
        if (!mBaseDrawViewWithTouch.getOutRect().contains((int) event.getRawX(), (int) event.getRawY())) {
            if (mOnOutRegionClickListener != null)
                return mOnOutRegionClickListener.onClick(event);
            else
                return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mInterceptTouchEnable;
    }

    private DrawerViewWithTouch getDrawerMenu() {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof DrawerViewWithTouch) {
                DrawerViewWithTouch view = (DrawerViewWithTouch) getChildAt(i);
                view.setOnMoveListener(this);
                return view;
            }
        }
        return null;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int layoutId = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);
        drawBg(canvas);
        drawShader(canvas);
        canvas.restoreToCount(layoutId);
    }

    private void drawBg(Canvas canvas) {
        if (mBaseDrawViewWithTouch == null) {
            mBaseDrawViewWithTouch = getDrawerMenu();
        }
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        Rect viewRect = mBaseDrawViewWithTouch.getOutRect();
        if (mLayoutPosition == LAYOUT_BOTTOM) {
            p.setAlpha((int) (150 * mPercent));
        } else if (mLayoutPosition == LAYOUT_TOP) {
            p.setAlpha((int) (150 * (1f - mPercent)));
        } else {
            p.setAlpha(0);
        }
        Rect r = new Rect();
        this.getHitRect(r);
        r.set(r.left, r.top, viewRect.left, viewRect.bottom);
        canvas.drawRect(r, p);

        this.getHitRect(r);
        r.set(viewRect.right, viewRect.top, r.right, r.bottom);
        canvas.drawRect(r, p);
    }

    private void drawShader(Canvas canvas) {
        if (mBaseDrawViewWithTouch == null) {
            mBaseDrawViewWithTouch = getDrawerMenu();
        }
        Rect r = mBaseDrawViewWithTouch.getOutRect();
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);

        Rect r1 = new Rect(r.left - mShaderWidth, r.top, r.left, r.bottom);
        LinearGradient lg1 = new LinearGradient(r1.left + mShaderWidth, 0, r1.left, 0, mStartColor, mEndColor, Shader.TileMode.REPEAT);
        p.setShader(lg1);
        canvas.drawRect(r1, p);

        Rect r2 = new Rect(r.right, r.top, r.right + mShaderWidth, r.bottom);
        LinearGradient lg2 = new LinearGradient(r2.right, 0, r2.right + mShaderWidth, 0, mStartColor, mEndColor, Shader.TileMode.REPEAT);
        p.setShader(lg2);
        canvas.drawRect(r2, p);

    }

    @Override
    public void onMove(float percent) {
        if (percent > 1.0 || percent < 0) {
            if (mLayoutPosition == LAYOUT_BOTTOM) {
                this.mPercent = 0f;
            } else if (mLayoutPosition == LAYOUT_TOP) {
                this.mPercent = 1.0f;
            } else {
                this.mPercent = 0f;
            }
            return;
        }
        this.mPercent = 1f - percent;

    }

    public boolean ismInterceptTouchEnable() {
        return mInterceptTouchEnable;
    }

    public void setmInterceptTouchEnable(boolean mInterceptTouchEnable) {
        this.mInterceptTouchEnable = mInterceptTouchEnable;
    }

    private OnOutRegionClickListener mOnOutRegionClickListener;

    public void setmOnOutRegionClickListener(OnOutRegionClickListener mOnOutRegionClickListener) {
        this.mOnOutRegionClickListener = mOnOutRegionClickListener;
    }

    public interface OnOutRegionClickListener {
        public boolean onClick(MotionEvent ev);
    }
}
