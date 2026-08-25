// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.text.TextPaint
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView
import com.hive.utils.GlobalApp
import kotlin.math.min

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
class ScriptItemScrollMultipleView : ScriptItemView() {

    private var mPointPath = Path()
    private var mAnimRunning = false
    private var mAnimPosMap = mutableMapOf<Int, Int>()

    private var mPaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = GlobalApp.dp2px(2)
    }

    private var mTextPaint = TextPaint().apply {
        textSize = GlobalApp.dp2px(12)
        color = Color.WHITE
    }


    override fun onDraw(canvas: Canvas) {
        val cmd = command as CmdScrollMultiple
        if (mAnimRunning) {
            cmd.pointList.forEach {
                val curIndex = mAnimPosMap[it] ?: -1
                if (curIndex > -1 && curIndex < (cmd.actualPoints[it]?.size ?: 0)) {
                    ScriptCommonDrawer.drawCircle(
                        canvas,
                        cmd.actualPoints[it]?.get(curIndex)?.x?.toFloat() ?: 0f,
                        cmd.actualPoints[it]?.get(curIndex)?.y?.toFloat() ?: 0f,
                        255
                    )
                    drawTouchTrack(canvas, cmd.actualPoints[it], curIndex)
                }
            }

        } else {
            mPaint.alpha = (getAlphaValueByIndex() * 255).toInt()
            mTextPaint.alpha = mPaint.alpha
            mPointPath.reset()
            cmd.pointList.forEach {
                cmd.actualPoints[it]?.run {
                    drawTouchTrack(canvas, this, this.size - 1)
                }
            }
        }
    }

    private fun drawTouchTrack(canvas: Canvas, points: MutableList<Point>?, curIndex: Int) {
        points ?: return
        val minIndex = min(points.size - 1, curIndex)
        mPointPath.reset()
        for (i in 0 until minIndex) {
            if (i == 0)
                mPointPath.moveTo(
                    points[i].x.toFloat(),
                    points[i].y.toFloat()
                )
            else
                mPointPath.lineTo(
                    points[i].x.toFloat(),
                    points[i].y.toFloat()
                )
        }
        ScriptCommonDrawer.drawPath(
            canvas,
            mPointPath,
            ((mTextPaint.alpha / 255f) * 100).toInt()
        )
    }

    override fun onCommandUpdate(cmd: ScriptCommand?) {
        val cmdScroll = command as CmdScrollMultiple
        cmdScroll.updateActualPoints()
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        super.onExecuteUpdate(type, cmd, obj)
        mAnimRunning = true
        val cmd = command as CmdScrollMultiple
        cmd.pointList.forEach { key ->
            val anim = ValueAnimator.ofInt(0, cmd.actualPoints[key]?.size ?: 0)
            anim.duration = cmd.timeMap[key]?.sum() ?: 0
            mAnimPosMap[key] = -1
            anim.addUpdateListener {
                mAnimRunning = true
                mAnimPosMap[key] = it.animatedValue as Int
                parentView?.invalidate()
            }
            anim.startDelay = (cmd.startTimeMap[key] ?: 0) as Long
            anim.start()
        }
        parentView?.postInvalidate()
    }

    override fun onExecuteEnd(cmd: ScriptCommand) {
        mAnimRunning = false
        parentView?.postInvalidate()
    }


}