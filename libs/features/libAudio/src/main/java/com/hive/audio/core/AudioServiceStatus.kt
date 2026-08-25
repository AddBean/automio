package com.hive.audio.core

import com.hive.annotation.NotProguard

/**
 * 音频服务状态
 * 用于监控音频服务的运行状态
 */
@NotProguard
data class AudioServiceStatus(
    val isInitialized: Boolean = false,
    val asrAvailable: Boolean = false,
    val ttsAvailable: Boolean = false,
    val audioManagerAvailable: Boolean = false
) {
    /**
     * 检查服务是否完全可用
     */
    fun isFullyAvailable(): Boolean {
        return isInitialized && asrAvailable && ttsAvailable && audioManagerAvailable
    }
    
    /**
     * 检查ASR服务是否可用
     */
    fun isAsrAvailable(): Boolean {
        return isInitialized && asrAvailable
    }
    
    /**
     * 检查TTS服务是否可用
     */
    fun isTtsAvailable(): Boolean {
        return isInitialized && ttsAvailable
    }
    
    /**
     * 获取状态描述
     */
    fun getStatusDescription(): String {
        return buildString {
            append("AudioServiceStatus(")
            append("initialized=$isInitialized, ")
            append("asr=$asrAvailable, ")
            append("tts=$ttsAvailable, ")
            append("manager=$audioManagerAvailable")
            append(")")
        }
    }
}
