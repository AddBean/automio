// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.scope

import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdRunSkill
import com.hive.script.base.core.ScriptLineTokenizer
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.script.net.data.ScriptCustomSkill
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.bundle.WorkflowBundleExporter
import com.hive.script.utils.bundle.WorkflowBundleManifest
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.GsonHelper
import java.io.File

data class ExportScanResult(
    val scannedSkillIds: Set<String>,
    val scannedToolPaths: Set<String>,
    /** Workflow 依赖路径（callScript 引用，放入 dependence/scripts） */
    val scannedScriptPaths: Set<String>,
    /** 从主脚本 + 依赖脚本 mate 中收集到的权限（默认勾选） */
    val scannedPermissions: Set<String>,
    /** 所有可选的权限列表 (permissionId to displayName) */
    val allPermissions: List<Pair<String, String>>,
    val allSkills: List<SkillSpec>,
    val allTools: List<ExportToolItem>,
    val errors: List<String>
)

data class ExportToolItem(
    val canonicalPath: String,
    val displayName: String,
    val toolId: String,
    /** 是否为 Tool 依赖（skill.allowedToolNames）；false 表示仅 Workflow 依赖（callScript） */
    val isTool: Boolean = true
)

object ScriptScopeBundleV2Exporter {
    private data class CollectedScript(
        val sourceDir: File,
        var displayName: String,
        var description: String,
        var sourceGlobalToolId: String? = null,
        var isFromCallScript: Boolean = false
    )

    /**
     * 扫描 workflow 依赖，返回可选的 Workflow/Skill/Tool 列表及扫描结果。
     * 依赖资源有三种：Workflow（callScript）、Skill（runSkill）、Tool（allowedToolNames）。
     * 合并多来源：全局 registry + 所有 dependence + SP custom tools，确保展示全部。
     */
    fun scanDependencies(workflowDir: File): ExportScanResult {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider

        val allSkills = loadAllSkillsFromAllSources(provider)
        val allSkillsMap = allSkills.associateBy { it.id }

        val (allToolsMap, toolIdToPath) = loadAllToolsFromAllSources()

        val pendingScripts = ArrayDeque<File>()
        pendingScripts.add(workflowDir)
        val pendingSkills = ArrayDeque<String>()
        val scannedScripts = LinkedHashSet<String>()
        val collectedScripts = LinkedHashMap<String, CollectedScript>()
        val collectedCallScriptPaths = LinkedHashSet<String>()
        val collectedSkills = LinkedHashMap<String, SkillSpec>()
        val errors = mutableListOf<String>()

        while (pendingScripts.isNotEmpty() || pendingSkills.isNotEmpty()) {
            while (pendingScripts.isNotEmpty()) {
                val scriptDir = pendingScripts.removeFirst()
                val canonical = scriptDir.canonicalPath
                if (!scannedScripts.add(canonical)) continue
                if (!scriptDir.exists() || !scriptDir.isDirectory) {
                    errors.add("Missing script directory: $canonical")
                    continue
                }
                val root = runCatching {
                    ScriptCommandRoot().also {
                        ScriptCommandRoot.loadScriptSync(
                            scriptDir.absolutePath,
                            it
                        )
                    }
                }.getOrElse {
                    errors.add("Unable to parse script: $canonical")
                    continue
                }
                root.getRootScript()?.forEachAllCommand { cmd ->
                    when (cmd) {
                        is CmdRunSkill -> {
                            cmd.skillId?.takeIf { it.isNotBlank() }
                                ?.let { pendingSkills.addLast(it) }
                        }

                        is CmdCallScript -> {
                            val rawPath = cmd.scriptPath?.takeIf { it.isNotBlank() }
                                ?: return@forEachAllCommand
                            val resolvedPath = when {
                                ScriptScopeRepository.isScopedScriptRef(rawPath) ->
                                    ScriptScopeRepository.resolveScopedScriptPath(
                                        currentScriptDir = scriptDir,
                                        ref = rawPath
                                    )
                                else -> rawPath
                            } ?: run {
                                if (ScriptScopeRepository.isScopedScriptRef(rawPath)) {
                                    errors.add("Cannot resolve scope ref: $rawPath")
                                }
                                return@forEachAllCommand
                            }
                            val dependencyDir = File(resolvedPath)
                            if (!dependencyDir.exists() || !dependencyDir.isDirectory) {
                                errors.add("Unsupported external script dependency: $rawPath")
                                return@forEachAllCommand
                            }
                            if (dependencyDir.canonicalPath == workflowDir.canonicalPath) return@forEachAllCommand
                            val scriptUidOrNull = ScriptScopeRepository.getScriptUid(dependencyDir)
                            if (scriptUidOrNull == null) {
                                errors.add("Missing scriptUid for workflow dependency: $rawPath")
                                return@forEachAllCommand
                            }
                            collectedCallScriptPaths.add(dependencyDir.canonicalPath)
                            collectedScripts.getOrPut(dependencyDir.canonicalPath) {
                                CollectedScript(
                                    sourceDir = dependencyDir,
                                    displayName = dependencyDir.name,
                                    description = GlobalApp.getString(
                                        com.hive.i8n.R.string.sc_bundle_default_tool_desc,
                                        dependencyDir.name
                                    ),
                                    isFromCallScript = true
                                )
                            }.isFromCallScript = true
                            pendingScripts.addLast(dependencyDir)
                        }
                    }
                }
            }

            while (pendingSkills.isNotEmpty()) {
                val skillId = pendingSkills.removeFirst()
                if (collectedSkills.containsKey(skillId)) continue
                val spec = allSkillsMap[skillId]
                if (spec == null) {
                    errors.add("Missing skill dependency: $skillId")
                    continue
                }
                collectedSkills[skillId] = spec
                spec.allowedToolNames.forEach { toolName ->
                    resolveToolToCollectedScript(
                        toolName = toolName,
                        toolIdToPath = toolIdToPath,
                        collectedScripts = collectedScripts,
                        pendingScripts = pendingScripts,
                        errors = errors
                    )
                }
                spec.fallbackSkillId?.takeIf { it.isNotBlank() }?.let { pendingSkills.addLast(it) }
            }
        }

        collectedScripts.forEach { (path, collected) ->
            if (path !in allToolsMap) {
                val toolId = ScriptScopeRepository.getScriptUid(collected.sourceDir)?.let {
                    ScriptScopeRepository.localToolFunctionName(it)
                }
                    ?: "${ScriptConst.SCRIPT_TOOL_ID_PREFIX}${collected.sourceDir.name}"
                allToolsMap[path] = ExportToolItem(
                    canonicalPath = path,
                    displayName = collected.displayName,
                    toolId = toolId,
                    isTool = collected.sourceGlobalToolId != null
                )
            }
        }

        // 从主脚本 + 依赖脚本 mate 中收集权限
        val scannedPermissions = mutableSetOf<String>()
        scannedPermissions.addAll(collectPermissionsFromDir(workflowDir))
        collectedScripts.keys.forEach { path ->
            scannedPermissions.addAll(collectPermissionsFromDir(File(path)))
        }
        val allPermissions = ScriptHelper.mPermissionMap.toList()

        return ExportScanResult(
            scannedSkillIds = collectedSkills.keys.toSet(),
            scannedToolPaths = collectedScripts.keys.toSet(),
            scannedScriptPaths = collectedCallScriptPaths,
            scannedPermissions = scannedPermissions,
            allPermissions = allPermissions,
            allSkills = allSkills.distinctBy { it.id },
            allTools = allToolsMap.values.distinctBy { it.canonicalPath },
            errors = errors.distinct()
        )
    }

    /**
     * 按用户选择的 skill/tool/权限 导出 bundle。
     * @param primaryType workflow | tool，默认 workflow。tool 导出时传 tool 以设置 manifest.primaryType
     * @param newWorkflowName 可选：用户自定义的工作流名称，用于覆盖原始名称
     */
    fun exportWithSelection(
        workflow: ScriptInfoModel,
        workflowDir: File,
        outputZip: File,
        scanResult: ExportScanResult,
        selectedSkillIds: Set<String>,
        selectedToolPaths: Set<String>,
        selectedPermissions: Set<String>,
        primaryType: String = WorkflowBundleManifest.PRIMARY_TYPE_WORKFLOW,
        newWorkflowName: String? = null
    ) {
        if (!selectedSkillIds.containsAll(scanResult.scannedSkillIds) ||
            !selectedToolPaths.containsAll(scanResult.scannedToolPaths)
        ) {
            throw IllegalStateException(
                GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_required_missing)
            )
        }
        val scriptUid = workflow.scriptMate?.scriptUid
            ?: ScriptScopeRepository.getScriptUid(workflowDir)
            ?: throw IllegalArgumentException("Workflow scriptUid missing")
        val scopeId = ScriptScopeRepository.scopeId(scriptUid)
        // 使用用户自定义名称或原始名称
        val workflowName = newWorkflowName?.takeIf { it.isNotBlank() }
            ?: workflow.scriptName?.takeIf { it.isNotBlank() }
            ?: workflowDir.name

        val allSkillsMap = scanResult.allSkills.associateBy { it.id }
        val toolIdToPath = scanResult.allTools.associate { it.toolId to it.canonicalPath }

        val collectedSkills = selectedSkillIds.mapNotNull { allSkillsMap[it] }.associateBy { it.id }
        val toolItemsByPath = scanResult.allTools.associateBy { it.canonicalPath }
        val collectedScripts = selectedToolPaths.mapNotNull { path ->
            toolItemsByPath[path]?.let { item ->
                val isFromCallScript = path in scanResult.scannedScriptPaths
                CollectedScript(
                    sourceDir = File(path),
                    displayName = item.displayName,
                    description = GlobalApp.getString(
                        com.hive.i8n.R.string.sc_bundle_default_tool_desc,
                        item.displayName
                    ),
                    sourceGlobalToolId = if (item.isTool) item.toolId else null,
                    isFromCallScript = isFromCallScript
                )
            }
        }.associateBy { it.sourceDir.canonicalPath }
            .toMutableMap()

        val toolScripts = collectedScripts.values.filter { it.sourceGlobalToolId != null }
        val dependencyOnlyWorkflows =
            collectedScripts.values.filter { it.isFromCallScript && it.sourceGlobalToolId == null }

        val toolSourceDirsByToolUid = HashMap<String, File>()
        val toolSpecs = toolScripts.map { collected ->
            val toolUid = ScriptScopeRepository.getScriptUid(collected.sourceDir)
                ?: throw IllegalStateException("Missing scriptUid: ${collected.sourceDir.absolutePath}")
            toolSourceDirsByToolUid[toolUid] = collected.sourceDir
            ScopedToolSpec(
                toolUid = toolUid,
                localId = ScriptScopeRepository.normalizeId(toolUid),
                functionName = ScriptScopeRepository.localToolFunctionName(toolUid),
                scriptDir = toolUid,
                name = collected.displayName,
                description = collected.description,
                sourceGlobalToolId = collected.sourceGlobalToolId,
                sourceScriptUid = toolUid,
                isPublic = false
            )
        }.sortedBy { it.localId }

        val dependencyScriptRefs = dependencyOnlyWorkflows.map { collected ->
            val scriptUid = ScriptScopeRepository.getScriptUid(collected.sourceDir)
                ?: throw IllegalStateException("Missing scriptUid: ${collected.sourceDir.absolutePath}")
            ScopedDependencyScriptRef(
                scriptUid = scriptUid,
                localId = ScriptScopeRepository.normalizeId(scriptUid),
                scriptDir = scriptUid,
                name = collected.displayName
            )
        }.sortedBy { it.localId }
        val dependencySourceDirsByUid = dependencyOnlyWorkflows.associate { collected ->
            ScriptScopeRepository.getScriptUid(collected.sourceDir)!! to collected.sourceDir
        }

        val localFunctionByGlobalToolId = toolSpecs
            .mapNotNull { spec -> spec.sourceGlobalToolId?.let { it to spec.functionName } }
            .toMap()
        val scopedSkills = collectedSkills.values.map { spec ->
            val allowedToolNames = spec.allowedToolNames.map { toolName ->
                val rawToolId = splitToolFunctionName(toolName).second
                val scriptToolId = if (toolName.startsWith(ScriptConst.SCRIPT_TOOL_ID_PREFIX)) toolName else rawToolId
                val toolPath = toolIdToPath[scriptToolId] ?: toolIdToPath[toolName]
                if (toolPath != null && toolPath in selectedToolPaths) {
                    toolName
                } else {
                    localFunctionByGlobalToolId[scriptToolId]
                        ?: localFunctionByGlobalToolId[toolName] ?: toolName
                }
            }
            ScopedSkillSpec.from(
                spec.copy(
                    allowedToolNames = allowedToolNames,
                    fallbackSkillId = spec.fallbackSkillId?.takeIf { it in selectedSkillIds }
                )
            ).copy(isPublic = false)
        }.sortedBy { it.id }

        val stagingRoot = createStagingRoot()
        val preparedWorkflowDir = File(stagingRoot, workflowName)
        FileUtils.makeDirs(preparedWorkflowDir.parentFile?.absolutePath ?: stagingRoot.absolutePath)
        FileUtils.copyFolderTo(workflowDir.absolutePath, preparedWorkflowDir.absolutePath)
        runCatching {
            File(preparedWorkflowDir, "bundle").takeIf { it.exists() && it.isDirectory }
                ?.let { FileUtils.clearDirectory(it, true); it.delete() }
        }
        toolSpecs.forEach { spec ->
            val sourceDir = toolSourceDirsByToolUid[spec.toolUid]
                ?: throw IllegalStateException("Missing tool sourceDir: ${spec.toolUid}")
            val destDir =
                File(ScriptScopeRepository.getToolsDir(preparedWorkflowDir), spec.scriptDir)
            FileUtils.makeDirs(destDir.parentFile?.absolutePath ?: preparedWorkflowDir.absolutePath)
            FileUtils.copyFolderTo(sourceDir.absolutePath, destDir.absolutePath)
        }
        dependencyScriptRefs.forEach { ref ->
            val sourceDir = dependencySourceDirsByUid[ref.scriptUid]
                ?: throw IllegalStateException("Missing workflow dependency sourceDir: ${ref.scriptUid}")
            val destDir =
                File(ScriptScopeRepository.getScriptsDir(preparedWorkflowDir), ref.scriptDir)
            FileUtils.makeDirs(destDir.parentFile?.absolutePath ?: preparedWorkflowDir.absolutePath)
            FileUtils.copyFolderTo(sourceDir.absolutePath, destDir.absolutePath)
        }

        val allBundledScriptUids = toolSourceDirsByToolUid.keys + dependencySourceDirsByUid.keys
        rewriteCallScriptPathsToScopedRef(
            rootDir = preparedWorkflowDir,
            toolUids = allBundledScriptUids
        )

        ScriptScopeRepository.save(
            preparedWorkflowDir,
            ScriptScopeSnapshot(
                scopeId = scopeId,
                scriptUid = scriptUid,
                tools = toolSpecs,
                skills = scopedSkills,
                scripts = dependencyScriptRefs
            )
        )

        val manifestScriptEntries = mutableListOf<WorkflowBundleManifest.BundleScriptEntry>()
        manifestScriptEntries.add(
            WorkflowBundleManifest.BundleScriptEntry(
                dir = workflowName,
                role = WorkflowBundleManifest.ROLE_WORKFLOW,
                entryType = WorkflowBundleManifest.ENTRY_PRIMARY
            )
        )
        dependencyScriptRefs.forEach { ref ->
            manifestScriptEntries.add(
                WorkflowBundleManifest.BundleScriptEntry(
                    dir = "$workflowName/${ScriptScopeRepository.DEPENDENCE_DIR_NAME}/${ScriptScopeRepository.SCRIPTS_DIR_NAME}/${ref.scriptDir}",
                    role = WorkflowBundleManifest.ROLE_WORKFLOW,
                    entryType = WorkflowBundleManifest.ENTRY_DEPENDENCY
                )
            )
        }
        val manifest = WorkflowBundleManifest(
            bundleVersion = WorkflowBundleManifest.BUNDLE_VERSION_V2,
            bundleId = "bundle.$scriptUid.${System.currentTimeMillis()}",
            title = workflowName,
            scripts = manifestScriptEntries,
            primaryScriptDir = workflowName,
            primaryScriptUid = scriptUid,
            installPolicy = WorkflowBundleManifest.InstallPolicy(
                onScriptDirConflict = "overwrite"
            ),
            permissions = selectedPermissions.takeIf { it.isNotEmpty() }?.toList(),
            primaryType = primaryType,
            authorId = null
        )
        writeManifestAndExport(stagingRoot, manifest, outputZip)
    }

    /**
     * 导出 skill bundle。从 skillSpec.allowedToolNames 解析依赖的 script tools，构建 scope 目录（无 main.script），打包为 zip。
     * @param newSkillName 可选：用户自定义的 skill 名称，用于覆盖原始名称
     */
    fun exportSkillBundle(skillSpec: SkillSpec, outputZip: File, newSkillName: String? = null) {
        val skillName = newSkillName?.takeIf { it.isNotBlank() } ?: skillSpec.name
        val (_, toolIdToPath) = loadAllToolsFromAllSources()
        val collectedScripts = mutableMapOf<String, CollectedScript>()
        skillSpec.allowedToolNames.forEach { toolName ->
            val (_, rawToolId) = splitToolFunctionName(toolName)
            val scriptToolId = if (toolName.startsWith(ScriptConst.SCRIPT_TOOL_ID_PREFIX)) toolName else rawToolId
            val path = toolIdToPath[scriptToolId] ?: toolIdToPath[toolName] ?: return@forEach
            val toolDir = File(path)
            if (!toolDir.exists() || !toolDir.isDirectory) return@forEach
            collectedScripts.getOrPut(toolDir.canonicalPath) {
                val displayName = toolDir.name
                CollectedScript(
                    sourceDir = toolDir,
                    displayName = displayName,
                    description = GlobalApp.getString(
                        com.hive.i8n.R.string.sc_bundle_default_tool_desc,
                        displayName
                    ),
                    sourceGlobalToolId = scriptToolId
                )
            }
        }

        val scopeId = skillSpec.id
        val scriptUid = skillSpec.id
        val toolSourceDirsByToolUid = HashMap<String, File>()
        val toolSpecs = collectedScripts.values.map { collected ->
            val toolUid = ScriptScopeRepository.getScriptUid(collected.sourceDir)
                ?: throw IllegalStateException("Missing scriptUid: ${collected.sourceDir.absolutePath}")
            toolSourceDirsByToolUid[toolUid] = collected.sourceDir
            ScopedToolSpec(
                toolUid = toolUid,
                localId = ScriptScopeRepository.normalizeId(toolUid),
                functionName = ScriptScopeRepository.localToolFunctionName(toolUid),
                scriptDir = toolUid,
                name = collected.displayName,
                description = collected.description,
                sourceGlobalToolId = collected.sourceGlobalToolId,
                sourceScriptUid = toolUid,
                isPublic = true
            )
        }.sortedBy { it.localId }

        // 使用自定义名称覆盖 skillSpec.name
        val scopedSkill = ScopedSkillSpec.from(skillSpec.copy(name = skillName)).copy(isPublic = true)

        val stagingRoot = createStagingRoot()
        val scopeDirName = "scope_${ScriptScopeRepository.normalizeId(skillSpec.id)}"
        val preparedScopeDir = File(stagingRoot, scopeDirName)
        FileUtils.makeDirs(preparedScopeDir.absolutePath)
        FileUtils.makeDirs(ScriptScopeRepository.getToolsDir(preparedScopeDir).absolutePath)
        runCatching {
            File(preparedScopeDir, "bundle").takeIf { it.exists() && it.isDirectory }
                ?.let { FileUtils.clearDirectory(it, true); it.delete() }
        }
        toolSpecs.forEach { spec ->
            val sourceDir = toolSourceDirsByToolUid[spec.toolUid]
                ?: throw IllegalStateException("Missing tool sourceDir: ${spec.toolUid}")
            val destDir = File(ScriptScopeRepository.getToolsDir(preparedScopeDir), spec.scriptDir)
            FileUtils.makeDirs(destDir.parentFile?.absolutePath ?: preparedScopeDir.absolutePath)
            FileUtils.copyFolderTo(sourceDir.absolutePath, destDir.absolutePath)
        }

        ScriptScopeRepository.save(
            preparedScopeDir,
            ScriptScopeSnapshot(
                scopeId = scopeId,
                scriptUid = scriptUid,
                tools = toolSpecs,
                skills = listOf(scopedSkill)
            )
        )

        val manifest = WorkflowBundleManifest(
            bundleVersion = WorkflowBundleManifest.BUNDLE_VERSION_V2,
            bundleId = "bundle.skill.${skillSpec.id}.${System.currentTimeMillis()}",
            title = skillName,
            scripts = listOf(
                WorkflowBundleManifest.BundleScriptEntry(
                    dir = scopeDirName,
                    role = WorkflowBundleManifest.ROLE_WORKFLOW,
                    entryType = WorkflowBundleManifest.ENTRY_PRIMARY
                )
            ),
            primaryScriptDir = scopeDirName,
            primaryScriptUid = scriptUid,
            primaryType = WorkflowBundleManifest.PRIMARY_TYPE_SKILL,
            authorId = null
        )
        writeManifestAndExport(stagingRoot, manifest, outputZip)
    }

    private fun createStagingRoot(): File {
        val stagingRoot = File(
            GlobalApp.getContext().cacheDir,
            "bundle_export_${System.currentTimeMillis()}"
        )
        if (stagingRoot.exists()) FileUtils.clearDirectory(stagingRoot, true)
        FileUtils.makeDirs(stagingRoot.absolutePath)
        return stagingRoot
    }

    private fun writeManifestAndExport(stagingRoot: File, manifest: WorkflowBundleManifest, outputZip: File) {
        File(stagingRoot, WorkflowBundleManifest.DEFAULT_MANIFEST_NAME)
            .writeText(GsonHelper.getInstance().toJson(manifest))
        WorkflowBundleExporter.exportZipFromStaging(outputZip, stagingRoot)
        runCatching { FileUtils.clearDirectory(stagingRoot, true) }
    }

    /**
     * 解析 skill 的 allowedToolNames 中的 script tool，从全局 registry 的 toolIdToPath 查找并加入 collectedScripts。
     */
    private fun resolveToolToCollectedScript(
        toolName: String,
        toolIdToPath: Map<String, String>,
        collectedScripts: MutableMap<String, CollectedScript>,
        pendingScripts: ArrayDeque<File>,
        errors: MutableList<String>
    ) {
        val (_, rawToolId) = splitToolFunctionName(toolName)
        val scriptToolId = when {
            toolName.startsWith(ScriptConst.SCRIPT_TOOL_ID_PREFIX) -> toolName
            rawToolId.startsWith(ScriptConst.SCRIPT_TOOL_ID_PREFIX) -> rawToolId
            else -> return
        }
        val path = toolIdToPath[scriptToolId] ?: toolIdToPath[rawToolId]
        if (path == null) {
            errors.add("Missing script tool dependency: $scriptToolId")
            return
        }
        val toolDir = File(path)
        if (!toolDir.exists() || !toolDir.isDirectory) {
            errors.add("Missing tool script directory: $path")
            return
        }
        collectedScripts.getOrPut(toolDir.canonicalPath) {
            CollectedScript(
                sourceDir = toolDir,
                displayName = toolDir.name,
                description = GlobalApp.getString(
                    com.hive.i8n.R.string.sc_bundle_default_tool_desc,
                    toolDir.name
                ),
                sourceGlobalToolId = scriptToolId
            )
        }.sourceGlobalToolId = scriptToolId
        pendingScripts.addLast(toolDir)
    }

    /** 合并全局 registry + 所有 dependence 中的 skills，确保展示全部 */
    private fun loadAllSkillsFromAllSources(provider: IAgentProvider?): List<SkillSpec> {
        val fromRegistry =
            runCatching { provider?.listSkills().orEmpty() }.getOrElse { emptyList() }
        val fromDependence = mutableListOf<SkillSpec>()
        LocalResourceListRepository.listWorkflows().forEach { dir ->
            runCatching { ScriptScopeRepository.load(dir, validate = false) }
                .getOrNull()?.skills?.forEach { scoped ->
                    fromDependence.add(scoped.toSkillSpec())
                }
        }
        val fromSkillSp = readCustomSkillsFromRefs()
        return (fromRegistry + fromDependence + fromSkillSp)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
    }

    /** 合并全局 registry + 所有 dependence + SP custom tools，确保展示全部 */
    private fun loadAllToolsFromAllSources(): Pair<LinkedHashMap<String, ExportToolItem>, MutableMap<String, String>> {
        val allToolsMap = LinkedHashMap<String, ExportToolItem>()
        val toolIdToPath = mutableMapOf<String, String>()

        LocalResourceListRepository.listWorkflows().forEach { workflowDir ->
            runCatching { ScriptScopeRepository.load(workflowDir, validate = false) }
                .getOrNull()?.tools?.forEach { spec ->
                    val toolDir =
                        File(ScriptScopeRepository.getToolsDir(workflowDir), spec.scriptDir)
                    if (toolDir.exists() && toolDir.isDirectory) {
                        val canonical = toolDir.canonicalPath
                        if (canonical !in allToolsMap) {
                            allToolsMap[canonical] = ExportToolItem(
                                canonicalPath = canonical,
                                displayName = spec.name,
                                toolId = spec.functionName
                            )
                            toolIdToPath[spec.functionName] = canonical
                        }
                    }
                }
        }

        CustomStorage.readCustomTools().forEach { tool ->
            val toolDir = File(tool.scriptPath)
            if (toolDir.exists() && toolDir.isDirectory) {
                val canonical = toolDir.canonicalPath
                val toolId = tool.scriptId
                if (canonical !in allToolsMap) {
                    allToolsMap[canonical] = ExportToolItem(
                        canonicalPath = canonical,
                        displayName = tool.scriptName,
                        toolId = toolId
                    )
                    toolIdToPath[toolId] = canonical
                }
            }
        }

        return Pair(allToolsMap, toolIdToPath)
    }

    private fun readCustomSkillsFromRefs(): List<SkillSpec> {
        return CustomStorage.readCustomSkillRefs()
            .filter { File(it.skillPath).exists() && File(it.skillPath).isFile }
            .mapNotNull { SkillFileHelper.readSkillFile(it.skillPath) }
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
    }

    private fun collectPermissionsFromDir(dir: File): Set<String> {
        val perms = mutableSetOf<String>()
        runCatching {
            ScriptInfoModel().parseInfoFile(dir).scriptMate?.permission?.let { perms.addAll(it) }
            ScriptInfoModel().parseMainFile(dir).scriptMate?.permission?.let { perms.addAll(it) }
        }
        return perms
    }

    private fun rewriteCallScriptPathsToScopedRef(
        rootDir: File,
        toolUids: Set<String>
    ) {
        if (toolUids.isEmpty()) return
        val scriptFiles = mutableListOf<File>()
        collectScriptFiles(rootDir, scriptFiles)
        scriptFiles.forEach { file ->
            val original = runCatching { file.readText() }.getOrNull() ?: return@forEach
            var changed = false
            val lines = original.split('\n')
            val rewritten = lines.joinToString("\n") { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@joinToString line
                if (ScriptLineTokenizer.getCommandName(trimmed) != "callScript") return@joinToString line
                val rawPath = ScriptLineTokenizer.parseKeyValueParams(trimmed)["path"]
                    ?: return@joinToString line
                if (ScriptScopeRepository.isScopedScriptRef(rawPath)) return@joinToString line

                val depDir = File(rawPath)
                if (!depDir.exists() || !depDir.isDirectory) return@joinToString line
                val depUid = ScriptScopeRepository.getScriptUid(depDir) ?: return@joinToString line
                if (depUid !in toolUids) return@joinToString line

                val scopedRef =
                    ScriptScopeRepository.toScopedScriptRef(ScriptScopeRepository.normalizeId(depUid))
                val replaced = replaceCmdQuotedParam(line, key = "path", newValue = scopedRef)
                if (replaced != line) changed = true
                replaced
            }
            if (changed) {
                runCatching { file.writeText(rewritten) }
            }
        }
    }

    private fun collectScriptFiles(dir: File, out: MutableList<File>) {
        if (!dir.exists()) return
        val children = dir.listFiles() ?: return
        children.forEach { f ->
            if (f.isDirectory) {
                collectScriptFiles(f, out)
            } else if (f.isFile && f.name.endsWith(ScriptConst.SCRIPT_SUFFIX)) {
                out.add(f)
            }
        }
    }

    private fun replaceCmdQuotedParam(line: String, key: String, newValue: String): String {
        val escaped = newValue.replace("\\", "\\\\").replace("\"", "\\\"")
        val quoted = Regex("""\b${Regex.escape(key)}="(?:[^"\\]|\\.)*"""")
        val replacedQuoted = line.replace(quoted, """$key="$escaped"""")
        if (replacedQuoted != line) return replacedQuoted
        val unquoted = Regex("""\b${Regex.escape(key)}=[^\s#@]+""")
        return line.replace(unquoted, """$key="$escaped"""")
    }

    private fun splitToolFunctionName(name: String): Pair<String, String> {
        val idx = name.indexOf('.')
        return if (idx > 0 && idx < name.length - 1) {
            Pair(name.substring(0, idx + 1), name.substring(idx + 1))
        } else {
            Pair("", name)
        }
    }

}
