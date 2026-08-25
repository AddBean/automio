// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.graphics.Canvas
import android.graphics.Paint

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/20
 */
open class PreviewSeekBarPlayingDrawer : AbsPreviewSeekBarDrawer() {

    private val Color_Background = 0x33B8BCF1
    private val Color_Foreground = 0xffFFFFFF.toInt()
    private val Line_Width = 1f * dp
    private var paint = Paint().apply {
        this.style = Paint.Style.STROKE
        this.color = Color_Background
        this.strokeWidth = Line_Width
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }
    private val paintProgress = Paint().apply {
        this.style = Paint.Style.STROKE
        this.color = Color_Foreground
        this.strokeWidth = Line_Width
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {
        drawBgLine(canvas)
        drawProgress(canvas, progress)
    }

    private fun drawBgLine(canvas: Canvas) {
        canvas.drawLine(
            insetRect.left,
            insetRect.centerY(),
            insetRect.right,
            insetRect.centerY(),
            paint
        )
    }

    private fun drawProgress(canvas: Canvas, progress: Float) {
        val progress = ((insetRect.right - insetRect.left) * progress).toInt()
        canvas.drawLine(
            insetRect.left,
            insetRect.centerY(),
            insetRect.left + progress,
            insetRect.centerY(),
            paintProgress
        )
    }
}