// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonObject
import com.hive.app.script.R
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.net.data.ScriptCustomMcpTool
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.ResourceDetailIntroEditor
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.ui.base.BaseResourceDetailActivity
import com.hive.ui.common.ResourceQuickRunController
import com.hive.ui.common.ResourceRunStateStore
import com.hive.utils.utils.IntentUtils
import com.hive.utils.file.FileUtils
import com.hive.views.widgets.CommonToast
import com.hive.views.resource.ResourceDetailActionStyle
import com.hive.views.resource.ResourceDetailBadgeVariant
import com.hive.views.resource.ResourceDetailType
import com.hive.views.resource.ResourceDetailIntroSectionBinder
import com.hive.views.resource.ResourceDetailTypeStyleResolver
import com.hive.views.resource.ResourceDetailViewFactory
import com.hive.views.resource.ResourceOverflowAction
import com.hive.views.resource.ResourceOverflowMenuHelper
import java.io.File

class ActivityMcpToolDetail : BaseResourceDetailActivity<ActivityMcpToolDetail.ToolData>() {

    data class ToolData(
        val toolName: String,
        val toolDisplayName: String,
        val toolDescription: String,
        val toolType: String,
        val inputSchema: String,
        val customTool: ScriptCustomMcpTool?
    )

    private var layoutParamsContainer: LinearLayout? = null
    private var introSectionBinder: ResourceDetailIntroSectionBinder? = null

    private val scriptProvider: IScriptProvider? by lazy {
        ComponentManager.getInstance().getProvider(IScriptProvider::class.java) as? IScriptProvider
    }

    private val runStateListener: () -> Unit = {
        updateRunButtonState()
    }

    override fun getLayoutId(): Int = R.layout.activity_mcp_tool_detail

    override fun loadDataFromIntent(): ToolData? {
        val toolName = intent.getStringExtra(EXTRA_TOOL_NAME).orEmpty()
        val toolDisplayName = intent.getStringExtra(EXTRA_TOOL_DISPLAY_NAME).orEmpty()
        val toolDescription = intent.getStringExtra(EXTRA_TOOL_DESCRIPTION).orEmpty()
        val toolType = intent.getStringExtra(EXTRA_TOOL_TYPE).orEmpty()
        val inputSchema = intent.getStringExtra(EXTRA_TOOL_SCHEMA).orEmpty()
        val customTool = intent.getSerializableExtra(EXTRA_CUSTOM_TOOL) as? ScriptCustomMcpTool
        return if (toolName.isNotBlank()) {
            ToolData(toolName, toolDisplayName, toolDescription, toolType, inputSchema, customTool)
        } else null
    }

    override fun bindViews() {
        bindCommonViews()
        ResourceRunStateStore.ensureRegistered()
        ResourceRunStateStore.addListener(runStateListener)
        layoutParamsContainer = findViewById(R.id.layout_params_container)
        introSectionBinder = ResourceDetailIntroSectionBinder.attach(
            findViewById(R.id.layout_section_description),
            findViewById(R.id.btn_edit_intro),
            findViewById(R.id.layout_intro_content),
            findViewById(R.id.layout_intro_add_host)
        )
        introSectionBinder?.setOnEditListener { editDescription() }
        ResourceDetailViewFactory.styleActionButton(btnEdit, ResourceDetailActionStyle.ACCENT)
    }

    override fun bindActions() {
        btnRun?.setOnClickListener { runTool() }
        btnEdit?.setOnClickListener { openInfoEditor() }
        val header = findViewById<com.carlos.ui.header.CommonHeader>(R.id.header)
        header.setRightClickListener { showMoreMenu(header.getRightLayout()) }
    }

    override fun render(data: ToolData) {
        val isCustom = isCustom()
        tvName?.text = data.toolDisplayName.ifBlank { data.toolName }
        tvId?.text = data.toolName
        tvTypeBadge?.text = getString(
            if (isCustom) com.hive.i8n.R.string.mcp_tool_type_custom else com.hive.i8n.R.string.mcp_tool_type_builtin
        )
        ResourceDetailTypeStyleResolver.applyBadge(
            tvTypeBadge,
            ResourceDetailType.TOOL,
            if (isCustom) ResourceDetailBadgeVariant.FILLED else ResourceDetailBadgeVariant.SUBTLE
        )
        tvDescription?.text = data.toolDescription
        introSectionBinder?.bind(data.toolDescription, isCustom())
        updateActionsVisibility()
        updateRunButtonState()
        renderParams(parseSchema())
    }

    private fun parseSchema(): JsonObject {
        return try {
            com.google.gson.JsonParser().parse(currentData?.inputSchema ?: "{}").asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }
    }

    private fun renderParams(schema: JsonObject) {
        val container = layoutParamsContainer ?: return
        container.removeAllViews()
        val properties = schema.getAsJsonObject("properties")
        val required = schema.getAsJsonArray("required")?.let { array ->
            (0 until array.size()).map { index -> array[index].asString }.toSet()
        } ?: emptySet()

        if (properties == null || properties.entrySet().isEmpty()) {
            container.addView(
                createParamCard(
                    getString(com.hive.i8n.R.string.mcp_tool_detail_no_params),
                    getString(com.hive.i8n.R.string.mcp_tool_detail_no_params_desc)
                )
            )
            return
        }

        properties.entrySet().forEach { entry ->
            val obj = entry.value?.asJsonObject
            val desc = obj?.get("description")?.asString.orEmpty()
            val title = if (entry.key in required) {
                getString(com.hive.i8n.R.string.mcp_tool_detail_param_required, entry.key)
            } else {
                entry.key
            }
            container.addView(createParamCard(title, desc.ifBlank {
                getString(com.hive.i8n.R.string.mcp_tool_detail_no_param_desc)
            }))
        }
    }

    private fun createParamCard(title: String, desc: String): View {
        val topMargin = if ((layoutParamsContainer?.childCount ?: 0) > 0) {
            resources.getDimensionPixelSize(R.dimen.design_spacing_2_5)
        } else {
            0
        }
        return ResourceDetailViewFactory.createInfoCard(this, title, desc, topMargin)
    }

    private fun openScriptEditor(scriptPath: String) {
        val scriptDir = File(scriptPath)
        if (!scriptDir.exists() || !scriptDir.isDirectory) return
        val infoModel = ScriptHelper.getScriptInfoModelByPath(scriptPath)
        DialogScriptEdit.create(infoModel.scriptMate)
            ?.setScriptPath(scriptPath)
            ?.setTitleName(scriptDir.name)
            ?.setFromSource(ScriptConst.From.FROM_SCRIPT_UNKNOWN)
            ?.show()
    }

    override fun isCustom(): Boolean = currentData?.toolType != McpConst.Tool_Type_BuildIn

    override fun performDelete() {
        currentData?.toolName?.let { toolName ->
            scriptProvider?.unregisterCustomTool(toolName)
            finish()
        }
    }

    private fun runTool() {
        val data = currentData ?: return
        ResourceQuickRunController.runTool(
            this,
            ResourceQuickRunController.buildToolTarget(
                toolName = data.toolName,
                displayName = data.toolDisplayName,
                schema = parseSchema(),
                customTool = data.customTool
            )
        )
    }

    private fun updateRunButtonState() {
        val data = currentData ?: return
        val running = ResourceRunStateStore.isToolRunning(data.toolName)
        btnRun?.isSelected = running
        btnRun?.setText(
            if (running) com.hive.i8n.R.string.script_state_running
            else com.hive.i8n.R.string.sc_list_item_run
        )
    }

    private fun editDescription() {
        val data = currentData ?: return
        if (!isCustom()) return
        val scriptPath = data.customTool?.scriptPath ?: return
        ResourceDetailIntroEditor.show(this, data.toolDescription) { newDescription ->
            ScriptMcpRegister.registerCustomTool(
                scriptName = data.toolDisplayName.ifBlank { data.toolName },
                scriptDesc = newDescription,
                scriptPath = scriptPath,
                toolId = data.toolName,
                overwriteIfExists = true,
                persistToSp = true
            )
            currentData = data.copy(toolDescription = newDescription)
            render(currentData!!)
        }
    }

    private fun openInfoEditor() {
        currentData?.let { data ->
            ActivityCreateMcpTool.startForEdit(
                this,
                toolId = data.toolName,
                toolName = data.toolDisplayName,
                toolDesc = data.toolDescription,
                scriptPath = data.customTool?.scriptPath ?: ""
            )
        }
    }

    private fun showMoreMenu(anchor: View) {
        val data = currentData ?: return
        if (!isCustom()) return
        moreMenuPopup?.dismiss()
        moreMenuPopup = ResourceOverflowMenuHelper.show(
            anchor = anchor,
            actions = listOf(
                ResourceOverflowAction(getString(com.hive.i8n.R.string.mcp_tool_edit_script)) {
                    data.customTool?.scriptPath?.let { openScriptEditor(it) }
                },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_rename)) {
                    renameTool(
                        data
                    )
                },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_copy)) {
                    copyTool(
                        data
                    )
                },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.delete), danger = true) {
                    showDeleteConfirm(data.toolDisplayName.ifBlank { data.toolName })
                }
            )
        )
    }

    private fun renameTool(data: ToolData) {
        DialogInputMessage(
            this,
            title = getString(com.hive.i8n.R.string.btn_file_rename),
            hint = getString(com.hive.i8n.R.string.sc_dialog_name_hint),
            txtHold = data.toolDisplayName.ifBlank { data.toolName },
            inputType = android.text.InputType.TYPE_CLASS_TEXT,
            checkInputFun = { editText ->
                val value = editText.text.toString().trim()
                if (value.isBlank()) {
                    throw Exception(getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
            }
        ) { _, input ->
            val scriptPath = data.customTool?.scriptPath ?: return@DialogInputMessage
            ScriptMcpRegister.registerCustomTool(
                scriptName = input.trim(),
                scriptDesc = data.toolDescription,
                scriptPath = scriptPath,
                toolId = data.toolName,
                overwriteIfExists = true,
                persistToSp = true
            )
            currentData = data.copy(toolDisplayName = input.trim())
            render(currentData!!)
        }.show()
    }

    private fun copyTool(data: ToolData) {
        val sourcePath = data.customTool?.scriptPath ?: return
        val sourceDir = File(sourcePath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            CommonToast.show(com.hive.i8n.R.string.sc_error_file_not_exist)
            return
        }

        val targetDir = createCopiedWorkflowDir(sourceDir) ?: run {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_fail)
            return
        }

        runCatching {
            val model = ScriptInfoModel().parseMainFile(targetDir)
            val toolId = ensureToolId(targetDir.absolutePath, model)
            ScriptMcpRegister.registerCustomTool(
                scriptName = generateCopyName(data.toolDisplayName.ifBlank { data.toolName }) {
                    File("${ScriptConst.Save_Script_Path}/$it/").exists()
                },
                scriptDesc = data.toolDescription,
                scriptPath = targetDir.absolutePath,
                toolId = toolId,
                overwriteIfExists = false,
                persistToSp = true
            )
        }.onSuccess {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
            finish()
        }.onFailure {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_fail)
        }
    }

    private fun createCopiedWorkflowDir(sourceDir: File): File? {
        val targetDir = File(
            "${ScriptConst.Save_Script_Path}/${
                generateCopyName(sourceDir.name) {
                    File("${ScriptConst.Save_Script_Path}/$it/").exists()
                }
            }/"
        )
        return runCatching {
            FileUtils.makeDirs(targetDir.absolutePath)
            FileUtils.copyFolderTo(sourceDir.absolutePath, targetDir.absolutePath)
            val model = ScriptInfoModel().parseMainFile(targetDir)
            val mate = (model.scriptMate ?: ScriptMate()).apply {
                scriptUid = ScriptMate.generateScriptUid()
            }
            model.scriptMate = mate
            model.saveMate()
            val plainMain = File(targetDir, ScriptConst.SCRIPT_MAIN_FILE_NAME)
            if (plainMain.exists()) {
                val lines = plainMain.readLines().toMutableList()
                if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                    lines[0] = mate.getCommandLines()
                    plainMain.writeText(lines.joinToString("\n"))
                }
            }
            targetDir
        }.getOrNull()
    }

    private fun ensureToolId(path: String, model: ScriptInfoModel): String {
        val mate = model.scriptMate ?: ScriptMate().also { model.scriptMate = it }
        if (mate.scriptUid.isNullOrBlank()) {
            mate.scriptUid = ScriptMate.generateScriptUid()
            model.saveMate()
        }
        return "${ScriptConst.SCRIPT_TOOL_ID_PREFIX}${mate.scriptUid}"
    }

    companion object {
        private const val EXTRA_TOOL_NAME = "tool_name"
        private const val EXTRA_TOOL_DISPLAY_NAME = "tool_display_name"
        private const val EXTRA_TOOL_DESCRIPTION = "tool_description"
        private const val EXTRA_TOOL_TYPE = "tool_type"
        private const val EXTRA_TOOL_SCHEMA = "tool_schema"
        private const val EXTRA_CUSTOM_TOOL = "custom_tool"

        fun start(
            context: Context,
            toolName: String,
            toolDisplayName: String,
            toolDescription: String,
            toolType: String,
            toolSchema: String,
            customTool: ScriptCustomMcpTool? = null
        ) {
            val intent = Intent(context, ActivityMcpToolDetail::class.java).apply {
                putExtra(EXTRA_TOOL_NAME, toolName)
                putExtra(EXTRA_TOOL_DISPLAY_NAME, toolDisplayName)
                putExtra(EXTRA_TOOL_DESCRIPTION, toolDescription)
                putExtra(EXTRA_TOOL_TYPE, toolType)
                putExtra(EXTRA_TOOL_SCHEMA, toolSchema)
                putExtra(EXTRA_CUSTOM_TOOL, customTool)
            }
            IntentUtils.safeStartActivity(context, intent)
        }
    }

    override fun onDestroy() {
        ResourceRunStateStore.removeListener(runStateListener)
        super.onDestroy()
    }
}
