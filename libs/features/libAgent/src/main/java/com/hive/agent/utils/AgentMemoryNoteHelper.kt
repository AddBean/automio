// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.hive.script.base.params.ScriptParamEnv
import com.hive.utils.global.MMKVTools

/**
 * 读取 memoryNote 保存的 AI 记忆内容
 * 数据来源：ScriptParamEnv（内存）+ MMKV（持久化）
 */
object AgentMemoryNoteHelper {

    private const val MEMORY_GROUP = "memory"
    private const val MMKV_PREFIX = "script_param_$MEMORY_GROUP."

    data class MemoryItem(
        val key: String,
        val value: String
    )

    /**
     * 获取 memory 组的所有 key-value
     */
    fun getMemoryNoteContent(): List<MemoryItem> {
        val result = mutableMapOf<String, String>()

        // 1. 从 ScriptParamEnv 读取（当前会话内存）
        val env = ScriptParamEnv.getParamEnv()
        val group = env.readGroup(MEMORY_GROUP)
        group?.params?.forEach { param ->
            val value = env.readParam(param.getFullId()) ?: ""
            result[param.id] = value
        }

        // 2. 从 MMKV 读取持久化数据（补充可能未在 env 中的）
        val mmkvKeys = MMKVTools.getKeysWithPrefix(MMKV_PREFIX)
        mmkvKeys.forEach { fullKey ->
            val paramKey = fullKey.removePrefix(MMKV_PREFIX)
            if (paramKey.isNotEmpty() && !result.containsKey(paramKey)) {
                val value = MMKVTools.getScriptParamString(fullKey, "") ?: ""
                result[paramKey] = value
            }
        }

        return result.map { MemoryItem(it.key, it.value) }
            .filter { it.value.isNotEmpty() }
            .sortedBy { it.key }
    }

    /**
     * 清空 memory 组的所有数据（内存 + MMKV）
     */
    fun clearAllMemory() {
        val env = ScriptParamEnv.getParamEnv()
        val group = env.readGroup(MEMORY_GROUP)
        group?.params?.toList()?.forEach { param ->
            env.writeParam(param.getFullId(), "", persist = false)
        }

        val mmkvKeys = MMKVTools.getKeysWithPrefix(MMKV_PREFIX)
        mmkvKeys.forEach { key ->
            MMKVTools.getInstance().remove(key, true)
        }
    }

    /**
     * 删除指定 key 的记忆
     */
    fun removeMemory(key: String) {
        val env = ScriptParamEnv.getParamEnv()
        val fullId = "$MEMORY_GROUP.$key"
        env.writeParam(fullId, "", persist = false)

        val mmkvKey = MMKV_PREFIX + key
        MMKVTools.getInstance().remove(mmkvKey, true)
    }
}
