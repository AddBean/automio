// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp.registry

import com.hive.plugin.mcp.model.McpResource
import java.util.concurrent.ConcurrentHashMap

/**
 * Android兼容的资源管理器
 */
class ResourceManager {
    private val resources = ConcurrentHashMap<String, McpResource>()
    
    /**
     * 注册资源
     */
    fun registerResource(resource: McpResource) {
        resources[resource.uri] = resource
    }
    
    /**
     * 注销资源
     */
    fun unregisterResource(uri: String) {
        resources.remove(uri)
    }
    
    /**
     * 获取所有资源
     */
    fun getResources(): Map<String, McpResource> = resources.toMap()
    
    /**
     * 检查资源是否存在
     */
    fun hasResource(uri: String): Boolean = resources.containsKey(uri)
} 