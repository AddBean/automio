package com.hive.audio.core

import android.content.Context
import androidx.core.content.ContextCompat
import com.hive.annotation.NotProguard
import com.hive.plugin.audio.AudioConfiguration
import com.hive.utils.debug.DLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.hive.audio.providers.ms.MSAudioTtsEngine
import com.hive.audio.utils.AudioRecordEngine
import com.hive.audio.utils.AudioStreamPlayer
import com.hive.audio.utils.LanguageHelper
import com.hive.audio.interfaces.IAudioManager

/**
 * 音频管理器
 * 统一管理音频相关的核心功能
 */
@NotProguard
class AudioManager(
    private val context: Context,
    private val configuration: AudioConfiguration
) : IAudioManager {
    
    companion object {
        private const val TAG = "AudioManager"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 音频录制引擎
    private var audioRecordEngine: AudioRecordEngine? = null
    
    // 音频播放器
    private var audioStreamPlayer: AudioStreamPlayer? = null
    
    // 语言助手
    private val languageHelper = LanguageHelper
    
    init {
        DLog.d(TAG, "AudioManager initialized")
    }
    
    /**
     * 创建音频录制引擎
     */
    override fun createAudioRecordEngine(saveName: String?): AudioRecordEngine? {
        return try {
            val engine = AudioRecordEngine(saveName)
            audioRecordEngine = engine
            DLog.d(TAG, "AudioRecordEngine created: $saveName")
            engine
        } catch (e: Exception) {
            DLog.e(TAG, "Failed to create AudioRecordEngine: ${e.message}")
            null
        }
    }
    
    /**
     * 创建音频流播放器
     */
    override fun createAudioStreamPlayer(): AudioStreamPlayer? {
        return try {
            val ttsEngine = MSAudioTtsEngine.get()
            ttsEngine.init(configuration)
            val player = AudioStreamPlayer(ttsEngine)
            audioStreamPlayer = player
            DLog.d(TAG, "AudioStreamPlayer created")
            player
        } catch (e: Exception) {
            DLog.e(TAG, "Failed to create AudioStreamPlayer: ${e.message}")
            null
        }
    }
    
    /**
     * 获取语言助手
     */
    override fun getLanguageHelper(): LanguageHelper {
        return languageHelper
    }
    
    /**
     * 检查权限
     */
    override fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 请求权限
     */
    override suspend fun requestPermissions(): Boolean {
        // 权限请求应该在应用层处理
        return checkPermissions()
    }
    
    /**
     * 获取音频配置
     */
    override fun getConfiguration(): AudioConfiguration {
        return configuration
    }
    
    /**
     * 释放所有资源
     */
    override fun close() {
        try {
            scope.cancel()
            audioRecordEngine = null
            audioStreamPlayer = null
            DLog.d(TAG, "AudioManager released")
        } catch (e: Exception) {
            DLog.e(TAG, "Error releasing AudioManager: ${e.message}")
        }
    }
    
    /**
     * 释放资源（兼容旧接口）
     */
    override fun release() {
        close()
    }
}
