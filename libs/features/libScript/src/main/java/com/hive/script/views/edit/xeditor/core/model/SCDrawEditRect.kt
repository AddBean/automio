// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.core.model

import android.graphics.PointF

/**
 *
 * @author jiadou
 * @date 5/7/21
 */
data class SCDrawEditRect(
    var lt: PointF = PointF(),
    var rt: PointF = PointF(),
    var lb: PointF = PointF(),
    var rb: PointF = PointF()
) {
    fun inset(insetX: Float, insetY: Float) {
        lt.offset(insetX, insetY)
        rt.offset(-insetX, insetY)
        lb.offset(insetX, -insetY)
        rb.offset(-insetX, -insetY)
    }

    fun centerX(): Float = lt.x + (rt.x - lt.x) / 2f

    fun centerY(): Float = lt.y + (lb.y - lt.y) / 2f

    fun toRectF(): android.graphics.RectF {
        return android.graphics.RectF(lt.x, lt.y, rb.x, rb.y)
    }

    fun toRect(): android.graphics.Rect {
        return android.graphics.Rect(lt.x.toInt(), lt.y.toInt(), rb.x.toInt(), rb.y.toInt())
    }

    fun set(lt: PointF, rt: PointF, lb: PointF, rb: PointF) {
        this.lt.set(lt)
        this.rt.set(rt)
        this.lb.set(lb)
        this.rb.set(rb)
    }

    fun set(rect: android.graphics.RectF) {
        this.lt.set(rect.left, rect.top)
        this.rt.set(rect.right, rect.top)
        this.lb.set(rect.left, rect.bottom)
        this.rb.set(rect.right, rect.bottom)
    }

    fun set(rect: android.graphics.Rect) {
        this.lt.set(rect.left.toFloat(), rect.top.toFloat())
        this.rt.set(rect.right.toFloat(), rect.top.toFloat())
        this.lb.set(rect.left.toFloat(), rect.bottom.toFloat())
        this.rb.set(rect.right.toFloat(), rect.bottom.toFloat())
    }
}


