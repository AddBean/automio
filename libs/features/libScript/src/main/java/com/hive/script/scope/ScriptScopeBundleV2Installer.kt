// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.base.ScriptConst
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.net.data.ScriptCustomSkill
import com.hive.script.utils.bundle.WorkflowBundleExporter
import com.hive.script.utils.bundle.WorkflowBundleInstaller
import com.hive.script.utils.bundle.WorkflowBundleManifest
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import java.io.File

object ScriptScopeBundleV2Installer {
    data class InstallExecution(
        val result: WorkflowBundleInstaller.InstallResult,
        val runPostInstall: (() -> Unit)? = null
    )

    fun install(
        manifest: WorkflowBundleManifest,
        tempRoot: File
    ): WorkflowBundleInstaller.InstallResult {
        return installPrepared(manifest, tempRoot, emptyMap())
            .also { it.runPostInstall?.invoke() }
            .result
    }

    fun install(
        manifest: WorkflowBundleManifest,
        tempRoot: File,
        conflictActions: Map<String, WorkflowBundleInstaller.ConflictAction>
    ): WorkflowBundleInstaller.InstallResult {
        return installPrepared(manifest, tempRoot, conflictActions)
            .also { it.runPostInstall?.invoke() }
            .result
    }

    fun installPrepared(
        manifest: WorkflowBundleManifest,
        tempRoot: File,
        conflictActions: Map<String, WorkflowBundleInstaller.ConflictAction>
    ): InstallExecution {
        val primaryDir = manifest.resolvePrimaryScriptDir()?.let { WorkflowBundleExporter.normalizeBundlePath(it) }
            ?.let { File(tempRoot, it) }
            ?: return InstallExecution(
                fail(
                    manifest,
                    GlobalApp.getString(
                        com.hive.i8n.R.string.sc_bundle_import_missing_script_dir,
                        "primary"
                    )
                )
            )
        if (!primaryDir.exists() || !primaryDir.isDirectory) {
            return InstallExecution(
                fail(
                    manifest,
                    GlobalApp.getString(
                        com.hive.i8n.R.string.sc_bundle_import_missing_script_dir,
                        primaryDir.name
                    )
                )
            )
        }

        val snapshotResult = runCatching { ScriptScopeRepository.load(primaryDir, validate = true) }
        val snapshot = when {
            snapshotResult.isFailure -> return InstallExecution(
                fail(
                    manifest,
                    snapshotResult.exceptionOrNull()?.message
                        ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
                )
            )
            snapshotResult.getOrNull() == null -> return InstallExecution(
                fail(
                    manifest,
                    GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
                )
            )
            else -> snapshotResult.getOrNull()!!
        }

        val primaryType = manifest.primaryType?.lowercase() ?: WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW
        val scriptUid = when (primaryType) {
            WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW -> ScriptScopeRepository.getScriptUid(primaryDir)
            else -> snapshot.scriptUid
        } ?: return InstallExecution(
            fail(
                manifest,
                GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid)
            )
        )
        if (!manifest.primaryScriptUid.isNullOrBlank() && manifest.primaryScriptUid != scriptUid) {
            return InstallExecution(
                fail(
                    manifest,
                    GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid)
                )
            )
        }

        return when (primaryType) {
            WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW -> installWorkflow(
                manifest, tempRoot, primaryDir, snapshot, scriptUid, conflictActions
            )
            WorkflowBundleManifest.PRIMARY_TYPE_SKILL -> installSkill(
                manifest, tempRoot, primaryDir, snapshot, conflictActions
            )
            WorkflowBundleManifest.PRIMARY_TYPE_TOOL -> installTool(
                manifest, tempRoot, primaryDir, snapshot, scriptUid, conflictActions
            )
            else -> installWorkflow(
                manifest, tempRoot, primaryDir, snapshot, scriptUid, conflictActions
            )
        }
    }

    private fun installWorkflow(
        manifest: WorkflowBundleManifest,
        tempRoot: File,
        primaryDir: File,
        snapshot: ScriptScopeSnapshot,
        scriptUid: String,
        conflictActions: Map<String, WorkflowBundleInstaller.ConflictAction>
    ): InstallExecution {
        val existingMatches = ScriptScopeRepository.findLocalScriptDirsByUid(scriptUid)
        if (existingMatches.size > 1) {
            return InstallExecution(fail(manifest, "Duplicate local scriptUid detected: $scriptUid"))
        }

        val existingDir = existingMatches.firstOrNull()
        val workflowAction =
            conflictActions["workflow"] ?: WorkflowBundleInstaller.ConflictAction.OVERWRITE
        if (existingDir != null && existingDir.exists() && workflowAction == WorkflowBundleInstaller.ConflictAction.SKIP) {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            return InstallExecution(
                WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedScripts = listOf(existingDir.name),
                    warnings = listOf("workflow_conflict_skipped")
                )
            )
        }
        val targetDir = resolveTargetDir(
            scriptUid = scriptUid,
            workflowName = primaryDir.name,
            existingDir = existingDir,
            workflowAction = workflowAction
        ) ?: run {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            return InstallExecution(
                WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedScripts = emptyList(),
                    warnings = listOf("workflow_conflict_skipped")
                )
            )
        }
        var backupDir: File? = null
        return try {
            if (targetDir.exists()) {
                backupDir = File(
                    targetDir.parentFile,
                    "${targetDir.name}.bak_${System.currentTimeMillis()}"
                )
                if (!targetDir.renameTo(backupDir)) {
                    return InstallExecution(
                        fail(
                            manifest,
                            GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_install_failed)
                        )
                    )
                }
            }

            if (!moveDir(primaryDir, targetDir)) {
                restoreBackup(targetDir, backupDir)
                return InstallExecution(
                    fail(
                        manifest,
                        GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_install_failed)
                    )
                )
            }

            runCatching { backupDir?.let { FileUtils.clearDirectory(it, true) } }
            persistInstalledWorkflowMetadata(targetDir, manifest)
            upsertPackageIndex(
                manifest = manifest,
                primaryType = WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW,
                primaryResourceId = scriptUid,
                installPath = targetDir.absolutePath
            )
            InstallExecution(
                result = WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedScripts = listOf(targetDir.name)
                )
            )
        } catch (t: Throwable) {
            restoreBackup(targetDir, backupDir)
            InstallExecution(
                fail(
                    manifest,
                    t.message ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
                )
            )
        } finally {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
        }
    }

    private fun installSkill(
        manifest: WorkflowBundleManifest,
        tempRoot: File,
        primaryDir: File,
        snapshot: ScriptScopeSnapshot,
        conflictActions: Map<String, WorkflowBundleInstaller.ConflictAction>
    ): InstallExecution {
        val primarySkill = resolvePrimarySkill(snapshot, manifest) ?: return InstallExecution(
            fail(
                manifest,
                GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_manifest_invalid)
            )
        )
        val skillAction =
            conflictActions["skill:${primarySkill.id}"] ?: WorkflowBundleInstaller.ConflictAction.OVERWRITE
        val targetDir = SkillFileHelper.getSkillPackageDir(primarySkill.id)
        if (targetDir.exists() && skillAction == WorkflowBundleInstaller.ConflictAction.SKIP) {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            return InstallExecution(
                WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedSkillIds = listOf(primarySkill.id),
                    warnings = listOf("skill_conflict_skipped")
                )
            )
        }

        return try {
            if (targetDir.exists()) {
                runCatching { FileUtils.clearDirectory(targetDir, true) }
            }
            if (!moveDir(primaryDir, targetDir)) {
                return InstallExecution(
                    fail(
                        manifest,
                        GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_install_failed)
                    )
                )
            }

            persistInstalledSkillMetadata(targetDir, manifest, primarySkill)
            upsertPackageIndex(
                manifest = manifest,
                primaryType = WorkflowBundleManifest.PRIMARY_TYPE_SKILL,
                primaryResourceId = primarySkill.id,
                installPath = targetDir.absolutePath
            )
            runCatching {
                val refs = CustomStorage.readCustomSkillRefs().toMutableList()
                refs.removeAll { it.skillId == primarySkill.id }
                refs.add(
                    ScriptCustomSkill(
                        skillId = primarySkill.id,
                        skillName = primarySkill.name,
                        skillPath = SkillFileHelper.getPrimarySkillFilePath(targetDir)
                    )
                )
                CustomStorage.saveCustomSkillRefs(refs)
            }
            InstallExecution(
                result = WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedSkillIds = listOf(primarySkill.id)
                ),
                runPostInstall = {
                    val provider = ComponentManager.getInstance()
                        .getProvider(IAgentProvider::class.java) as? IAgentProvider
                    runCatching { provider?.registerSkillSpec(primarySkill.toSkillSpec()) }
                }
            )
        } catch (t: Throwable) {
            InstallExecution(
                fail(
                    manifest,
                    t.message ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
                )
            )
        } finally {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
        }
    }

    private fun installTool(
        manifest: WorkflowBundleManifest,
        tempRoot: File,
        primaryDir: File,
        snapshot: ScriptScopeSnapshot,
        scriptUid: String,
        conflictActions: Map<String, WorkflowBundleInstaller.ConflictAction>
    ): InstallExecution {
        val toolAction = conflictActions["workflow"] ?: conflictActions["tool:${ScriptScopeRepository.localToolFunctionName(scriptUid)}"]
            ?: WorkflowBundleInstaller.ConflictAction.OVERWRITE
        if (toolAction == WorkflowBundleInstaller.ConflictAction.SKIP) {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            return InstallExecution(
                WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    warnings = listOf("tool_conflict_skipped")
                )
            )
        }

        val toolRoot = File(ScriptConst.Save_Tool_Path)
        FileUtils.makeDirs(toolRoot.absolutePath)
        val targetDir = File(toolRoot, scriptUid)
        if (targetDir.exists()) {
            runCatching { FileUtils.clearDirectory(targetDir, true) }
        }

        return try {
            if (!moveDir(primaryDir, targetDir)) {
                return InstallExecution(
                    fail(
                        manifest,
                        GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_install_failed)
                    )
                )
            }

            val toolId = ScriptScopeRepository.localToolFunctionName(scriptUid)
            persistInstalledToolMetadata(targetDir, manifest)
            upsertPackageIndex(
                manifest = manifest,
                primaryType = WorkflowBundleManifest.PRIMARY_TYPE_TOOL,
                primaryResourceId = toolId,
                installPath = targetDir.absolutePath
            )
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            InstallExecution(
                result = WorkflowBundleInstaller.InstallResult(
                    success = true,
                    isBundle = true,
                    bundleId = manifest.bundleId,
                    title = manifest.title,
                    installedToolIds = listOf(toolId)
                ),
                runPostInstall = {
                    registerInstalledTool(
                        scriptName = manifest.title.ifBlank { primaryDir.name },
                        scriptDesc = "",
                        scriptPath = targetDir.absolutePath,
                        toolId = toolId
                    )
                }
            )
        } catch (t: Throwable) {
            runCatching { FileUtils.clearDirectory(tempRoot, true) }
            InstallExecution(
                fail(
                    manifest,
                    t.message ?: GlobalApp.getString(com.hive.i8n.R.string.sc_bundle_import_failed)
                )
            )
        }
    }

    private fun persistInstalledWorkflowMetadata(
        workflowDir: File,
        manifest: WorkflowBundleManifest
    ) {
        runCatching {
            if (!workflowDir.exists()) return@runCatching
            val manifestFile = File(workflowDir, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
            manifestFile.writeText(GsonHelper.getInstance().toJson(manifest))
            File(workflowDir, "bundle").takeIf { it.exists() && it.isDirectory }
                ?.let { FileUtils.clearDirectory(it, true); it.delete() }
        }
    }

    private fun resolvePrimarySkill(
        snapshot: ScriptScopeSnapshot,
        manifest: WorkflowBundleManifest
    ): ScopedSkillSpec? {
        val primaryId = manifest.primaryScriptUid?.takeIf { it.isNotBlank() }
        return when {
            primaryId != null -> snapshot.skills.firstOrNull { it.id == primaryId } ?: snapshot.skills.firstOrNull()
            else -> snapshot.skills.firstOrNull()
        }
    }

    private fun persistInstalledSkillMetadata(
        skillPackageDir: File,
        manifest: WorkflowBundleManifest,
        primarySkill: ScopedSkillSpec
    ) {
        runCatching {
            if (!skillPackageDir.exists()) return@runCatching
            File(skillPackageDir, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
                .writeText(GsonHelper.getInstance().toJson(manifest))
            SkillFileHelper.writePrimarySkillFileToPackageDir(skillPackageDir, primarySkill.toSkillSpec())
        }
    }

    private fun persistInstalledToolMetadata(
        toolDir: File,
        manifest: WorkflowBundleManifest
    ) {
        runCatching {
            if (!toolDir.exists()) return@runCatching
            File(toolDir, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
                .writeText(GsonHelper.getInstance().toJson(manifest))
        }
    }

    private fun upsertPackageIndex(
        manifest: WorkflowBundleManifest,
        primaryType: String,
        primaryResourceId: String,
        installPath: String
    ) {
        val packageId = manifest.bundleId.takeIf { it.isNotBlank() } ?: return
        PackageIndexRepository.upsert(
            PackageIndexRecord(
                packageId = packageId,
                source = "bundle",
                version = null,
                primaryType = primaryType,
                primaryResourceId = primaryResourceId,
                installPath = installPath
            )
        )
    }

    private fun registerInstalledTool(
        scriptName: String,
        scriptDesc: String,
        scriptPath: String,
        toolId: String
    ) {
        ScriptMcpRegister.registerCustomTool(
            scriptName = scriptName,
            scriptDesc = scriptDesc,
            scriptPath = scriptPath,
            toolId = toolId,
            overwriteIfExists = true,
            persistToSp = true
        )
    }

    private fun moveDir(from: File, to: File): Boolean {
        FileUtils.makeDirs(to.parentFile?.absolutePath ?: ScriptConst.Save_Script_Path)
        if (from.renameTo(to)) return true
        return runCatching {
            FileUtils.copyFolderTo(from.absolutePath, to.absolutePath)
            FileUtils.clearDirectory(from, true)
            true
        }.getOrDefault(false)
    }

    private fun restoreBackup(targetDir: File, backupDir: File?) {
        runCatching {
            if (targetDir.exists()) FileUtils.clearDirectory(targetDir, true)
            if (backupDir != null && backupDir.exists()) {
                backupDir.renameTo(targetDir)
            }
        }
    }

    /**
     * 解析目标目录，保证幂等：优先使用 bundle 中的 workflow 名，仅在 SKIP 时返回 null。
     */
    private fun resolveTargetDir(
        scriptUid: String,
        workflowName: String,
        existingDir: File?,
        workflowAction: WorkflowBundleInstaller.ConflictAction
    ): File? {
        if (existingDir != null) return existingDir
        val baseTargetDir = File(ScriptConst.Save_Script_Path, workflowName)
        if (!baseTargetDir.exists()) return baseTargetDir
        val existingUid = ScriptScopeRepository.getScriptUid(baseTargetDir)
        return when {
            existingUid == scriptUid -> baseTargetDir
            workflowAction == WorkflowBundleInstaller.ConflictAction.SKIP -> null
            else -> baseTargetDir
        }
    }

    private fun fail(
        manifest: WorkflowBundleManifest,
        message: String
    ): WorkflowBundleInstaller.InstallResult {
        return WorkflowBundleInstaller.InstallResult(
            success = false,
            isBundle = true,
            bundleId = manifest.bundleId,
            title = manifest.title,
            errorMessage = message
        )
    }
}
