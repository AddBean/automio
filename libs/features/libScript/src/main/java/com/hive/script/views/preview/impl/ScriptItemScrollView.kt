// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.text.TextPaint
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdScroll
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptItemScrollView : ScriptItemView() {

    private var mPointPath = Path()
    private var mAnimRunning = false
    private var mAnimPos = 0

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
        val cmd = command as CmdScroll
        if (mAnimRunning) {
            val curIndex = mAnimPos
            if (curIndex < cmd.actualPoints.size) {
                ScriptCommonDrawer.drawCircle(
                    canvas,
                    cmd.actualPoints[curIndex].x.toFloat(),
                    cmd.actualPoints[curIndex].y.toFloat(),
                    255
                )
            }
            drawTouchTrack(canvas, cmd, curIndex)
        } else {
            val curIndex = cmd.actualPoints.size - 1
            drawTouchTrack(canvas, cmd, curIndex)
        }
    }

    private fun drawTouchTrack(canvas: Canvas, cmd: CmdScroll, curIndex: Int) {
        mPaint.alpha = (getAlphaValueByIndex() * 255).toInt()
        mTextPaint.alpha = mPaint.alpha
        mPointPath.reset()
        for (i in 0 until curIndex) {
            if (i == 0)
                mPointPath.moveTo(
                    cmd.actualPoints[i].x.toFloat(),
                    cmd.actualPoints[i].y.toFloat()
                )
            else
                mPointPath.lineTo(
                    cmd.actualPoints[i].x.toFloat(),
                    cmd.actualPoints[i].y.toFloat()
                )
        }
        ScriptCommonDrawer.drawPath(
            canvas,
            mPointPath,
            ((mTextPaint.alpha / 255f) * 100).toInt()
        )
    }

    override fun onCommandUpdate(cmd: ScriptCommand?) {
        val cmdScroll = command as CmdScroll
        cmdScroll.updateActualPoints()
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        super.onExecuteUpdate(type, cmd, obj)
        mAnimRunning = true
        parentView?.postInvalidate()
        val cmd = command as CmdScroll
        val anim = ValueAnimator.ofInt(0, cmd.actualPoints.size)
        anim.duration = cmd.duration
        mAnimPos = 0
        anim.addUpdateListener {
            mAnimRunning = true
            mAnimPos = it.animatedValue as Int
            parentView?.invalidate()
        }
        anim.start()
    }

    override fun onExecuteEnd(cmd: ScriptCommand) {
        mAnimRunning = false
        parentView?.postInvalidate()
    }


}