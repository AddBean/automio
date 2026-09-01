package com.hive.script.views.tips

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Pair
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptTips
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.views.widgets.VoiceWaveformView
import kotlin.math.abs
import kotlin.math.sin

class ScriptVoiceInteractTipView(context: Context) : BaseScriptTips(context) {

    private var barRoot: View? = null
    private var collapsedRoot: View? = null
    private var btnCollapseExpanded: View? = null
    private var tvCollapsedMode: TextView? = null
    private var tvStatus: TextView? = null
    private var scrollContent: ScrollView? = null
    private var tvContent: TextView? = null
    private var waveformView: VoiceWaveformView? = null
    private var collapsedWaveformView: VoiceWaveformView? = null

    private var tvTitle: TextView? = null
    private var onCancelListener: (() -> Unit)? = null
    private var mode: Mode = Mode.LISTENING
    private var lastSpeakingAmplitude: Float = 0.15f
    private var autoCollapseHandler: Handler? = null
    private var autoCollapseRunnable: Runnable? = null
    private var isCollapsed = false

    @SuppressLint("ClickableViewAccessibility")
    override fun initWindow() {
        super.initWindow()
        barRoot = findViewById(R.id.layout_bar_root)
        collapsedRoot = findViewById(R.id.layout_collapsed_root)
        btnCollapseExpanded = findViewById(R.id.btn_collapse_expanded)
        tvCollapsedMode = findViewById(R.id.tv_collapsed_mode)
        tvStatus = findViewById(R.id.tv_status)
        tvTitle = findViewById(R.id.tv_title)
        scrollContent = findViewById(R.id.scroll_content)
        tvContent = findViewById(R.id.tv_content)
        waveformView = findViewById(R.id.waveform_view)
        collapsedWaveformView = findViewById(R.id.waveform_view_collapsed)

        setOptEnable(false)
        setCancelClickListener {
            onCancelListener?.invoke()
        }
        btnCollapseExpanded?.setOnClickListener {
            collapse()
        }
        collapsedRoot?.setOnClickListener {
            expand()
        }
        scrollContent?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                resetAutoCollapseTimer()
            }
            false
        }
        barRoot?.setOnClickListener {
            resetAutoCollapseTimer()
        }
        // 注意：BaseScriptDialog 在构造阶段会回调 initWindow()，
        // 此时子类字段（包含 mode）可能尚未初始化完成，避免读取字段导致 NPE。
        applyModeStyle(Mode.LISTENING)
        expand()
        resetAutoCollapseTimer()
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.script_voice_interact_tip_view

    override fun isTouchOutsideDismissed(): Boolean = false

    override fun getHeightByOrientation(): Int = FrameLayout.LayoutParams.WRAP_CONTENT

    override fun getMarginParams(): Array<Int> {
        val base = super.getMarginParams()
        return arrayOf(20.dp, base[1], 20.dp, base[3])
    }

    fun setDialogTitle(title: String): ScriptVoiceInteractTipView {
        val t = title.trim()
        tvTitle?.text = t
        tvTitle?.visibility = if (t.isEmpty()) GONE else VISIBLE
        return this
    }

    fun setStatusText(status: String): ScriptVoiceInteractTipView {
        tvStatus?.text = status
        tvCollapsedMode?.text = status
        return this
    }

    fun setOnCancelListener(listener: (() -> Unit)?): ScriptVoiceInteractTipView {
        onCancelListener = listener
        return this
    }

    override fun setCancelText(text: String): ScriptVoiceInteractTipView {
        super.setCancelText(text)
        return this
    }

    fun showListeningState(recognizedText: CharSequence?) {
        mode = Mode.LISTENING
        lastSpeakingAmplitude = 0.15f
        applyModeStyle(mode)
        waveformView?.reset()
        collapsedWaveformView?.reset()
        tvContent?.text = recognizedText?.toString()?.trim().orEmpty()
        scrollToBottom()
        resetAutoCollapseTimer()
    }

    fun showSpeakingState(text: CharSequence?) {
        mode = Mode.SPEAKING
        applyModeStyle(mode)
        waveformView?.reset()
        collapsedWaveformView?.reset()
        tvContent?.text = text?.toString()?.trim().orEmpty()
        scrollToBottom()
        resetAutoCollapseTimer()
    }

    fun updateRecognizedText(text: CharSequence?) {
        tvContent?.text = text?.toString()?.trim().orEmpty()
        scrollToBottom()
        resetAutoCollapseTimer()
    }

    fun updateVolume(normalized: Float) {
        if (mode == Mode.LISTENING) {
            val value = normalized.coerceIn(0f, 1f)
            waveformView?.updateAmplitude(value)
            collapsedWaveformView?.updateAmplitude(value)
        }
    }

    fun updateSpeakingVisemeData(visemeData: List<Pair<Float, Long>>?) {
        if (mode != Mode.SPEAKING) return
        val last = visemeData?.lastOrNull()
        val visemeId = last?.second?.toInt() ?: return
        val raw = (visemeId.coerceAtLeast(0) % 22) / 21f
        val shaped = 0.15f + raw * 0.85f
        val smoothed = (lastSpeakingAmplitude * 0.62f + shaped * 0.38f).coerceIn(0f, 1f)
        lastSpeakingAmplitude = smoothed
        waveformView?.updateAmplitude(smoothed)
        collapsedWaveformView?.updateAmplitude(smoothed)
    }

    fun tickSpeakingWaveform(seed: Long) {
        if (mode != Mode.SPEAKING) return
        val t = (seed % 10_000L) / 1000f
        val v = 0.18f + 0.42f * abs(sin(t * 2.8f)) + 0.22f * abs(sin(t * 6.4f))
        val value = v.coerceIn(0f, 1f)
        waveformView?.updateAmplitude(value)
        collapsedWaveformView?.updateAmplitude(value)
    }

    override fun onDismiss() {
        super.onDismiss()
        clearAutoCollapseTimer()
        onCancelListener = null
    }

    private fun scrollToBottom() {
        val scroll = scrollContent ?: return
        scroll.post {
            val child = scroll.getChildAt(0) ?: return@post
            val scrollRange =
                (child.height - scroll.height + scroll.paddingTop + scroll.paddingBottom).coerceAtLeast(
                    0
                )
            scroll.smoothScrollTo(0, scrollRange)
        }
    }

    private fun applyModeStyle(mode: Mode?) {
        val finalMode: Mode = mode ?: Mode.LISTENING
        val accentColor = when (finalMode) {
            Mode.LISTENING -> GlobalApp.getColor(com.hive.i8n.R.color.tech_cyan_light)
            Mode.SPEAKING -> GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
        }

        fun withAlpha(color: Int, alpha: Int): Int {
            val a = alpha.coerceIn(0, 255) shl 24
            return a or (color and 0x00FFFFFF)
        }

        val rootDrawable = barRoot?.background as? GradientDrawable
        rootDrawable?.mutate()
        rootDrawable?.setStroke(1 * GlobalApp.DP, withAlpha(accentColor, 0x33))

        val chipDrawable = tvStatus?.background as? GradientDrawable
        chipDrawable?.mutate()
        chipDrawable?.setStroke(1 * GlobalApp.DP, withAlpha(accentColor, 0x55))
        tvStatus?.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary))
        tvCollapsedMode?.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary))
    }

    private fun resetAutoCollapseTimer() {
        if (isCollapsed) return
        val handler = autoCollapseHandler ?: Handler(Looper.getMainLooper()).also {
            autoCollapseHandler = it
        }
        val runnable = autoCollapseRunnable ?: Runnable {
            if (!isCollapsed && isShown) {
                collapse()
            }
        }.also { autoCollapseRunnable = it }
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, AUTO_COLLAPSE_DELAY)
    }

    private fun clearAutoCollapseTimer() {
        autoCollapseHandler?.removeCallbacksAndMessages(null)
    }

    private fun collapse() {
        if (isCollapsed) return
        isCollapsed = true
        barRoot?.visibility = GONE
        collapsedRoot?.visibility = VISIBLE
        clearAutoCollapseTimer()
    }

    private fun expand() {
        if (!isCollapsed) {
            barRoot?.visibility = VISIBLE
            collapsedRoot?.visibility = GONE
            resetAutoCollapseTimer()
            return
        }
        isCollapsed = false
        collapsedRoot?.visibility = GONE
        barRoot?.visibility = VISIBLE
        resetAutoCollapseTimer()
    }

    private enum class Mode { LISTENING, SPEAKING }

    companion object {
        private const val AUTO_COLLAPSE_DELAY = 8000L
    }
}