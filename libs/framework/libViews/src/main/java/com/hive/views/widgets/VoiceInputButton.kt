package com.hive.views.widgets

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hive.views.R
import com.hive.views.widgets.chat.ChatInputAsrProvider
import com.hive.i8n.R as i8nR
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import kotlin.math.pow

/**
 * 独立的语音输入按钮控件，ASR 由宿主通过 setAsrProvider 注入
 */
class VoiceInputButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val logTag: String = "VoiceInputButton"

    private var voiceButton: ImageView
    private var onVoiceResultListener: ((String) -> Unit)? = null
    private var onVoiceStateChangeListener: ((Boolean) -> Unit)? = null
    private var onVoiceButtonClickListener: (() -> Unit)? = null
    private var onPermissionRequestListener: ((() -> Unit) -> Unit)? = null
    private var recordingCallback: VoiceRecordingCallback? = null
    private var asrProvider: ChatInputAsrProvider? = null
    private var isVoiceRecognizing: Boolean = false
    private var volumeLogCounter: Int = 0

    private val voiceInputEnabled: Boolean
        get() = asrProvider != null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_voice_input_button, this, true)
        voiceButton = findViewById(R.id.voiceButton)
        voiceButton.setOnClickListener {
            animateButtonClick(voiceButton) {
                if (onVoiceButtonClickListener != null) onVoiceButtonClickListener?.invoke()
                else toggleVoiceInput()
            }
        }
    }

    /** 由宿主注入 ASR 实现（如通过 libAudio 的 IAudioProvider 适配），未设置时语音不可用 */
    fun setAsrProvider(provider: ChatInputAsrProvider?) {
        asrProvider = provider
    }

    fun setOnVoiceResultListener(listener: (String) -> Unit) { onVoiceResultListener = listener }
    fun setOnVoiceStateChangeListener(listener: (Boolean) -> Unit) { onVoiceStateChangeListener = listener }
    fun setOnVoiceButtonClickListener(listener: () -> Unit) { onVoiceButtonClickListener = listener }
    fun setOnPermissionRequestListener(listener: ((() -> Unit) -> Unit)) { onPermissionRequestListener = listener }
    fun setRecordingCallback(callback: VoiceRecordingCallback?) { recordingCallback = callback }

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun isRecognizing(): Boolean = isVoiceRecognizing
    fun isVoiceInputEnabled(): Boolean = voiceInputEnabled

    fun setKeyboardMode(isKeyboardMode: Boolean) {
        voiceButton.setImageResource(if (isKeyboardMode) R.drawable.ic_keyboard else R.drawable.ic_mic)
    }

    fun toggleVoiceInput() { if (isVoiceRecognizing) stopVoiceInput() else startVoiceInput() }

    fun startVoiceInput() {
        if (asrProvider == null) {
            Log.w(logTag, "startVoiceInput ignored: asrProvider is null (ChatInputAsrProvider not injected?)")
            return
        }
        if (!hasRecordAudioPermission()) {
            Log.w(logTag, "startVoiceInput blocked: RECORD_AUDIO not granted")
            if (onPermissionRequestListener != null) onPermissionRequestListener?.invoke { startVoiceInput() }
            else requestRecordAudioPermission()
            return
        }
        try {
            isVoiceRecognizing = true
            volumeLogCounter = 0
            updateButtonState()
            onVoiceStateChangeListener?.invoke(true)
            Log.d(logTag, "startVoiceInput -> asrProvider.startRecognize() callback=${recordingCallback != null}")
            asrProvider?.startRecognize(object : ChatInputAsrProvider.OnRecognizedListener {
                override fun onRecognizedResult(result: String, audioPath: String) {
                    if (result.isNotEmpty()) {
                        recordingCallback?.onRecognizedText(result)
                        onVoiceResultListener?.invoke(result)
                    }
                }
                override fun onVolume(volume: Int) {
                    // MS 原始音量在低区间偏小，增加灵敏度以便波形可见
                    val linear = (volume / 35f).coerceIn(0f, 1f)
                    val boosted = linear.pow(0.42f)
                    val normalized = if (volume > 0) boosted.coerceAtLeast(0.1f) else 0f
                    if (volumeLogCounter++ % 30 == 0) {
                        Log.d(logTag, "onVolume raw=$volume linear=$linear normalized=$normalized callback=${recordingCallback != null}")
                    }
                    recordingCallback?.onVolume(normalized)
                }
                override fun onStop() {
                    isVoiceRecognizing = false
                    updateButtonState()
                    onVoiceStateChangeListener?.invoke(false)
                    Log.d(logTag, "onStop()")
                }
                override fun onError() {
                    isVoiceRecognizing = false
                    updateButtonState()
                    onVoiceStateChangeListener?.invoke(false)
                    Log.w(logTag, "onError()")
                }
            })
        } catch (e: Exception) {
            isVoiceRecognizing = false
            updateButtonState()
            onVoiceStateChangeListener?.invoke(false)
            Log.e(logTag, "startVoiceInput exception: ${e.message}", e)
        }
    }

    fun stopVoiceInput() {
        if (isVoiceRecognizing) {
            try {
                asrProvider?.stopRecognize()
                isVoiceRecognizing = false
                updateButtonState()
                onVoiceStateChangeListener?.invoke(false)
            } catch (e: Exception) { DLog.e("VoiceInputButton", "Stop error: ${e.message}") }
        }
    }

    private fun updateButtonState() {
        if (isVoiceRecognizing) {
            voiceButton.setColorFilter(context.getColor(com.hive.i8n.R.color.colorAccent))
            voiceButton.contentDescription = context.getString(i8nR.string.chat_voice_input_stop)
        } else {
            voiceButton.setColorFilter(context.getColor(com.hive.i8n.R.color.colorTextSecondary))
            voiceButton.contentDescription = context.getString(i8nR.string.chat_voice_input_start)
        }
    }

    private fun animateButtonClick(button: View, action: () -> Unit) {
        button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { action() }.start()
            }.start()
    }

    private fun requestRecordAudioPermission() {
        (context as? Activity ?: GlobalApp.getAvailableActivity())?.let {
            ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
        }
    }

    fun release() { stopVoiceInput() }
}
