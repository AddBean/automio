package com.hive.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.hive.views.R
import com.hive.i8n.R as i8nR

/**
 * 微信风格「按住说话」按钮
 */
class HoldToSpeakButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var holdToSpeakText: TextView

    var onStartRecording: (() -> Boolean)? = null
    var onStopRecording: (() -> Unit)? = null
    var onZoneChange: ((zone: VoiceRecordingZone) -> Unit)? = null
    var onReleaseInNeutralZone: (() -> Unit)? = null
    var onReleaseInCancelZone: (() -> Unit)? = null
    var onReleaseInSendZone: (() -> Unit)? = null
    var onCheckZone: ((rawX: Float, rawY: Float) -> VoiceRecordingZone)? = null

    private var currentZone: VoiceRecordingZone = VoiceRecordingZone.NEUTRAL
    private var touchDownTime: Long = 0
    private val minPressDurationMs = 200L

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_hold_to_speak_button, this, true)
        holdToSpeakText = findViewById(R.id.holdToSpeakText)
        holdToSpeakText.text = context.getString(i8nR.string.chat_voice_hold_to_speak)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                if (onStartRecording?.invoke() == true) {
                    requestParentDisallowIntercept(true)
                    currentZone = VoiceRecordingZone.NEUTRAL
                    onZoneChange?.invoke(currentZone)
                    return true
                }
                requestParentDisallowIntercept(false)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                requestParentDisallowIntercept(true)
                val zone = onCheckZone?.invoke(event.rawX, event.rawY) ?: VoiceRecordingZone.NEUTRAL
                if (zone != currentZone) {
                    currentZone = zone
                    onZoneChange?.invoke(currentZone)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                requestParentDisallowIntercept(false)
                onStopRecording?.invoke()
                val valid = (System.currentTimeMillis() - touchDownTime) >= minPressDurationMs
                when {
                    !valid || currentZone == VoiceRecordingZone.CANCEL -> onReleaseInCancelZone?.invoke()
                    currentZone == VoiceRecordingZone.SEND -> onReleaseInSendZone?.invoke()
                    else -> onReleaseInNeutralZone?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                requestParentDisallowIntercept(false)
                onStopRecording?.invoke()
                onReleaseInCancelZone?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun requestParentDisallowIntercept(disallow: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(disallow)
    }
}
