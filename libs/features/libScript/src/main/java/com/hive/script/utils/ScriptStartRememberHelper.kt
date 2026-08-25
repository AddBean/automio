// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hive.utils.global.MMKVTools

/**
 * CmdScriptStart 参数记忆存储
 * 用于「是否记住」功能，下次运行脚本时自动使用上次输入的值
 *
 * @author jiadou
 */
object ScriptStartRememberHelper {

    private const val PREFIX = "script_start_remember_"
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    /**
     * 构建记忆存储的 key
     * @param scriptPath 脚本路径，可为空（如录制未保存时用 "unsaved"）
     * @param dialogParams 参数 id 列表，用 | 分隔
     */
    fun buildKey(scriptPath: String, dialogParams: String): String {
        val path = scriptPath.ifEmpty { "unsaved" }
        return "$PREFIX${path}::${dialogParams.orEmpty()}"
    }

    /**
     * 加载已记住的参数值
     * @return 参数 id -> 值的映射，无数据时返回 null
     */
    fun load(rememberKey: String): Map<String, String>? {
        val json = MMKVTools.getScriptParamString(rememberKey, null) ?: return null
        if (json.isEmpty()) return null
        return try {
            gson.fromJson<Map<String, String>>(json, mapType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存参数值到本地
     */
    fun save(rememberKey: String, values: Map<String, String>) {
        if (values.isEmpty()) return
        val json = gson.toJson(values)
        MMKVTools.putScriptParamString(rememberKey, json)
    }

    /**
     * 清除指定 key 的记忆
     */
    fun clear(rememberKey: String) {
        MMKVTools.putScriptParamString(rememberKey, "")
    }
}
