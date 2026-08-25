// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends


/**
 * 保留指定小数位数
 */
fun Float.toDigits(count: Int): Float {
    val factor = Math.pow(10.0, count.toDouble()).toFloat()
    return (this * factor).toInt() / factor
}

