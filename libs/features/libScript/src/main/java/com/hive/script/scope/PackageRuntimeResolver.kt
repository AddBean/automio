// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.script.utils.bundle.WorkflowBundleManifest
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.mcp.McpConst
import com.hive.utils.utils.GsonHelper
import java.io.File

/**
 * V1 运行时解析门面：
 * 1) 先查当前 package（primary + dependence）
 * 2) 未命中时由上层决定是否回退 global primary
 */
object PackageRuntimeResolver {

    data class RuntimePackage(
        val packageId: String?,
        val primaryType: String?,
        val primaryResourceId: String?,
        val rootDir: File,
        val manifest: WorkflowBundleManifest?,
        val snapshot: ScriptScopeSnapshot?
    ) {
        fun resolveLocalScriptPath(ref: String): String? {
            if (ref.isBlank()) return null
            val localId = if (ScriptScopeRepository.isScopedScriptRef(ref)) {
                ref.removePrefix(ScriptScopeRepository.SCOPED_SCRIPT_PATH_PREFIX)
            } else {
                ref
            }
            val scriptRef = snapshot?.scripts?.firstOrNull {
                it.localId == localId || it.scriptDir == localId || it.scriptUid == localId
            } ?: return null
            val path = File(ScriptScopeRepository.getScriptsDir(rootDir), scriptRef.scriptDir).absolutePath
            return path.takeIf { File(it).exists() }
        }

        fun resolveLocalSkill(skillId: String): ScopedSkillSpec? {
            if (skillId.isBlank()) return null
            return snapshot?.skills?.firstOrNull { it.id == skillId }
        }

        fun isGlobalSkillAllowed(skillId: String): Boolean {
            if (skillId.isBlank()) return false
            val allowList = manifest?.allowedGlobalSkillIds
                ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
                ?.toSet()
            // 未配置白名单时，保持兼容：允许回退全局
            if (allowList == null) return true
            return skillId in allowList
        }

        fun resolveLocalToolBindings(allowedToolNames: Collection<String>): Map<String, McpTool> {
            if (allowedToolNames.isEmpty()) return emptyMap()
            val allowed = allowedToolNames.toHashSet()
            val result = LinkedHashMap<String, McpTool>()

            val toolsDir = ScriptScopeRepository.getToolsDir(rootDir)
            snapshot?.tools?.forEach { spec ->
                if (spec.functionName !in allowed) return@forEach
                val toolPath = File(toolsDir, spec.scriptDir).absolutePath
                if (!File(toolPath).exists()) return@forEach
                if (result.containsKey(spec.functionName)) return@forEach
                result[spec.functionName] = ScriptMcpRegister.createLocalScopeTool(
                    toolId = spec.functionName,
                    toolName = spec.name.ifBlank { spec.functionName },
                    toolDescription = spec.description,
                    scriptPath = toolPath
                )
            }

            if (result.isEmpty()
                && primaryType.equals(WorkflowBundleManifest.PRIMARY_TYPE_TOOL, ignoreCase = true)
                && !primaryResourceId.isNullOrBlank()
                && primaryResourceId in allowed
                && rootDir.exists()
                && rootDir.isDirectory
            ) {
                result[primaryResourceId] = ScriptMcpRegister.createLocalScopeTool(
                    toolId = primaryResourceId,
                    toolName = manifest?.title?.ifBlank { rootDir.name } ?: rootDir.name,
                    toolDescription = manifest?.description ?: "",
                    scriptPath = rootDir.absolutePath
                )
            }
            return result
        }
    }

    data class SkillLookupResult(
        val localSkillId: String? = null,
        val allowGlobalFallback: Boolean = true,
        val runtimePackage: RuntimePackage? = null
    )

    fun resolveByScriptDir(currentScriptDir: File): RuntimePackage? {
        val owner = findOwnerPackageRoot(currentScriptDir) ?: return null
        return buildRuntimePackage(owner, null)
    }

    fun resolveByScopeId(scopeId: String?): RuntimePackage? {
        if (scopeId.isNullOrBlank()) return null
        val workflowDir = ScriptScopeRepository.findLocalScriptDirsByUid(scopeId).firstOrNull()
        if (workflowDir != null) {
            return buildRuntimePackage(workflowDir, null)
        }
        val indexRecord = PackageIndexRepository.findByPrimary(
            WorkflowBundleManifest.PRIMARY_TYPE_SKILL,
            scopeId
        ) ?: PackageIndexRepository.findByPackageId(scopeId)
        val installPath = indexRecord?.installPath?.takeIf { it.isNotBlank() } ?: return null
        return buildRuntimePackage(File(installPath), indexRecord)
    }

    fun resolveByPrimarySkillId(skillId: String): RuntimePackage? {
        if (skillId.isBlank()) return null
        val indexRecord = PackageIndexRepository.findByPrimary(
            WorkflowBundleManifest.PRIMARY_TYPE_SKILL,
            skillId
        ) ?: return null
        val installPath = indexRecord.installPath.takeIf { it.isNotBlank() } ?: return null
        return buildRuntimePackage(File(installPath), indexRecord)
    }

    fun resolveCallScriptPath(currentScriptDir: File, ref: String): String? {
        if (ref.isBlank()) return null
        val runtimePackage = resolveByScriptDir(currentScriptDir)
        runtimePackage?.resolveLocalScriptPath(ref)?.let { return it }

        ScriptScopeRepository.resolveScopedScriptPath(currentScriptDir, ref)
            ?.takeIf { File(it).exists() }
            ?.let { return it }

        return ref.takeIf { File(it).exists() }
    }

    fun resolveSkillForScript(
        currentScriptDir: File,
        rawSkillId: String
    ): SkillLookupResult {
        if (rawSkillId.isBlank()) {
            return SkillLookupResult(localSkillId = null, allowGlobalFallback = false, runtimePackage = null)
        }
        val runtimePackage = resolveByScriptDir(currentScriptDir)
        val local = runtimePackage?.resolveLocalSkill(rawSkillId)
        if (local != null) {
            return SkillLookupResult(
                localSkillId = local.id,
                allowGlobalFallback = true,
                runtimePackage = runtimePackage
            )
        }
        val allowGlobal = runtimePackage?.isGlobalSkillAllowed(rawSkillId) ?: true
        return SkillLookupResult(
            localSkillId = null,
            allowGlobalFallback = allowGlobal,
            runtimePackage = runtimePackage
        )
    }

    private fun buildRuntimePackage(
        rootDir: File,
        indexRecord: PackageIndexRecord?
    ): RuntimePackage? {
        if (!rootDir.exists() || !rootDir.isDirectory) return null
        val snapshot = runCatching { ScriptScopeRepository.load(rootDir, validate = false) }.getOrNull()
        val manifest = readManifest(rootDir)
        val primaryType = indexRecord?.primaryType ?: manifest?.primaryType
        val primaryResourceId = indexRecord?.primaryResourceId ?: derivePrimaryResourceId(primaryType, rootDir, snapshot)
        val packageId = indexRecord?.packageId ?: manifest?.bundleId
        return RuntimePackage(
            packageId = packageId,
            primaryType = primaryType,
            primaryResourceId = primaryResourceId,
            rootDir = rootDir,
            manifest = manifest,
            snapshot = snapshot
        )
    }

    private fun derivePrimaryResourceId(
        primaryType: String?,
        rootDir: File,
        snapshot: ScriptScopeSnapshot?
    ): String? {
        return when (primaryType?.lowercase()) {
            WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW -> ScriptScopeRepository.getScriptUid(rootDir)
            WorkflowBundleManifest.PRIMARY_TYPE_SKILL -> snapshot?.skills?.firstOrNull()?.id
            WorkflowBundleManifest.PRIMARY_TYPE_TOOL ->
                ScriptScopeRepository.getScriptUid(rootDir)?.let { ScriptScopeRepository.localToolFunctionName(it) }
            else -> null
        }
    }

    private fun findOwnerPackageRoot(currentScriptDir: File): File? {
        var cursor: File? = currentScriptDir.takeIf { it.isDirectory } ?: currentScriptDir.parentFile
        while (cursor != null) {
            val hasDependence = ScriptScopeRepository.getDependenceDir(cursor).takeIf { it.exists() && it.isDirectory } != null
            val hasManifest = File(cursor, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME).let { it.exists() && it.isFile }
            if (hasDependence || hasManifest) {
                return cursor
            }
            cursor = cursor.parentFile
        }
        return null
    }

    private fun readManifest(rootDir: File): WorkflowBundleManifest? {
        val file = File(rootDir, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            GsonHelper.getInstance().fromJson(file.readText(), WorkflowBundleManifest::class.java)
        }.getOrNull()
    }
}
