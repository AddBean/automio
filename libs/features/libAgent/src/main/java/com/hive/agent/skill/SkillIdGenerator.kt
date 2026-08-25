// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.skill

import java.util.UUID

/**
 * 生成 8 位随机 Skill ID，便于 AI 识别，与 scriptUid 风格一致。
 * 格式: skill.xxxxxxxx
 */
object SkillIdGenerator {
    private const val SKILL_ID_PREFIX = "skill."

    fun generate(): String =
        SKILL_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").take(8)

    /** Skill id 规范化：确保以 skill. 开头 */
    fun normalizeSkillId(id: String): String {
        if (id.isBlank()) return ""
        return if (id.startsWith(SKILL_ID_PREFIX)) id else (SKILL_ID_PREFIX + id)
    }
}
