// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.beans

import com.hive.script.utils.ScriptCoordinateAdapter

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/24
 */
data class PointVectorFloat(
    var fromX: Float = 0f, var fromY: Float = 0f, var toX: Float = 0f, var toY: Float = 0f
) {

    fun set(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        this.fromX = fromX
        this.fromY = fromY
        this.toX = toX
        this.toY = toY
    }

    fun toRealDiffX(): Int {
        return ScriptCoordinateAdapter.get().toRealX(toX - fromX)
    }

    fun toRealDiffY(): Int {
        return ScriptCoordinateAdapter.get().toRealY(toY - fromY)
    }

    fun copy(): PointVectorFloat {
        return PointVectorFloat(fromX, fromY, toX, toY)
    }

}