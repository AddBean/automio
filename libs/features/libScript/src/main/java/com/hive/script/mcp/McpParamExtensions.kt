// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp

/**
 * MCP 参数解析扩展：兼容模型传入的 double 格式（如 "1000.0"）。
 * Kotlin 的 toLongOrNull/toIntOrNull 对 "1000.0" 返回 null，需先按 double 解析再转整。
 */

/** "1000" 或 "1000.0" -> 1000L */
fun String?.toLongOrNullCompat(): Long? =
    this?.toLongOrNull() ?: this?.toDoubleOrNull()?.toLong()

/** "1000" 或 "1000.0" -> 1000 */
fun String?.toIntOrNullCompat(): Int? =
    this?.toIntOrNull() ?: this?.toDoubleOrNull()?.toInt()

/** 带默认值的 Long 解析 */
fun String?.toLongCompat(default: Long): Long = toLongOrNullCompat() ?: default

/** 带默认值的 Int 解析 */
fun String?.toIntCompat(default: Int): Int = toIntOrNullCompat() ?: default
