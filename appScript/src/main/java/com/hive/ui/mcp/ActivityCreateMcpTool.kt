// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.app.script.R
import com.hive.base.BaseActivity
import com.hive.extension.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.mcp.ScriptMcpRegister
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.dialog.DialogScriptListSelector
import com.hive.utils.debug.DLog
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 创建 AI 工具页面
 * 对齐设计图 CreateViewOverlay.tsx 的 tool-form 模式
 *
 * @author jiadou
 */
class ActivityCreateMcpTool : BaseActivity() {

    private var ivBack: ImageView? = null
    private var tvTitle: TextView? = null
    private var layoutTip: View? = null
    private var etToolName: EditText? = null
    private var etToolDesc: EditText? = null
    private var layoutWorkflowSelector: View? = null
    private var tvWorkflowName: TextView? = null
    private var tvWorkflowEmpty: TextView? = null
    private var btnCreate: TextView? = null

    private var selectedWorkflowPath: String? = null
    private var selectedWorkflowModel: ScriptInfoModel? = null
    private var workflows: List<ScriptInfoModel> = emptyList()

    // 编辑模式参数
    private var isEditMode = false
    private var editToolId: String? = null

    private val mcpProvider: IMcpProvider? by lazy {
        ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider
    }

    private val agentProvider: IAgentProvider? by lazy {
        ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
    }

    private val scriptProvider: IScriptProvider? by lazy {
        ComponentManager.getInstance().getProvider(IScriptProvider::class.java) as? IScriptProvider
    }

    override fun getLayoutId(): Int = R.layout.activity_create_mcp_tool

    override fun doOnCreate() {
        ivBack = findViewById(R.id.iv_back)
        tvTitle = findViewById(R.id.tv_title)
        layoutTip = findViewById(R.id.layout_tip)
        etToolName = findViewById(R.id.et_tool_name)
        etToolDesc = findViewById(R.id.et_tool_desc)
        layoutWorkflowSelector = findViewById(R.id.layout_workflow_selector)
        tvWorkflowName = findViewById(R.id.tv_workflow_name)
        tvWorkflowEmpty = findViewById(R.id.tv_workflow_empty)
        btnCreate = findViewById(R.id.btn_create)

        // 判断是否为编辑模式
        isEditMode = intent.hasExtra(EXTRA_EDIT_MODE)
        editToolId = intent.getStringExtra(EXTRA_EDIT_TOOL_ID)
        val editToolName = intent.getStringExtra(EXTRA_EDIT_TOOL_NAME)
        val editToolDesc = intent.getStringExtra(EXTRA_EDIT_TOOL_DESC)
        val editScriptPath = intent.getStringExtra(EXTRA_EDIT_SCRIPT_PATH)

        if (isEditMode) {
            tvTitle?.text = getString(com.hive.i8n.R.string.edit_mcp_tool_title)
            btnCreate?.text = getString(com.hive.i8n.R.string.skill_save)
            // 填充已有数据
            etToolName?.setText(editToolName ?: "")
            etToolDesc?.setText(editToolDesc ?: "")
            selectedWorkflowPath = editScriptPath
        }

        setupTipCard()
        loadWorkflows()
        setupEvents()
        updateButtonState()
    }

    private fun setupEvents() {
        ivBack?.setOnClickListener { finish() }

        layoutWorkflowSelector?.setOnClickListener {
            showWorkflowSelector()
        }

        btnCreate?.setOnClickListener {
            createTool()
        }

        etToolName?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonState()
            }
        })

        etToolDesc?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonState()
            }
        })
    }

    private fun setupTipCard() {
        layoutTip?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 24f, 24f, 24f, 24f)
            setColor(ContextCompat.getColor(this@ActivityCreateMcpTool, com.hive.i8n.R.color.design_publish_sky_fill_15))
            setStroke(1, ContextCompat.getColor(this@ActivityCreateMcpTool, com.hive.i8n.R.color.design_publish_sky_border_35))
        }
    }

    private fun loadWorkflows() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allScripts = ScriptHelper.listAllScripts(true) ?: emptyList()
                workflows = allScripts

                withContext(Dispatchers.Main) {
                    updateWorkflowUI()
                }
            } catch (e: Exception) {
                DLog.e("ActivityCreateMcpTool", "加载工作流失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    workflows = emptyList()
                    updateWorkflowUI()
                }
            }
        }
    }

    private fun updateWorkflowUI() {
        tvWorkflowEmpty?.visibleOrGone(workflows.isEmpty())
        tvWorkflowName?.visibleOrGone(workflows.isNotEmpty())

        if (selectedWorkflowModel != null) {
            tvWorkflowName?.text = selectedWorkflowModel?.scriptName ?: ""
        } else if (selectedWorkflowPath != null && isEditMode) {
            // 编辑模式：从路径加载工作流名称
            val workflowModel = workflows.find { it.scriptPath == selectedWorkflowPath }
            if (workflowModel != null) {
                selectedWorkflowModel = workflowModel
                tvWorkflowName?.text = workflowModel.scriptName ?: ""
            } else {
                tvWorkflowName?.text = selectedWorkflowPath?.substringAfterLast("/") ?: ""
            }
        } else {
            tvWorkflowName?.text = resources.getString(com.hive.i8n.R.string.create_mcp_tool_select_workflow_placeholder)
        }
    }

    private fun showWorkflowSelector() {
        if (workflows.isEmpty()) {
            CommonToast.getInstance().showToast(resources.getString(com.hive.i8n.R.string.create_mcp_tool_no_workflow))
            return
        }

        DialogScriptListSelector(this, true)
            .setTitle(resources.getString(com.hive.i8n.R.string.create_mcp_tool_select_workflow))
            .setOnScriptSelectListener(object : DialogScriptListSelector.OnScriptSelectListener {
                override fun onSelected(dialog: DialogScriptListSelector, model: ScriptInfoModel) {
                    dialog.dismiss()
                    selectedWorkflowPath = model.scriptPath
                    selectedWorkflowModel = model
                    tvWorkflowName?.text = model.scriptName ?: ""
                    updateButtonState()
                }

                override fun onDismissed() {}
            })
            .show()
    }

    private fun updateButtonState() {
        val nameValid = etToolName?.text?.toString()?.trim()?.isNotEmpty() == true
        val descValid = etToolDesc?.text?.toString()?.trim()?.isNotEmpty() == true
        val workflowValid = selectedWorkflowPath != null

        btnCreate?.isEnabled = nameValid && descValid && workflowValid
        btnCreate?.alpha = if (nameValid && descValid && workflowValid) 1f else 0.5f
    }

    private fun createTool() {
        val toolName = etToolName?.text?.toString()?.trim() ?: return
        val toolDesc = etToolDesc?.text?.toString()?.trim() ?: return
        val scriptPath = selectedWorkflowPath ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val model = ScriptHelper.getScriptMainModelByPath(scriptPath)
                val toolId = ensureToolIdAndPersistUidIfMissing(scriptPath, model)

                val alreadyExists = mcpProvider?.getRegisteredTools()?.any { it.name == toolId } == true

                withContext(Dispatchers.Main) {
                    if (alreadyExists) {
                        showOverwriteDialog(toolId, toolName, toolDesc, scriptPath)
                    } else {
                        doRegister(toolId, toolName, toolDesc, scriptPath)
                    }
                }
            } catch (e: Exception) {
                DLog.e("ActivityCreateMcpTool", "创建工具失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    CommonToast.getInstance().showToast(resources.getString(com.hive.i8n.R.string.create_mcp_tool_failed))
                }
            }
        }
    }

    private fun showOverwriteDialog(toolId: String, toolName: String, toolDesc: String, scriptPath: String) {
        DialogScriptAlert(this)
            .setTitle(com.hive.i8n.R.string.sc_upload_same_name_title)
            .setContent(com.hive.i8n.R.string.sc_upload_same_name_content)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        doRegister(toolId, toolName, toolDesc, scriptPath, overwrite = true)
                    }
                }
            })
            .show()
    }

    private fun doRegister(
        toolId: String,
        toolName: String,
        toolDesc: String,
        scriptPath: String,
        overwrite: Boolean = false
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 编辑模式：先注销旧工具
                if (isEditMode && editToolId != null && editToolId != toolId) {
                    scriptProvider?.unregisterCustomTool(editToolId!!)
                }

                ScriptMcpRegister.registerCustomTool(
                    scriptName = toolName,
                    scriptDesc = toolDesc,
                    scriptPath = scriptPath,
                    toolId = toolId,
                    overwriteIfExists = overwrite || isEditMode,
                    persistToSp = true
                )

                agentProvider?.refreshAllMcpServer {
                    EventBus.getDefault().post(RefreshScriptListEvent())
                    CoroutineScope(Dispatchers.Main).launch {
                        val message = if (isEditMode) {
                            resources.getString(com.hive.i8n.R.string.edit_mcp_tool_success)
                        } else {
                            resources.getString(com.hive.i8n.R.string.create_mcp_tool_success)
                        }
                        CommonToast.getInstance().showToast(message)
                        finish()
                    }
                }
            } catch (e: Exception) {
                DLog.e("ActivityCreateMcpTool", "注册工具失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    CommonToast.getInstance().showToast(resources.getString(com.hive.i8n.R.string.create_mcp_tool_failed))
                }
            }
        }
    }

    private fun ensureToolIdAndPersistUidIfMissing(path: String, model: ScriptInfoModel): String {
        val mate = model.scriptMate ?: ScriptMate().also { model.scriptMate = it }
        var uid = mate.scriptUid

        if (uid.isNullOrBlank()) {
            uid = ScriptMate.generateScriptUid()
            mate.scriptUid = uid
            model.scriptMate = mate
            model.saveMate()

            val plainMainFile = java.io.File(path, ScriptConst.SCRIPT_MAIN_FILE_NAME)
            if (plainMainFile.exists()) {
                runCatching {
                    val lines = plainMainFile.readLines().toMutableList()
                    if (lines.isNotEmpty() && lines[0].startsWith("mate")) {
                        lines[0] = mate.getCommandLines()
                        plainMainFile.writeText(lines.joinToString("\n"))
                    }
                }
            }
        }

        return "${ScriptConst.SCRIPT_TOOL_ID_PREFIX}$uid"
    }

    companion object {
        private const val EXTRA_EDIT_MODE = "edit_mode"
        private const val EXTRA_EDIT_TOOL_ID = "edit_tool_id"
        private const val EXTRA_EDIT_TOOL_NAME = "edit_tool_name"
        private const val EXTRA_EDIT_TOOL_DESC = "edit_tool_desc"
        private const val EXTRA_EDIT_SCRIPT_PATH = "edit_script_path"

        fun start(context: Context) {
            val intent = Intent(context, ActivityCreateMcpTool::class.java)
            IntentUtils.safeStartActivity(context, intent)
        }

        fun startForEdit(
            context: Context,
            toolId: String,
            toolName: String,
            toolDesc: String,
            scriptPath: String
        ) {
            val intent = Intent(context, ActivityCreateMcpTool::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
                putExtra(EXTRA_EDIT_TOOL_ID, toolId)
                putExtra(EXTRA_EDIT_TOOL_NAME, toolName)
                putExtra(EXTRA_EDIT_TOOL_DESC, toolDesc)
                putExtra(EXTRA_EDIT_SCRIPT_PATH, scriptPath)
            }
            IntentUtils.safeStartActivity(context, intent)
        }
    }
}