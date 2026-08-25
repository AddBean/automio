// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.view.View;

/**
 * 防止快速重复点击
 */
public abstract class OnClickFilteListener implements View.OnClickListener {
    private static final int THROTTLE_TIME_DEFAULT = 200; // 0.2s

    private long mLastClickTime = 0, mThrottleTime = THROTTLE_TIME_DEFAULT;

    public OnClickFilteListener() {
    }

    public OnClickFilteListener(long throttleTime) {
        mThrottleTime = throttleTime;
    }

    public final long getThrottleTime() {
        return mThrottleTime;
    }

    public abstract void throttleClick(View view);

    @Override
    public void onClick(View v) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastClickTime < getThrottleTime()) {
            return;
        }
        mLastClickTime = currentTime;
        throttleClick(v);
    }


    public static final class ViewFastClickCheck {

        private static long lastClickTime;

        /**
         * 判断是否是快速点击
         *
         * @return
         */
        public static boolean isFastClick() {
            boolean flag = false;
            long curClickTime = System.currentTimeMillis();
            if ((curClickTime - lastClickTime) <= THROTTLE_TIME_DEFAULT) {
                flag = true;
            }
            lastClickTime = curClickTime;
            return flag;
        }
    }

}
