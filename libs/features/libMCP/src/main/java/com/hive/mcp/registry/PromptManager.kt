// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.registry

import com.hive.plugin.mcp.model.McpPrompt
import java.util.concurrent.ConcurrentHashMap

/**
 * Android兼容的提示管理器
 */
class PromptManager {
    private val prompts = ConcurrentHashMap<String, McpPrompt>()
    
    /**
     * 注册提示
     */
    fun registerPrompt(prompt: McpPrompt) {
        prompts[prompt.name] = prompt
    }
    
    /**
     * 注销提示
     */
    fun unregisterPrompt(name: String) {
        prompts.remove(name)
    }
    
    /**
     * 获取所有提示
     */
    fun getPrompts(): Map<String, McpPrompt> = prompts.toMap()
    
    /**
     * 检查提示是否存在
     */
    fun hasPrompt(name: String): Boolean = prompts.containsKey(name)
} 