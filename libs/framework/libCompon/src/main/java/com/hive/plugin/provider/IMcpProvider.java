// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.mcp.model.McpTool;
import com.hive.plugin.mcp.model.McpResource;
import com.hive.plugin.mcp.model.McpPrompt;

/**
 * MCP 服务提供者接口
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2024/12/19
 */
public interface IMcpProvider extends IComponentProvider {

    /**
     * 启动 MCP 服务
     *
     * @return 是否启动成功
     */
    void startMcpService(int ssePort, int streamablePort, IMcpProvider.OnServiceStatusCallback callback);


    /**
     * 停止 MCP 服务
     *
     * @return 是否停止成功
     */
    boolean stopMcpService();

    /**
     * 检查 MCP 服务是否正在运行
     *
     * @return 是否正在运行
     */
    boolean isMcpServiceRunning();


    /**
     * 注册工具
     *
     * @param tool 工具对象
     */
    void registerTool(McpTool tool);

    /**
     * 注销工具
     *
     * @param toolId 工具对象
     */
    void unregisterTool(String toolId);

    /**
     * 获取 MCP 服务 URL
     * @return
     */
    String getStreamableServerUrl();

    /**
     * 获取 MCP 服务 URL
     * @return
     */
    String getSseServerUrl();

    /**
     * 获取已注册的工具列表
     *
     * @return 工具列表
     */
    McpTool[] getRegisteredTools();

    /**
     * 注册资源
     *
     * @param resource 资源对象
     */
    void registerResource(McpResource resource);

    /**
     * 注销资源
     *
     * @param resource 资源对象
     */
    void unregisterResource(McpResource resource);

    /**
     * 获取已注册的资源列表
     *
     * @return 资源列表
     */
    McpResource[] getRegisteredResources();

    /**
     * 注册提示词
     *
     * @param prompt 提示词对象
     */
    void registerPrompt(McpPrompt prompt);

    /**
     * 注销提示词
     *
     * @param prompt 提示词对象
     */
    void unregisterPrompt(McpPrompt prompt);

    /**
     * 获取已注册的提示词列表
     *
     * @return 提示词列表
     */
    McpPrompt[] getRegisteredPrompts();

    /**
     * 存储二进制数据到 BlobStore，返回 blobId
     *
     * @param data     二进制数据
     * @param mimeType MIME 类型，如 image/jpeg
     * @return blobId，失败或服务未运行返回 null
     */
    String putBlob(byte[] data, String mimeType);

    /**
     * 获取 Blob 的 HTTP URL
     *
     * @param blobId putBlob 返回的 blobId
     * @return 可访问的 URL，如 http://127.0.0.1:6666/blob/{blobId}
     */
    String getBlobUrl(String blobId);

    interface OnServiceStatusCallback {

        void onMcpServiceReady();

    }
} 