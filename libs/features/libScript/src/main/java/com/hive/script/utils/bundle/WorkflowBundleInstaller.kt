// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils.bundle

import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.base.ScriptConst
import com.hive.script.scope.CustomStorage
import com.hive.script.scope.ScriptScopeBundleV2Installer
import com.hive.script.scope.ScriptScopeRepository
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.file.ZipUtils
import com.hive.utils.utils.GsonHelper
import java.io.File
import java.util.zip.ZipFile

object WorkflowBundleInstaller {

    enum class ConflictAction { OVERWRITE, SKIP }

    data class BundleConflict(
        /** 唯一 key：workflow 或 skill:<id> */
        val key: String,
        /** workflow | skill */
        val type: String,
        val id: String,
        val name: String,
        val existingVersion: String? = null,
        val incomingVersion: String? = null,
        val defaultAction: ConflictAction = ConflictAction.OVERWRITE
    )

    /**
     * 若存在需要用户决策的冲突，tryInstall() 会返回 pendingInstall。
     * 调用方应在 UI 上让用户选择后调用 continueInstall()，或 cancelPending() 清理临时目录。
     */
    data class PendingInstall(
        val manifest: WorkflowBundleManifest,
        val tempRootPath: String,
        val defaultActions: Map<String, ConflictAction> = emptyMap()
    )

    data class InstallResult(
        val success: Boolean,
        val isBundle: Boolean,
        val bundleId: String? = null,
        val title: String? = null,
        val installedScripts: List<String> = emptyList(),
        val installedToolIds: List<String> = emptyList(),
        val installedSkillIds: List<String> = emptyList(),
        val missingTools: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val pendingInstall: PendingInstall? = null,
        val pendingConflicts: List<BundleConflict> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * 若 zip 为 bundle（根目录存在 manifest.json），则执行安装并返回结果；
     * 否则返回 null，交由普通脚本 zip 导入逻辑处理。
     */
    fun tryInstall(zipFile: File): InstallResult? {
        if (!zipFile.exists() || !zipFile.isFile) return null

        val manifestText = readRootManifest(zipFile) ?: return null
        val manifest = runCatching {
            GsonHelper.getInstance().fromJson(manifestText, WorkflowBundleManifest::class.java)
        }.getOrNull()
            ?: throw RuntimeException(GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid))

        val tempRoot =
            File(ScriptConst.Save_Import_Temp_Path, "bundle_${System.currentTimeMillis()}/")
        runCatching {
            FileUtils.makeDirs(tempRoot.absolutePath)
            FileUtils.clearDirectory(tempRoot, false)
        }
        val unzipOk = runCatching {
            ZipUtils.startUnzipFiles(zipFile, tempRoot.absolutePath, null)
        }.getOrDefault(false)
        if (!unzipOk) {
            throw RuntimeException(GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_unzip_failed))
        }

        if (!manifest.isBundleV2()) {
            throw RuntimeException(GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_version_unsupported))
        }

        // 冲突检测（阶段三）：若需要用户决策，返回 pendingInstall 并保留 tempRoot
        val conflictCheck = runCatching { detectConflicts(manifest, tempRoot) }.getOrElse { e ->
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            return InstallResult(
                success = false,
                isBundle = true,
                bundleId = manifest.bundleId,
                title = manifest.title,
                errorMessage = e.message
                    ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
            )
        }
        if (conflictCheck.pendingConflicts.isNotEmpty()) {
            return InstallResult(
                success = false,
                isBundle = true,
                bundleId = manifest.bundleId,
                title = manifest.title,
                pendingInstall = PendingInstall(
                    manifest = manifest,
                    tempRootPath = tempRoot.absolutePath,
                    defaultActions = conflictCheck.defaultActions
                ),
                pendingConflicts = conflictCheck.pendingConflicts
            )
        }

        return runCatching {
            ScriptScopeBundleV2Installer.installPrepared(
                manifest = manifest,
                tempRoot = tempRoot,
                conflictActions = conflictCheck.defaultActions
            ).also { it.runPostInstall?.invoke() }.result
        }.getOrElse { e ->
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            InstallResult(
                success = false,
                isBundle = true,
                bundleId = manifest.bundleId,
                title = manifest.title,
                errorMessage = e.message
                    ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
            )
        }
    }

    fun continueInstall(
        pending: PendingInstall,
        resolvedActions: Map<String, ConflictAction>
    ): InstallResult {
        val tempRoot = File(pending.tempRootPath)
        val mergedActions = HashMap<String, ConflictAction>()
        mergedActions.putAll(pending.defaultActions)
        mergedActions.putAll(resolvedActions)
        return runCatching {
            ScriptScopeBundleV2Installer.installPrepared(
                manifest = pending.manifest,
                tempRoot = tempRoot,
                conflictActions = mergedActions
            ).also { it.runPostInstall?.invoke() }.result
        }.getOrElse { e ->
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            InstallResult(
                success = false,
                isBundle = true,
                bundleId = pending.manifest.bundleId,
                title = pending.manifest.title,
                errorMessage = e.message
                    ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
            )
        }
    }

    fun cancelPending(pending: PendingInstall) {
        runCatching { FileUtils.clearDirectory(File(pending.tempRootPath), true) }
    }

    private data class ConflictCheckResult(
        val pendingConflicts: List<BundleConflict>,
        val defaultActions: Map<String, ConflictAction>
    )

    private fun detectConflicts(
        manifest: WorkflowBundleManifest,
        tempRoot: File
    ): ConflictCheckResult {
        val primaryDir = manifest.resolvePrimaryScriptDir()?.let { WorkflowBundleExporter.normalizeBundlePath(it) }
            ?.let { File(tempRoot, it) }
            ?: throw IllegalStateException(
                GlobalApp.getString(
                    com.hive.i8n.R.string.sc_bundle_import_missing_script_dir,
                    "primary"
                )
            )
        val snapshot = ScriptScopeRepository.load(primaryDir, validate = true)
            ?: throw IllegalStateException(GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid))

        val primaryType =
            manifest.primaryType?.lowercase() ?: WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW
        val scriptUid = when (primaryType) {
            WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW -> ScriptScopeRepository.getScriptUid(primaryDir)
            else -> snapshot.scriptUid
        }
            ?: throw IllegalStateException(GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid))

        val pending = mutableListOf<BundleConflict>()
        val defaults = LinkedHashMap<String, ConflictAction>()

        // 1) workflow 冲突：仅当 primaryType == workflow 时检查同 scriptUid 已存在
        if (primaryType == WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW) {
            val existingMatches = ScriptScopeRepository.findLocalScriptDirsByUid(scriptUid)
            if (existingMatches.isNotEmpty()) {
                val existingDir = existingMatches.first()
                pending.add(
                    BundleConflict(
                        key = "workflow",
                        type = "workflow",
                        id = scriptUid,
                        name = existingDir.name,
                        defaultAction = ConflictAction.OVERWRITE
                    )
                )
            }
        }

        // 2) skill 冲突：仅当 primaryType == skill 时检查 primary skill
        if (primaryType == WorkflowBundleManifest.PRIMARY_TYPE_SKILL) {
            val provider = ComponentManager.getInstance()
                .getProvider(IAgentProvider::class.java) as? IAgentProvider
            val existingSkillsById = runCatching { provider?.listSkills().orEmpty() }
                .getOrDefault(emptyList())
                .associateBy { it.id }
            val primarySkill = manifest.primaryScriptUid
                ?.let { sid -> snapshot.skills.firstOrNull { it.id == sid } }
                ?: snapshot.skills.firstOrNull()
            primarySkill?.let { scoped ->
                val incoming = scoped.toSkillSpec()
                val existing = existingSkillsById[incoming.id]
                if (existing != null) {
                    val incomingVer = incoming.version
                    val existingVer = existing.version
                    val key = "skill:${incoming.id}"
                    val auto = autoResolveBySemver(existingVer, incomingVer)
                    if (auto != null) {
                        defaults[key] = auto
                    } else {
                        pending.add(
                            BundleConflict(
                                key = key,
                                type = "skill",
                                id = incoming.id,
                                name = incoming.name,
                                existingVersion = existingVer,
                                incomingVersion = incomingVer,
                                defaultAction = ConflictAction.OVERWRITE
                            )
                        )
                    }
                }
            }
        }

        // 3) tool 冲突：当 primaryType == tool 时，检查 .tool/ 或 custom tools SP 中是否已存在
        if (primaryType == WorkflowBundleManifest.PRIMARY_TYPE_TOOL) {
            val existingToolIds = findExistingToolIds()
            snapshot.tools.forEach { spec ->
                val toolId = spec.functionName
                if (toolId in existingToolIds) {
                    pending.add(
                        BundleConflict(
                            key = "tool:$toolId",
                            type = "tool",
                            id = toolId,
                            name = spec.name,
                            defaultAction = ConflictAction.OVERWRITE
                        )
                    )
                }
            }
        }

        return ConflictCheckResult(pending, defaults)
    }

    private fun findExistingToolIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val toolRoot = File(ScriptConst.Save_Tool_Path)
        if (toolRoot.exists() && toolRoot.isDirectory) {
            toolRoot.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                ids.add(ScriptScopeRepository.localToolFunctionName(dir.name))
            }
        }
        CustomStorage.readCustomTools().forEach { ids.add(it.scriptId) }
        return ids
    }

    private fun autoResolveBySemver(existing: String?, incoming: String?): ConflictAction? {
        if (existing.isNullOrBlank() || incoming.isNullOrBlank()) return null
        val cmp = compareSemver(incoming, existing) ?: return null
        if (cmp == 0) return null
        return if (cmp > 0) ConflictAction.OVERWRITE else ConflictAction.SKIP
    }

    private fun compareSemver(a: String, b: String): Int? {
        fun parse(v: String): List<Int>? {
            val core = v.trim().split('-', limit = 2).firstOrNull()?.trim().orEmpty()
            if (core.isBlank()) return null
            val parts = core.split('.')
            if (parts.isEmpty()) return null
            val nums = parts.take(3).map {
                it.toIntOrNull() ?: return null
            }
            return listOf(nums.getOrElse(0) { 0 }, nums.getOrElse(1) { 0 }, nums.getOrElse(2) { 0 })
        }

        val pa = parse(a) ?: return null
        val pb = parse(b) ?: return null
        return pa.zip(pb).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) } ?: 0
    }

    private fun readRootManifest(zipFile: File): String? {
        return runCatching {
            ZipFile(zipFile).use { zf ->
                val entry = zf.getEntry(WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
                    ?: zf.getEntry("/" + WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
                    ?: return@use null
                zf.getInputStream(entry).bufferedReader().use { it.readText() }
            }
        }.getOrNull()
    }
}
