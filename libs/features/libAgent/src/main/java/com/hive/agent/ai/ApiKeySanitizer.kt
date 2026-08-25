// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

/**
 * 清洗用户粘贴的 API Key，避免多余空白、引号或重复 Bearer 导致 401。
 */
object ApiKeySanitizer {

    fun sanitize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var key = raw.trim()
            .trim('"')
            .trim('\'')
            .replace("\uFEFF", "") // BOM
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        if (key.startsWith("Bearer ", ignoreCase = true)) {
            key = key.substring(7).trim()
        }
        return key
    }
}
