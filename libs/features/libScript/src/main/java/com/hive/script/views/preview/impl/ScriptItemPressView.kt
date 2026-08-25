// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.graphics.Canvas
import android.text.TextPaint
import com.hive.script.cmd.CmdPress
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.views.preview.ScriptItemView
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptItemPressView : ScriptItemView() {


    private var mTextPaint = TextPaint().apply {
        textSize = GlobalApp.dp2px(12)
        isAntiAlias=true
        color = 0x5f000000
    }

    private val mTextBias = (mTextPaint.fontMetrics.descent - mTextPaint.fontMetrics.ascent) / 2 - mTextPaint.fontMetrics.descent

    override fun onDraw(canvas: Canvas) {
        val cmd = command as CmdPress

        mTextPaint.alpha = (getAlphaValueByIndex() * 255).toInt()

        ScriptCommonDrawer.drawCircle(
            canvas,
            cmd.actualX.toFloat(),
            cmd.actualY.toFloat(),
            mTextPaint.alpha,
            0xff00ff00.toInt()
        )

        val text = "$index"
        canvas.drawText(text, cmd.actualX.toFloat() - mTextPaint.measureText(text) / 2, cmd.actualY.toFloat() + mTextBias, mTextPaint)
    }

}