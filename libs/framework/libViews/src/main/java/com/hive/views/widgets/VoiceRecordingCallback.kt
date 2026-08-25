package com.hive.views.widgets

/**
 * 录音过程中的 UI 反馈（波形 + 实时文字）
 */
interface VoiceRecordingCallback {
    fun onVolume(normalized: Float)
    fun onRecognizedText(text: CharSequence?)
}
