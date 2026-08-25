// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.effect_text;


import android.graphics.Point;

/**
 * Created by AddBean on 2016/10/20.
 */

public class EffectTextDot {
    private int mSize;
    private int mColor;
    private Point mLoc;
    private float mAlpha;
    private int mState;

    public int getmState() {
        return mState;
    }

    public void setmState(int mState) {
        this.mState = mState;
    }

    public EffectTextDot(int mSize, int mColor, Point mLoc, float mAlpha, int mState) {
        this.mSize = mSize;
        this.mColor = mColor;
        this.mLoc = mLoc;
        this.mAlpha = mAlpha;
        this.mState=mState;
    }

    public float getmAlpha() {
        return mAlpha;
    }

    public void setmAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    public int getmSize() {
        return mSize;
    }

    public void setmSize(int mSize) {
        this.mSize = mSize;
    }

    public int getmColor() {
        return mColor;
    }

    public void setmColor(int mColor) {
        this.mColor = mColor;
    }

    public Point getmLoc() {
        return mLoc;
    }

    public void setmLoc(Point mLoc) {
        this.mLoc = mLoc;
    }
}