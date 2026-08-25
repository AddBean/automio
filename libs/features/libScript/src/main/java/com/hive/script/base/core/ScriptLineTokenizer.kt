// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

/**
 * 行级脚本词法分析器（Tokenizer）
 *
 * 替代正则匹配，采用逐字符扫描将一行脚本拆成 token 流，再组装为 parserCmdLine 所需的 Map。
 * 详见：docs/script-command-system-refactor-plan.md 13.5 节
 *
 * 示例：click x=0.5 y=0.5 random=0 @delay(start=500,end=1000) # 注释
 * → cmdLine="click x=0.5 y=0.5 random=0", delayStart=500, delayEnd=1000, comment=注释
 *
 * 通用参数支持：
 * - 修饰符 KV：@delay(start=500,end=1000) @rect(left=0.1,top=0.2,right=0.9,bottom=0.8)
 * - 行内 KV：start=500 end=1000, left=0.1 top=0.2 等
 */
object ScriptLineTokenizer {

    sealed class Token {
        data class Command(val name: String) : Token()
        data class KeyValue(val key: String, val value: String) : Token()
        data class Modifier(val name: String, val args: List<String>) : Token()
        data class Comment(val text: String) : Token()
    }

    /**
     * 从 cmdLine 提取命令名（第一个 Command token）
     */
    fun getCommandName(cmdLine: String?): String? {
        cmdLine ?: return null
        return (tokenize(cmdLine.trim()).firstOrNull() as? Token.Command)?.name
    }

    /**
     * 从 cmdLine 解析 key=value 参数表（供各 Cmd.parseCmd 使用）
     */
    fun parseKeyValueParams(cmdLine: String): Map<String, String> {
        return tokenize(cmdLine).filterIsInstance<Token.KeyValue>().associate { it.key to it.value }
    }

    /**
     * 解析一行脚本，输出与 ScriptParser.parserCmdLine 相同的 Map 格式
     */
    fun parseLine(line: String?): Map<String, String?> {
        if (line == null) return emptyMap()
        val trimmed = line.replace("\t", "").trim()
        val tokens = tokenize(trimmed)
        return tokensToMap(trimmed, tokens)
    }

    /**
     * 词法分析：将字符串拆成 token 流
     */
    fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var isFirstToken = true

        while (i < line.length) {
            if (line[i].isWhitespace()) {
                i++
                continue
            }
            if (line[i] == '#') {
                tokens.add(Token.Comment(line.substring(i + 1).trim()))
                return tokens
            }
            if (line[i] == '@') {
                val (modifier, next) = readModifier(line, i)
                tokens.add(modifier)
                i = next
                isFirstToken = false
                continue
            }
            val (segment, next) = readSegment(line, i)
            if (segment.contains('=')) {
                val eq = segment.indexOf('=')
                tokens.add(Token.KeyValue(segment.substring(0, eq), segment.substring(eq + 1)))
            } else {
                tokens.add(if (isFirstToken) Token.Command(segment) else Token.KeyValue(segment, ""))
            }
            i = next
            isFirstToken = false
        }
        return tokens
    }

    /**
     * 读取一个片段：引号字符串、key="value"（值可含空格）、或普通 token
     */
    private fun readSegment(line: String, start: Int): Pair<String, Int> {
        var i = start
        if (line[i] == '"') {
            i++
            val begin = i
            while (i < line.length && line[i] != '"') {
                if (line[i] == '\\') i++
                i++
            }
            val value = line.substring(begin, i).replace("\\\"", "\"")
            if (i < line.length) i++
            return Pair(value, i)
        }
        val begin = i
        while (i < line.length) {
            when {
                line[i].isWhitespace() || line[i] == '#' || line[i] == '@' ->
                    return Pair(line.substring(begin, i), i)
                line[i] == '=' && i + 1 < line.length && line[i + 1] == '"' -> {
                    val keyPart = line.substring(begin, i + 1)
                    i += 2
                    val valueStart = i
                    while (i < line.length && line[i] != '"') {
                        if (line[i] == '\\') i++
                        i++
                    }
                    val value = line.substring(valueStart, i).replace("\\\"", "\"")
                    if (i < line.length) i++
                    return Pair(keyPart + value, i)
                }
                else -> i++
            }
        }
        return Pair(line.substring(begin, i), i)
    }

    /**
     * 读取修饰符 @name(arg1,arg2,...)
     */
    private fun readModifier(line: String, start: Int): Pair<Token.Modifier, Int> {
        var i = start
        if (i >= line.length || line[i] != '@') return Pair(Token.Modifier("", emptyList()), i)
        i++
        val nameStart = i
        while (i < line.length && line[i].isLetterOrDigit()) i++
        val name = line.substring(nameStart, i)
        if (i >= line.length || line[i] != '(') return Pair(Token.Modifier(name, emptyList()), i)
        i++
        val args = mutableListOf<String>()
        var argStart = i
        var depth = 1
        while (i < line.length && depth > 0) {
            when (line[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0 && argStart < i) {
                        args.add(line.substring(argStart, i).trim())
                    }
                }
                ',' -> if (depth == 1) {
                    args.add(line.substring(argStart, i).trim())
                    argStart = i + 1
                }
            }
            i++
        }
        return Pair(Token.Modifier(name, args), i)
    }

    /** 通用参数 KV 键名 → parserCmdLine 输出 map 的 key */
    private val commonParamKeyMap = mapOf(
        "start" to "delayStart", "end" to "delayEnd",
        "delayStart" to "delayStart", "delayEnd" to "delayEnd",
        "rectLeft" to "rectLeft", "rectTop" to "rectTop", "rectRight" to "rectRight", "rectBottom" to "rectBottom",
        "left" to "rectLeft", "top" to "rectTop", "right" to "rectRight", "bottom" to "rectBottom",
        "offsetFromX" to "offsetFromX", "offsetFromY" to "offsetFromY", "offsetToX" to "offsetToX", "offsetToY" to "offsetToY",
        "dragFromX" to "dragFromX", "dragFromY" to "dragFromY", "dragToX" to "dragToX", "dragToY" to "dragToY",
        "dragType" to "dragType", "dragDuration" to "dragDuration", "dragPressDuration" to "dragPressDuration",
    )

    /** delay 命令独占 start/end，不视为通用参数 */
    private val cmdExclusiveKeys = mapOf("delay" to setOf("start", "end"))

    /**
     * 将 token 流组装为 parserCmdLine 输出的 Map
     */
    private fun tokensToMap(originalLine: String, tokens: List<Token>): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        val cmdParts = mutableListOf<String>()
        var currentCmd: String? = null

        for (token in tokens) {
            when (token) {
                is Token.Comment -> map["comment"] = token.text
                is Token.Command -> {
                    currentCmd = token.name
                    cmdParts.add(token.name)
                }
                is Token.KeyValue -> {
                    val mapKey = commonParamKeyMap[token.key]
                    val isExclusive = currentCmd?.let { cmdExclusiveKeys[it]?.contains(token.key) } == true
                    if (mapKey != null && !isExclusive) {
                        map[mapKey] = token.value
                    } else {
                        val kv = when {
                            token.value.isEmpty() -> token.key
                            token.value.any { it.isWhitespace() || it == '"' || it == '=' } ->
                                "${token.key}=\"${token.value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                            else -> "${token.key}=${token.value}"
                        }
                        cmdParts.add(kv)
                    }
                }
                is Token.Modifier -> applyModifier(map, token)
            }
        }

        map["cmdLine"] = cmdParts.joinToString(" ").trim().ifEmpty { null }
        return map
    }

    private fun applyModifier(map: MutableMap<String, String?>, mod: Token.Modifier) {
        if (!mod.args.any { it.contains('=') }) return
        val kvMap = when (mod.name) {
            "delay" -> mapOf("start" to "delayStart", "end" to "delayEnd")
            "rect" -> mapOf("left" to "rectLeft", "top" to "rectTop", "right" to "rectRight", "bottom" to "rectBottom")
            "drift" -> mapOf("fromX" to "offsetFromX", "fromY" to "offsetFromY", "toX" to "offsetToX", "toY" to "offsetToY")
            "drag" -> mapOf("fromX" to "dragFromX", "fromY" to "dragFromY", "toX" to "dragToX", "toY" to "dragToY", "type" to "dragType", "duration" to "dragDuration", "pressDuration" to "dragPressDuration")
            else -> emptyMap()
        }
        for (arg in mod.args) {
            val eq = arg.indexOf('=')
            if (eq > 0) {
                val key = arg.substring(0, eq).trim()
                val value = arg.substring(eq + 1).trim()
                kvMap[key]?.let { mapKey -> map[mapKey] = value }
            }
        }
    }
}
