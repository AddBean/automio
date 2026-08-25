// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.inputmethod

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.script.R
import com.hive.script.driver.ScriptAccessHelper
import com.hive.script.inputmethod.ScriptInputMethodHelper.clearPendingText
import com.hive.script.inputmethod.ScriptInputMethodHelper.getPendingText
import com.hive.script.inputmethod.ScriptInputMethodHelper.shouldAnimate

/**
 * 自定义输入法服务，用于稳定地输入文本
 * 
 * @author jiadou
 * @date 2024
 */
class ScriptInputMethodService : InputMethodService() {
    
    companion object {
        private const val TAG = "ScriptInputMethod"
    }
    
    private var inputView: View? = null
    private var tvAppName: TextView? = null
    private var ivAppIcon: ImageView? = null
    private var tvStatus: TextView? = null
    private var viewStatusIndicator: View? = null
    private var tvInputContent: TextView? = null
    private var tvInputMode: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreateInputView(): View? {
        // 创建输入法视图
        inputView = LayoutInflater.from(this).inflate(R.layout.input_method_view, null)
        
        // 初始化视图组件
        tvAppName = inputView?.findViewById(R.id.tvAppName)
        ivAppIcon = inputView?.findViewById(R.id.ivAppIcon)
        tvStatus = inputView?.findViewById(R.id.tvStatus)
        viewStatusIndicator = inputView?.findViewById(R.id.viewStatusIndicator)
        tvInputContent = inputView?.findViewById(R.id.tvInputContent)
        tvInputMode = inputView?.findViewById(R.id.tvInputMode)
        
        // 更新界面信息
        updateAppInfo()
        updateInputContent()
        updateStatus(getString(com.hive.i8n.R.string.input_method_status_ready), StatusType.READY)
        
        return inputView
    }
    
    /**
     * 状态类型
     */
    private enum class StatusType {
        READY,      // 就绪
        INPUTTING, // 输入中
        SUCCESS,   // 成功
        ERROR      // 错误
    }
    
    /**
     * 更新应用信息
     */
    private fun updateAppInfo() {
        try {
            val packageName = ScriptAccessHelper.getForegroundAppPackageName()
            if (!TextUtils.isEmpty(packageName)) {
                val packageManager = packageManager
                val appInfo: ApplicationInfo? = try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                
                if (appInfo != null) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon: Drawable? = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        null
                    }
                    
                    handler.post {
                        tvAppName?.text = appName
                        if (appIcon != null) {
                            ivAppIcon?.setImageDrawable(appIcon)
                        }
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, getString(com.hive.i8n.R.string.input_method_log_app_info_failed, e.message ?: ""), e)
        }
        
        // 默认显示
        handler.post {
            tvAppName?.text = getString(com.hive.i8n.R.string.input_method_unknown_app)
            ivAppIcon?.setImageResource(R.drawable.ic_input)
        }
    }
    
    /**
     * 更新输入内容显示
     */
    private fun updateInputContent() {
        val pendingText = getPendingText()
        handler.post {
            if (!TextUtils.isEmpty(pendingText)) {
                tvInputContent?.text = pendingText
                // 更新输入模式
                val mode = if (shouldAnimate()) {
                    getString(com.hive.i8n.R.string.input_method_mode_animated)
                } else {
                    getString(com.hive.i8n.R.string.input_method_mode_normal)
                }
                tvInputMode?.text = mode
            } else {
                tvInputContent?.text = getString(com.hive.i8n.R.string.input_method_no_content)
                tvInputMode?.text = getString(com.hive.i8n.R.string.input_method_mode_normal)
            }
        }
    }
    
    /**
     * 更新状态显示
     */
    private fun updateStatus(statusText: String, statusType: StatusType) {
        handler.post {
            tvStatus?.text = statusText
            
            val statusColor = when (statusType) {
                StatusType.READY -> ContextCompat.getColor(this@ScriptInputMethodService, com.hive.i8n.R.color.tech_cyan)
                StatusType.INPUTTING -> ContextCompat.getColor(this@ScriptInputMethodService, com.hive.i8n.R.color.tech_blue_light)
                StatusType.SUCCESS -> ContextCompat.getColor(this@ScriptInputMethodService, com.hive.i8n.R.color.colorGreen)
                StatusType.ERROR -> ContextCompat.getColor(this@ScriptInputMethodService, com.hive.i8n.R.color.colorRed)
            }
            
            viewStatusIndicator?.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(statusColor)
        }
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        ScriptInputMethodHelper.setInputMethodService(this)
        updateAppInfo()
        updateStatus(getString(com.hive.i8n.R.string.input_method_status_ready), StatusType.READY)
        Log.d(TAG, getString(com.hive.i8n.R.string.input_method_log_started))
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        ScriptInputMethodHelper.setInputMethodActive(false)
        Log.d(TAG, getString(com.hive.i8n.R.string.input_method_log_finished))
    }
    
    /**
     * 输入文本到当前焦点输入框
     * 
     * @param text 要输入的文本
     * @param clearFirst 是否先清空现有文本
     * @return 是否成功输入
     */
    fun commitText(text: String, clearFirst: Boolean = false): Boolean {
        val inputConnection = currentInputConnection ?: return false
        
        return try {
            updateStatus(getString(com.hive.i8n.R.string.input_method_status_inputting), StatusType.INPUTTING)
            
            if (clearFirst) {
                // 先清空现有文本
                inputConnection.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
            }
            
            // 提交文本
            val result = inputConnection.commitText(text, 1)
            
            if (result) {
                updateStatus(getString(com.hive.i8n.R.string.input_method_status_success), StatusType.SUCCESS)
                handler.postDelayed({
                    updateStatus(getString(com.hive.i8n.R.string.input_method_status_ready), StatusType.READY)
                }, 1500)
                Log.d(TAG, getString(com.hive.i8n.R.string.input_method_log_text_committed, text))
            } else {
                updateStatus(getString(com.hive.i8n.R.string.input_method_status_error), StatusType.ERROR)
            }
            
            result
        } catch (e: Exception) {
            updateStatus(getString(com.hive.i8n.R.string.input_method_status_error), StatusType.ERROR)
            Log.e(TAG, getString(com.hive.i8n.R.string.input_method_log_text_failed, e.message ?: ""), e)
            false
        }
    }
    
    /**
     * 逐字输入文本（模拟打字效果）
     * 
     * @param text 要输入的文本
     * @param clearFirst 是否先清空现有文本
     * @param delayMs 每个字符之间的延迟（毫秒）
     * @return 是否成功输入
     */
    fun commitTextAnimated(
        text: String, 
        clearFirst: Boolean = false,
        delayMs: Long = 50
    ): Boolean {
        val inputConnection = currentInputConnection ?: return false
        
        return try {
            updateStatus(getString(com.hive.i8n.R.string.input_method_status_animated_inputting), StatusType.INPUTTING)
            
            if (clearFirst) {
                inputConnection.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
            }
            
            // 逐字输入
            for ((index, char) in text.withIndex()) {
                inputConnection.commitText(char.toString(), 1)
                
                // 更新进度显示
                val progress = ((index + 1) * 100 / text.length)
                handler.post {
                    tvInputContent?.text = "${text.substring(0, index + 1)}..."
                }
                
                Thread.sleep(delayMs)
            }
            
            updateStatus(getString(com.hive.i8n.R.string.input_method_status_success), StatusType.SUCCESS)
            handler.postDelayed({
                updateStatus(getString(com.hive.i8n.R.string.input_method_status_ready), StatusType.READY)
                updateInputContent()
            }, 1500)
            
            Log.d(TAG, getString(com.hive.i8n.R.string.input_method_log_animated_committed, text))
            true
        } catch (e: Exception) {
            updateStatus(getString(com.hive.i8n.R.string.input_method_status_error), StatusType.ERROR)
            Log.e(TAG, getString(com.hive.i8n.R.string.input_method_log_animated_failed, e.message ?: ""), e)
            false
        }
    }
    
    /**
     * 追加文本到当前输入框
     * 
     * @param text 要追加的文本
     * @return 是否成功追加
     */
    fun appendText(text: String): Boolean {
        val inputConnection = currentInputConnection ?: return false
        
        return try {
            // 将光标移到末尾
            inputConnection.setSelection(
                inputConnection.getTextBeforeCursor(Int.MAX_VALUE, 0)?.length ?: 0,
                inputConnection.getTextBeforeCursor(Int.MAX_VALUE, 0)?.length ?: 0
            )
            
            // 追加文本
            inputConnection.commitText(text, 1)
            Log.d(TAG, getString(com.hive.i8n.R.string.input_method_log_text_appended, text))
            true
        } catch (e: Exception) {
            Log.e(TAG, getString(com.hive.i8n.R.string.input_method_log_text_append_failed, e.message ?: ""), e)
            false
        }
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        ScriptInputMethodHelper.setInputMethodActive(true)
        
        // 更新界面信息
        updateAppInfo()
        updateInputContent()
        updateStatus(getString(com.hive.i8n.R.string.input_method_status_ready), StatusType.READY)
        
        // 检查是否有待输入的文本
        val pendingText = getPendingText()
        if (!TextUtils.isEmpty(pendingText)) {
            // 延迟一下确保输入框已准备好
            window?.window?.decorView?.postDelayed({
                val clearFirst = ScriptInputMethodHelper.shouldClearFirst()
                val animated = ScriptInputMethodHelper.shouldAnimate()
                
                if (animated) {
                    commitTextAnimated(pendingText!!, clearFirst)
                } else {
                    commitText(pendingText!!, clearFirst)
                }
                
                // 清空待输入文本
                clearPendingText()
                updateInputContent()
            }, 200)
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 拦截某些按键，避免影响输入
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 返回键时隐藏输入法
            requestHideSelf(0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

