// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import android.content.Context
import android.net.Uri
import android.util.Base64

/**
 * Agent 图片附件相关工具
 * 统一处理选图后的 URI 转 data URL、附件构建等逻辑，供聊天相关界面复用
 */
object AgentImageHelper {

    /**
     * 将 content URI 转为 data URL（data:image/xxx;base64,...）
     * @return data URL 或 null（读取失败时）
     */
    fun buildDataUrlFromUri(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            resolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mime;base64,$b64"
            }
        } catch (_: Exception) {
            null
        }
    }
}
