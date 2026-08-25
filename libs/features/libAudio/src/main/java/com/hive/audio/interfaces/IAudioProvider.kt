package com.hive.audio.interfaces

import android.content.Context
import com.hive.plugin.IComponentProvider
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.plugin.provider.IAudioTtsProvider
import com.hive.audio.core.AudioManager
import com.hive.audio.core.AudioServiceStatus

/**
 * 统一的音频服务接口
 * 遵循libCompon的设计模式，提供统一的音频服务入口
 */
interface IAudioProvider : IComponentProvider {
    
    /**
     * 使用配置初始化音频服务
     */
    fun init(context: Context, configuration: AudioConfiguration)
    
    /**
     * 获取ASR提供者
     */
    fun getAsrProvider(): IAudioAsrProvider?
    
    /**
     * 获取TTS提供者
     */
    fun getTtsProvider(): IAudioTtsProvider?
    
    /**
     * 获取音频管理器
     */
    fun getAudioManager(): AudioManager?
    
    /**
     * 获取当前配置
     */
    fun getConfiguration(): AudioConfiguration?
    
    /**
     * 获取音频服务状态
     */
    fun getServiceStatus(): AudioServiceStatus
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean
    
    /**
     * 释放所有资源
     */
    fun release()
}
