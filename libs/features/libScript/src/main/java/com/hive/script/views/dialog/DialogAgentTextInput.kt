package com.hive.script.views.dialog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hive.audio.interfaces.IAudioProvider
import com.hive.config.SpeechCredentialHelper
import com.hive.extension.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp
import com.hive.utils.system.CommonUtils
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.VoiceWaveformView
import kotlin.math.pow

class DialogAgentTextInput(context: Context?) : BaseScriptDialog(context) {

    private var commonListener: OnCommonListener? = null

    private var tvTitle: TextView? = null
    private var tvTip: TextView? = null
    private var ivClose: View? = null
    private var btnSubmit: View? = null
    private var btnMic: FrameLayout? = null
    private var editText: EditText? = null

    private var voiceLayout: View? = null
    private var inputBar: View? = null
    private var btnStopAndFinish: View? = null
    private var waveformView: VoiceWaveformView? = null
    private var tvVoiceResult: TextView? = null

    private var asrProvider: IAudioAsrProvider? = null
    private var isRecognizing: Boolean = false
    private var lastRecognizedText: String = ""
    private var hasSubmitted: Boolean = false

    override fun initWindow() {
        btnSubmit = findViewById(R.id.btn_submit)
        editText = findViewById(R.id.edit_text)
        tvTitle = findViewById(R.id.tv_title)
        tvTip = findViewById(R.id.tv_tip)
        ivClose = findViewById(R.id.iv_close)
        btnMic = findViewById(R.id.btn_mic)
        voiceLayout = findViewById(R.id.layout_voice_recognizing)
        inputBar = findViewById(R.id.layout_input_bar)
        btnStopAndFinish = findViewById(R.id.btn_stop_and_finish)
        waveformView = findViewById(R.id.waveform_view)
        tvVoiceResult = findViewById(R.id.tv_voice_result)

        ivClose?.setOnClickListener { dismiss() }
        btnSubmit?.setOnClickListener { submitText() }
        btnMic?.setOnClickListener { startRecognizeIfPossible() }
        btnStopAndFinish?.setOnClickListener { stopRecognize() }

        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSendButtonState()
            }
        })

        updateSendButtonState()
        showInputState()
        editText?.requestFocus()
        editText?.post { CommonUtils.openKeyboard(editText) }
    }

    fun setTitle(title: String): DialogAgentTextInput {
        tvTitle?.text = title
        return this
    }

    fun setHint(hint: String): DialogAgentTextInput {
        editText?.hint = hint
        return this
    }

    fun setText(text: String): DialogAgentTextInput {
        editText?.setText(text)
        editText?.setSelection(text.length)
        updateSendButtonState()
        return this
    }

    fun setOnCommonListener(callback: OnCommonListener): DialogAgentTextInput {
        commonListener = callback
        return this
    }

    override fun enableFadeAnimation() = true

    override fun getWindowLayoutId() = R.layout.dialog_agent_text_input

    override fun onDismiss() {
        super.onDismiss()
        stopRecognize()
        asrProvider?.release()
        asrProvider = null
        if (!hasSubmitted) {
            try {
                commonListener?.onCanceled()
            } catch (_: Exception) {
            }
        }
        commonListener = null
    }

    private fun submitText() {
        val content = editText?.text?.toString()?.trim() ?: ""
        if (TextUtils.isEmpty(content)) {
            CommonToast.show(getString(com.hive.i8n.R.string.sc_check_scheme_input_check_empty))
            return
        }
        try {
            hasSubmitted = true
            commonListener?.onSubmitted(content)
            dismiss()
        } catch (e: Exception) {
            CommonToast.show(e.message)
        }
    }

    private fun updateSendButtonState() {
        val enabled = editText?.text?.toString()?.trim()?.isNotEmpty() == true
        btnSubmit?.isEnabled = enabled
        btnSubmit?.alpha = if (enabled) 1f else 0.45f
    }

    private fun showInputState() {
        voiceLayout?.visibility = View.GONE
        inputBar?.visibility = View.VISIBLE
        tvTip?.visibility = View.VISIBLE
        btnMic?.isEnabled = true
    }

    private fun showRecognizingState() {
        voiceLayout?.visibility = View.VISIBLE
        inputBar?.visibility = View.GONE
        tvTip?.visibility = View.GONE
        btnMic?.isEnabled = false
        waveformView?.reset()
        updateVoiceMessage("")
    }

    private fun startRecognizeIfPossible() {
        if (isRecognizing) return
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission()
            return
        }
        val provider = prepareAsrProvider()
        if (provider == null) {
            CommonToast.show(getString(com.hive.i8n.R.string.tool_voice_interact_error_asr_provider_unavailable))
            return
        }

        lastRecognizedText = ""
        isRecognizing = true
        showRecognizingState()
        try {
            provider.startRecognize(null, object : IAudioAsrProvider.OnAudioRecognizedListener {
                override fun onRecognizedStart() {}
                override fun onRecognizedStop() {
                    post {
                        isRecognizing = false
                        if (lastRecognizedText.isNotBlank()) {
                            val current = editText?.text?.toString()?.trim().orEmpty()
                            val merged =
                                if (current.isEmpty()) lastRecognizedText else "$current $lastRecognizedText"
                            editText?.setText(merged)
                            editText?.setSelection(merged.length)
                        }
                        showInputState()
                        updateSendButtonState()
                        editText?.requestFocus()
                        CommonUtils.openKeyboard(editText)
                    }
                }

                override fun onRecognizedError() {
                    post {
                        isRecognizing = false
                        showInputState()
                        CommonToast.show(getString(com.hive.i8n.R.string.chat_voice_input_error))
                    }
                }

                override fun onRecognizedChanged(volume: Int, data: ByteArray) {
                    val normalized = normalizeVolume(volume)
                    post { waveformView?.updateAmplitude(normalized) }
                }

                override fun onRecognizedResult(result: String, audioPath: String) {
                    val text = result.trim()
                    if (text.isNotEmpty()) {
                        lastRecognizedText = text
                        post {
                            updateVoiceMessage(text)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            isRecognizing = false
            showInputState()
            CommonToast.show(e.message)
        }
    }

    private fun updateVoiceMessage(text: String) {
        tvVoiceResult?.visibleOrGone(text.isNotEmpty())
        tvVoiceResult?.text = text
    }

    private fun stopRecognize() {
        if (!isRecognizing) return
        try {
            asrProvider?.stopRecognize()
        } catch (_: Exception) {
            isRecognizing = false
            post { showInputState() }
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            GlobalApp.getContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        (GlobalApp.getAvailableActivity())?.let { activity ->
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1001
            )
        }
    }

    private fun prepareAsrProvider(): IAudioAsrProvider? {
        if (asrProvider != null) return asrProvider
        val audioProvider = try {
            ComponentManager.getInstance()
                .getProvider(IAudioProvider::class.java) as? IAudioProvider
        } catch (_: Exception) {
            null
        } ?: return null

        if (!audioProvider.isInitialized()) {
            val appKey = SpeechCredentialHelper.getMsSpeechKey()
            val region = SpeechCredentialHelper.getMsSpeechRegion()
            if (appKey.isNullOrBlank() || region.isNullOrBlank()) return null
            try {
                audioProvider.init(
                    GlobalApp.getContext(),
                    AudioConfiguration(appKey, region, GlobalApp.getContext().filesDir.absolutePath)
                )
            } catch (_: Exception) {
                return null
            }
        }
        asrProvider = audioProvider.getAsrProvider()
        return asrProvider
    }

    private fun normalizeVolume(volume: Int): Float {
        val v = (volume / 100f).coerceIn(0f, 1f)
        val boosted = v.pow(0.55f)
        return if (volume > 0) boosted.coerceAtLeast(0.04f) else 0f
    }

    interface OnCommonListener {
        fun onSubmitted(content: String)
        fun onCanceled()
    }
}