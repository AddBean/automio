package com.hive.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.max
import kotlin.math.pow

/**
 * 录音波形指示条，显示在录音气泡内，与深色主题协调。
 */
class VoiceWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val logTag: String = "VoiceWaveformView"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(com.hive.i8n.R.color.white)
        alpha = (0.92 * 255).toInt()
        style = Paint.Style.FILL
    }

    private val barCount = 5
    private val barWidthDp = 2f
    private val barGapDp = 2.5f
    private val minBarHeightDp = 2f
    private val maxBarHeightDp = 20f

    private val density: Float = context.resources.displayMetrics.density
    private val barWidthPx = max(1f, barWidthDp * density)
    private val barGapPx = max(0f, barGapDp * density)
    private val minBarHeightPx = max(1f, minBarHeightDp * density)
    private val maxBarHeightPx = max(minBarHeightPx + 1f, maxBarHeightDp * density)
    private val amplitudeBuffer = FloatArray(barCount) { 0f }
    private val decayFactor = 0.88f
    private val minAmplitude = 0.02f
    private var amplitudeLogCounter: Int = 0

    private val decayRunnable = object : Runnable {
        override fun run() {
            if (parent == null) return
            var changed = false
            for (i in 0 until barCount) {
                val v = amplitudeBuffer[i]
                if (v > minAmplitude) {
                    amplitudeBuffer[i] = (v * decayFactor).coerceAtLeast(0f)
                    changed = true
                }
            }
            if (changed) postInvalidateOnAnimation()
            postDelayed(this, 50)
        }
    }

    init { post(decayRunnable) }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(decayRunnable)
    }

    fun updateAmplitude(normalized: Float) {
        val v = normalized.coerceIn(0f, 1f)
        if (amplitudeLogCounter++ % 30 == 0) {
            Log.d(logTag, "updateAmplitude normalized=$v parent=${parent != null} w=$width h=$height")
        }
        for (i in 0 until barCount - 1) amplitudeBuffer[i] = amplitudeBuffer[i + 1]
        amplitudeBuffer[barCount - 1] = v
        if (parent != null) postInvalidateOnAnimation()
    }

    fun reset() {
        amplitudeLogCounter = 0
        for (i in 0 until barCount) amplitudeBuffer[i] = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val totalWidth = barCount * barWidthPx + (barCount - 1) * barGapPx
        var left = (width - totalWidth) / 2f
        for (i in 0 until barCount) {
            val ratio = amplitudeBuffer[i].coerceIn(0f, 1f)
            // 视觉增强：对低振幅做“抬升”，让波形变化更明显（不改变外部输入的 0..1 语义）
            val shaped = ratio.toDouble().pow(0.45).toFloat().coerceIn(0f, 1f)
            val barHeight = minBarHeightPx + shaped * (maxBarHeightPx - minBarHeightPx)
            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2
            val radius = (barWidthPx / 2).coerceAtMost(2f * density)
            canvas.drawRoundRect(RectF(left, top, left + barWidthPx, bottom), radius, radius, paint)
            left += barWidthPx + barGapPx
        }
    }
}
