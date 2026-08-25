package com.hive.views.widgets.chat

/**
 * 聊天输入控件所需的 ASR 能力抽象，由宿主（如 appScript）注入实现。
 * 避免 libViews 依赖 libAudio。
 */
interface ChatInputAsrProvider {
    fun startRecognize(listener: OnRecognizedListener)
    fun stopRecognize()

    interface OnRecognizedListener {
        fun onRecognizedResult(result: String, audioPath: String)
        fun onVolume(volume: Int)
        fun onStop()
        fun onError()
    }
}
