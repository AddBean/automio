package com.hive.audio.providers

import android.content.Context
import android.os.Bundle
import com.hive.annotation.NotProguard
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.utils.GlobalApp.getResources
import com.hive.utils.debug.DLog
import com.hive.utils.file.FileUtils
import com.hive.audio.utils.JsonParser
import com.hive.audio.BuildConfig
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.Setting
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechRecognizer
import com.iflytek.cloud.SpeechUtility
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.UUID


@NotProguard
class XFAudioAsrProvider : IAudioAsrProvider, InitListener, RecognizerListener {

    private var preferLanguage: String? = null
    private var listener: IAudioAsrProvider.OnAudioRecognizedListener? = null
    private val mIatResults: HashMap<String, String> = LinkedHashMap()

    private var configuration: AudioConfiguration? = null
    private var context: Context? = null

    private var mIat: SpeechRecognizer? = null

    override fun init(context: Context?) {

    }

    override fun init(context: Context, configuration: AudioConfiguration) {
        this.context = context
        this.configuration = configuration
        if (!File(configuration.savePath).exists()) {
            FileUtils.makeDirs(configuration.savePath)
        }
        SpeechUtility.createUtility(context, "appid=" + configuration.appKey)
        Setting.setShowLog(BuildConfig.DEBUG)
    }

    override fun startRecognize(
        preferLanguage: String?,
        listener: IAudioAsrProvider.OnAudioRecognizedListener?
    ) {
        if (configuration == null) {
            listener?.onRecognizedError()
            return
        }
        this.preferLanguage = preferLanguage
        mIatResults.clear()
        this.listener = listener
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        if (mIat == null) {
            mIat = SpeechRecognizer.createRecognizer(context, this)
        }
        // 设置参数
        setParam()
        mIat?.startListening(this)
    }

    override fun stopRecognize() {
        // stopListening 更符合“主动停止”；cancel 偏向异常中断
        try {
            mIat?.stopListening()
        } catch (_: Exception) {
            mIat?.cancel()
        }
    }

    private fun setParam() {

        // 清空参数
        mIat?.setParameter(SpeechConstant.PARAMS, null)
        // 设置听写引擎
        mIat?.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        // 设置返回结果格式
        mIat?.setParameter(SpeechConstant.RESULT_TYPE, "json")

        preferLanguage?.let {
            mIat?.setParameter(SpeechConstant.LANGUAGE, it)
        }

        // 端点参数：尽量快（但太小会截断），优先读取统一配置
        val vadBos = if ((configuration?.asrInitialSilenceTimeoutMs ?: -1) > 0) {
            configuration?.asrInitialSilenceTimeoutMs ?: 8000
        } else 8000
        val vadEos = if ((configuration?.asrEndSilenceTimeoutMs ?: -1) > 0) {
            configuration?.asrEndSilenceTimeoutMs ?: 1200
        } else 1200

        // 设置语音前端点:静音超时时间
        mIat?.setParameter(SpeechConstant.VAD_BOS, vadBos.toString())
        // 设置语音后端点:后端点静音检测时间
        mIat?.setParameter(SpeechConstant.VAD_EOS, vadEos.toString())
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat?.setParameter(
            SpeechConstant.ASR_PTT,
            "1"
        )
        // 设置音频保存路径，保存音频格式支持pcm、wav.
        mIat?.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
        mIat?.setParameter(
            SpeechConstant.ASR_AUDIO_PATH,
            configuration?.savePath + "/${getRandomName()}.wav"
        )

        // 动态修正，开启动态修正功能（便于流式中间结果更快趋于稳定）
        mIat?.setParameter("dwa", "wpgs")
    }


    override fun onInit(code: Int) {
        if (code != ErrorCode.SUCCESS) {
            DLog.e("初始化失败，错误码：$code,请点击网址https://www.xfyun.cn/document/error-code查询解决方案")
        }
    }

    override fun onVolumeChanged(values: Int, data: ByteArray?) {
        listener?.onRecognizedChanged(values, data)
    }

    override fun onBeginOfSpeech() {
        listener?.onRecognizedStart()
    }

    override fun onEndOfSpeech() {
        listener?.onRecognizedStop()
    }

    override fun onResult(results: RecognizerResult?, isLast: Boolean) {
        val text = JsonParser.parseIatResult(results?.resultString)

        var sn: String? = null
        var pgs: String? = null
        var rg: String? = null
        // 读取json结果中的sn字段
        // 读取json结果中的sn字段
        try {
            val resultJson = JSONObject(results!!.resultString)
            sn = resultJson.optString("sn")
            pgs = resultJson.optString("pgs")
            rg = resultJson.optString("rg")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
        //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
        if (pgs == "rpl") {
            val strings = rg!!.replace("[", "").replace("]", "").split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val begin = strings[0].toInt()
            val end = strings[1].toInt()
            for (i in begin..end) {
                mIatResults.remove(i.toString() + "")
            }
        }

        mIatResults[sn!!] = text
        val resultBuffer = StringBuffer()
        for (key in mIatResults.keys) {
            resultBuffer.append(mIatResults[key])
        }


        listener?.onRecognizedResult(
            resultBuffer.toString(),
            mIat?.getParameter(SpeechConstant.ASR_AUDIO_PATH)
        )
    }

    override fun onError(p0: SpeechError?) {
        listener?.onRecognizedError()
    }

    override fun onEvent(p0: Int, p1: Int, p2: Int, p3: Bundle?) {

    }

    private fun getRandomName(): String {
        return UUID.randomUUID().toString()
    }

    override fun release() {
        mIat?.destroy()
        mIat = null
    }
}