package com.hive.audio.interfaces

import android.content.Context
import com.hive.plugin.audio.AudioConfiguration
import com.hive.audio.utils.AudioRecordEngine
import com.hive.audio.utils.AudioStreamPlayer
import com.hive.audio.utils.LanguageHelper

/**
 * 音频管理器接口
 * 统一管理音频相关的核心功能
 */
interface IAudioManager : AutoCloseable {
    
    /**
     * 创建音频录制引擎
     */
    fun createAudioRecordEngine(saveName: String?): AudioRecordEngine?
    
    /**
     * 创建音频流播放器
     */
    fun createAudioStreamPlayer(): AudioStreamPlayer?
    
    /**
     * 获取语言助手
     */
    fun getLanguageHelper(): LanguageHelper
    
    
    /**
     * 检查权限
     */
    fun checkPermissions(): Boolean
    
    /**
     * 请求权限
     */
    suspend fun requestPermissions(): Boolean
    
    /**
     * 获取音频配置
     */
    fun getConfiguration(): AudioConfiguration
    
    /**
     * 释放资源（兼容旧接口）
     */
    fun release()
}
