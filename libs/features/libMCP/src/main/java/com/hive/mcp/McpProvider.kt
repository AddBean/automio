// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.mcp

import android.content.Context
import com.hive.annotation.NotProguard
import com.hive.mcp.service.McpService
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.mcp.model.McpPrompt
import com.hive.plugin.mcp.model.McpResource
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.provider.IMcpProvider

/**
 * libMCP 对外统一入口
 * 仅保留对外启动、停止、状态查询等接口，去除所有无效依赖和遗留实现。
 */
@NotProguard
class McpProvider : IMcpProvider {

    private var context: Context? = null

    override fun init(context: Context) {
        this.context = context
    }

    override fun startMcpService(
        ssePort: Int,
        streamablePort: Int,
        callback: IMcpProvider.OnServiceStatusCallback
    ) {
        context?.let { McpService.start(context!!, ssePort, streamablePort, callback) }
    }

    override fun stopMcpService(): Boolean {
        context?.let { McpService.stop(it) }
        return !McpService.isRunning()
    }

    override fun isMcpServiceRunning(): Boolean {
        return McpService.isRunning()
    }

    /**
     * 注册工具
     */
    override fun registerTool(tool: McpTool) {
        val service = McpService.getInstance() ?: return
        service.getToolRegistry().registerTool(tool)
    }

    override fun unregisterTool(toolId: String?) {
        if (toolId == null) return
        val service = McpService.getInstance() ?: return
        service.getToolRegistry().unregisterTool(toolId)
    }

    override fun getStreamableServerUrl(): String {
        return "http://127.0.0.1:${McpConst.StreamablePort}/mcp"
    }

    override fun getSseServerUrl(): String {
        return "http://127.0.0.1:${McpConst.SsePort}/mcp"
    }

    override fun getRegisteredTools(): Array<McpTool> {
        val service = McpService.getInstance() ?: return emptyArray()
        return service.getToolRegistry().getTools().values.toTypedArray()
    }

    /**
     * 注册资源
     */
    override fun registerResource(resource: McpResource) {
        val service = McpService.getInstance() ?: return
        service.getResourceManager().registerResource(resource)
    }

    override fun unregisterResource(resource: McpResource?) {
        if (resource == null) return
        val service = McpService.getInstance() ?: return
        service.getResourceManager().unregisterResource(resource.uri)
    }

    override fun getRegisteredResources(): Array<McpResource> {
        val service = McpService.getInstance() ?: return emptyArray()
        return service.getResourceManager().getResources().values.toTypedArray()
    }

    /**
     * 注册提示词
     */
    override fun registerPrompt(prompt: McpPrompt) {
        val service = McpService.getInstance() ?: return
        service.getPromptManager().registerPrompt(prompt)
    }

    override fun unregisterPrompt(prompt: McpPrompt?) {
        if (prompt == null) return
        val service = McpService.getInstance() ?: return
        service.getPromptManager().unregisterPrompt(prompt.name)
    }

    override fun getRegisteredPrompts(): Array<McpPrompt> {
        val service = McpService.getInstance() ?: return emptyArray()
        return service.getPromptManager().getPrompts().values.toTypedArray()
    }

    override fun putBlob(data: ByteArray, mimeType: String): String? {
        val service = McpService.getInstance() ?: return null
        return service.getBlobStore().put(data, mimeType)
    }

    override fun getBlobUrl(blobId: String?): String? {
        if (blobId.isNullOrBlank()) return null
        return "http://127.0.0.1:${McpConst.StreamablePort}/blob/$blobId"
    }

} 