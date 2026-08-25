// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.hive.utils.utils.GsonHelper

fun Any.deepCopyByGson(): Any {
    return GsonHelper.getInstance()
        .fromJson(GsonHelper.getInstance().toJson(this), this::class.java)
}

fun <T> List<T>.deepCopyListByGson(): List<T> {
    return GsonHelper.getInstance()
        .fromJson(GsonHelper.getInstance().toJson(this), object : TypeToken<List<T>>() {}.type)
}

/**
 * 从json 格式的 string 中取指定类型的value数据
 */
fun <T> String.getJsonKey(key: String): T? {
    try {
        val jsonElement = Gson().fromJson(this, JsonElement::class.java)
        return jsonElement.asJsonObject.get(key)?.asJsonPrimitive?.let {
            when {
                it.isString -> it.asString as T
                it.isNumber -> it.asInt as T
                it.isBoolean -> it.asBoolean as T
                else -> it.asString as T
            }
        }
    } catch (e: Exception) {
        return null
    }
}

fun Any.toJson(): String {
    return GsonHelper.getInstance().toJson(this)
}

fun Any.toFormatJson(): String {
    return GsonHelper.getInstance().toFormatJson(this)
}

/**
 * 将 Map 转换为 Gson JsonObject
 */
fun Map<String, Any>.toGsonJsonObject(): JsonObject {
    val obj = JsonObject()
    forEach { (k, v) ->
        obj.add(k, when (v) {
            is String -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            is Boolean -> JsonPrimitive(v)
            is Map<*, *> -> (v as Map<String, Any>).toGsonJsonObject()
            is List<*> -> {
                val arr = JsonArray()
                (v as List<*>).forEach { item ->
                    when (item) {
                        is String -> arr.add(JsonPrimitive(item))
                        is Number -> arr.add(JsonPrimitive(item))
                        is Boolean -> arr.add(JsonPrimitive(item))
                        is Map<*, *> -> arr.add((item as Map<String, Any>).toGsonJsonObject())
                        else -> arr.add(JsonPrimitive(item?.toString() ?: ""))
                    }
                }
                arr
            }
            else -> JsonPrimitive(v.toString())
        })
    }
    return obj
}

/**
 * 将任意对象转换为 YAML 格式
 */
fun String.jsonList2Yaml(): String {
    val list = Gson().fromJson<List<Any>>(this, object : TypeToken<List<Any>>() {}.type)
    return listToYaml(list)
}


/**
 * 将任意对象转换为 YAML 格式
 */
fun Any.json2Yaml(): String {
    return try {
        when (this) {
            is JsonObject -> {
                val map = Gson().fromJson<Map<String, Any>>(
                    this.toString(),
                    object : TypeToken<Map<String, Any>>() {}.type
                )
                mapToYaml(map)
            }

            is JsonArray -> {
                val list = Gson().fromJson<List<Any>>(
                    this.toString(),
                    object : TypeToken<List<Any>>() {}.type
                )
                listToYaml(list)
            }

            is JsonPrimitive -> {
                val value = when {
                    this.isString -> this.asString
                    this.isNumber -> this.asNumber
                    this.isBoolean -> this.asBoolean
                    else -> this.asString
                }
                value.toString()
            }

            is JsonElement -> {
                when (this) {
                    is JsonObject -> this.json2Yaml()
                    is JsonArray -> this.json2Yaml()
                    is JsonPrimitive -> this.json2Yaml()
                    else -> this.toString()
                }
            }

            is String -> {
                try {
                    // 先尝试解析为 JSON 对象
                    val jsonElement = Gson().fromJson(this, JsonElement::class.java)
                    jsonElement.json2Yaml()
                } catch (e: Exception) {
                    e.printStackTrace()
                    this
                }
            }

            else -> {
                Gson().toJson(this).let { jsonString ->
                    // 尝试将 JSON 字符串转换为 JsonElement
                    val jsonElement = Gson().fromJson(jsonString, JsonElement::class.java)
                    jsonElement.json2Yaml()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        this.toString()
    }
}

/**
 * 将 Map 转换为 YAML 格式
 */
private fun mapToYaml(map: Map<String, Any>, indent: Int = 0): String {
    val indentStr = "  ".repeat(indent)
    return map.entries.joinToString("\n") { (key, value) ->
        when (value) {
            is Map<*, *> -> "$indentStr$key:\n${mapToYaml(value as Map<String, Any>, indent + 1)}"
            is List<*> -> "$indentStr$key:\n${listToYaml(value as List<Any>, indent + 1)}"
            is String -> "$indentStr$key: \"$value\""
            else -> "$indentStr$key: $value"
        }
    }
}

/**
 * 将 List 转换为 YAML 格式
 */
private fun listToYaml(list: List<Any>, indent: Int = 0): String {
    val indentStr = "  ".repeat(indent)
    return list.joinToString("\n") { item ->
        when (item) {
            is Map<*, *> -> "$indentStr- ${mapToYaml(item as Map<String, Any>, indent + 1)}"
            is List<*> -> "$indentStr- ${listToYaml(item as List<Any>, indent + 1)}"
            is String -> "$indentStr- \"$item\""
            else -> "$indentStr- $item"
        }
    }
}

/**
 * 将 JSON 字符串转换为 YAML 格式
 * @param jsonString JSON 字符串
 * @return YAML 格式的字符串
 */
fun jsonToYaml(jsonString: String): String {
    return try {
        // 1. 解析 JSON 字符串为 JsonElement
        val jsonElement = Gson().fromJson(jsonString, JsonElement::class.java)

        // 2. 转换为 YAML
        jsonElement.json2Yaml()
    } catch (e: Exception) {
        e.printStackTrace()
        throw IllegalArgumentException("Invalid JSON string: ${e.message}")
    }
}

/**
 * 将 JSON 字符串转换为 YAML 格式（安全版本，返回原始字符串如果转换失败）
 * @param jsonString JSON 字符串
 * @return YAML 格式的字符串，如果转换失败则返回原始字符串
 */
fun jsonToYamlSafe(jsonString: String): String {
    return try {
        jsonToYaml(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
        jsonString
    }
}

/**
 * 检查字符串是否为有效的 JSON 格式
 * @param jsonString 待检查的字符串
 * @return 是否为有效的 JSON
 */
fun isValidJson(jsonString: String): Boolean {
    return try {
        Gson().fromJson(jsonString, JsonElement::class.java)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * 将 JSON 字符串转为扁平化的键值对列表视图（风格 A），去除 {}[] 等符号
 * 示例输出：
 *   status: success
 *   message: 页面已加载
 *   items:
 *     [0] label: 首页
 *         url: /home
 *     [1] title: 设置
 *         url: /settings
 *
 * 智能模式：自动识别混合文本中的 JSON 片段并转换，前后非 JSON 文本原样保留。
 *
 * @param indent 每级缩进的空格数，默认 1
 * @return 格式化后的字符串，如果未找到任何 JSON 则返回原始字符串
 */
fun String.jsonToListView(indent: Int = 1): String {
    // 先尝试整体解析
    return try {
        val element = Gson().fromJson(this, JsonElement::class.java)
        val sb = StringBuilder()
        sb.appendElement(element, "", indent, 0)
        sb.toString().trimEnd()
    } catch (e: Exception) {
        // 整体不是 JSON，尝试扫描片段中的 JSON
        parseMixedContentJson(indent)
    }
}

/**
 * 扫描文本，识别并转换其中独立的 JSON 片段（以 { 或 [ 开头），其余部分原样保留。
 */
private fun String.parseMixedContentJson(indent: Int): String {
    val result = StringBuilder()
    var pos = 0
    while (pos < length) {
        val jsonStart = indexOf('{', pos).let { startBrace ->
            val startBracket = indexOf('[', pos)
            when {
                startBrace == -1 && startBracket == -1 -> -1
                startBrace == -1 -> startBracket
                startBracket == -1 -> startBrace
                else -> minOf(startBrace, startBracket)
            }
        }
        if (jsonStart == -1) {
            // 后面没有 JSON 了，追加剩余文本
            result.append(substring(pos))
            break
        }
        // 追加 JSON 前的纯文本
        if (jsonStart > pos) {
            result.append(substring(pos, jsonStart))
        }
        // 尝试找到匹配的 JSON 片段
        val jsonCandidate = tryFindJsonBlock(jsonStart)
        if (jsonCandidate != null) {
            val (jsonStr, endPos) = jsonCandidate
            // 转换 JSON 片段
            val converted = tryConvertJsonToListView(jsonStr, indent)
            result.append(converted)
            pos = endPos
        } else {
            // 不是合法 JSON，原样输出第一个字符，继续扫描
            result.append(this[jsonStart])
            pos = jsonStart + 1
        }
    }
    return result.toString().trimEnd()
}

/**
 * 从 startIndex 开始，尝试找到匹配的完整 JSON 块。
 * 返回 (json字符串, 结束位置) 或 null。
 */
private fun String.tryFindJsonBlock(startIndex: Int): Pair<String, Int>? {
    val openChar = this[startIndex]
    val closeChar = when (openChar) {
        '{' -> '}'
        '[' -> ']'
        else -> return null
    }
    // 用栈匹配括号
    var depth = 0
    var inString = false
    var escapeNext = false
    var endPos = -1

    for (i in startIndex until length) {
        val c = this[i]
        when {
            escapeNext -> escapeNext = false
            c == '\\' && inString -> escapeNext = true
            c == '"' && !escapeNext -> inString = !inString
            inString -> continue
            c == openChar -> depth++
            c == closeChar -> {
                depth--
                if (depth == 0) {
                    endPos = i + 1
                    break
                }
            }
        }
    }

    if (endPos == -1) return null
    val candidate = substring(startIndex, endPos)
    // 验证是否为合法 JSON
    return try {
        Gson().fromJson(candidate, JsonElement::class.java)
        candidate to endPos
    } catch (e: Exception) {
        null
    }
}

/**
 * 尝试将字符串转为 JSON 并渲染为 list view，失败则原样返回。
 */
private fun tryConvertJsonToListView(json: String, indent: Int): String {
    return try {
        val element = Gson().fromJson(json, JsonElement::class.java)
        val sb = StringBuilder()
        sb.appendElement(element, "", indent, 0)
        sb.toString().trimEnd()
    } catch (e: Exception) {
        json
    }
}

private fun StringBuilder.appendElement(element: JsonElement, prefix: String, indent: Int, depth: Int) {
    when {
        element.isJsonObject -> {
            element.asJsonObject.entrySet().forEach { (key, value) ->
                appendKeyValue(key, value, indent, depth)
            }
        }
        element.isJsonArray -> {
            if (element.isPrimitiveArray()) {
                val joined = element.asJsonArray.joinToString(", ") { it.elementAsString() }
                if (prefix.isEmpty()) {
                    appendLine(joined)
                } else {
                    appendLine("$prefix$joined")
                }
            } else {
                element.asJsonArray.forEachIndexed { index, value ->
                    val indentStr = " ".repeat(indent * depth)
                    val itemLabel = "[$index]"
                    when {
                        value.isJsonObject -> {
                            appendLine("$indentStr$itemLabel")
                            value.asJsonObject.entrySet().forEach { (key, childValue) ->
                                appendKeyValue(key, childValue, indent, depth + 1)
                            }
                        }
                        value.isJsonArray -> {
                            appendLine("$indentStr$itemLabel")
                            appendElement(value, "", indent, depth + 1)
                        }
                        else -> {
                            appendLine("$indentStr$itemLabel ${value.elementAsString()}")
                        }
                    }
                }
            }
        }
        element.isJsonPrimitive -> {
            appendLine("$prefix${element.asJsonPrimitive.primitiveValue()}")
        }
        element.isJsonNull -> {
            appendLine("${prefix}null")
        }
    }
}

private fun StringBuilder.appendKeyValue(key: String, value: JsonElement, indent: Int, depth: Int) {
    val indentStr = " ".repeat(indent * depth)
    when {
        value.isJsonObject -> {
            appendLine("$indentStr$key:")
            appendElement(value, "", indent, depth + 1)
        }
        value.isJsonArray -> {
            if (value.isPrimitiveArray()) {
                val joined = value.asJsonArray.joinToString(", ") { it.elementAsString() }
                appendLine("$indentStr$key: $joined")
            } else {
                appendLine("$indentStr$key:")
                appendElement(value, "", indent, depth + 1)
            }
        }
        else -> {
            appendLine("$indentStr$key: ${value.elementAsString()}")
        }
    }
}

private fun JsonElement.isPrimitiveArray(): Boolean {
    return asJsonArray.all { it.isJsonPrimitive || it.isJsonNull }
}

private fun JsonElement.elementAsString(): String {
    return when {
        isJsonPrimitive -> asJsonPrimitive.asString
        isJsonNull -> "null"
        else -> toString()
    }
}

private fun JsonPrimitive.primitiveValue(): String {
    return when {
        isString -> asString
        isNumber -> asString
        isBoolean -> asString
        else -> asString
    }
}
