// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar

import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.hive.utils.GlobalApp
import com.hive.views.BuildConfig

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/20
 */
class PreviewSeekBarTrackingDrawer(icon:Int) : AbsPreviewSeekBarDrawer() {
    private val Color_Background = 0x33B8BCF1.toInt()
    private var Color_Foreground = 0xFfFF6490.toInt()
    private var Color_Segment_Background = 0xffFFffff.toInt()
    private var Color_Segment_Foreground = 0xFfFF6490.toInt()
    private val Line_Width = 6f * dp
    private val SegmentPadding = 1f * dp
    private val iconBmp = BitmapFactory.decodeResource(
        GlobalApp.getResources(),icon
    )

    private var paintBg = Paint().apply {
        this.style = Paint.Style.FILL
        this.color = Color_Background
        this.strokeWidth = 1f
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }
    private var paintDot = Paint().apply {
        this.strokeWidth = 1f
        this.isAntiAlias = true
        this.style = Paint.Style.FILL
        this.color = Color_Foreground
        this.strokeCap = Paint.Cap.ROUND
    }

    private var paintDebugSegment = Paint().apply {
        this.strokeWidth = 1f * dp
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
        this.style = Paint.Style.FILL
        this.color = Color_Segment_Foreground
        this.strokeCap = Paint.Cap.ROUND
    }

    private var paintSegment = Paint().apply {
        this.strokeWidth = 1f
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
        this.style = Paint.Style.FILL
        this.color = Color_Segment_Foreground
        this.strokeCap = Paint.Cap.ROUND
    }

    private val paintProgress = Paint().apply {
        this.style = Paint.Style.FILL
        this.color = Color_Foreground
        this.strokeWidth = 1f
        this.strokeCap = Paint.Cap.ROUND
        this.isAntiAlias = true
    }

    private val clipPaint = Paint()

    private val leftRect = RectF()

    private val rightRect = RectF()

    override fun onDraw(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {
        drawBgLine(canvas, progress, segments)
        drawSegments(canvas, progress, segments)
        if (BuildConfig.DEBUG) {
            drawDebugSegments(canvas)
        }
        if (segments.isEmpty()) {
            drawProgress(canvas, progress)
        }
        drawDot(canvas, progress)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun drawSegments(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {
        val progressPx = (insetRect.right - insetRect.left) * progress

        rightRect.set(insetRect.left + progressPx, insetRect.top, insetRect.right, insetRect.bottom)
        var count = canvas.saveLayer(rightRect, clipPaint)
        paintSegment.color = Color_Segment_Background
        segments.forEach {
            if (it.isCanReplace) {
                val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
                val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
                drawRoundLine(
                    canvas,
                    insetRect.left + start,
                    insetRect.centerY(),
                    insetRect.left + end,
                    insetRect.centerY(),
                    paintSegment
                )
            }
        }
        canvas.restoreToCount(count)


        leftRect.set(insetRect.left, insetRect.top, insetRect.left + progressPx, insetRect.bottom)
        count = canvas.saveLayer(leftRect, clipPaint)
        paintSegment.color = Color_Segment_Foreground
        segments.forEach {
            val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
            val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
            drawRoundLine(
                canvas,
                insetRect.left + start,
                insetRect.centerY(),
                insetRect.left + end,
                insetRect.centerY(),
                paintSegment
            )
        }
        canvas.restoreToCount(count)
    }

    private fun drawDebugSegments(canvas: Canvas) {

        var originalSegments = seekBar?.getOriginalSegments()
        var index = 0;
        originalSegments?.filter { !it.isCanReplace }?.forEach {
            paintDebugSegment.color = Color.WHITE
            val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
            val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
            canvas.drawLine(
                insetRect.left + start,
                insetRect.centerY() + 5 * dp + index * 1f * dp,
                insetRect.left + end,
                insetRect.centerY() + 5 * dp + index * 1f * dp,
                paintDebugSegment
            )
            index++
        }
        originalSegments?.filter { it.isCanReplace }?.forEach {it->
            paintDebugSegment.color = 0xFfFF6490.toInt()
            val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
            val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
            canvas.drawLine(
                insetRect.left + start,
                insetRect.centerY() + 5 * dp + index * 1f * dp,
                insetRect.left + end,
                insetRect.centerY() + 5 * dp + index * 1f * dp,
                paintDebugSegment
            )
            index++
        }

        var newSegment = seekBar?.getSegments()
        index = 0;
        newSegment?.filter { !it.isCanReplace }?.forEach {
            paintDebugSegment.color = Color.WHITE
            val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
            val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
            canvas.drawLine(
                insetRect.left + start,
                insetRect.centerY() - 5 * dp - index * 1f * dp,
                insetRect.left + end,
                insetRect.centerY() - 5 * dp - index * 1f * dp,
                paintDebugSegment
            )
            index++
        }
        newSegment?.filter { it.isCanReplace }?.forEach {
            paintDebugSegment.color = 0xFfFF6490.toInt()
            val start = ((insetRect.right - insetRect.left) * it.inPoint).toInt()
            val end = ((insetRect.right - insetRect.left) * it.outPoint).toInt()
            canvas.drawLine(
                insetRect.left + start,
                insetRect.centerY() - 5 * dp - index * 1f * dp,
                insetRect.left + end,
                insetRect.centerY() - 5 * dp - index * 1f * dp,
                paintDebugSegment
            )
            index++
        }
    }


    private fun drawBgLine(canvas: Canvas, progress: Float, segments: List<PreviewSegmentData>) {
        val splitPoint = findSpiltPoint(progress, segments)
        if (splitPoint > 0) {
            drawRoundLine(
                canvas,
                insetRect.left,
                insetRect.centerY(),
                insetRect.left + insetRect.width() * splitPoint,
                insetRect.centerY(),
                paintBg
            )
        }
        drawRoundLine(
            canvas,
            insetRect.left + insetRect.width() * splitPoint,
            insetRect.centerY(),
            insetRect.right,
            insetRect.centerY(),
            paintBg
        )
    }

    private fun drawProgress(canvas: Canvas, progress: Float) {
        val progress = ((insetRect.right - insetRect.left) * progress).toInt()
        drawRoundLine(
            canvas,
            insetRect.left,
            insetRect.centerY(),
            insetRect.left + progress,
            insetRect.centerY(),
            paintProgress
        )
    }

    private fun drawDot(canvas: Canvas, progress: Float) {
        val progress = ((insetRect.right - insetRect.left) * progress).toInt()
        if (iconBmp == null) return
        canvas.drawBitmap(
            iconBmp,
            insetRect.left + progress - iconBmp.width / 2f,
            insetRect.centerY() - iconBmp.width / 2f,
            paintDot
        )
    }

    private fun findSpiltPoint(progress: Float, segments: List<PreviewSegmentData>): Float {
        var targetClip = segments.filter { !it.isCanReplace }
            .find { it.inPoint < progress && it.outPoint > progress }
        if (targetClip == null) {
            targetClip = segments.filter { !it.isCanReplace }.find { it.inPoint > progress }
        }
        return targetClip?.inPoint ?: 0f
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
            startX + SegmentPadding / 2,
            startY - width / 2,
            stopX - SegmentPadding / 2,
            stopY + width / 2,
            width / 2,
            width / 2,
            paint
        )

    }
}