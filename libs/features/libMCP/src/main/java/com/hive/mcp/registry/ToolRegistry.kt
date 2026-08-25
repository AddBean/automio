// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.registry

import com.hive.plugin.mcp.model.McpTool
import java.util.concurrent.ConcurrentHashMap

/**
 * Android兼容的工具注册器
 */
class ToolRegistry {
    private val tools = ConcurrentHashMap<String, McpTool>()
    
    /**
     * 注册工具
     */
    fun registerTool(tool: McpTool) {
        tools[tool.name] = tool
    }
    
    /**
     * 注销工具
     */
    fun unregisterTool(name: String) {
        tools.remove(name)
    }
    
    /**
     * 获取所有工具
     */
    fun getTools(): Map<String, McpTool> = tools.toMap()
    
    /**
     * 检查工具是否存在
     */
    fun hasTool(name: String): Boolean = tools.containsKey(name)
} 