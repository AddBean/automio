// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCoordinateAdapter
import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.beans.PointVectorInt
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.extends.dp
import com.hive.utils.extends.string
import com.hive.utils.utils.ScreenUtils

/**
 *
 * @author jiadou
 * @date 7/16/21
 */
class ScriptDragView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val canvasRect = Rect()

    private var touchRadius = 56.dp

    private var touchLast = PointF()

    private var touchType = -1

    private var vector = PointVectorInt()

    private var normalizedVector = PointVectorFloat()

    fun updateData() {
        vector.set(lastVector!!)
        updateNormalizedXY()
        postInvalidate()
    }

    fun getDragData(): PointVectorFloat {
        return normalizedVector
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

            if (ScriptRecordManager.dragViewType == ScriptRecordManager.RecordDragViewType.OFFSET) {
                ScriptCommonDrawer.drawMeasureInfo(
                    canvas,
                    vector.fromX,
                    vector.fromY,
                    vector.toX,
                    vector.toY
                )

//                ScriptCommonDrawer.drawTouchDot(canvas, vector.fromX, vector.fromY)
//
//                ScriptCommonDrawer.drawTouchDot(canvas, vector.toX, vector.toY)

                ScriptCommonDrawer.drawTextInfo(
                    canvas,
                    com.hive.i8n.R.string.sc_drag_start_1.string(),
                    vector.fromX,
                    vector.fromY
                )

                ScriptCommonDrawer.drawTextInfo(
                    canvas,
                    com.hive.i8n.R.string.sc_drag_end_1.string(),
                    vector.toX,
                    vector.toY
                )

            } else {

                ScriptCommonDrawer.drawGuideArrLine(
                    canvas,
                    vector.fromX,
                    vector.fromY,
                    vector.toX,
                    vector.toY
                )

//                ScriptCommonDrawer.drawTouchDot(canvas, vector.fromX, vector.fromY)
//
//                ScriptCommonDrawer.drawTouchDot(canvas, vector.toX, vector.toY)

                ScriptCommonDrawer.drawTextInfo(
                    canvas,
                    com.hive.i8n.R.string.sc_drag_start_2.string(),
                    vector.fromX,
                    vector.fromY
                )

                ScriptCommonDrawer.drawTextInfo(
                    canvas,
                    com.hive.i8n.R.string.sc_drag_end_2.string(),
                    vector.toX,
                    vector.toY
                )
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchLast.x = event.x
                touchLast.y = event.y
                touchType = when {
                    isTouchPoint(vector.toX, vector.toY, event) -> 0
                    isTouchPoint(vector.fromX, vector.fromY, event) -> 1
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
                        vector.toX = (vector.toX + dx).toInt()
                        vector.toY = (vector.toY + dy).toInt()
                    }

                    1 -> {
                        vector.fromX = (vector.fromX + dx).toInt()
                        vector.fromY = (vector.fromY + dy).toInt()
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
        normalizedVector.toX = ScriptCoordinateAdapter.get().toNormalizedX(vector.toX)
        normalizedVector.toY = ScriptCoordinateAdapter.get().toNormalizedY(vector.toY)
        normalizedVector.fromX = ScriptCoordinateAdapter.get().toNormalizedX(vector.fromX)
        normalizedVector.fromY = ScriptCoordinateAdapter.get().toNormalizedY(vector.fromY)
    }

    private fun checkXY() {
        if (vector.toX < 0) vector.toX = 0
        if (vector.fromX < 0) vector.fromX = 0

        if (vector.toY < 0) vector.toY = 0
        if (vector.fromY < 0) vector.fromY = 0

        if (vector.toX > width) vector.toX = width
        if (vector.fromX > width) vector.fromX = width

        if (vector.toY > height) vector.toY = height
        if (vector.fromY > height) vector.fromY = height
    }

    private fun isTouchPoint(x: Int, y: Int, e: MotionEvent): Boolean {
        val r = Rect(x, y, x, y)
        r.inset(-touchRadius / 2, -touchRadius / 2)
        return r.contains(e.x.toInt(), e.y.toInt())
    }

    companion object {

        private var lastVector: PointVectorInt? = PointVectorInt()

        fun setNormalizedVector(vector: PointVectorFloat?) {
            val sw = ScreenUtils.getScreenWidth()
            val sh = ScreenUtils.getScreenHeight()

            val length = sw / 2
            val cx = (sw - length) / 2
            val cy = sh - (sh - length) / 2
            val x1 = sw - (sw - length) / 2
            val y1 = (sh - length) / 2
            lastVector?.set(cx, cy, x1, y1)
            vector ?: return
            if (vector.fromX == 0f && vector.fromY == 0f && vector.toX == 0f && vector.toY == 0f) {
                return
            }
            lastVector?.fromX = ScriptCoordinateAdapter.get().toRealX(vector.fromX)
            lastVector?.fromY = ScriptCoordinateAdapter.get().toRealY(vector.fromY)
            lastVector?.toX = ScriptCoordinateAdapter.get().toRealX(vector.toX)
            lastVector?.toY = ScriptCoordinateAdapter.get().toRealY(vector.toY)
        }
    }
}