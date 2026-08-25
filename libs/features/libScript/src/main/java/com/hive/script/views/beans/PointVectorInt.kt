// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.beans

/**
 *
 * @author jiadou
 * @date 2021/10/24
 */
data class PointVectorInt(
    var fromX: Int = 0, var fromY: Int = 0, var toX: Int = 0, var toY: Int = 0
) {

    fun set(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        this.fromX = fromX
        this.fromY = fromY
        this.toX = toX
        this.toY = toY
    }

    fun set(v: PointVectorInt) {
        this.fromX = v.fromX
        this.fromY = v.fromY
        this.toX = v.toX
        this.toY = v.toY
    }

}