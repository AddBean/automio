// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.logger

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.hive.script.base.ScriptCommand
import com.hive.utils.extends.dpf
import com.hive.utils.extends.string
import com.hive.views.widgets.TextDrawableView

class ScriptLoggerNextView(context: Context?, attrs: AttributeSet?) :
    TextDrawableView(context, attrs) {

    private var duration: Long = 0

    private var leftDuration: Long = 0

    private var animator: ValueAnimator? = null

//    private val rectPath = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        drawProgress(canvas)
        super.onDraw(canvas)
    }

    private fun drawProgress(canvas: Canvas) {
//        canvas.save()
        val progression = leftDuration.toFloat() / duration
        val w = width * progression
        paint.color = 0xff018671.toInt()
        val r = 12.dpf()
        //使用xfermode src_out模式绘制仅有左上角的圆角矩形，其他为直角
        canvas.saveLayer(0f, 0f, w, height.toFloat(), paint)

        //绘制左上角的圆角path,其他为直角
        val path = android.graphics.Path()
        path.addRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f),
            android.graphics.Path.Direction.CW
        )
        canvas.drawPath(path, paint)
        paint.xfermode =
            android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, w, height.toFloat(), paint)
        paint.xfermode = null

        canvas.restore()
//        rectPath.reset()
//        rectPath.addRoundRect(
//            0f,
//            0f,
//            width,
//            height.toFloat(),
//            floatArrayOf(r, r, 0f, 0f, 0f, 0f, 0f, 0f),
//            android.graphics.Path.Direction.CW
//        )
//        canvas.clipPath(rectPath)
//        canvas.drawRect(0f, 0f, width, height.toFloat(), paint)
//        canvas.restore()
    }

    /**
     * 下一条命令
     */
    fun nextCommand(cmd: ScriptCommand, leftDuration: Long) {
        this.duration = leftDuration
        this.leftDuration = leftDuration
        text = com.hive.i8n.R.string.sc_next_cmd_info.string(cmd.getCommandName())
        animator?.cancel()
        animator = null
        //动画
        ValueAnimator.ofFloat(0f, 1f).apply {
            animator = this
            duration = leftDuration
            addUpdateListener {
                this@ScriptLoggerNextView.leftDuration =
                    (leftDuration - it.animatedFraction * leftDuration).toLong()
                invalidate()
            }
            start()
        }
    }
}