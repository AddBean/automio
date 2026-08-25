// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils.bundle

/**
 * 工作流分享包（bundle.zip）描述文件。
 *
 * 约定：bundle.zip 根目录包含 manifest.json、主脚本目录（扁平结构）、resources/。
 */
data class WorkflowBundleManifest(
    val bundleVersion: Int = 2,
    val bundleId: String,
    val title: String,
    val description: String? = null,
    val scripts: List<BundleScriptEntry> = emptyList(),
    val installPolicy: InstallPolicy? = null,
    val primaryScriptDir: String? = null,
    val primaryScriptUid: String? = null,
    val scopeDir: String? = null,
    /** 脚本需要的权限列表，用于导入时写入 mate */
    val permissions: List<String>? = null,
    /** primary 类型：workflow | skill | tool，默认 workflow，安装时按此分支 */
    val primaryType: String = PRIMARY_TYPE_WORKFLOW,
    /** 可选：运行时允许回退到全局的 skillId 白名单 */
    val allowedGlobalSkillIds: List<String>? = null,
    /** 发布者用户 ID，用于判断「作者本人」；未登录时可为空 */
    val authorId: String? = null
) {
    data class BundleScriptEntry(
        /** zip 内相对路径，例如：MyWorkflow */
        val dir: String,
        /** workflow | tool | asset */
        val role: String,
        /**
         * primary | dependency
         * - primary: 主脚本（通常是 workflow）
         * - dependency: 依赖脚本（例如脚本型 custom tool）
         */
        val entryType: String = ENTRY_DEPENDENCY,
        /** role=tool 时可选：注册成 MCP tool 的展示名 */
        val toolName: String? = null,
        /** role=tool 时可选：注册成 MCP tool 的描述 */
        val toolDesc: String? = null
    )

    data class InstallPolicy(
        /** rename | overwrite | skip */
        val onScriptDirConflict: String? = null,
        /** overwrite | skip | rename */
        val onSkillIdConflict: String? = null,
        /** overwrite | skip | rename */
        val onToolIdConflict: String? = null
    )

    companion object {
        const val DEFAULT_MANIFEST_NAME: String = "manifest.json"
        const val BUNDLE_VERSION_V2: Int = 2

        const val ROLE_WORKFLOW: String = "workflow"
        const val ROLE_TOOL: String = "tool"
        const val ROLE_ASSET: String = "asset"

        const val ENTRY_PRIMARY: String = "primary"
        const val ENTRY_DEPENDENCY: String = "dependency"

        const val PRIMARY_TYPE_WORKFLOW: String = "workflow"
        const val PRIMARY_TYPE_SKILL: String = "skill"
        const val PRIMARY_TYPE_TOOL: String = "tool"
    }

    fun resolvePrimaryScriptDir(): String? {
        if (!primaryScriptDir.isNullOrBlank()) return primaryScriptDir
        return scripts.firstOrNull { (it.entryType ?: "").lowercase() == ENTRY_PRIMARY }?.dir
            ?: scripts.firstOrNull {
                (it.role ?: "").lowercase() == ROLE_WORKFLOW && (it.entryType ?: "").lowercase() == ENTRY_PRIMARY
            }?.dir
    }

    fun isBundleV2(): Boolean = bundleVersion >= BUNDLE_VERSION_V2
}
