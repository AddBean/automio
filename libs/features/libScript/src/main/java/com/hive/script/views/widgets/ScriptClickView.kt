// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.utils.extends.dp
import com.hive.utils.utils.ScreenUtils

/**
 *
 * @author jiadou
 * @date 7/16/21
 */
class ScriptClickView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val canvasRect = Rect()

    private var touchRadius = 56.dp

    private var touchLast = PointF()

    private var touchType = -1

    private var realPoint = Point()

    private var normalizedPoint = PointF()

    fun updateData() {
        realPoint.set(lastPoint.x, lastPoint.y)
        updateNormalizedXY()
        postInvalidate()
    }

    fun getFinalPoint(): PointF {
        return normalizedPoint
    }

    private fun updateViewRect() {
        if (measuredWidth > 0) {
            canvasRect.set(0, 0, measuredWidth, measuredHeight)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewRect()
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.run {
            ScriptCommonDrawer.drawTouchCrossDot(canvas, realPoint.x, realPoint.y)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchLast.x = event.x
                touchLast.y = event.y
                touchType = when {
                    isTouchPoint(realPoint.x, realPoint.y, event) -> 0
                    else -> -1
                }
            }

            else -> {
                val dx = event!!.x - touchLast.x
                val dy = event.y - touchLast.y
                touchLast.x = event.x
                touchLast.y = event.y
                when (touchType) {
                    0 -> {
                        realPoint.x = (realPoint.x + dx).toInt()
                        realPoint.y = (realPoint.y + dy).toInt()
                    }
                }
            }
        }

        checkXY()

        updateNormalizedXY()

        invalidate()

        return true
    }

    private fun updateNormalizedXY() {
        normalizedPoint.x = ScriptCoordinateAdapter.get().toNormalizedX(realPoint.x)
        normalizedPoint.y = ScriptCoordinateAdapter.get().toNormalizedY(realPoint.y)
    }

    private fun checkXY() {
        if (realPoint.x < 0) realPoint.x = 0

        if (realPoint.y < 0) realPoint.y = 0

        if (realPoint.x > width) realPoint.x = width

        if (realPoint.y > height) realPoint.y = height
    }

    private fun isTouchPoint(x: Int, y: Int, e: MotionEvent): Boolean {
        val r = Rect(x, y, x, y)
        r.inset(-touchRadius / 2, -touchRadius / 2)
        return r.contains(e.x.toInt(), e.y.toInt())
    }

    companion object {

        private var lastPoint = Point()

        fun setNormalizedPoint(point: PointF?) {
            val sw = ScreenUtils.getScreenWidth()
            val sh = ScreenUtils.getScreenHeight()
            val cx = sw / 2
            val cy = sh / 2
            lastPoint.set(cx, cy)
            point ?: return
            if (point.x == 0f && point.y == 0f) {
                return
            }
            lastPoint.x = ScriptCoordinateAdapter.get().toRealX(point.x)
            lastPoint.y = ScriptCoordinateAdapter.get().toRealY(point.y)
        }
    }
}