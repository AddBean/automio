// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

import com.hive.plugin.agent.model.SkillSpec
import java.util.concurrent.ConcurrentHashMap

/**
 * Skill 注册表，支持全局与 scope 分区。
 * - 全局：key = "global:${spec.id}"
 * - Scope：key = "${scopeId}:${spec.id}"
 */
class SkillRegistry {

    private val specs = ConcurrentHashMap<String, SkillSpec>()

    private fun globalKey(id: String): String = "global:$id"
    private fun scopedKey(scopeId: String, id: String): String = "$scopeId:$id"

    fun register(spec: SkillSpec) {
        specs[globalKey(spec.id)] = spec
    }

    fun unregister(skillId: String) {
        specs.remove(globalKey(skillId))
    }

    /**
     * 按 scope 注册，spec.id 为 local id
     */
    fun registerScoped(scopeId: String, spec: SkillSpec) {
        if (scopeId.isBlank()) return
        specs[scopedKey(scopeId, spec.id)] = spec
    }

    /**
     * 按 scope 注销
     */
    fun unregisterScoped(scopeId: String, skillId: String) {
        specs.remove(scopedKey(scopeId, skillId))
    }

    /**
     * 单 key 查找，仅查全局
     */
    fun get(skillId: String): SkillSpec? {
        return specs[globalKey(skillId)]
    }

    /**
     * 按 scope 解析：先查 scope 内，再 fallback 全局。
     */
    fun get(scopeId: String?, skillId: String): SkillSpec? {
        val candidates = SkillIdResolver.getLookupCandidates(skillId, scopeId)
        if (!scopeId.isNullOrBlank()) {
            for (id in candidates) {
                specs[scopedKey(scopeId, id)]?.let { return it }
            }
        }
        for (id in candidates) {
            specs[globalKey(id)]?.let { return it }
        }
        return null
    }

    fun list(): List<SkillSpec> {
        return list(scopeId = null, includeGlobal = true)
    }

    /**
     * 按 scope 列出：scope 内 + 可选全局
     */
    fun list(scopeId: String?, includeGlobal: Boolean): List<SkillSpec> {
        val result = mutableListOf<SkillSpec>()
        if (!scopeId.isNullOrBlank()) {
            specs.keys.filter { it.startsWith("$scopeId:") }.mapNotNull { specs[it] }.let { result.addAll(it) }
        }
        if (includeGlobal) {
            specs.keys.filter { it.startsWith("global:") }.mapNotNull { specs[it] }.let { result.addAll(it) }
        }
        return result.distinct().sortedBy { it.id }
    }
}
