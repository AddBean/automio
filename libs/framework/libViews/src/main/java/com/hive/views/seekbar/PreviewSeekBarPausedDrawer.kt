// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/20
 */
class PreviewSeekBarPausedDrawer(icon: Int) : AbsPreviewSeekBarDrawer() {
    private val Color_Background = 0x1AB8BCF1.toInt()
    private var Color_Foreground = 0xffFF6490.toInt()
    private val Line_Width = 3f * dp
    private val iconBmp = BitmapFactory.decodeResource(
        GlobalApp.getContext()?.resources,
        icon
    )
    private var paintBg = Paint().apply {
        this.style = Paint.Style.STROKE
        this.color = Color_Background
        this.style = Paint.Style.FILL
        this.strokeWidth = 1f
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }
    private var paintDot = Paint().apply {
        this.strokeWidth = Line_Width
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
        this.style = Paint.Style.FILL
        this.color = Color_Foreground
        this.strokeCap = Paint.Cap.ROUND
    }
    private val paintProgress = Paint().apply {
        this.style = Paint.Style.STROKE
        this.color = Color_Foreground
        this.strokeWidth = 1f
        this.style = Paint.Style.FILL
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {
        drawBgLine(canvas)
        drawProgress(canvas, progress)
        drawDot(canvas, progress)
    }

    private fun drawBgLine(canvas: Canvas) {
        canvas.drawLine(
            insetRect.left,
            insetRect.centerY(),
            insetRect.right,
            insetRect.centerY(),
            paintBg
        )
    }

    private fun drawProgress(canvas: Canvas, progress: Float) {
        val progress = insetRect.left + (insetRect.width() * progress).toInt()
        drawRoundLine(
            canvas,
            insetRect.left,
            insetRect.centerY(),
            progress,
            insetRect.centerY(),
            paintProgress
        )

    }

    private fun drawDot(canvas: Canvas, progress: Float) {
        val progress = insetRect.left + (insetRect.width() * progress).toInt()
        if(iconBmp==null)return
        canvas.drawBitmap(
            iconBmp,
            progress - iconBmp.width / 2f,
            insetRect.centerY() - iconBmp.width / 2f,
            paintDot
        )
    }

    private fun drawRoundLine(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float,
        paint: Paint
    ) {
        val width = Line_Width
        canvas.drawRoundRect(
            startX,
            startY - width / 2,
            stopX,
            stopY + width / 2,
            width / 2,
            width / 2,
            paint
        )

    }
}