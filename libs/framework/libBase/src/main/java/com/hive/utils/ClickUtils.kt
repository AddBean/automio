// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import android.os.SystemClock
import androidx.annotation.IntRange

object ClickUtils {
    private var sLastClickTime = 0L
    val MIN_DELAY_DURATION = 500

    /**
     * 用户快速点击是否在大于默认间隔内
     * 表示当前点击在最小间隔条件下是否有效
     *
     * @return
     */
    fun isFastClickable(): Boolean {
        return isFastClickable(MIN_DELAY_DURATION)
    }

    /**
     * 表示当前用户的快速点击是否有效
     *
     *
     * 当两次快速点击间隔在最小时间间隔范围内，则无效，反之有效
     *
     * @param mineDuration 最小间隔时长 毫秒单位
     * @return 当前用户的快速点击间隔时间大于mineDuration时返回true
     */
    fun isFastClickable(@IntRange(from = 0) mineDuration: Int): Boolean {
        if (mineDuration == 0) {
            return true
        }
        val currentTime = SystemClock.elapsedRealtime()
        val duration = currentTime - sLastClickTime
        if (duration > 0 && duration < mineDuration) {
            return false
        }
        sLastClickTime = currentTime
        return true
    }

}