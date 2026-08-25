package com.hive.agent.views.chat

import android.content.Context
import android.text.TextUtils
import com.hive.audio.interfaces.IAudioProvider
import com.hive.config.BuildConfigHelper
import com.hive.config.SpeechCredentialHelper
import com.hive.plugin.ComponentManager
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.utils.debug.DLog
import com.hive.views.widgets.chat.ChatInputAsrProvider

/**
 * 使用 libAudio 的 IAudioProvider 实现 ChatInputAsrProvider，供 Agent 页与首页注入到 ChatInputContainer
 */
class ChatInputAsrProviderImpl(private val context: Context) : ChatInputAsrProvider {

    private val logTag: String = "ChatInputAsrProviderImpl"

    private var audioProvider: IAudioProvider? = null
    private var asrProvider: IAudioAsrProvider? = null
    private var currentListener: ChatInputAsrProvider.OnRecognizedListener? = null

    init {
        initAsrProvider(force = true)
    }

    private fun initAsrProvider(force: Boolean = false) {
        if (!force && asrProvider != null) return
        try {
            val rawProvider = ComponentManager.getInstance().getProvider(IAudioProvider::class.java)
            audioProvider = rawProvider as? IAudioProvider

            if (audioProvider == null) {
                asrProvider = null
            } else {
                val providerId = SpeechCredentialHelper.getAsrProviderId(
                    context,
                    AudioConfiguration.PROVIDER_MS
                )
                val msKey = SpeechCredentialHelper.getMsSpeechKey(context).orEmpty()
                val msRegion = SpeechCredentialHelper.getMsSpeechRegion(context).orEmpty()

                val configuration = when (providerId) {
                    AudioConfiguration.PROVIDER_XF -> {
                        val xfAppId = SpeechCredentialHelper.getXfAppId(context).orEmpty()
                        if (TextUtils.isEmpty(xfAppId)) null
                        else AudioConfiguration(xfAppId, "", context.filesDir.absolutePath).apply {
                            asrProviderId = AudioConfiguration.PROVIDER_XF
                        }
                    }
                    else -> {
                        if (TextUtils.isEmpty(msKey) || TextUtils.isEmpty(msRegion)) null
                        else AudioConfiguration(msKey, msRegion, context.filesDir.absolutePath).apply {
                            asrProviderId = AudioConfiguration.PROVIDER_MS
                            asrInitialSilenceTimeoutMs =
                                BuildConfigHelper.getMapInteger("asrInitialSilenceTimeoutMs", -1)
                            asrEndSilenceTimeoutMs =
                                BuildConfigHelper.getMapInteger("asrEndSilenceTimeoutMs", -1)
                            asrSegmentationSilenceTimeoutMs =
                                BuildConfigHelper.getMapInteger("asrSegmentationSilenceTimeoutMs", -1)
                        }
                    }
                }

                if (configuration != null) {
                    audioProvider?.init(context, configuration)
                    asrProvider = audioProvider?.getAsrProvider()
                } else {
                    asrProvider = null
                    DLog.w(
                        logTag,
                        "initAsrProvider: configuration is null (configure speech keys via Keystore / local.properties)"
                    )
                }
            }
        } catch (e: Exception) {
            asrProvider = null
            DLog.e(logTag, "initAsrProvider failed: ${e.message}", e)
        }
    }

    override fun startRecognize(listener: ChatInputAsrProvider.OnRecognizedListener) {
        currentListener = listener
        if (asrProvider == null) {
            initAsrProvider(force = true)
        }
        val provider = asrProvider
        if (provider == null) {
            listener.onError()
            return
        }
        provider.startRecognize(null, object : IAudioAsrProvider.OnAudioRecognizedListener {
            override fun onRecognizedStart() {}
            override fun onRecognizedStop() {
                currentListener?.onStop()
                currentListener = null
            }
            override fun onRecognizedError() {
                currentListener?.onError()
                currentListener = null
            }
            override fun onRecognizedChanged(volume: Int, data: ByteArray) { listener.onVolume(volume) }
            override fun onRecognizedResult(result: String, audioPath: String) {
                listener.onRecognizedResult(result, audioPath)
            }
        })
    }

    override fun stopRecognize() {
        asrProvider?.stopRecognize()
    }

    fun release() {
        currentListener = null
        audioProvider?.release()
    }
}
