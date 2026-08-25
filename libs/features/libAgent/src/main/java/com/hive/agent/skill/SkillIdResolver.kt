// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

/**
 * 集中处理 skill id 的解析与查找候选。
 * 统一使用 skill.xxx 格式，无需 prefix 转换。
 */
object SkillIdResolver {

    /**
     * 返回用于 registry 查找的 id 候选列表。
     */
    fun getLookupCandidates(rawId: String, scopeId: String?): List<String> {
        if (rawId.isBlank()) return emptyList()
        return listOf(rawId)
    }
}
