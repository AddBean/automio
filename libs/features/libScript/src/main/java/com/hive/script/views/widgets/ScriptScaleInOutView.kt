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
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.extensions.scaleVectorInRect
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.beans.PointVectorInt
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/16/21
 */
class ScriptScaleInOutView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var previewPoints: List<PointVectorInt>? = null
    private val canvasRect = Rect()

    private var dp = GlobalApp.DP

    var mCmdScale: CmdPinchZoom? = null

    private var touchRadius = 56 * dp

    var enableTouch = true

    private var touchLast = PointF()

    private var touchType = -1

    private var pointList = mutableListOf(PointVectorInt(), PointVectorInt())

    fun loadCmd(cmd: CmdPinchZoom) {
        mCmdScale = cmd
        mCmdScale?.updateNormalizedXY()
        updatePreviewPoints()
        invalidate()
        onCommandReadyListener?.onCommandReady(cmd)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePreviewPoints()
        invalidate()
    }

    private fun updatePreviewPoints() {
        if (measuredWidth > 0) {
            canvasRect.set(0, 0, measuredWidth, measuredHeight)
            mCmdScale?.run {
                pointList[0].set(
                    actualX1,
                    actualY1,
                    actualCX,
                    actualCY
                )
                pointList[1].set(
                    actualX2,
                    actualY2,
                    actualCX,
                    actualCY
                )
            }
            previewPoints = pointList.scaleVectorInRect(canvasRect)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.run {
            mCmdScale?.run {

                val degrees1 = if (action == CmdPinchZoom.ACTION_SCALE_OUT) {
                    180
                } else {
                    0
                }
                val degrees2 = if (action == CmdPinchZoom.ACTION_SCALE_OUT) {
                    180
                } else {
                    0
                }
                pointList[0].set(
                    actualX1,
                    actualY1,
                    actualCX,
                    actualCY
                )
                pointList[1].set(
                    actualX2,
                    actualY2,
                    actualCX,
                    actualCY
                )
                if (enableTouch) {
                    ScriptCommonDrawer.drawGuideArrLine(
                        canvas,
                        pointList[0].fromX,
                        pointList[0].fromY,
                        pointList[0].toX,
                        pointList[0].toY,
                        degrees1
                    )
                    ScriptCommonDrawer.drawGuideArrLine(
                        canvas,
                        pointList[1].fromX,
                        pointList[1].fromY,
                        pointList[1].toX,
                        pointList[1].toY,
                        degrees2
                    )

                    ScriptCommonDrawer.drawTouchDot(canvas, actualX1, actualY1)

                    ScriptCommonDrawer.drawTouchDot(canvas, actualX2, actualY2)

                    ScriptCommonDrawer.drawTouchDot(canvas, actualCX, actualCY)

                } else {
                    //缩放到rect中间
                    previewPoints?.run {
                        ScriptCommonDrawer.drawGuideArrLine(
                            canvas,
                            this[0].fromX,
                            this[0].fromY,
                            this[0].toX,
                            this[0].toY,
                            degrees1
                        )

                        ScriptCommonDrawer.drawGuideArrLine(
                            canvas,
                            this[1].fromX,
                            this[1].fromY,
                            this[1].toX,
                            this[1].toY,
                            degrees2
                        )

                        ScriptCommonDrawer.drawTouchPreviewDot(canvas, this[0].fromX, this[0].fromY)

                        ScriptCommonDrawer.drawTouchPreviewDot(canvas, this[1].fromX, this[1].fromY)

                        ScriptCommonDrawer.drawTouchPreviewDot(canvas, this[0].toX, this[0].toY)

                        ScriptCommonDrawer.drawTouchPreviewDot(canvas, this[1].toX, this[1].toY)
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!enableTouch) return false
        mCmdScale?.run {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchLast.x = event.x
                    touchLast.y = event.y
                    touchType = when {
                        isTouchPoint(actualX1, actualY1, event) -> 0
                        isTouchPoint(actualX2, actualY2, event) -> 1
                        isTouchPoint(actualCX, actualCY, event) -> 2
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
                            actualX1 = (actualX1 + dx).toInt()
                            actualY1 = (actualY1 + dy).toInt()
                        }

                        1 -> {
                            actualX2 = (actualX2 + dx).toInt()
                            actualY2 = (actualY2 + dy).toInt()
                        }

                        2 -> {
                            actualX1 = (actualX1 + dx).toInt()
                            actualY1 = (actualY1 + dy).toInt()
                            actualX2 = (actualX2 + dx).toInt()
                            actualY2 = (actualY2 + dy).toInt()
                            actualCX = (actualCX + dx).toInt()
                            actualCY = (actualCY + dy).toInt()
                        }
                    }
                }
            }

            checkXY()

            updateNormalizedXY()
        }


        invalidate()
        return true
    }

    private fun checkXY() {
        mCmdScale?.run {
            if (actualX1 < 0) actualX1 = 0
            if (actualX2 < 0) actualX2 = 0
            if (actualCX < 0) actualCX = 0

            if (actualY1 < 0) actualY1 = 0
            if (actualY2 < 0) actualY2 = 0
            if (actualCY < 0) actualCY = 0

            if (actualX1 > width) actualX1 = width
            if (actualX2 > width) actualX2 = width
            if (actualCX > width) actualCX = width

            if (actualY1 > height) actualY1 = height
            if (actualY2 > height) actualY2 = height
            if (actualCY > height) actualCY = height
        }
    }

    private fun isTouchPoint(x: Int, y: Int, e: MotionEvent): Boolean {
        val r = Rect(x, y, x, y)
        r.inset(-touchRadius / 2, -touchRadius / 2)
        return r.contains(e.x.toInt(), e.y.toInt())
    }

    fun loadCmd2(cmd: CmdPinchZoom, sw: Int, sh: Int) {
        mCmdScale = CmdPinchZoom()
        cmd.run {
            mCmdScale?.action = action
            mutableListOf<Point>().apply {
                add(Point(actualX1, actualY1))
                add(Point(actualX2, actualY2))
                add(Point(actualCX, actualCY))
            }.run {
                val maxX = maxByOrNull { it.x }?.x ?: 0
                val maxY = maxByOrNull { it.y }?.y ?: 0
                val minX = minByOrNull { it.x }?.x ?: 0
                val minY = minByOrNull { it.y }?.y ?: 0
                var dw = maxX - minX.toFloat()
                var dh = maxY - minY.toFloat()
                val margin = GlobalApp.dp2px(20)
                var w = sw - margin * 3f
                var h = sh - margin * 3f

                if (dw < 1) dw = 1f
                if (dh < 1) dh = 1f

                if (dw < dh) {
                    val w1 = h * (dw / dh)
                    if (w1 > w) {
                        h = w * (dh / dw)
                    } else {
                        w = w1
                    }
                } else {
                    val h1 = w * (dh / dw)
                    if (h1 > h) {
                        w = h * (dw / dh)
                    } else {
                        h = h1
                    }
                }

                mCmdScale?.actualX1 = (margin + (this[0].x.toFloat() - minX) * w / dw).toInt()

                mCmdScale?.actualX2 = (margin + (this[1].x.toFloat() - minX) * w / dw).toInt()

                mCmdScale?.actualCX = (margin + (this[2].x.toFloat() - minX) * w / dw).toInt()

                mCmdScale?.actualY1 = (margin + ((this[0].y.toFloat() - minY) * h / dh)).toInt()

                mCmdScale?.actualY2 = (margin + (this[1].y.toFloat() - minY) * h / dh).toInt()

                mCmdScale?.actualCY = (margin + (this[2].y.toFloat() - minY) * h / dh).toInt()
                updateNormalizedXY()
            }
        }
        updatePreviewPoints()
        invalidate()
    }

    var onCommandReadyListener: OnCommandReadyListener? = null

    interface OnCommandReadyListener {
        fun onCommandReady(cmd: CmdPinchZoom)
    }
}