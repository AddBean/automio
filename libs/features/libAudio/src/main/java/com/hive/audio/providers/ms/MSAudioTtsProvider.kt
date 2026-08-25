package com.hive.audio.providers.ms

import android.content.Context
import com.hive.annotation.NotProguard
import com.hive.audio.utils.AudioStreamPlayer
import com.hive.audio.interfaces.OnAudioVisemeListener
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioTtsProvider
import com.hive.utils.debug.DLog
import java.text.BreakIterator
import java.util.UUID


/**
 * role="Girl"	声音模仿女孩。
role="Boy"	声音模仿男孩。
role="YoungAdultFemale"	声音模仿年轻的成年女性。
role="YoungAdultMale"	声音模仿年轻的成年男性。
role="OlderAdultFemale"	声音模仿年长的成年女性。
role="OlderAdultMale"	声音模仿年长的成年男性。
role="SeniorFemale"	声音模仿年老女性。
role="SeniorMale"	声音模仿年老男性。

style="advertisement_upbeat"	用兴奋和精力充沛的语气推广产品或服务。
style="affectionate"	以较高的音调和音量表达温暖而亲切的语气。 说话者处于吸引听众注意力的状态。 说话者的个性往往是讨喜的。
style="angry"	表达生气和厌恶的语气。
style="assistant"	数字助理用的是热情而轻松的语气。
style="calm"	以沉着冷静的态度说话。 语气、音调和韵律与其他语音类型相比要统一得多。
style="chat"	表达轻松随意的语气。
style="cheerful"	表达积极愉快的语气。
style="customerservice"	以友好热情的语气为客户提供支持。
style="depressed"	调低音调和音量来表达忧郁、沮丧的语气。
style="disgruntled"	表达轻蔑和抱怨的语气。 这种情绪的语音表现出不悦和蔑视。
style="documentary-narration"	用一种轻松、感兴趣和信息丰富的风格讲述纪录片，适合配音纪录片、专家评论和类似内容。
style="embarrassed"	在说话者感到不舒适时表达不确定、犹豫的语气。
style="empathetic"	表达关心和理解。
style="envious"	当你渴望别人拥有的东西时，表达一种钦佩的语气。
style="excited"	表达乐观和充满希望的语气。 似乎发生了一些美好的事情，说话人对此非常满意。
style="fearful"	以较高的音调、较高的音量和较快的语速来表达恐惧、紧张的语气。 说话人处于紧张和不安的状态。
style="friendly"	表达一种愉快、怡人且温暖的语气。 听起来很真诚且满怀关切。
style="gentle"	以较低的音调和音量表达温和、礼貌和愉快的语气。
style="hopeful"	表达一种温暖且渴望的语气。 听起来像是会有好事发生在说话人身上。
style="lyrical"	以优美又带感伤的方式表达情感。
style="narration-professional"	以专业、客观的语气朗读内容。
style="narration-relaxed"	为内容阅读表达一种舒缓而悦耳的语气。
style="newscast"	以正式专业的语气叙述新闻。
style="newscast-casual"	以通用、随意的语气发布一般新闻。
style="newscast-formal"	以正式、自信和权威的语气发布新闻。
style="poetry-reading"	在读诗时表达出带情感和节奏的语气。
style="sad"	表达悲伤语气。
style="serious"	表达严肃和命令的语气。 说话者的声音通常比较僵硬，节奏也不那么轻松。
style="shouting"	就像从遥远的地方说话或在外面说话，但能让自己清楚地听到
style="sports_commentary"	用轻松有趣的语气播报体育赛事。
style="sports_commentary_excited"	用快速且充满活力的语气播报体育赛事精彩瞬间。
style="whispering"	说话非常柔和，发出的声音小且温柔
style="terrified"	表达一种非常害怕的语气，语速快且声音颤抖。 听起来说话人处于不稳定的疯狂状态。
style="unfriendly"	表达一种冷淡无情的语气。
 */
@NotProguard
class MSAudioTtsProvider : IAudioTtsProvider {
    private var currentListener: IAudioTtsProvider.OnAudioSpeakListener? = null
    private var currentSentenceSpeakIndex: Int = 0
    private var currentSentences: MutableList<String> = mutableListOf()

    private var configuration: AudioConfiguration? = null
    private var currentStreamText = ""
    private val engine: MSAudioTtsEngine = MSAudioTtsEngine.get()
    private val audioStreamPlayer = AudioStreamPlayer(engine)

    override fun init(context: Context?, configuration: AudioConfiguration?) {
        this.configuration = configuration
        engine.init(configuration)
    }

    override fun init(context: Context?) {

    }


    /**
     * 流式播放
     */
    override fun startSpeakStream(
        voiceName: String?,
        voiceStyle: String?,
        voiceRole: String?,
        voiceRate: String?,
        delta: String?,
        isFinished: Boolean,
        listener: IAudioTtsProvider.OnAudioSpeakListener?
    ) {
        currentListener = listener
        if (currentSentences.isEmpty()) {
            engine.initEngine(getRandomName(), object : OnAudioVisemeListener {
                override fun onVisemeChanged(visme: List<Pair<Float, Long>>) {
                    listener?.onSpeakVisemeData(visme.map {
                        android.util.Pair(
                            it.first,
                            it.second
                        )
                    })
                }

                override fun onAudioCompleted() {

                }
            })
            currentListener?.onSpeakStart()
        }
        engine.setConfigs(voiceName, voiceStyle, voiceRole,voiceRate)
        currentStreamText += delta
        currentSentences = splitSentences(currentStreamText)
        if (!isFinished) {
            while (currentSentences.size > 1 && currentSentences.size > currentSentenceSpeakIndex + 1) {
                DLog.e(
                    "startSpeakStream",
                    "currentSentences[${currentSentenceSpeakIndex}]=" + currentSentences[currentSentenceSpeakIndex]
                )
                audioStreamPlayer.enqueueText(currentSentences[currentSentenceSpeakIndex],
                    { listener?.onSpeakSentences(it) })
                currentSentenceSpeakIndex++
            }
        } else {
            if (currentSentences.size > currentSentenceSpeakIndex) {
                audioStreamPlayer.enqueueText(currentSentences[currentSentenceSpeakIndex], {
                    listener?.onSpeakSentences(it)
                }) {
                    engine.saveAudio { path, duration ->
                        listener?.onSpeakFinished(path, duration)
                        resetStreamState()
                    }
                }
                DLog.e(
                    "startSpeakStream",
                    "currentSentences[${currentSentenceSpeakIndex}]=" + currentSentences[currentSentenceSpeakIndex]
                )
            }
        }

    }

    override fun startSpeak(
        voiceName: String?,
        voiceStyle: String?,
        voiceRole: String?,
        voiceRate:String?,
        text: String?,
        listener: IAudioTtsProvider.OnAudioSpeakListener?
    ) {
        text ?: return
        engine.setConfigs(voiceName, voiceStyle, voiceRole,voiceRate)
        engine.initEngine(getRandomName(), object : OnAudioVisemeListener {
            override fun onVisemeChanged(visme: List<Pair<Float, Long>>) {
                listener?.onSpeakVisemeData(visme.map {
                    android.util.Pair(
                        it.first,
                        it.second
                    )
                })
            }

            override fun onAudioCompleted() {

            }
        })
        listener?.onSpeakStart()
        audioStreamPlayer.enqueueText(text, {
            listener?.onSpeakSentences(it)
        }) {
            engine.saveAudio { path, duration ->
                listener?.onSpeakFinished(path, duration)
            }
        }
    }

    override fun release() {
        audioStreamPlayer.release()
        resetStreamState()
        currentListener = null
    }

    /**
     * 立即停止当前播报（用于取消/打断）。不做全局释放，仅清空队列并关闭本次合成会话。
     */
    @Synchronized
    fun stopSpeak() {
        try {
            audioStreamPlayer.release()
        } catch (_: Exception) {
        }
        resetStreamState()
        currentListener = null
    }

    private fun resetStreamState() {
        currentSentenceSpeakIndex = 0
        currentStreamText = ""
        currentSentences.clear()
    }



    private fun getRandomName(): String {
        return "tts_" + UUID.randomUUID().toString()
    }

    companion object{
        fun splitSentences(text: String): MutableList<String> {
            return try {
                val sentenceList: MutableList<String> = ArrayList()

                val iterator: BreakIterator = BreakIterator.getSentenceInstance()
                iterator.setText(text)

                var start: Int = iterator.first()
                var end: Int = iterator.next()

                while (end != BreakIterator.DONE) {
                    val sentence: String = text.substring(start, end).trim()
                    if (!sentence.isEmpty()) {
                        sentenceList.add(sentence)
                    }
                    start = end
                    end = iterator.next()
                }

                return sentenceList.toMutableList()
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf()
            }

        }
    }
}