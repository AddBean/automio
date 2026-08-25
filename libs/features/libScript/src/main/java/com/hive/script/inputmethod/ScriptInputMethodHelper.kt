// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.inputmethod

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.hive.script.inputmethod.ScriptInputMethodService
import com.hive.utils.GlobalApp
import java.lang.ref.WeakReference

/**
 * 输入法辅助类，用于切换输入法和输入文本
 * 
 * @author jiadou
 * @date 2024
 */
object ScriptInputMethodHelper {
    
    private const val TAG = "ScriptInputMethodHelper"
    
    // 待输入的文本
    private var pendingText: String? = null
    
    // 是否先清空现有文本
    private var shouldClearFirst: Boolean = true
    
    // 是否使用动画输入（逐字输入）
    private var shouldAnimate: Boolean = false
    
    // 输入法服务实例的弱引用
    private var inputMethodServiceRef: WeakReference<ScriptInputMethodService>? = null
    
    // 输入法是否激活
    private var isActive: Boolean = false
    
    /**
     * 设置输入法服务实例
     */
    fun setInputMethodService(service: ScriptInputMethodService?) {
        inputMethodServiceRef = service?.let { WeakReference(it) }
    }
    
    /**
     * 获取输入法服务实例
     */
    private fun getInputMethodService(): ScriptInputMethodService? {
        return inputMethodServiceRef?.get()
    }
    
    /**
     * 检查输入法是否已启用
     */
    fun isInputMethodEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledInputMethods = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS
        )
        
        val packageName = context.packageName
        return enabledInputMethods?.contains(packageName) == true
    }
    
    /**
     * 获取输入法ID
     */
    fun getInputMethodId(context: Context): String {
        return "${context.packageName}/.inputmethod.ScriptInputMethodService"
    }
    
    /**
     * 切换到自定义输入法
     * 
     * @param context 上下文
     * @return 是否成功切换
     */
    fun switchToInputMethod(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val inputMethodId = getInputMethodId(context)
            imm?.setInputMethod(null, inputMethodId)
            Log.d(TAG, "切换到自定义输入法: $inputMethodId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "切换输入法失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 设置待输入的文本
     */
    fun setPendingText(text: String, clearFirst: Boolean = true, animate: Boolean = false) {
        pendingText = text
        shouldClearFirst = clearFirst
        shouldAnimate = animate
        Log.d(TAG, "设置待输入文本: $text, clearFirst=$clearFirst, animate=$animate")
    }
    
    /**
     * 获取待输入的文本
     */
    fun getPendingText(): String? {
        return pendingText
    }
    
    /**
     * 清空待输入的文本
     */
    fun clearPendingText() {
        pendingText = null
        shouldClearFirst = true
        shouldAnimate = false
    }
    
    /**
     * 是否应该先清空现有文本
     */
    fun shouldClearFirst(): Boolean {
        return shouldClearFirst
    }
    
    /**
     * 是否应该使用动画输入
     */
    fun shouldAnimate(): Boolean {
        return shouldAnimate
    }
    
    /**
     * 直接输入文本（如果输入法已激活）
     * 
     * @param text 要输入的文本
     * @param clearFirst 是否先清空现有文本
     * @param animate 是否逐字输入
     * @return 是否成功输入
     */
    fun inputText(text: String, clearFirst: Boolean = true, animate: Boolean = false): Boolean {
        val service = getInputMethodService()
        if (service == null) {
            Log.w(TAG, "输入法服务未激活，无法直接输入")
            return false
        }
        
        return if (animate) {
            service.commitTextAnimated(text, clearFirst)
        } else {
            service.commitText(text, clearFirst)
        }
    }
    
    /**
     * 追加文本（如果输入法已激活）
     * 
     * @param text 要追加的文本
     * @return 是否成功追加
     */
    fun appendText(text: String): Boolean {
        val service = getInputMethodService()
        if (service == null) {
            Log.w(TAG, "输入法服务未激活，无法追加文本")
            return false
        }
        
        return service.appendText(text)
    }
    
    /**
     * 设置输入法激活状态
     */
    fun setInputMethodActive(active: Boolean) {
        isActive = active
        Log.d(TAG, "输入法激活状态: $active")
    }
    
    /**
     * 检查输入法是否激活
     */
    fun isInputMethodActive(): Boolean {
        return isActive
    }
    
    /**
     * 隐藏输入法
     */
    fun hideInputMethod(context: Context) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(null, 0)
        } catch (e: Exception) {
            Log.e(TAG, "隐藏输入法失败: ${e.message}", e)
        }
    }
}

