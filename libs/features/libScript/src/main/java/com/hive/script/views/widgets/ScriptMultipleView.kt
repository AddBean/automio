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
import com.hive.script.cmd.CmdPinch
import com.hive.script.extensions.scaleVectorInRect
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.beans.PointVectorInt
import com.hive.utils.GlobalApp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/16/21
 */
class ScriptMultipleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var previewPoints: List<PointVectorInt>? = null

    private var dp = GlobalApp.DP

    private val rectSize = 48 * dp

    private var lRect = Rect(0, 0, rectSize, rectSize)

    private var rRect = Rect(0, 0, rectSize, rectSize)

    private var tRect = Rect(0, 0, rectSize, rectSize)

    private var bRect = Rect(0, 0, rectSize, rectSize)

    private var cRect = Rect(0, 0, rectSize, rectSize)

    private var mTouchFlag = -1

    var mCmdMultiple: CmdPinch? = null

    var enableTouch: Boolean = true

    var onCommandLoadListener: OnCommandLoadListener? = null

    private var canvasRect = Rect()

    fun loadCmd(cmd: CmdPinch) {
        mCmdMultiple = cmd
        mCmdMultiple?.updateActualXY()
        calculation()
        updatePreviewPoints()
        invalidate()
        onCommandLoadListener?.onCommandLoaded(cmd)
    }

    fun loadCmd2(cmd: CmdPinch, sw: Int, sh: Int) {
        mCmdMultiple = CmdPinch()
        cmd.fingerDistance =
            if (cmd.fingerCount > 1) (min(sw, sh) * 2 / ((cmd.fingerCount - 1) * 3)) else 0
        mCmdMultiple?.actualX2 = cmd.actualX2
        mCmdMultiple?.actualY2 = cmd.actualY2
        mCmdMultiple?.actualX1 = cmd.actualX1
        mCmdMultiple?.actualY1 = cmd.actualY1
        mCmdMultiple?.duration = cmd.duration
        mCmdMultiple?.fingerCount = cmd.fingerCount
        mCmdMultiple?.fingerDistance = cmd.fingerDistance
        mCmdMultiple?.updateNormalizedXY()
        calculation()
        updatePreviewPoints()
        invalidate()
    }

    private fun calculation() {
        mCmdMultiple?.let { cmd ->
            cmd.calculationVectors()
            val degree = atan2(
                (cmd.actualX2 - cmd.actualX1).toDouble(),
                (cmd.actualY2 - cmd.actualY1).toDouble()
            )

            val distance = (cmd.fingerDistance + 30 * dp) * (cmd.fingerCount - 1) / 2

            val nx1 = cmd.actualX1
            val nx2 = cmd.actualX2
            val ny1 = cmd.actualY1
            val ny2 = cmd.actualY2

            val dx = distance * cos(degree)
            val dy = distance * sin(degree)

            lRect.transRect(nx1, ny1)
            rRect.transRect(nx2, ny2)

            tRect.transRect(((nx1 + nx2) / 2 - dx).toInt(), ((ny1 + ny2) / 2 + dy).toInt())
            bRect.transRect(((nx1 + nx2) / 2 + dx).toInt(), ((ny1 + ny2) / 2 - dy).toInt())

            cRect.transRect((nx1 + nx2) / 2, (ny1 + ny2) / 2)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePreviewPoints()
    }

    private fun updatePreviewPoints() {
        if (measuredWidth > 0) {
            canvasRect.set(0, 0, measuredWidth, measuredHeight)
            previewPoints = mCmdMultiple?.getActualVector()?.scaleVectorInRect(canvasRect)
            invalidate()
        } else post {
            canvasRect.set(0, 0, measuredWidth, measuredHeight)
            previewPoints = mCmdMultiple?.getActualVector()?.scaleVectorInRect(canvasRect)
            invalidate()
        }

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.run {
            mCmdMultiple?.run {
                drawLines(canvas, this)
            }
        }
    }

    private fun drawLines(canvas: Canvas, cmd: CmdPinch) {
        if (enableTouch) {
            cmd.getActualVector().forEach {
                ScriptCommonDrawer.drawGuideArrLine(
                    canvas,
                    it.fromX,
                    it.fromY,
                    it.toX,
                    it.toY
                )
            }
        } else {
            previewPoints?.forEach {
                ScriptCommonDrawer.drawGuideArrLine(
                    canvas,
                    it.fromX,
                    it.fromY,
                    it.toX,
                    it.toY
                )
            }
        }

        if (enableTouch) {
            ScriptCommonDrawer.drawTouchDot(
                canvas,
                rRect.centerX(),
                rRect.centerY()
            )
            ScriptCommonDrawer.drawTouchDot(
                canvas,
                lRect.centerX(),
                lRect.centerY()
            )
            ScriptCommonDrawer.drawTouchDot(
                canvas,
                tRect.centerX(),
                tRect.centerY()
            )
            ScriptCommonDrawer.drawTouchDot(
                canvas,
                bRect.centerX(),
                bRect.centerY()
            )
            ScriptCommonDrawer.drawTouchDot(
                canvas,
                cRect.centerX(),
                cRect.centerY()
            )
        }
    }

    private var lastPoint = PointF(0f, 0f)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (!enableTouch) return false
        e?.run {
            val ex = e.x.toInt()
            val ey = e.y.toInt()

            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastPoint.x = e.x
                    lastPoint.y = e.y
                    mTouchFlag = when {
                        cRect.contains(ex, ey) -> 0
                        lRect.contains(ex, ey) -> 1
                        rRect.contains(ex, ey) -> 2
                        tRect.contains(ex, ey) -> 3
                        bRect.contains(ex, ey) -> 4
                        else -> -1
                    }
                }

                else -> {
                    val dx = e.x - lastPoint.x
                    val dy = e.y - lastPoint.y
                    mCmdMultiple?.run {
                        when (mTouchFlag) {
                            0 -> {
                                actualX1 = (actualX1 + dx).toInt()
                                actualY1 = (actualY1 + dy).toInt()
                                actualX2 = (actualX2 + dx).toInt()
                                actualY2 = (actualY2 + dy).toInt()
                            }

                            1 -> {
                                actualX1 = (actualX1 + dx).toInt()
                                actualY1 = (actualY1 + dy).toInt()
                            }

                            2 -> {
                                actualX2 = (actualX2 + dx).toInt()
                                actualY2 = (actualY2 + dy).toInt()
                            }

                            3, 4 -> {
                                val s1 = sqrt(dx * dx + dy * dy)
                                val th1 = atan2(
                                    (actualX2 - actualX1).toDouble(),
                                    (actualY2 - actualY1).toDouble()
                                )
                                val th2 = atan2(-dx, -dy)
                                val th = Math.PI / 2 - (th2 - th1)
                                var sd = s1 * cos(th)
                                sd = if (mTouchFlag == 3) {
                                    sd
                                } else {
                                    -sd
                                }
                                val td = (fingerDistance + sd / ((fingerCount - 1) / 2f)).toInt()
                                if (td > 0) {
                                    fingerDistance = td
                                }
                            }

                        }
                    }

                    lastPoint.x = e.x
                    lastPoint.y = e.y
                }
            }
        }
        mCmdMultiple?.updateNormalizedXY()
        calculation()
        invalidate()
        return true
    }

    private fun Rect.transRect(x: Int, y: Int) {
        val w = this.width()
        val h = this.height()
        this.left = x - (w / 2)
        this.right = x + (w / 2)
        this.top = y - (h / 2)
        this.bottom = y + (h / 2)
    }

    fun changeFingerCount(count: Int) {
        mCmdMultiple?.fingerCount = count
        loadCmd(mCmdMultiple!!)
    }

    interface OnCommandLoadListener {
        fun onCommandLoaded(cmd: CmdPinch)
    }
}