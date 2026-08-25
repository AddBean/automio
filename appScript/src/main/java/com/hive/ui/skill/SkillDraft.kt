// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.content.Intent

private const val EXTRA_SKILL_DRAFT_NAME = "extra_skill_draft_name"
private const val EXTRA_SKILL_DRAFT_DESC = "extra_skill_draft_desc"
private const val EXTRA_SKILL_DRAFT_PROMPT = "extra_skill_draft_prompt"
private const val EXTRA_SKILL_DRAFT_TOOLS = "extra_skill_draft_tools"
private const val EXTRA_SKILL_DRAFT_MAX_ROUNDS = "extra_skill_draft_max_rounds"
private const val EXTRA_SKILL_DRAFT_TIMEOUT_MS = "extra_skill_draft_timeout_ms"

data class SkillDraft(
    val name: String,
    val description: String,
    val systemPrompt: String,
    val allowedToolNames: List<String> = emptyList(),
    val maxRounds: Int? = null,
    val timeoutMs: Long? = null
)

fun SkillDraft.toIntent(intent: Intent = Intent()): Intent = intent.apply {
    putExtra(EXTRA_SKILL_DRAFT_NAME, name)
    putExtra(EXTRA_SKILL_DRAFT_DESC, description)
    putExtra(EXTRA_SKILL_DRAFT_PROMPT, systemPrompt)
    if (allowedToolNames.isNotEmpty()) {
        putStringArrayListExtra(EXTRA_SKILL_DRAFT_TOOLS, ArrayList(allowedToolNames))
    }
    maxRounds?.let { putExtra(EXTRA_SKILL_DRAFT_MAX_ROUNDS, it) }
    timeoutMs?.let { putExtra(EXTRA_SKILL_DRAFT_TIMEOUT_MS, it) }
}

fun Intent.getSkillDraft(): SkillDraft? {
    val name = getStringExtra(EXTRA_SKILL_DRAFT_NAME)?.trim().orEmpty()
    val description = getStringExtra(EXTRA_SKILL_DRAFT_DESC)?.trim().orEmpty()
    val prompt = getStringExtra(EXTRA_SKILL_DRAFT_PROMPT)?.trim().orEmpty()
    if (name.isBlank() || description.isBlank() || prompt.isBlank()) return null

    val toolNames = getStringArrayListExtra(EXTRA_SKILL_DRAFT_TOOLS).orEmpty()
    val maxRounds = if (hasExtra(EXTRA_SKILL_DRAFT_MAX_ROUNDS)) {
        val value = getIntExtra(EXTRA_SKILL_DRAFT_MAX_ROUNDS, Int.MIN_VALUE)
        if (value != Int.MIN_VALUE) value else null
    } else {
        null
    }
    val timeoutMs = if (hasExtra(EXTRA_SKILL_DRAFT_TIMEOUT_MS)) {
        val value = getLongExtra(EXTRA_SKILL_DRAFT_TIMEOUT_MS, Long.MIN_VALUE)
        if (value != Long.MIN_VALUE) value else null
    } else {
        null
    }

    return SkillDraft(
        name = name,
        description = description,
        systemPrompt = prompt,
        allowedToolNames = toolNames,
        maxRounds = maxRounds,
        timeoutMs = timeoutMs
    )
}
