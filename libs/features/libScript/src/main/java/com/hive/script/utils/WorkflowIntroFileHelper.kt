// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import java.io.File

object WorkflowIntroFileHelper {

    private const val README_FILE = "README.md"

    fun readIntro(scriptPath: String?): String {
        if (scriptPath.isNullOrBlank()) return ""
        val file = File(scriptPath, README_FILE)
        if (!file.exists() || !file.isFile) return ""
        return runCatching { file.readText().trim() }.getOrDefault("")
    }

    fun writeIntro(scriptPath: String?, content: String): Boolean {
        if (scriptPath.isNullOrBlank()) return false
        val dir = File(scriptPath)
        if (!dir.isDirectory) return false
        val file = File(dir, README_FILE)
        return runCatching {
            if (content.isBlank()) {
                if (file.exists()) file.delete()
            } else {
                file.writeText(content)
            }
            true
        }.getOrDefault(false)
    }
}
