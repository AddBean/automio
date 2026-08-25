package com.hive.audio.providers.ms

import android.R.attr
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.text.TextUtils
import com.hive.audio.interfaces.OnAudioVisemeListener
import com.hive.audio.utils.WavMergeUtil
import com.hive.plugin.audio.AudioConfiguration
import com.hive.utils.debug.DLog
import com.hive.utils.file.FileUtils
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.Connection
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesisEventArgs
import com.microsoft.cognitiveservices.speech.SpeechSynthesisVisemeEventArgs
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.util.EventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class MSAudioTtsEngine {

    private var saveAudioScope: Job? = null

    private var audioTrack: AudioTrack? = null
    private var onAudioVisemeListener: OnAudioVisemeListener? = null
    private var saveAudioFilePath: String? = null
    private var saveAudioVisemePath: String? = null
    private var configuration: AudioConfiguration? = null

    private var audioConfig: AudioConfig? = null
    private var speechSynthesizer: SpeechSynthesizer? = null
    private var voiceName: String? = null
    private var voiceStyle: String? = null
    private var voiceRole: String? = null
    private var voiceRate: String = "0%"
    private var connection: Connection? = null
    private var speechConfig: SpeechConfig? = null
    private var cacheBytes: MutableList<ByteArray>? = null
    private var voiceVisemes: MutableList<MutableList<Pair<Float, Long>>> = mutableListOf()
    @Volatile
    private var currentSpeakLatch: CountDownLatch? = null

    private val eventListener = { o: Any?, e: SpeechSynthesisVisemeEventArgs ->
        voiceVisemes.lastOrNull()?.add((e.audioOffset / 10000f) to e.visemeId)
        DLog.e("${e.animation}")
        onAudioVisemeListener?.onVisemeChanged(voiceVisemes.lastOrNull() ?: mutableListOf())
        Unit
    }

    fun init(configuration: AudioConfiguration?) {
        this.configuration = configuration
    }

    fun initEngine(name: String?, listener: OnAudioVisemeListener) {
        name ?: run {
            saveAudioFilePath = null
            saveAudioVisemePath = null
            return
        }
        onAudioVisemeListener = listener
        saveAudioFilePath = configuration?.savePath + "/$name.wav"
        saveAudioVisemePath = "$saveAudioFilePath.viseme"
        cacheBytes?.clear()
        cacheBytes = mutableListOf()
        voiceVisemes.clear()
    }

    fun setConfigs(
        voiceName: String?, voiceStyle: String?, voiceRole: String?, voiceRate: String?
    ) {
        this.voiceName = voiceName
        this.voiceStyle = voiceStyle
        this.voiceRole = voiceRole
        this.voiceRate = normalizeRate(voiceRate)
    }

    fun start(text: String) {
        val latch = CountDownLatch(1)
        currentSpeakLatch = latch

        val completedListener = EventHandler<SpeechSynthesisEventArgs> { _, _ ->
            try {
                DLog.e("AudioTtsEngine", "SynthesisCompleted")
                onAudioVisemeListener?.onAudioCompleted()
            } finally {
                latch.countDown()
            }
            Unit
        }

        if (speechSynthesizer != null) {
            speechConfig?.close();
            speechSynthesizer?.close();
            connection?.close();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTrack = AudioTrack(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                AudioTrack.getMinBufferSize(
                    24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ) * 2,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }
        speechConfig = SpeechConfig.fromSubscription(configuration?.appKey, configuration?.region)
        speechConfig?.setProperty(PropertyId.SpeechServiceResponse_RequestSentenceBoundary, "false")
        audioConfig = AudioConfig.fromDefaultSpeakerOutput()
        voiceVisemes.add(mutableListOf())
        DLog.e("AudioTtsEngine", "start text=$text")
        val finalVoiceName = voiceName?.trim()
            ?.takeIf { it.isNotEmpty() && it != "-" && it.lowercase() != "null" }
            ?: "zh-CN-XiaoxiaoNeural"
        val ssml = """
            <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xmlns:mstts="https://www.w3.org/2001/mstts" xml:lang="zh-CN">
                <voice name="$finalVoiceName">
                    <mstts:express-as 
                        ${if (TextUtils.isEmpty(voiceStyle)) "" else """ style="$voiceStyle" """}   
                        ${if (TextUtils.isEmpty(voiceRole)) "" else """ role="$voiceRole" """} 
                        styledegree="2">
                        
                        <prosody rate="$voiceRate">$text</prosody>
                        
                    </mstts:express-as>
                </voice>
            </speak>
        """
        speechSynthesizer = SpeechSynthesizer(speechConfig, audioConfig)
        connection = Connection.fromSpeechSynthesizer(speechSynthesizer);
        speechSynthesizer?.SynthesisCompleted?.addEventListener(completedListener)
        speechSynthesizer?.VisemeReceived?.addEventListener(eventListener)
        try {
            val result = speechSynthesizer?.SpeakSsml(ssml)
            if (result?.reason === ResultReason.SynthesizingAudioCompleted) {
                DLog.e(
                    "AudioTtsEngine", "Speech synthesized to speaker for text [" + attr.text + "]"
                )
            } else if (result?.reason === ResultReason.Canceled) {
                val cancellation = SpeechSynthesisCancellationDetails.fromResult(result)
                DLog.e("AudioTtsEngine", "CANCELED: Reason=" + cancellation.reason)
                if (cancellation.reason == CancellationReason.Error) {
                    DLog.e("AudioTtsEngine", "CANCELED: ErrorCode=" + cancellation.errorCode)
                    DLog.e("AudioTtsEngine", "CANCELED: ErrorDetails=" + cancellation.errorDetails)
                    DLog.e(
                        "AudioTtsEngine",
                        "CANCELED: Did you set the speech resource key and region values?"
                    )
                }
                latch.countDown()
            }
            DLog.e("AudioTtsEngine", "speechSynthesisResult: " + result?.reason)
            result?.audioData?.run {
                cacheBytes?.add(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            latch.countDown()
        } finally {
            // 确保播报真正结束后才返回，避免上层命令提前结束
            try {
                latch.await(5, TimeUnit.MINUTES)
            } catch (_: Exception) {
            }
            try {
                speechSynthesizer?.SynthesisCompleted?.removeEventListener(completedListener)
            } catch (_: Exception) {
            }
            if (currentSpeakLatch === latch) {
                currentSpeakLatch = null
            }
        }
    }

    @Synchronized
    fun saveAudio(onSaved: (path: String?, duration: Int) -> Unit) {
        saveAudioScope = GlobalScope.launch(Dispatchers.IO) {
            try {
                val tempFiles = mutableListOf<File>()
                var i = 0
                cacheBytes?.forEach {
                    val tempPath = saveAudioFilePath + i
                    writeBytes2File(tempPath, it)
                    tempFiles.add(File(tempPath))
                    i++
                }
                var info = ""
                saveAudioVisemePath?.run {
                    val mergeList = mutableListOf<Pair<Float, Long>>()
                    var currentStart = 0f
                    voiceVisemes.forEachIndexed { index, pairs ->
                        val durationAudio = withContext(Dispatchers.IO) {
                            if (!tempFiles[index].exists()) return@withContext 0
                            if (tempFiles[index].length() == 0L) return@withContext 0
                            getDuration(tempFiles[index].path)
                        }
                        mergeList.addAll(pairs.map { pair ->
                            (pair.first + currentStart) to pair.second
                        })
                        currentStart += durationAudio.toFloat()
                    }
                    info = mergeList.joinToString(separator = "\n") { pair ->
                        "${pair.first},${pair.second}"
                    }
                    FileUtils.writeFile(
                        this, info, true
                    )
                }
                WavMergeUtil.mergeWav(tempFiles, File(saveAudioFilePath))
                tempFiles.forEach {
                    it.delete()
                }
                val durationAudio = withContext(Dispatchers.IO) {
                    if (!File(saveAudioFilePath).exists()) return@withContext null
                    if (File(saveAudioFilePath).length() == 0L) return@withContext 0
                    getDuration(saveAudioFilePath).toInt()
                }
                withContext(Dispatchers.Main) {
                    durationAudio?.run {
                        cacheBytes?.clear()
                        onSaved.invoke(saveAudioFilePath, durationAudio)
                    } ?: kotlin.run {
                        onSaved.invoke(null, 0)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onSaved.invoke(null, 0)
                }
            }
        }
    }

    /**
     * 获取 视频 或 音频 时长
     * @param path 视频 或 音频 文件路径
     * @return 时长 毫秒值
     */
    private fun getDuration(path: String?): Long {
        val mmr = MediaMetadataRetriever()
        var duration: Long = 0
        try {
            if (path != null) {
                mmr.setDataSource(path)
            }
            val time = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = time!!.toLong()
            if (duration == 0L) {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
                duration = mediaPlayer.duration.toLong()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            mmr.release()
        }
        return duration
    }

    private fun writeBytes2File(filePath: String?, bytes: ByteArray?) {
        val outputStream = FileOutputStream(filePath, false) // 第二个参数为 true，表示追加写入
        outputStream.write(bytes)
        outputStream.close()
    }

    private fun normalizeRate(rate: String?): String {
        val raw = rate?.trim()
        if (raw.isNullOrEmpty()) return "0%"
        // 允许 "0" / "0%" / "+10%" / "-10%" 等
        return if (raw.endsWith("%")) raw else "${raw}%"
    }


    fun preCacheAudio(text: String) {

    }

    fun release() {
        releaseSpeech()
        releaseAudio()
    }

    fun releaseSpeech() {
        try {
            if (saveAudioScope?.isActive == true)
                saveAudioScope?.cancel()
            currentSpeakLatch?.countDown()
            speechSynthesizer?.VisemeReceived?.removeEventListener(eventListener)
            // Release speech synthesizer and its dependencies
            speechSynthesizer?.impl?.close()
            speechSynthesizer = null
            connection?.closeConnection()
            speechConfig?.impl?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releaseAudio() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        private val instance: MSAudioTtsEngine by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MSAudioTtsEngine()
        }

        fun get(): MSAudioTtsEngine {
            return instance
        }
    }


}