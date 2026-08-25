// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.Field;
public class SpeedViewPager extends ViewPager {
    public SpeedViewPager(@NonNull Context context) {
        super(context);
        initView();
    }

    public SpeedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        fixScrollerSpeed(700);
    }


    /**
     * 通过反射来修改 ViewPager的mScroller属性
     */
    public void fixScrollerSpeed(int animDuration) {
        try {
            Class clazz = Class.forName("androidx.viewpager.widget.ViewPager");
            Field f = clazz.getDeclaredField("mScroller");
            InfiniteSpeedScroller fixedSpeedScroller = new InfiniteSpeedScroller(getContext(), new LinearOutSlowInInterpolator());
            fixedSpeedScroller.setFixedDuration(animDuration);
            f.setAccessible(true);
            f.set(this, fixedSpeedScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean isScrollable = true;

    public void setTouchScrollable(boolean scrollable) {
        isScrollable = scrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return isScrollable && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isScrollable && super.onInterceptTouchEvent(ev);
    }
}
