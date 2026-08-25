// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.graphics.Canvas
import android.graphics.Rect
import android.text.TextPaint
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClick
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.preview.ScriptItemView
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
class ScriptItemClickView : ScriptItemView() {

    private var mAnimRunning = false

    private var mTextPaint = TextPaint().apply {
        textSize = GlobalApp.dp2px(12)
        color = 0x5f000000
        isAntiAlias = true
    }

    private var limitRect: Rect? = null

    private val mTextBias =
        (mTextPaint.fontMetrics.descent - mTextPaint.fontMetrics.ascent) / 2 - mTextPaint.fontMetrics.descent

    override fun onDraw(canvas: Canvas) {
        val cmd = command as CmdClick
        limitRect = ScriptCommonUtils.covertToScreenRect(cmd.limitRect)
        if (mAnimRunning) {
            mTextPaint.alpha = 255
            limitRect?.run {
                ScriptCommonDrawer.drawLimitRect(canvas, this, (1f * 255).toInt())
            }
            ScriptCommonDrawer.drawCircle(canvas, cmd.actualX.toFloat(), cmd.actualY.toFloat(), 255)
            val text = "$index"
            canvas.drawText(
                text,
                cmd.actualX.toFloat() - mTextPaint.measureText(text) / 2,
                cmd.actualY.toFloat() + mTextBias,
                mTextPaint
            )
        } else {
            val p = getAlphaValueByIndex()
            mTextPaint.alpha = (p * 255).toInt()
            limitRect?.run {
                ScriptCommonDrawer.drawLimitRect(canvas, this, (p * 255).toInt())
            }

            ScriptCommonDrawer.drawCircle(
                canvas,
                cmd.actualX.toFloat(),
                cmd.actualY.toFloat(),
                mTextPaint.alpha
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
        mAnimRunning = true
        parentView?.postInvalidate()
    }


    override fun onExecuteEnd(cmd: ScriptCommand) {
        mAnimRunning = false
        parentView?.postInvalidate()
    }
}