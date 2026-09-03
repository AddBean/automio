package com.hive.views.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.text.TextWatcher
import android.text.Editable
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.views.R
import com.hive.views.widgets.chat.ChatInputAsrProvider
import com.hive.i8n.R as i8nR

/**
 * 可复用的聊天输入控件（libViews 自定义控件）
 * 支持文字/语音切换、按住说话、选图/模型选择、附件预览、发送。
 * 权限请求、ASR 由宿主通过 setOnRequestRecordPermission、setAsrProvider 注入。
 */
class ChatInputContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var rootContainer: View
    private var voiceButtonContainer: FrameLayout
    private var textActionContainer: LinearLayout
    private var inputEdit: EditText
    private var sendButton: ImageView
    private var pickImageButton: ImageView? = null
    private var modelSelectButton: TextView? = null
    private var voiceInputButton: VoiceInputButton? = null
    private var holdToSpeakButton: HoldToSpeakButton? = null
    private var attachmentPreviewContainer: FrameLayout? = null
    private var attachmentPreviewImage: ImageView? = null
    private var removeAttachmentButton: ImageView? = null
    private var isVoiceMode: Boolean = false
    private var voiceRecordingOverlay: VoiceRecordingOverlay? = null
    private var onSendClickListener: ((String) -> Unit)? = null
    private var onStopClickListener: (() -> Unit)? = null
    private var onPickImageClickListener: (() -> Unit)? = null
    private var onModelSelectClickListener: (() -> Unit)? = null
    private var onVoiceInputClickListener: (() -> Unit)? = null
    private var showImagePicker: Boolean = true
    private var showModelSelector: Boolean = false
    private var showVoiceInput: Boolean = true
    private var pendingSendResult: Boolean = false
    private var pendingFillToEdit: Boolean = false
    private var lastVoiceResult: String = ""
    private var useAgentStyle: Boolean = false
    private var isAgentExecuting: Boolean = false
    private var isStopMode: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_chat_input_container, this, true)
        rootContainer = findViewById(R.id.chatInputRoot)
        voiceButtonContainer = findViewById(R.id.voiceButtonContainer)
        textActionContainer = findViewById(R.id.textActionContainer)
        inputEdit = findViewById(R.id.chatInputEdit)
        sendButton = findViewById(R.id.sendChatBtn)
        pickImageButton = findViewById(R.id.pickImageBtn)
        modelSelectButton = findViewById(R.id.modelSelectBtn)
        voiceInputButton = findViewById(R.id.voiceInputBtn)
        holdToSpeakButton = findViewById(R.id.holdToSpeakBtn)
        attachmentPreviewContainer = findViewById(R.id.attachmentPreviewContainer)
        attachmentPreviewImage = findViewById(R.id.attachmentPreviewImage)
        removeAttachmentButton = findViewById(R.id.removeAttachmentBtn)

        sendButton.setOnClickListener {
            val stopClicked = isStopMode
            if (!stopClicked) {
                dismissKeyboard()
            }
            animateButtonClick(sendButton) {
                if (stopClicked) {
                    onStopClickListener?.invoke()
                } else {
                    onSendClickListener?.invoke(inputEdit.text.toString().trim())
                    inputEdit.setText("")
                }
            }
        }
        pickImageButton?.setOnClickListener {
            animateButtonClick(pickImageButton!!) { onPickImageClickListener?.invoke() }
        }
        modelSelectButton?.setOnClickListener {
            animateButtonClick(modelSelectButton!!) { onModelSelectClickListener?.invoke() }
        }
        voiceInputButton?.setOnVoiceResultListener { result ->
            val finalResult = result.trim()
            if (finalResult.isNotEmpty()) lastVoiceResult = finalResult
            if (finalResult.isEmpty()) return@setOnVoiceResultListener
            applyVoiceResult(finalResult)
        }
        voiceInputButton?.setOnVoiceStateChangeListener { isRecognizing ->
            if (!isRecognizing && (pendingSendResult || pendingFillToEdit) && lastVoiceResult.isNotEmpty()) {
                val result = lastVoiceResult
                lastVoiceResult = ""
                applyVoiceResult(result)
            }
        }
        voiceInputButton?.setOnVoiceButtonClickListener {
            if (onVoiceInputClickListener != null) onVoiceInputClickListener?.invoke()
            else toggleVoiceMode()
        }
        holdToSpeakButton?.onStartRecording = fun(): Boolean {
            lastVoiceResult = ""
            if (!voiceInputButton!!.hasRecordAudioPermission()) {
                requestRecordPermissionAndRetry { holdToSpeakButton?.onStartRecording?.invoke() }
                return false
            }
            if (!voiceInputButton!!.isVoiceInputEnabled()) return false
            showVoiceRecordingOverlay()
            voiceInputButton?.setRecordingCallback(voiceRecordingOverlay)
            voiceInputButton?.startVoiceInput()
            return true
        }
        holdToSpeakButton?.onStopRecording = {
            voiceInputButton?.setRecordingCallback(null)
            voiceInputButton?.stopVoiceInput()
        }
        holdToSpeakButton?.onZoneChange = { voiceRecordingOverlay?.setZone(it) }
        holdToSpeakButton?.onReleaseInNeutralZone = { dismissVoiceRecordingOverlay(); pendingFillToEdit = true }
        holdToSpeakButton?.onReleaseInCancelZone = { dismissVoiceRecordingOverlay() }
        holdToSpeakButton?.onReleaseInSendZone = { dismissVoiceRecordingOverlay(); pendingSendResult = true }
        voiceInputButton?.setOnPermissionRequestListener { onGranted ->
            (context as? android.app.Activity)?.let { activity ->
                onRequestRecordPermission?.invoke(activity, onGranted)
                    ?: com.hive.utils.debug.DLog.w("ChatInputContainer", "RECORD_AUDIO: setOnRequestRecordPermission not set")
            }
        }
        removeAttachmentButton?.setOnClickListener { hideAttachmentPreview(); updateSendButtonState() }
        inputEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotEmpty() == true) switchToTextMode()
                updateSendButtonState()
            }
        })
        updateSendButtonState()
        updateImagePickerVisibility()
        updateModelSelectorVisibility()
        updateVoiceInputVisibility()
    }

    fun setShowImagePicker(show: Boolean) { showImagePicker = show; updateImagePickerVisibility() }
    fun setShowModelSelector(show: Boolean) { showModelSelector = show; updateModelSelectorVisibility() }
    fun setShowVoiceInput(show: Boolean) { showVoiceInput = show; updateVoiceInputVisibility() }

    /** 设为 false 时选图按钮变暗并禁用点击 */
    fun setImagePickerEnabled(enabled: Boolean) {
        pickImageButton?.isEnabled = enabled
        pickImageButton?.isClickable = enabled
        pickImageButton?.alpha = if (enabled) 1f else 0.5f
    }

    /** 更新模型入口文案；isUnset=true 时用强调色提示去设置 */
    fun setModelSelectorText(text: CharSequence, isUnset: Boolean) {
        modelSelectButton?.text = text
        modelSelectButton?.setTextColor(
            ContextCompat.getColor(
                context,
                if (isUnset) i8nR.color.design_accent_rose else i8nR.color.colorTextSecondary
            )
        )
    }

    private fun updateImagePickerVisibility() {
        pickImageButton?.visibility =
            if (showImagePicker && !isVoiceMode) View.VISIBLE else View.GONE
    }

    private fun updateModelSelectorVisibility() {
        modelSelectButton?.visibility =
            if (showModelSelector && !isVoiceMode) View.VISIBLE else View.GONE
    }

    private fun updateVoiceInputVisibility() { voiceInputButton?.visibility = if (showVoiceInput) View.VISIBLE else View.GONE }

    fun setOnSendClickListener(listener: (String) -> Unit) { onSendClickListener = listener }
    fun setOnStopClickListener(listener: (() -> Unit)?) { onStopClickListener = listener }

    /** Agent 执行中：空输入时右侧变为停止；有文字/附件时仍为发送 */
    fun setAgentExecuting(executing: Boolean) {
        if (isAgentExecuting == executing) return
        isAgentExecuting = executing
        updateSendButtonState()
    }

    fun setOnPickImageClickListener(listener: () -> Unit) { onPickImageClickListener = listener }
    fun setOnModelSelectClickListener(listener: () -> Unit) { onModelSelectClickListener = listener }
    fun setOnVoiceInputClickListener(listener: () -> Unit) { onVoiceInputClickListener = listener }
    fun getInputEdit(): EditText = inputEdit
    fun setInputHint(hint: String) { inputEdit.hint = hint }

    fun setAgentStyle(enabled: Boolean) {
        useAgentStyle = enabled
        rootContainer.setBackgroundResource(
            if (enabled) R.drawable.chat_input_agent_shell_bg else R.drawable.xml_input_container_bg
        )
        val rootPadding = if (enabled) dp(15) else dp(12)
        rootContainer.setPadding(rootPadding, rootPadding, rootPadding, rootPadding)
        voiceButtonContainer.setBackgroundResource(
            if (enabled) R.drawable.chat_input_agent_edit_bg else 0
        )
        textActionContainer.setBackgroundResource(
            if (enabled) R.drawable.chat_input_agent_edit_bg else 0
        )
        inputEdit.setBackgroundResource(
            if (enabled) android.R.color.transparent else R.drawable.xml_input_edit_bg
        )
        holdToSpeakButton?.setBackgroundResource(
            if (enabled) android.R.color.transparent else R.drawable.xml_input_edit_bg
        )
        val iconPadding = if (enabled) dp(12) else dp(10)
        pickImageButton?.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        sendButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        updateSendButtonState()
    }

    /** 设置输入框文本，并自动切换到输入态（文本模式） */
    fun setText(text: CharSequence?) {
        inputEdit.setText(text)
        switchToTextMode()
        updateSendButtonState()
    }

    /** 由宿主设置：加载附件缩略图，未设置时仅显示占位 */
    var attachmentImageLoader: ((context: Context, imageView: ImageView, uri: String) -> Unit)? = null

    fun showAttachmentPreview(imageUri: Any) {
        attachmentPreviewContainer?.visibility = View.VISIBLE
        attachmentImageLoader?.invoke(context, attachmentPreviewImage!!, imageUri.toString())
            ?: Unit
        updateSendButtonState()
    }

    fun hideAttachmentPreview() {
        attachmentPreviewContainer?.visibility = View.GONE
        updateSendButtonState()
    }

    fun updateSendButtonState() {
        val hasText = inputEdit.text?.toString()?.trim()?.isNotEmpty() == true
        val hasAttachment = attachmentPreviewContainer?.visibility == View.VISIBLE
        val canSend = hasText || hasAttachment
        val showStop = isAgentExecuting && !canSend

        if (showStop != isStopMode) {
            isStopMode = showStop
            if (showStop) {
                sendButton.setImageResource(R.drawable.ic_chat_stop)
                sendButton.contentDescription = context.getString(R.string.chat_stop_generation)
            } else {
                sendButton.setImageResource(R.drawable.ic_send)
                sendButton.contentDescription = context.getString(R.string.chat_send_message)
            }
        }

        val enabled = canSend || showStop
        sendButton.isEnabled = enabled
        sendButton.alpha = if (enabled) 1f else 0.5f
        val enabledColor = if (useAgentStyle) {
            com.hive.i8n.R.color.color_blue
        } else {
            com.hive.i8n.R.color.textColorPrimary
        }
        sendButton.setColorFilter(
            ContextCompat.getColor(
                context,
                if (enabled) enabledColor else com.hive.i8n.R.color.colorTextSecondary
            )
        )
    }

    private fun dismissKeyboard() {
        inputEdit.clearFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(inputEdit.windowToken, 0)
    }

    private fun animateButtonClick(button: View, action: () -> Unit) {
        button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { action() }.start()
            }.start()
    }

    private fun toggleVoiceMode() {
        isVoiceMode = !isVoiceMode
        if (isVoiceMode) {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(inputEdit.windowToken, 0)
            inputEdit.visibility = View.GONE
            holdToSpeakButton?.visibility = View.VISIBLE
            voiceInputButton?.setKeyboardMode(true)
            pickImageButton?.visibility = View.GONE
            modelSelectButton?.visibility = View.GONE
            sendButton.visibility = View.GONE
        } else {
            inputEdit.visibility = View.VISIBLE
            holdToSpeakButton?.visibility = View.GONE
            voiceInputButton?.setKeyboardMode(false)
            updateImagePickerVisibility()
            updateModelSelectorVisibility()
            sendButton.visibility = View.VISIBLE
        }
    }

    private fun switchToTextMode() {
        if (isVoiceMode) {
            isVoiceMode = false
            inputEdit.visibility = View.VISIBLE
            holdToSpeakButton?.visibility = View.GONE
            voiceInputButton?.setKeyboardMode(false)
            updateImagePickerVisibility()
            updateModelSelectorVisibility()
            sendButton.visibility = View.VISIBLE
        }
    }

    private fun showVoiceRecordingOverlay() {
        if (voiceRecordingOverlay == null) voiceRecordingOverlay = VoiceRecordingOverlay(context)
        (rootView as? android.view.ViewGroup)?.let { root ->
            voiceRecordingOverlay?.show(root)
            voiceRecordingOverlay?.setZone(VoiceRecordingZone.NEUTRAL)
            voiceRecordingOverlay?.post {
                holdToSpeakButton?.onCheckZone = { x, y -> voiceRecordingOverlay?.hitTestZone(x, y) ?: VoiceRecordingZone.NEUTRAL }
            }
        }
    }

    private fun applyVoiceResult(finalResult: String) {
        when {
            pendingSendResult -> {
                pendingSendResult = false
                onSendClickListener?.invoke(finalResult)
                inputEdit.setText("")
                updateSendButtonState()
            }
            pendingFillToEdit -> {
                pendingFillToEdit = false
                switchToTextMode()
                val current = inputEdit.text?.toString().orEmpty()
                inputEdit.setText(if (current.isEmpty()) finalResult else "$current $finalResult")
                inputEdit.setSelection(inputEdit.text?.length ?: 0)
                updateSendButtonState()
            }
        }
    }

    private fun dismissVoiceRecordingOverlay() {
        holdToSpeakButton?.onCheckZone = null
        voiceInputButton?.setRecordingCallback(null)
        voiceRecordingOverlay?.dismiss()
    }

    private fun requestRecordPermissionAndRetry(onGranted: () -> Unit) {
        (context as? android.app.Activity)?.let { activity ->
            onRequestRecordPermission?.invoke(activity, onGranted)
        }
    }

    /** 由宿主设置：请求录音权限，未设置时无法请求权限 */
    var onRequestRecordPermission: ((activity: android.app.Activity, onGranted: () -> Unit) -> Unit)? = null

    /** 由宿主设置：ASR 实现，未设置时语音按钮不可用 */
    fun setAsrProvider(provider: ChatInputAsrProvider?) {
        voiceInputButton?.setAsrProvider(provider)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    fun release() { dismissVoiceRecordingOverlay(); voiceInputButton?.release() }
    fun startVoiceInput() { voiceInputButton?.startVoiceInput() }
    fun stopVoiceInput() { voiceInputButton?.stopVoiceInput() }
}
