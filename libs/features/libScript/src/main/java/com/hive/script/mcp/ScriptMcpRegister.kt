// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.mcp

import android.graphics.Bitmap
import android.graphics.RectF
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.InferenceType
import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpTool
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.ScriptScreenShotService
import com.hive.script.base.ScriptConst
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.tools.McpFileResultHelper
import com.hive.script.mcp.tools.Mcp_Tools_Register_Set
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.script.record.AgentCommandRecorder
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.utils.ScreenPageInfoFormatter
import com.hive.script.utils.ScriptLayoutReader
import com.hive.script.views.agent.ScriptAgentTopView
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.GsonHelper
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import java.io.File
import com.hive.script.base.ScriptMate
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogCmdDialogInput
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.ocr.OcrResult
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.scope.CustomStorage

object ScriptMcpRegister {
    private const val TAG = "ScriptMcpRegister"

    private val mcpToolsCache = mutableMapOf<String, McpToolBuilder>()

    /** 运行时缓存：scriptId -> ScriptCustomMcpTool。包含来自 workflow 依赖的工具（persistToSp=false），SP 中无记录 */
    private val customToolRuntimeCache = mutableMapOf<String, ScriptCustomMcpTool>()

    /**
     * 注册所有 MCP 工具到 MCP 组件。
     * 仅注册 Mcp_Tools_Register_Set 中的内置工具；脚本工具（custom.*）由 registerCustomTool 直接注册到 MCP。
     */
    fun registerAll() {
        Mcp_Tools_Register_Set.forEach {
            val inst = it.newInstance() as McpToolBuilder
            DLog.d(TAG, "Register MCP Tool: ${inst.getActionName()}")
            register(inst)
        }
    }

    /**
     * 注册 ScriptMcpImpl 到 MCP 组件
     */
    fun register(tool: McpToolBuilder?) {
        tool ?: return
        try {
            val actionName = tool.getActionName() ?: return
            mcpToolsCache[normalizeBuildInActionName(actionName)] = tool
            val mcpProvider = ComponentManager.getInstance()
                .getProvider(IMcpProvider::class.java) as? IMcpProvider
            if (mcpProvider != null) {
                try {
                    mcpProvider.registerTool(buildMcpTool(tool.getToolAction() ?: return))
                    DLog.i(TAG, "ScriptMcp tools registered to MCP provider.")
                } catch (e: Exception) {
                    DLog.e(TAG, "Error registering tools: ${e.message}")
                }
            } else {
                DLog.e(TAG, "IMcpProvider not found, registration failed.")
            }
        } catch (e: Exception) {
            DLog.e(TAG, "Error registering ScriptMcp tools: ${e.message}")
        }
    }

    /**
     * 将 McpAction 转换为 McpTool
     * @return 对应的 McpTool 实例
     */
    private fun buildMcpTool(action: McpAction): McpTool {
        val normalizedAction = normalizeBuildInActionName(action.action)
        val inputSchema = JsonObject().apply {
            addProperty("\$schema", "http://json-schema.org/draft-07/schema#")
            addProperty("type", "object")
            addProperty("title", "$normalizedAction - Input Parameters")
            addProperty("description", action.description ?: "Tool function detailed description")
            add("mcpMetadata", JsonObject().apply {
                addProperty("toolType", "RPA")
                addProperty("privacyLevel", "LOW")
                addProperty("timeoutMs", 5000)
                addProperty("confidential", false)
            })
            val requiredParams = action.paramInfo.filter { it.required }.map { it.name }
            add("required", JsonArray().apply { requiredParams.forEach { add(JsonPrimitive(it)) } })
            add("properties", JsonObject().apply {
                action.paramInfo.forEach { param ->
                    add(param.name, JsonObject().apply {
                        addProperty("type", param.type)
                        addProperty("description", param.description ?: "")
                        param.format?.let { addProperty("format", it) }
                        param.examples?.let { exs ->
                            add(
                                "examples",
                                JsonArray().apply { exs.forEach { add(JsonPrimitive(it)) } })
                        }
                        param.enumValues?.let { values ->
                            add(
                                "enum",
                                JsonArray().apply { values.forEach { add(JsonPrimitive(it)) } })
                        }
                    })
                }
            })
        }
        return McpTool(
            name = normalizedAction,
            description = action.description ?: "No description available",
            extraName = action.extraName ?: action.action,
            extraType = action.extraType ?: McpConst.Tool_Type_BuildIn,
            inputSchema = inputSchema,
            handler = { params ->
                val paramMap = params.entrySet().associate { entry ->
                    entry.key to when {
                        entry.value.isJsonPrimitive -> entry.value.asJsonPrimitive.getAsString()
                        entry.value.isJsonNull -> ""
                        else -> entry.value.toString()
                    }
                }
                executeActionTool(
                    McpAction(
                        action = normalizedAction,
                        paramInfo = action.paramInfo,
                        paramValues = paramMap,
                        description = action.description,
                        extraName = action.extraName,
                        extraType = action.extraType
                    )
                )
            }
        )
    }


    /**
     * 根据 MCP 动作名称查找对应的工具
     * @param action 要查找的 MCP 动作名称
     * @return 对应的 McpToolBuilder 实例或 null
     */
    private fun findToolByAction(action: String): McpToolBuilder? {
        return mcpToolsCache[action]
    }

    /**
     * 公开方法：根据动作名称查找工具（用于命令提取器）
     */
    fun findToolByActionPublic(action: String): McpToolBuilder? {
        return mcpToolsCache[normalizeBuildInActionName(action)]
    }

    /**
     * 执行 MCP 动作
     * @param mcpAction 要执行的 MCP 动作
     * @return ActionResult 执行结果
     */
    private suspend fun executeActionTool(mcpAction: McpAction): ActionResult {
        val tool = findToolByAction(mcpAction.action) ?: return ActionResult(
            success = false,
            message = "No tool found for action: ${mcpAction.action}"
        )
        DLog.i(
            TAG,
            "Executing MCP action: ${mcpAction.action} with params: ${mcpAction.paramValues}"
        )
        tool.checkAction(mcpAction).let { checkResult ->
            if (!checkResult.success) {
                return ActionResult(
                    success = false,
                    message = "Action check failed: ${checkResult.message ?: "Unknown error"}"
                )
            }
        }
        // 检查权限
        tool.checkPermission(mcpAction).let { permissionResult ->
            if (!permissionResult.success) {
                return ActionResult(
                    success = false,
                    message = "Permission check failed: ${permissionResult.message ?: "Unknown error"}"
                )
            }
        }
        delay(200)
        val actionResult = tool.executeAction(mcpAction.paramValues)
        if (tool.withScreenLayout() && ScriptScreenShotService.checkPermission()) {
            val layoutResult = ScriptLayoutReader.getCurrentLayout()
            var ocrResultData: OcrResult? = null
            withOverlayHidden {
                ScriptEventHelper.get().tryReadOcrTextInSync(
                    RectF(0f, 0f, 1f, 1f)
                )?.second?.run {
                    ocrResultData = this
                }
            }
            actionResult.extra = ScreenPageInfoFormatter.buildJson(layoutResult, ocrResultData)
        }
        if (tool.withScreenShot() && ScriptScreenShotService.checkPermission() && isMultimodalModelConfigured()) {
            var bmp: Bitmap? = null
            withOverlayHidden {
                bmp = ScriptScreenShotService.instance?.getScreenShot()
            }
            bmp?.let {
                val compressBmp = BitmapUtils.compressAndResize(it, 720, 160)
                val capturePath = ScriptConst.newRandomTempImagePath("jpg")
                BitmapUtils.saveBitmapLocal(compressBmp, capturePath)
                it.recycle()
                compressBmp.recycle()
                McpFileResultHelper.createFileResult(capturePath, "image/jpeg")?.let { resultFile ->
                    actionResult.files = (actionResult.files ?: emptyList()) + listOf(resultFile)
                }
            }
        }
        tool.getLastExecutedCommand()?.let { cmd ->
            AgentCommandRecorder.addCommand(cmd)
        }
        return actionResult
    }

    /**
     * 让 Agent 浮窗 NOT_TOUCHABLE 后执行 block，避免自动化操作点到浮窗。
     * 使用 hideForCapture/showForCapture 不销毁实例，避免 pending Handler 回调 NPE。
     */
    private inline fun withOverlayHidden(block: () -> Unit) {
        ScriptAgentTopView.hideForCapture()
        ScriptThreadManager.delay(100)
        try {
            block()
        } finally {
            ScriptAgentTopView.showForCapture()
        }
    }

    fun registerCustomTool(
        scriptName: String,
        scriptDesc: String,
        scriptPath: String,
        toolId: String? = null,
        overwriteIfExists: Boolean = false,
        persistToSp: Boolean = true,
        onOverwriteConfirm: (() -> Boolean)? = null
    ) {
        val scriptDir = File(scriptPath)
        if (!scriptDir.exists() || !scriptDir.isDirectory) {
            DLog.w(TAG, "registerCustomTool skip, invalid scriptPath=$scriptPath")
            return
        }

        val normalizedId = toolId ?: run {
            val infoModel = ScriptInfoModel().parseMainFile(File(scriptPath))
            val mate = infoModel.scriptMate ?: ScriptMate().also { infoModel.scriptMate = it }

            var uid = mate.scriptUid
            if (uid.isNullOrBlank()) {
                uid = ScriptMate.generateScriptUid()
                mate.scriptUid = uid
                infoModel.scriptMate = mate
                // 写入 main.jds.info（即使主文件加密也能保留 uid）
                infoModel.saveMate()
                // 若主文件是明文 main.jds，同时把 mate 行写回，避免下次解析仍从 main.jds 读不到 uid
                val plainMainFile = File((infoModel.scriptPath ?: scriptPath), ScriptConst.SCRIPT_MAIN_FILE_NAME)
                if (plainMainFile.exists()) {
                    try {
                        val lines = plainMainFile.readLines().toMutableList()
                        if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                            lines[0] = mate.getCommandLines()
                            plainMainFile.writeText(lines.joinToString("\n"))
                        }
                    } catch (e: Exception) {
                        DLog.e(TAG, "Failed to persist scriptUid to main file: ${e.message}")
                    }
                }
            }
            "${ScriptConst.SCRIPT_TOOL_ID_PREFIX}$uid"
        }

        val mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as? IMcpProvider
        if (mcpProvider == null) {
            DLog.e(TAG, "IMcpProvider not found, register script tool to MCP failed.")
            return
        }

        val alreadyExists = runCatching {
            mcpProvider.getRegisteredTools().any { it.name == normalizedId }
        }.getOrDefault(false)
        if (alreadyExists) {
            val allowOverwrite = overwriteIfExists || (onOverwriteConfirm?.invoke() == true)
            if (!allowOverwrite) return
            runCatching { mcpProvider.unregisterTool(normalizedId) }
        }

        val customTool = ScriptCustomMcpTool(
            scriptId = normalizedId,
            scriptName = scriptName,
            scriptDesc = scriptDesc,
            scriptPath = scriptPath
        )

        runCatching {
            mcpProvider.registerTool(buildScriptMcpTool(normalizedId, scriptName, scriptDesc, scriptPath))
        }.onFailure { e ->
            DLog.e(TAG, "register script tool failed: $normalizedId, ${e.message}")
            return
        }

        customToolRuntimeCache[normalizedId] = customTool

        if (persistToSp) {
            val tools = readCustomToolsFormSP().toMutableList()
            tools.removeAll { it.scriptId == normalizedId }
            tools.add(customTool)
            CustomStorage.saveCustomTools(tools)
        }

        // 刷新 Agent 中的 MCP 客户端工具列表（tools/list），让 custom.* 立刻可见
        (ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider)
            ?.refreshAllMcpServer { }
    }

    fun unregisterCustomTool(toolId: String) {
        val normalizedId = toolId
        mcpToolsCache.remove(normalizedId)
        mcpToolsCache.remove(toolId)
        customToolRuntimeCache.remove(normalizedId)
        customToolRuntimeCache.remove(toolId)
        val mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as? IMcpProvider
        mcpProvider?.unregisterTool(normalizedId)
        val tools = readCustomToolsFormSP().toMutableList()
        tools.removeAll { it.scriptId == normalizedId }
        CustomStorage.saveCustomTools(tools)

        (ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider)
            ?.refreshAllMcpServer { }
    }

    /**
     * 检查当前是否已配置多模态模型（支持视觉的模型）
     * 仅当多模态模型有设置时，才应附带截图
     */
    private fun isMultimodalModelConfigured(): Boolean {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        val manager = provider?.getAIServiceManager() ?: return false
        return manager.getInferenceModel(InferenceType.IMAGE) != null
    }

    private fun readCustomToolsFormSP(): List<ScriptCustomMcpTool> {
        return CustomStorage.readCustomTools()
    }

    /**
     * 根据 scriptId 获取自定义工具配置（用于编辑脚本地址等）
     * 优先查运行时缓存（含 workflow 依赖注册的工具，SP 无记录），再查 SP
     */
    fun getCustomTool(scriptId: String): ScriptCustomMcpTool? {
        return customToolRuntimeCache[scriptId] ?: readCustomToolsFormSP().find { it.scriptId == scriptId }
    }

    /**
     * 构建本地作用域工具（用于 Skill 的 allowedToolNames）
     * @param toolId 工具ID（functionName）
     * @param toolName 工具名称
     * @param toolDescription 工具描述
     * @param scriptPath 脚本路径
     * @return McpTool 实例（包含 inputSchema 和 handler）
     */
    fun createLocalScopeTool(
        toolId: String,
        toolName: String,
        toolDescription: String,
        scriptPath: String
    ): McpTool = buildScriptMcpTool(toolId, toolName, toolDescription, scriptPath)

    private fun buildScriptMcpTool(
        toolId: String,
        toolName: String,
        toolDescription: String,
        scriptPath: String
    ): McpTool {
        val inputSchema = buildInputSchemaForScript(scriptPath)
        return McpTool(
            name = toolId,
            description = toolDescription,
            extraName = toolName,
            extraType = McpConst.Tool_Type_Custom,
            inputSchema = inputSchema,
            handler = { params ->
                val args = params.entrySet()
                    .mapNotNull { (k, v) ->
                        val key = k?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val value = when {
                            v.isJsonPrimitive -> v.asJsonPrimitive.let { p ->
                                when {
                                    p.isBoolean -> p.asBoolean.toString()
                                    p.isNumber -> p.asNumber.toString()
                                    else -> p.asString
                                }
                            }
                            v.isJsonNull -> ""
                            else -> v.toString()
                        }
                        key to value
                    }
                    .toMap()

                val cmd = CmdCallScript.createCommand(
                    scriptPath = scriptPath,
                    scriptName = toolName,
                    params = args
                )
                val result = cmd.executeSync()
                if (result.success) {
                    ActionResult.success(message = result.message, data = result.data)
                } else {
                    ActionResult.failure(
                        message = result.message ?: GlobalApp.getString(com.hive.i8n.R.string.error_execution_failed)
                    )
                }
            }
        )
    }

    private fun buildInputSchemaForScript(scriptPath: String): JsonObject {
        return runCatching {
            val startParams = findAllStartParams(scriptPath)
            JsonObject().apply {
                addProperty("\$schema", "http://json-schema.org/draft-07/schema#")
                addProperty("type", "object")
                add("properties", JsonObject().apply {
                    startParams.forEach { item ->
                        add(item.label, JsonObject().apply {
                            addProperty("type", "string")
                            addProperty("description", item.hint ?: "")
                        })
                    }
                })
                add("required", JsonArray().apply {
                    startParams.filter { it.required }.forEach { add(JsonPrimitive(it.label)) }
                })
            }
        }.getOrElse {
            JsonObject().apply {
                addProperty("\$schema", "http://json-schema.org/draft-07/schema#")
                addProperty("type", "object")
                add("properties", JsonObject())
                add("required", JsonArray())
            }
        }
    }

    private fun findAllStartParams(scriptPath: String): List<DialogCmdDialogInput.InputItem> {
        val commandRoot = ScriptCommandRoot().apply {
            ScriptCommandRoot.loadScriptSync(scriptPath, this)
        }
        val start = commandRoot.commandQueue.firstOrNull { it is CmdScriptStart } as? CmdScriptStart
            ?: return emptyList()
        return CmdScriptStart.parseInputItems(start)
    }

    private fun normalizeBuildInActionName(actionName: String): String {
        return if (actionName.startsWith(McpConst.Tool_Name_Prefix_BuildIn)) {
            actionName
        } else {
            McpConst.Tool_Name_Prefix_BuildIn + actionName
        }
    }

    fun startRecord() {
        AgentCommandRecorder.startRecord()
    }

    fun endRecord(): List<String> {
        return AgentCommandRecorder.endRecord()
    }

} 
