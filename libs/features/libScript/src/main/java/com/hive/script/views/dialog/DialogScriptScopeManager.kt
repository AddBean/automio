// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.scope.GlobalScriptRegistry
import com.hive.script.scope.ScriptScopeRepository
import com.hive.script.scope.ScriptScopeSnapshot
import com.hive.script.scope.ScopedSkillSpec
import com.hive.script.scope.ScopedToolSpec
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import java.io.File

class DialogScriptScopeManager(context: Context?) : BaseScriptDialog(context),
    ListRecyclerItemView.OnItemEventListener {

    private var recyclerView: ListRecyclerView? = null
    private var tvTitle: TextView? = null
    private var tvClose: TextView? = null
    private var snapshot: ScriptScopeSnapshot? = null
    private var scriptDir: File? = null

    override fun initWindow() {
        recyclerView = findViewById(R.id.recycler_view)
        tvTitle = findViewById(R.id.tv_title)
        tvClose = findViewById(R.id.tv_btn_cancel)
        tvClose?.setOnClickListener { dismiss() }
        tvTitle?.text = context?.getString(com.hive.i8n.R.string.sc_scope_manage_title)
        recyclerView?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int): ListRecyclerItemView {
                return object : ListRecyclerItemView(context), View.OnClickListener {
                    private var item: ScopeDisplayItem? = null

                    init {
                        LayoutInflater.from(context).inflate(R.layout.dialog_script_info_item, this)
                        setOnClickListener(this)
                    }

                    override fun bindData(data: Any?) {
                        item = data as? ScopeDisplayItem
                        findViewById<TextView>(R.id.tv_name).text = item?.title.orEmpty()
                        findViewById<TextView>(R.id.tv_info).text = item?.subtitle.orEmpty()
                    }

                    override fun onClick(v: View?) {
                        item?.let { postEvent(it) }
                    }
                }.apply {
                    onItemEventListener = this@DialogScriptScopeManager
                }
            }
        })
        refresh()
    }

    fun loadScript(scriptPath: String?): DialogScriptScopeManager {
        scriptDir = scriptPath?.let { File(it) }
        snapshot = scriptDir?.let { runCatching { ScriptScopeRepository.load(it, validate = false) }.getOrNull() }
        refresh()
        return this
    }

    private fun refresh() {
        val current = snapshot ?: return
        val items = mutableListOf<ScopeDisplayItem>()
        if (current.skills.isEmpty() && current.tools.isEmpty()) {
            items.add(
                ScopeDisplayItem(
                    type = ScopeItemType.INFO,
                    key = "empty",
                    title = context?.getString(com.hive.i8n.R.string.sc_scope_empty).orEmpty(),
                    subtitle = ""
                )
            )
        } else {
            current.skills.sortedBy { it.name }.forEach { skill ->
                items.add(
                    ScopeDisplayItem(
                        type = ScopeItemType.SKILL,
                        key = skill.id,
                        title = context?.getString(com.hive.i8n.R.string.sc_scope_item_skill, skill.name).orEmpty(),
                        subtitle = skill.allowedToolNames.joinToString(", ")
                    )
                )
            }
            current.tools.sortedBy { it.name }.forEach { tool ->
                items.add(
                    ScopeDisplayItem(
                        type = ScopeItemType.TOOL,
                        key = tool.localId,
                        title = context?.getString(com.hive.i8n.R.string.sc_scope_item_tool, tool.name).orEmpty(),
                        subtitle = tool.functionName
                    )
                )
            }
        }
        recyclerView?.submitDataSets(items)
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        val item = eventData as? ScopeDisplayItem ?: return
        val current = snapshot ?: return
        when (item.type) {
            ScopeItemType.SKILL -> {
                current.skills.firstOrNull { it.id == item.key }?.let { skill ->
                    val scopePath = scriptDir?.absolutePath
                    val handler = ScopeEditProvider.onEditSkill
                    if (scopePath != null && handler != null) {
                        handler.invoke(context!!, skill, scopePath) {
                            snapshot = scriptDir?.let { runCatching { ScriptScopeRepository.load(it, validate = false) }.getOrNull() }
                            refresh()
                        }
                    } else {
                        editSkill(skill)
                    }
                }
            }

            ScopeItemType.TOOL -> {
                current.tools.firstOrNull { it.localId == item.key }?.let { openToolWorkflow(it) }
            }

            ScopeItemType.INFO -> Unit
        }
    }

    private fun editSkill(skill: ScopedSkillSpec) {
        val dialog = DialogCmdDialogInput(context)
        dialog.setTitle(context?.getString(com.hive.i8n.R.string.sc_scope_edit_skill_title) ?: "")
        dialog.setInputItems(
            mutableListOf(
                DialogCmdDialogInput.InputItem("id", "id", "id", true, defaultValue = skill.id),
                DialogCmdDialogInput.InputItem("name", "name", "name", true, defaultValue = skill.name),
                DialogCmdDialogInput.InputItem("desc", "description", "description", true, defaultValue = skill.description),
                DialogCmdDialogInput.InputItem("prompt", "prompt", "prompt", true, defaultValue = skill.systemPrompt),
                DialogCmdDialogInput.InputItem(
                    "tools",
                    "allowedToolNames",
                    context?.getString(com.hive.i8n.R.string.sc_scope_skill_tools_hint).orEmpty(),
                    false,
                    defaultValue = skill.allowedToolNames.joinToString(", ")
                ),
                DialogCmdDialogInput.InputItem("maxRounds", "maxRounds", "maxRounds", false, android.text.InputType.TYPE_CLASS_NUMBER, skill.maxRounds?.toString().orEmpty()),
                DialogCmdDialogInput.InputItem("timeoutMs", "timeoutMs", "timeoutMs", false, android.text.InputType.TYPE_CLASS_NUMBER, skill.timeoutMs?.toString().orEmpty())
            )
        )
        dialog.setInputListener(object : DialogCmdDialogInput.OnInputListener {
            override fun onConfirmed(dialog: DialogCmdDialogInput, inputs: List<DialogCmdDialogInput.InputItem>) {
                val updated = skill.copy(
                    name = inputs.valueOf("name"),
                    description = inputs.valueOf("desc"),
                    systemPrompt = inputs.valueOf("prompt"),
                    allowedToolNames = inputs.valueOf("tools").split(",").mapNotNull { it.trim().takeIf(String::isNotBlank) },
                    maxRounds = inputs.valueOf("maxRounds").toIntOrNull(),
                    timeoutMs = inputs.valueOf("timeoutMs").toLongOrNull()
                )
                saveSnapshot { it.copy(skills = it.skills.map { item -> if (item.id == skill.id) updated else item }) }
            }

            override fun onCancel() {
            }
        })
        dialog.show()
    }

    private fun openToolWorkflow(tool: ScopedToolSpec) {
        val ownerDir = scriptDir ?: return
        val toolScriptPath = File(ScriptScopeRepository.getToolsDir(ownerDir), tool.scriptDir).absolutePath
        dismiss()
        DialogScriptEdit.create(null)
            ?.setScriptPath(toolScriptPath)
            ?.setTitleName(tool.name)
            ?.show()
    }

    private fun saveSnapshot(transform: (ScriptScopeSnapshot) -> ScriptScopeSnapshot) {
        val dir = scriptDir ?: return
        val current = snapshot ?: return
        val updated = transform(current)
        ScriptScopeRepository.save(dir, updated)
        runCatching { GlobalScriptRegistry.registerFromWorkflow(dir) }
        snapshot = updated
        refresh()
    }

    override fun getWindowLayoutId(): Int = R.layout.dialog_script_info

    private data class ScopeDisplayItem(
        val type: ScopeItemType,
        val key: String,
        val title: String,
        val subtitle: String
    )

    private enum class ScopeItemType {
        INFO, SKILL, TOOL
    }

    private fun List<DialogCmdDialogInput.InputItem>.valueOf(id: String): String {
        return firstOrNull { it.id == id }?.value?.trim().orEmpty()
    }
}
