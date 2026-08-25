// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.markdown

import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater

object MarkdownEncoder {

    fun encodePlantUml(plantumlCode: String): String {
        // 1. 预处理文本
        val cleaned = plantumlCode
            .replace(Regex("@startuml\\s*"), "")
            .replace(Regex("@enduml\\s*"), "")
            .trim()

        // 2. Deflate压缩（无Zlib头）
        val compressed = deflateCompress(cleaned.toByteArray(Charsets.UTF_8))

        // 3. Base64变种编码
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder()
                .encodeToString(compressed)
                .replace('+', '-')
                .replace('/', '_')
                .replace(Regex("=+$"), "")
        } else {
            android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replace(Regex("=+$"), "")
        }
    }

    private fun deflateCompress(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true) // nowrap=true
        deflater.setInput(input)
        deflater.finish()

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        return output.toByteArray()
    }

}