package com.hive.views.widgets

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.hive.views.R
import com.hive.i8n.R as i8nR

/**
 * 录音浮层：与深色主题协调的气泡与底部操作面板
 */
class VoiceRecordingOverlay(context: Context) : FrameLayout(context), VoiceRecordingCallback {

    override fun onVolume(normalized: Float) = updateWaveformAmplitude(normalized)
    override fun onRecognizedText(text: CharSequence?) = setRecognizedText(text)

    private var ffBubble: LinearLayout
    private var waveformView: VoiceWaveformView
    private var recognizedText: TextView
    private var arcPanel: VoiceArcPanelView
    private var bottomPanel: FrameLayout
    // 设为非 NEUTRAL，确保首次 setZone(NEUTRAL) 会落地刷新 UI
    private var lastZone: VoiceRecordingZone = VoiceRecordingZone.CANCEL
    private var animationGeneration: Int = 0
    private var bottomInsetPx: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_voice_recording_overlay, this, true)
        ffBubble = findViewById(R.id.ffBubble)
        waveformView = findViewById(R.id.waveformView)
        recognizedText = findViewById(R.id.recognizedText)
        bottomPanel = findViewById(R.id.bottomPanel)
        arcPanel = findViewById(R.id.arcPanel)
        arcPanel.setLabels(
            context.getString(i8nR.string.chat_voice_cancel),
            context.getString(i8nR.string.chat_voice_send)
        )
        setZone(VoiceRecordingZone.NEUTRAL)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            bottomInsetPx = maxOf(navInsets.bottom, imeInsets.bottom)
            requestLayout()
            insets
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }

    fun hitTestZone(rawX: Float, rawY: Float): VoiceRecordingZone = arcPanel.hitTest(rawX, rawY)

    fun setRecognizedText(text: CharSequence?) {
        val str = text?.toString()?.trim()
        if (str.isNullOrEmpty()) {
            waveformView.visibility = View.VISIBLE
            recognizedText.visibility = View.GONE
            recognizedText.text = ""
        } else {
            waveformView.visibility = View.GONE
            recognizedText.text = str
            recognizedText.visibility = View.VISIBLE
        }
    }

    fun setZone(zone: VoiceRecordingZone) {
        if (zone == lastZone) return

        if (zone != VoiceRecordingZone.NEUTRAL) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        val centerText = when (zone) {
            VoiceRecordingZone.CANCEL -> context.getString(i8nR.string.chat_voice_slide_cancel)
            VoiceRecordingZone.SEND -> context.getString(i8nR.string.chat_voice_release_send)
            VoiceRecordingZone.NEUTRAL -> context.getString(i8nR.string.chat_voice_release_to_text)
        }
        arcPanel.setCenterText(centerText)

        arcPanel.setZone(zone)

        val emphasize = zone != VoiceRecordingZone.NEUTRAL
        ffBubble.animate().cancel()
        ffBubble.animate()
            .scaleX(if (emphasize) 0.94f else 1f)
            .scaleY(if (emphasize) 0.94f else 1f)
            .alpha(if (emphasize) 0.75f else 1f)
            .setDuration(150)
            .setInterpolator(DecelerateInterpolator())
            .start()

        lastZone = zone
    }

    fun updateWaveformAmplitude(normalized: Float) = waveformView.updateAmplitude(normalized)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) updateAdaptiveLayout(w, h)
    }

    fun show(parent: ViewGroup) {
        animationGeneration += 1
        animate().cancel()
        (this.parent as? ViewGroup)?.let { currentParent ->
            if (currentParent !== parent) currentParent.removeView(this)
        }
        if (this.parent == null) {
            parent.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        requestDisallowInterceptTouchEvent(true)
        visibility = View.VISIBLE
        waveformView.reset()
        setRecognizedText(null)
        setZone(VoiceRecordingZone.NEUTRAL)
        post {
            if (width > 0 && height > 0) updateAdaptiveLayout(width, height)
            ViewCompat.requestApplyInsets(this)
        }
        alpha = 0f
        scaleX = 0.92f
        scaleY = 0.92f
        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(AccelerateDecelerateInterpolator()).start()
    }

    fun dismiss() {
        val dismissGeneration = ++animationGeneration
        animate().cancel()
        parent?.requestDisallowInterceptTouchEvent(false)
        animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (animationGeneration == dismissGeneration) {
                    (parent as? ViewGroup)?.removeView(this)
                }
            }
            .start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private fun updateAdaptiveLayout(widthPx: Int, heightPx: Int) {
        val widthF = widthPx.toFloat()
        val heightF = heightPx.toFloat()
        val minPanelHeight = dp(272f)
        val maxPanelHeight = minOf(dp(382f), heightF * 0.46f)
        val desiredPanelHeight = (widthF * 0.76f + bottomInsetPx + dp(16f))
            .coerceIn(minPanelHeight, maxPanelHeight)
            .toInt()

        bottomPanel.updateLayoutParams<LayoutParams> {
            height = desiredPanelHeight
        }

        arcPanel.setBottomSafeInset(bottomInsetPx.toFloat())

        val bubbleBottomMargin = (
            desiredPanelHeight * 0.88f + bottomInsetPx + dp(10f)
            ).toInt()
        ffBubble.updateLayoutParams<LayoutParams> {
            bottomMargin = bubbleBottomMargin
        }

        val minBubbleWidth = dp(144f).toInt()
        val maxBubbleWidth = minOf(dp(292f), widthF * 0.54f).toInt()
        ffBubble.minimumWidth = minBubbleWidth.coerceAtMost(maxBubbleWidth)
        recognizedText.maxWidth = maxBubbleWidth - dp(36f).toInt()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
