package com.hive.audio.core

import android.content.Context
import com.hive.annotation.NotProguard
import com.hive.plugin.IComponentProvider
import com.hive.plugin.audio.AudioConfiguration
import com.hive.plugin.provider.IAudioAsrProvider
import com.hive.plugin.provider.IAudioTtsProvider
import com.hive.utils.debug.DLog
import com.hive.audio.providers.ms.MSAudioAsrProvider
import com.hive.audio.providers.ms.MSAudioTtsProvider
import com.hive.audio.providers.XFAudioAsrProvider
import com.hive.audio.interfaces.IAudioProvider
import com.hive.audio.core.AudioManager

/**
 * 统一的音频组件提供者
 * 遵循libCompon的设计模式，提供统一的音频服务入口
 */
@NotProguard
class AudioProvider : IAudioProvider {
    
    companion object {
        private const val TAG = "AudioProvider"
    }
    
    private var context: Context? = null
    private var configuration: AudioConfiguration? = null
    
    // 音频服务实例
    private var asrProvider: IAudioAsrProvider? = null
    private var ttsProvider: IAudioTtsProvider? = null
    private var audioManager: AudioManager? = null
    
    override fun init(context: Context) {
        this.context = context
        DLog.d(TAG, "AudioProvider initialized")
    }
    
    /**
     * 使用配置初始化音频服务
     */
    override fun init(context: Context, configuration: AudioConfiguration) {
        this.context = context
        this.configuration = configuration
        
        // 初始化音频管理器
        audioManager = AudioManager(context, configuration)
        
        // 初始化ASR提供者
        asrProvider = createAsrProvider()
        asrProvider?.init(context, configuration)
        
        // 初始化TTS提供者
        ttsProvider = createTtsProvider()
        ttsProvider?.init(context, configuration)
        
        DLog.d(TAG, "AudioProvider initialized with configuration")
    }
    
    /**
     * 获取ASR提供者
     */
    override fun getAsrProvider(): IAudioAsrProvider? {
        return asrProvider
    }
    
    /**
     * 获取TTS提供者
     */
    override fun getTtsProvider(): IAudioTtsProvider? {
        return ttsProvider
    }
    
    /**
     * 获取音频管理器
     */
    override fun getAudioManager(): AudioManager? {
        return audioManager
    }
    
    /**
     * 获取当前配置
     */
    override fun getConfiguration(): AudioConfiguration? {
        return configuration
    }
    
    /**
     * 创建ASR提供者实例
     * 可以根据配置选择不同的实现
     */
    private fun createAsrProvider(): IAudioAsrProvider? {
        return try {
            when (configuration?.asrProviderId) {
                AudioConfiguration.PROVIDER_XF -> XFAudioAsrProvider()
                AudioConfiguration.PROVIDER_MS, null -> MSAudioAsrProvider()
                else -> MSAudioAsrProvider()
            }
        } catch (e: Exception) {
            DLog.w(TAG, "Failed to create ASR provider: ${e.message}")
            null
        }
    }
    
    /**
     * 创建TTS提供者实例
     */
    private fun createTtsProvider(): IAudioTtsProvider? {
        return try {
            // 目前仅内置 MS TTS（后续可按 ttsProviderId 扩展更多实现）
            MSAudioTtsProvider()
        } catch (e: Exception) {
            DLog.e(TAG, "Failed to create TTS provider: ${e.message}")
            null
        }
    }
    
    /**
     * 释放所有资源
     */
    override fun release() {
        try {
            asrProvider?.release()
            ttsProvider?.release()
            audioManager?.release()
            
            asrProvider = null
            ttsProvider = null
            audioManager = null
            configuration = null
            context = null
            
            DLog.d(TAG, "AudioProvider released")
        } catch (e: Exception) {
            DLog.e(TAG, "Error releasing AudioProvider: ${e.message}")
        }
    }
    
    /**
     * 检查是否已初始化
     */
    override fun isInitialized(): Boolean {
        return context != null && configuration != null
    }
    
    /**
     * 获取音频服务状态
     */
    override fun getServiceStatus(): AudioServiceStatus {
        return AudioServiceStatus(
            isInitialized = isInitialized(),
            asrAvailable = asrProvider != null,
            ttsAvailable = ttsProvider != null,
            audioManagerAvailable = audioManager != null
        )
    }
}
