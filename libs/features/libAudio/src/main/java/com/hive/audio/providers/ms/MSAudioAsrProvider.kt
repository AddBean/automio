package com.hive.audio.providers.ms

import android.content.Context
import com.hive.annotation.NotProguard
import com.hive.audio.utils.AudioRecordEngine
import com.hive.audio.utils.LanguageHelper
import com.hive.audio.utils.MicrophoneStream
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.utils.debug.DLog
import com.hive.utils.file.FileUtils
import com.hive.utils.thread.UIHandlerUtils
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SessionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionCanceledEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.io.File
import java.util.Locale
import java.util.UUID


@NotProguard
class MSAudioAsrProvider : IAudioAsrProvider {
    private var microphoneStream: MicrophoneStream? = null
    private var audioRecordEngine: AudioRecordEngine? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: IAudioAsrProvider.OnAudioRecognizedListener? = null
    private var currentAudioName: String? = null
    private var configuration: AudioConfiguration? = null
    private var context: Context? = null

    private var fullFinalText: String = ""
    private var currentPartialText: String = ""
    private var isStopping: Boolean = false
    private var hasNotifiedStop: Boolean = false

    private val recognizingListener = { s: Any?, e: SpeechRecognitionEventArgs ->
        // recognizing 事件的 text 是“当前分段的临时结果”，不要累计到 fullFinalText，避免重复
        currentPartialText = e.result.text ?: ""
        val merged = (fullFinalText + currentPartialText).trim()
        executeInMainThread {
            if (merged.isNotEmpty()) listener?.onRecognizedResult(merged, currentAudioName)
        }
        DLog.d("MSAudioAsrProvider", "RECOGNIZING: Text=${e.result.text}")
    }

    private val recognizedListener = { s: Any?, e: SpeechRecognitionEventArgs ->
        if (e.result.reason == ResultReason.RecognizedSpeech) {
            val segment = e.result.text ?: ""
            if (segment.isNotEmpty()) {
                fullFinalText += segment
            }
            currentPartialText = ""
            executeInMainThread {
                val merged = fullFinalText.trim()
                if (merged.isNotEmpty()) listener?.onRecognizedResult(merged, currentAudioName)
            }

        } else if (e.result.reason == ResultReason.NoMatch) {
            // NoMatch 不一定意味着结束，交由 sessionStopped/stopRecognize 统一收尾
        }
    }

    private val cancelListener = { s: Any?, e: SpeechRecognitionCanceledEventArgs ->
        DLog.e("CANCELED: Reason=" + e.reason)
        if (e.reason == CancellationReason.Error) {
            DLog.e("CANCELED: ErrorCode=" + e.errorCode)
            DLog.e("CANCELED: ErrorDetails=" + e.errorDetails)
            DLog.e("CANCELED: Did you set the speech resource key and region values?")
        }
        executeInMainThread {
            listener?.onRecognizedError()

        }
        finishAndNotifyStop()
    }

    private val sessionStoppedListener = { s: Any?, e: SessionEventArgs? ->
        DLog.e("\n    Session stopped event.")
        finishAndNotifyStop()
    }

    override fun init(context: Context?) {

    }

    override fun init(context: Context, configuration: AudioConfiguration) {
        this.context = context
        this.configuration = configuration
        if (!File(configuration.savePath).exists()) {
            FileUtils.makeDirs(configuration.savePath)
        }
    }

    override fun startRecognize(
        preferLanguage: String?, listener: IAudioAsrProvider.OnAudioRecognizedListener?
    ) {
        if (configuration == null) {
            listener?.onRecognizedError()
            return
        }
        disposeSession(notifyStop = false)
        fullFinalText = ""
        currentPartialText = ""
        isStopping = false
        hasNotifiedStop = false
        listener?.onRecognizedStart()
        this.listener = listener
        val config = createSpeechConfig(configuration!!)
        val language = preferLanguage?.let { checkAndCorrectLanguageCode(it) } ?: getDefaultLanguage()
        if (!language.isNullOrEmpty()) config.speechRecognitionLanguage = language
        DLog.e("startRecognize")
        config.enableDictation()
        currentAudioName = configuration?.savePath + "/${getRandomName()}.wav"

        audioRecordEngine = AudioRecordEngine(currentAudioName)
        microphoneStream = MicrophoneStream(audioRecordEngine) { volume ->
            this.listener?.onRecognizedChanged(volume, ByteArray(0))
        }
        val audioConfig = AudioConfig.fromStreamInput(microphoneStream)
        speechRecognizer = SpeechRecognizer(config, audioConfig)
        addEventListener()
        speechRecognizer?.startContinuousRecognitionAsync()
    }

    private fun getDefaultLanguage(): String {
        return LanguageHelper.getLanguageTagByCodeAndRegion(
            Locale.getDefault().language,
            Locale.getDefault().country.uppercase(Locale.getDefault())
        )
    }

    private fun checkAndCorrectLanguageCode(preferLanguage: String): String? {
        if (!preferLanguage.contains("_")) return preferLanguage
        var langCode = preferLanguage.split("_")[0]
        var regionCode = preferLanguage.split("_")[1]
        return LanguageHelper.getLanguageTagByCodeAndRegion(langCode, regionCode)
    }


    private fun addEventListener() {

        speechRecognizer?.recognizing?.addEventListener(recognizingListener)

        speechRecognizer?.recognized?.addEventListener(recognizedListener)

        speechRecognizer?.canceled?.addEventListener(cancelListener)

        speechRecognizer?.sessionStopped?.addEventListener(sessionStoppedListener)
    }

    private fun executeInMainThread(function: () -> Unit) {
        UIHandlerUtils.getInstance().executeInMainThread {
            function.invoke()
        }
    }

    override fun stopRecognize() {
        if (isStopping) return
        isStopping = true
        try {
            speechRecognizer?.stopContinuousRecognitionAsync()
        } catch (_: Exception) {
            finishAndNotifyStop()
        }
    }

    private fun finishAndNotifyStop() {
        if (hasNotifiedStop) return
        hasNotifiedStop = true
        try {
            audioRecordEngine?.finishRecording()
        } catch (_: Exception) {}
        try {
            microphoneStream?.close()
        } catch (_: Exception) {}
        executeInMainThread {
            listener?.onRecognizedStop()
        }
    }

    private fun disposeSession(notifyStop: Boolean) {
        try {
            speechRecognizer?.recognizing?.removeEventListener(recognizingListener)
            speechRecognizer?.recognized?.removeEventListener(recognizedListener)
            speechRecognizer?.canceled?.removeEventListener(cancelListener)
            speechRecognizer?.sessionStopped?.removeEventListener(sessionStoppedListener)
        } catch (_: Exception) {}
        try {
            speechRecognizer?.stopContinuousRecognitionAsync()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.close()
        } catch (_: Exception) {}
        speechRecognizer = null
        try {
            audioRecordEngine?.finishRecording()
        } catch (_: Exception) {}
        audioRecordEngine = null
        try {
            microphoneStream?.close()
        } catch (_: Exception) {}
        microphoneStream = null
        if (notifyStop) {
            finishAndNotifyStop()
        } else {
            hasNotifiedStop = true
        }
    }

    private fun createSpeechConfig(configuration: AudioConfiguration): SpeechConfig {
        val config = SpeechConfig.fromSubscription(configuration.appKey, configuration.region)
        config.setProperty(PropertyId.SpeechServiceResponse_RequestSentenceBoundary, "false")
        // 端点参数：越小越快（但太小会截断），支持通过配置覆盖
        val initialTimeout = if (configuration.asrInitialSilenceTimeoutMs > 0) configuration.asrInitialSilenceTimeoutMs else 8000
        val endTimeout = if (configuration.asrEndSilenceTimeoutMs > 0) configuration.asrEndSilenceTimeoutMs else 700
        val segmentationTimeout = if (configuration.asrSegmentationSilenceTimeoutMs > 0) configuration.asrSegmentationSilenceTimeoutMs else 1200

        if (initialTimeout > 0) {
            config.setProperty(
                PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs,
                initialTimeout.toString()
            )
        }
        if (endTimeout > 0) {
            config.setProperty(
                PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs,
                endTimeout.toString()
            )
        }
        if (segmentationTimeout > 0) {
            config.setProperty(
                PropertyId.Speech_SegmentationSilenceTimeoutMs,
                segmentationTimeout.toString()
            )
        }
        return config
    }

    private fun getRandomName(): String {
        return UUID.randomUUID().toString()
    }

    override fun release() {
        try {
            disposeSession(notifyStop = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}