// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp.tools

import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.model.McpResultFile
import com.hive.plugin.provider.IMcpProvider
import com.hive.utils.file.FileUtils

/**
 * MCP 文件结果处理辅助类
 * 统一处理多模态文件（图片、视频等）的返回逻辑
 * 
 * 功能：
 * 1. 将本地文件转换为 McpResultFile，同时提供 blob URL 和 base64
 * 2. 提取纯 base64 数据（去掉 data URL 前缀）
 * 3. 统一管理文件处理逻辑，便于维护和扩展
 */
object McpFileResultHelper {

    /**
     * 从本地文件路径创建 McpResultFile
     * 
     * @param filePath 本地文件路径
     * @param mimeType MIME 类型，如 "image/jpeg", "image/png" 等
     * @param defaultMimeType 如果无法确定 MIME 类型时的默认值，默认为 "image/jpeg"
     * @return McpResultFile，包含 blob URL 和 base64 数据
     */
    fun createFileResult(
        filePath: String,
        mimeType: String? = null,
        defaultMimeType: String = "image/jpeg"
    ): McpResultFile? {
        if (filePath.isBlank()) return null

        val bytes = FileUtils.readFileToBytes(filePath) ?: return null
        val actualMimeType = mimeType ?: detectMimeType(filePath) ?: defaultMimeType
        
        // 尝试存储到 BlobStore 并获取 URL
        val provider = ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider
        val blobId = provider?.putBlob(bytes, actualMimeType)
        val blobUrl = blobId?.let { provider?.getBlobUrl(it) }

        // 转换为 base64（总是提供，作为回退方案）
        val base64Data = FileUtils.convertLocalFileToBase64(filePath, actualMimeType)

        return if (blobUrl != null) {
            // 有 blob URL：同时提供 URL 和 base64
            McpResultFile(
                name = FileUtils.getFileName(filePath),
                mimeType = actualMimeType,
                size = bytes.size.toLong(),
                url = blobUrl,
                base64 = base64Data
            )
        } else {
            // 没有 blob URL：只提供 base64
            McpResultFile(
                name = FileUtils.getFileName(filePath),
                mimeType = actualMimeType,
                size = -1L,
                url = filePath,
                base64 = base64Data
            )
        }
    }

    /**
     * 提取纯 base64 字符串（去掉 data URL 前缀）
     * 
     * @param base64Data 可能是 data URL 格式（data:image/jpeg;base64,xxx）或纯 base64 字符串
     * @return 纯 base64 字符串
     */
    fun extractPureBase64(base64Data: String?): String? {
        if (base64Data.isNullOrBlank()) return null
        
        return if (base64Data.startsWith("data:")) {
            // 提取 data URL 中的 base64 部分
            val commaIndex = base64Data.indexOf(',')
            if (commaIndex > 0) {
                base64Data.substring(commaIndex + 1)
            } else {
                base64Data
            }
        } else {
            base64Data
        }
    }

    /**
     * 检测文件的 MIME 类型
     * 
     * @param filePath 文件路径
     * @return MIME 类型，如果无法确定则返回 null
     */
    private fun detectMimeType(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "pdf" -> "application/pdf"
            else -> null
        }
    }

    /**
     * 检查文件是否为图片类型
     * 
     * @param mimeType MIME 类型
     * @return 如果是图片类型返回 true
     */
    fun isImageType(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    /**
     * 检查文件是否为视频类型
     * 
     * @param mimeType MIME 类型
     * @return 如果是视频类型返回 true
     */
    fun isVideoType(mimeType: String?): Boolean {
        return mimeType?.startsWith("video/") == true
    }
}
