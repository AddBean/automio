package com.hive.script.cmd

import android.Manifest
import android.content.pm.PackageManager
import com.hive.audio.interfaces.IAudioProvider
import com.hive.audio.providers.ms.MSAudioTtsProvider
import com.hive.config.SpeechCredentialHelper
import com.hive.plugin.ComponentManager
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.plugin.provider.IAudioTtsProvider
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.base.core.ScriptRegularInterface
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.views.tips.ScriptVoiceInteractTipView
import com.hive.utils.extends.decode
import com.hive.utils.extends.encode
import com.hive.utils.utils.StringUtils
import com.hive.utils.GlobalApp
import com.hive.utils.thread.UIHandlerUtils
import androidx.core.content.ContextCompat
import com.hive.script.utils.ScriptHelper
import com.hive.utils.file.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

@AutoCmdRegister(type = IDS.CmdVoiceInteract, name = "voiceInteract")
class CmdVoiceInteract : ScriptCommand(), ScriptRegularInterface {

    var mode: String = MODE_TTS
    var ttsText: String? = null
    var preferLanguage: String? = null
    var timeoutMs: Long = DEFAULT_TIMEOUT_MS
    /**
     * ASR 停止策略：
     * - duration：按固定时长监听（listenDurationMs）
     * - auto：检测静音自动停止（silenceStopMs）
     */
    var listenStopMode: String = LISTEN_STOP_MODE_AUTO
    var listenDurationMs: Long = DEFAULT_LISTEN_DURATION_MS
    var silenceStopMs: Long = DEFAULT_SILENCE_STOP_MS
    var title: String? = null
    var showUi: Boolean = true
    var appKey: String? = null
    var region: String? = null
    /** ASR 识别结果写入的变量 id */
    var targetParamId: String? = ScriptParamEnv.getDefaultParam()?.getFullId()
    /** 是否保留 TTS/ASR 生成的音频文件，默认 false 表示用完后删除 */
    var keepAudio: Boolean = false

    override fun onExecute(): CmdExecuteResult {
        val modeValue = mode.lowercase()
        val needAsr = modeValue == MODE_ASR || modeValue == MODE_TTS_ASR
        val needTts = modeValue == MODE_TTS || modeValue == MODE_TTS_ASR
        if (!needAsr && !needTts) {
            return CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_mode_invalid))
        }

        if (needTts && ttsText.isNullOrBlank()) {
            return CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_tts_text_empty))
        }

        if (needAsr && !hasRecordAudioPermission()) {
            return CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_permission_record_audio))
        }

        val providerResult = prepareAudioProvider()
        if (!providerResult.success) {
            return CmdExecuteResult.failure(providerResult.message)
        }
        val audioProvider = providerResult.provider ?: return CmdExecuteResult.failure(
            getString(com.hive.i8n.R.string.tool_voice_interact_error_audio_provider_unavailable)
        )

        val asrProvider = audioProvider.getAsrProvider()
        val ttsProvider = audioProvider.getTtsProvider()
        if (needAsr && asrProvider == null) {
            return CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_asr_provider_unavailable))
        }
        if (needTts && ttsProvider == null) {
            return CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_tts_provider_unavailable))
        }

        val waiting = AtomicBoolean(true)
        val asrRunning = AtomicBoolean(false)
        val asrStopRequested = AtomicBoolean(false)
        val ttsRunning = AtomicBoolean(false)
        val completed = AtomicBoolean(false)

        var latestResultText = ""
        var latestAudioPath = ""
        var ttsAudioPath = ""
        var ttsAudioDuration = 0
        var isTimeout = false
        var isCanceled = false
        var finalResult: CmdExecuteResult? = null

        var lastResultAt = 0L
        var asrStartedAt = 0L
        var lastVoiceAt = 0L
        var hasSpeechStarted = false
        var asrStopRequestedAt = 0L
        val startedAt = System.currentTimeMillis()

        var dialog: ScriptVoiceInteractTipView? = null

        fun normalizeListenStopMode(raw: String?, listenDurationMs: Long): String {
            val s = raw?.trim()?.lowercase()
            if (!s.isNullOrEmpty()) return s
            return if (listenDurationMs > 0) LISTEN_STOP_MODE_DURATION else LISTEN_STOP_MODE_AUTO
        }

        fun postUi(action: () -> Unit) {
            UIHandlerUtils.getInstance().post { action.invoke() }
        }

        fun stopTtsSafely() {
            if (!ttsRunning.get()) return
            ttsRunning.set(false)
            try {
                (ttsProvider as? MSAudioTtsProvider)?.stopSpeak() ?: ttsProvider?.release()
            } catch (_: Exception) {
            }
        }

        fun buildData(): Map<String, Any?> {
            val data = mutableMapOf<String, Any?>(
                "mode" to modeValue,
                "text" to latestResultText,
                "audioPath" to if (modeValue == MODE_TTS) ttsAudioPath else latestAudioPath,
                "isTimeout" to isTimeout,
                "isCanceled" to isCanceled
            )
            if (modeValue == MODE_TTS) {
                data["audioDuration"] = ttsAudioDuration
            }
            return data
        }

        fun finishResult(result: CmdExecuteResult) {
            if (completed.compareAndSet(false, true)) {
                if (needAsr && !targetParamId.isNullOrBlank() && latestResultText.isNotEmpty()) {
                    writeParam(targetParamId, latestResultText)
                }
                // 防止命令已结束但 TTS 仍在播
                stopTtsSafely()
                finalResult = result
                waiting.set(false)
                postUi { dialog?.dismiss() }
                // 默认用完删除 TTS/ASR 临时音频，避免累积占用存储
                if (!keepAudio) {
                    listOf(ttsAudioPath, latestAudioPath).forEach { path ->
                        if (!path.isNullOrBlank()) {
                            try {
                                FileUtils.simpleDeleteFile(File(path))
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        fun stopAsrSafely() {
            if (asrRunning.compareAndSet(true, false)) {
                asrStopRequested.set(true)
                asrStopRequestedAt = System.currentTimeMillis()
                try {
                    asrProvider?.stopRecognize()
                } catch (_: Exception) {
                    if (!completed.get()) {
                        finishResult(CmdExecuteResult.success(buildData(), getString(com.hive.i8n.R.string.script_command_execute_may_success)))
                    }
                }
            }
        }

        fun startAsrSession() {
            val provider = asrProvider ?: return
            asrRunning.set(true)
            asrStopRequested.set(false)
            val now = System.currentTimeMillis()
            asrStartedAt = now
            lastVoiceAt = now
            lastResultAt = now
            hasSpeechStarted = false
            postUi {
                dialog?.setStatusText(getString(com.hive.i8n.R.string.tool_voice_interact_status_listening))
                dialog?.showListeningState(null)
            }
            provider.startRecognize(preferLanguage, object : IAudioAsrProvider.OnAudioRecognizedListener {
                override fun onRecognizedStart() {
                    val t = System.currentTimeMillis()
                    lastResultAt = t
                    lastVoiceAt = t
                }

                override fun onRecognizedStop() {
                    if (!completed.get()) {
                        finishResult(
                            CmdExecuteResult.success(
                                buildData(),
                                getString(com.hive.i8n.R.string.script_command_execute_may_success)
                            )
                        )
                    }
                }

                override fun onRecognizedError() {
                    if (!completed.get()) {
                        finishResult(CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_asr_failed)))
                    }
                }

                override fun onRecognizedChanged(volume: Int, data: ByteArray) {
                    if (volume >= DEFAULT_VOICE_ACTIVE_VOLUME_THRESHOLD) {
                        lastVoiceAt = System.currentTimeMillis()
                        hasSpeechStarted = true
                    }
                    postUi { dialog?.updateVolume(normalizeVolume(volume)) }
                }

                override fun onRecognizedResult(result: String, audioPath: String) {
                    val merged = result.trim()
                    if (merged.isNotEmpty()) {
                        latestResultText = merged
                        latestAudioPath = audioPath
                        val t = System.currentTimeMillis()
                        lastResultAt = t
                        lastVoiceAt = t
                        hasSpeechStarted = true
                        postUi { dialog?.updateRecognizedText(merged) }
                    }
                }
            })
        }

        if (showUi) {
            postUi {
                dialog = ScriptVoiceInteractTipView(GlobalApp.getContext())
                    .setDialogTitle(title?.trim().orEmpty())
                    .setCancelText(getString(com.hive.i8n.R.string.sc_opt_cancel))
                    .setOnCancelListener {
                        isCanceled = true
                        // 取消时优先打断播报，避免命令结束但仍在播
                        stopTtsSafely()
                        if (asrRunning.get()) {
                            stopAsrSafely()
                        } else {
                            finishResult(
                                CmdExecuteResult.success(
                                    buildData(),
                                    getString(com.hive.i8n.R.string.script_command_execute_may_success)
                                )
                            )
                        }
                    }
                val decodedTts = StringUtils.decoding(ttsText)
                if (modeValue == MODE_TTS) {
                    dialog.setStatusText(getString(com.hive.i8n.R.string.tool_voice_interact_status_speaking))
                    dialog.showSpeakingState(decodedTts)
                } else if (modeValue == MODE_ASR) {
                    dialog.setStatusText(getString(com.hive.i8n.R.string.tool_voice_interact_status_listening))
                    dialog.showListeningState(null)
                } else {
                    dialog.setStatusText(getString(com.hive.i8n.R.string.tool_voice_interact_status_preparing))
                    dialog.showSpeakingState(decodedTts)
                }
                dialog.show()
            }
        }

        val resolvedTtsText = parseParamText(StringUtils.decoding(ttsText)) ?: StringUtils.decoding(ttsText) ?: ""
        if (needTts) {
            ttsRunning.set(true)
            ttsProvider?.startSpeak(
                null,
                null,
                null,
                null,
                resolvedTtsText,
                object : IAudioTtsProvider.OnAudioSpeakListener {
                    override fun onSpeakStart() {
                        if (completed.get()) return
                        postUi {
                            dialog?.setStatusText(getString(com.hive.i8n.R.string.tool_voice_interact_status_speaking))
                            dialog?.showSpeakingState(StringUtils.decoding(ttsText))
                        }
                    }

                    override fun onSpeakSentences(sentences: String) {
                        if (completed.get()) return
                        postUi { dialog?.showSpeakingState(sentences) }
                    }

                    override fun onSpeakFinished(audioPath: String, audioDuration: Int) {
                        ttsRunning.set(false)
                        if (completed.get()) return
                        ttsAudioPath = audioPath
                        ttsAudioDuration = audioDuration
                        if (modeValue == MODE_TTS) {
                            finishResult(
                                CmdExecuteResult.success(
                                    buildData(),
                                    getString(com.hive.i8n.R.string.script_command_execute_success)
                                )
                            )
                        } else {
                            startAsrSession()
                        }
                    }

                    override fun onSpeakVisemeData(vismeData: MutableList<android.util.Pair<Float, Long>>?) {
                        if (completed.get()) return
                        postUi { dialog?.updateSpeakingVisemeData(vismeData) }
                    }

                    override fun onSpeakError() {
                        ttsRunning.set(false)
                        finishResult(CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_voice_interact_error_tts_failed)))
                    }
                }
            )
        } else {
            startAsrSession()
        }

        while (waiting.get()) {
            val now = System.currentTimeMillis()
            if (showUi && needTts && !asrRunning.get() && !completed.get()) {
                postUi { dialog?.tickSpeakingWaveform(now) }
            }
            if (!completed.get() && now - startedAt >= timeoutMs) {
                isTimeout = true
                if (asrRunning.get()) {
                    stopAsrSafely()
                } else {
                    stopTtsSafely()
                    finishResult(
                        CmdExecuteResult.success(
                            buildData(),
                            getString(com.hive.i8n.R.string.script_command_execute_may_success)
                        )
                    )
                }
            }

            if (asrRunning.get()) {
                val stopMode = normalizeListenStopMode(listenStopMode, listenDurationMs)
                if (stopMode == LISTEN_STOP_MODE_DURATION) {
                    if (listenDurationMs > 0 && asrStartedAt > 0 && now - asrStartedAt >= listenDurationMs) {
                        stopAsrSafely()
                    }
                } else {
                    // auto：仅在检测到用户开口之后才开始按静音计时，避免刚开始就快速结束（兼容旧行为）
                    if (silenceStopMs > 0 && hasSpeechStarted && now - lastVoiceAt >= silenceStopMs) {
                        stopAsrSafely()
                    }
                }
            }

            if (asrStopRequested.get() && !completed.get() && now - asrStopRequestedAt >= 1500L) {
                finishResult(
                    CmdExecuteResult.success(
                        buildData(),
                        getString(com.hive.i8n.R.string.script_command_execute_may_success)
                    )
                )
            }
            ScriptThreadManager.delay(100)
        }
        return finalResult ?: CmdExecuteResult.failure(getString(com.hive.i8n.R.string.tool_execute_failed))
    }

    override fun getCommandName(): String = getString(com.hive.i8n.R.string.cmd_voice_interact_name)

    override fun getCommandDescribe(): String = getString(com.hive.i8n.R.string.cmd_voice_interact_des)

    override fun getCommandIcon(): Int = R.drawable.sc_ic_dialogue

    override fun getCommand(): String {
        fun q(v: String?): String = when {
            v.isNullOrBlank() -> ""
            else -> v.encode()
        }
        return "${cmdPrefix()} mode=${q(mode)} ttsText=${q(ttsText)} preferLanguage=${q(preferLanguage)} timeoutMs=$timeoutMs listenStopMode=${q(listenStopMode)} listenDurationMs=$listenDurationMs silenceStopMs=$silenceStopMs title=${q(title)} showUi=$showUi appKey=${q(appKey)} region=${q(region)} output=${q(targetParamId)} keepAudio=$keepAudio"
    }

    override fun parseCmd(cmd: String) {
        val p = ScriptLineTokenizer.parseKeyValueParams(cmd)
        mode = p["mode"]?.decode()?.trim()?.lowercase() ?: MODE_TTS
        ttsText = p["ttsText"]?.decode()?.takeIf { it.isNotBlank() }
        preferLanguage = p["preferLanguage"]?.decode()?.takeIf { it.isNotBlank() }
        timeoutMs = p["timeoutMs"]?.toLongOrNull() ?: DEFAULT_TIMEOUT_MS
        listenStopMode = p["listenStopMode"]?.decode()?.trim()?.lowercase() ?: LISTEN_STOP_MODE_AUTO
        listenDurationMs = p["listenDurationMs"]?.toLongOrNull() ?: DEFAULT_LISTEN_DURATION_MS
        silenceStopMs = p["silenceStopMs"]?.toLongOrNull() ?: DEFAULT_SILENCE_STOP_MS
        title = p["title"]?.decode()?.takeIf { it.isNotBlank() }
        showUi = p["showUi"]?.toBooleanStrictOrNull() ?: true
        appKey = p["appKey"]?.decode()?.takeIf { it.isNotBlank() }
        region = p["region"]?.decode()?.takeIf { it.isNotBlank() }
        targetParamId = p["output"]?.decode()?.takeIf { it.isNotBlank() }
        keepAudio = p["keepAudio"]?.toBooleanStrictOrNull() ?: false
    }

    override fun getPermissionRequest(): List<String>? {
        return if (mode == MODE_ASR || mode == MODE_TTS_ASR) {
            mutableListOf(ScriptHelper.PERMISSION_RECORD_AUDIO)
        } else {
            null
        }
    }

    override fun isSupportDelay(): Boolean = true

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            GlobalApp.getContext(),
            ScriptHelper.PERMISSION_RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun prepareAudioProvider(): AudioProviderResult {
        val provider = try {
            ComponentManager.getInstance().getProvider(IAudioProvider::class.java) as? IAudioProvider
        } catch (_: Exception) {
            null
        } ?: return AudioProviderResult(false, getString(com.hive.i8n.R.string.tool_voice_interact_error_audio_provider_unavailable), null)

        if (!provider.isInitialized()) {
            val finalAppKey = SpeechCredentialHelper.normalize(appKey)
                ?: SpeechCredentialHelper.getMsSpeechKey()
            val finalRegion = SpeechCredentialHelper.normalize(region)
                ?: SpeechCredentialHelper.getMsSpeechRegion()
            if (finalAppKey.isNullOrBlank() || finalRegion.isNullOrBlank()) {
                return AudioProviderResult(
                    false,
                    getString(com.hive.i8n.R.string.tool_voice_interact_error_audio_config_missing),
                    null
                )
            }
            try {
                provider.init(
                    GlobalApp.getContext(),
                    AudioConfiguration(finalAppKey, finalRegion, GlobalApp.getContext().filesDir.absolutePath)
                )
            } catch (_: Exception) {
                return AudioProviderResult(
                    false,
                    getString(com.hive.i8n.R.string.tool_voice_interact_error_audio_init_failed),
                    null
                )
            }
        }
        return AudioProviderResult(true, null, provider)
    }
    
    

    private fun normalizeVolume(volume: Int): Float {
        // volume 期望范围：0..100（不同 ASR provider 的回调强度不同，这里做更“灵敏”的曲线）
        val v = (volume / 100f).coerceIn(0f, 1f)
        val boosted = v.pow(0.55f) // 提升低音量变化
        return if (volume > 0) boosted.coerceAtLeast(0.04f) else 0f
    }

    data class AudioProviderResult(
        val success: Boolean,
        val message: String?,
        val provider: IAudioProvider?
    )

    companion object {
        const val MODE_TTS = "tts"
        const val MODE_ASR = "asr"
        const val MODE_TTS_ASR = "tts_asr"
        const val LISTEN_STOP_MODE_DURATION = "duration"
        const val LISTEN_STOP_MODE_AUTO = "auto"
        private const val DEFAULT_TIMEOUT_MS = 15000L
        private const val DEFAULT_LISTEN_DURATION_MS = 10*1000L
        private const val DEFAULT_SILENCE_STOP_MS = 1200L
        private const val DEFAULT_VOICE_ACTIVE_VOLUME_THRESHOLD = 2

        fun createCommand(
            mode: String,
            ttsText: String?,
            preferLanguage: String?,
            timeoutMs: Long,
            listenStopMode: String,
            listenDurationMs: Long,
            silenceStopMs: Long,
            title: String?,
            showUi: Boolean,
            appKey: String?,
            region: String?,
            targetParamId: String? = null,
            keepAudio: Boolean = false
        ): CmdVoiceInteract {
            return CmdVoiceInteract().apply {
                this.mode = mode
                this.ttsText = ttsText
                this.preferLanguage = preferLanguage
                this.timeoutMs = timeoutMs
                this.listenStopMode = listenStopMode
                this.listenDurationMs = listenDurationMs
                this.silenceStopMs = silenceStopMs
                this.title = title
                this.showUi = showUi
                this.appKey = appKey
                this.region = region
                this.targetParamId = targetParamId ?: ScriptParamEnv.getDefaultParam()?.getFullId()
                this.keepAudio = keepAudio
            }
        }
    }
}
