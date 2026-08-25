// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.text.TextPaint
import android.view.animation.LinearInterpolator
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView
import com.hive.utils.GlobalApp
import com.hive.utils.extends.color

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
class ScriptItemFastClickView : ScriptItemView() {

    private var mAnimPos: Int = 0
    private var mAnimRunning = false

    private var mTextPaint = TextPaint().apply {
        textSize = GlobalApp.dp2px(12)
        color = 0x5f000000
        isAntiAlias = true
    }

    private val mTextBias =
        (mTextPaint.fontMetrics.descent - mTextPaint.fontMetrics.ascent) / 2 - mTextPaint.fontMetrics.descent

    override fun onDraw(canvas: Canvas) {
        val cmd = command as CmdRepeatTap
        if (mAnimRunning) {
            ScriptCommonDrawer.drawCircle(
                canvas, cmd.actualX.toFloat(), cmd.actualY.toFloat(), 255,
                com.hive.i8n.R.color.colorAccent.color()
            )
            val text = "$mAnimPos"
            canvas.drawText(
                text,
                cmd.actualX.toFloat() - mTextPaint.measureText(text) / 2,
                cmd.actualY.toFloat() + mTextBias,
                mTextPaint
            )
        } else {
            mTextPaint.alpha = (getAlphaValueByIndex() * 255).toInt()
            ScriptCommonDrawer.drawCircle(
                canvas,
                cmd.actualX.toFloat(),
                cmd.actualY.toFloat(),
                mTextPaint.alpha,
                com.hive.i8n.R.color.colorAccent.color()
            )
            val text = "$index"
            canvas.drawText(
                text,
                cmd.actualX.toFloat() - mTextPaint.measureText(text) / 2,
                cmd.actualY.toFloat() + mTextBias,
                mTextPaint
            )
        }
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        super.onExecuteUpdate(type, cmd, obj)
        mAnimPos = obj as Int
        mAnimRunning = true
        val cmd = command as CmdRepeatTap
        val anim = ValueAnimator.ofInt(0, cmd.count)
        anim.interpolator = LinearInterpolator()
        anim.duration = cmd.gap / 2
        anim.addUpdateListener {
            mAnimRunning = true
            parentView?.invalidate()
        }
        anim.start()
    }


    override fun onExecuteEnd(cmd: ScriptCommand) {
        mAnimRunning = false
        parentView?.postInvalidate()
    }
}