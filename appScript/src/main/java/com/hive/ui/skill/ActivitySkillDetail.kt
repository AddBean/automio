// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.hive.agent.XAgent
import com.hive.agent.skill.SkillIdGenerator
import com.hive.agent.skill.SkillPersistence
import com.hive.app.script.R
import com.hive.markdown.MarkdownTextView
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IAgentProvider
import com.hive.plugin.provider.IMcpProvider
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.ResourceDetailIntroEditor
import com.hive.ui.base.BaseResourceDetailActivity
import com.hive.ui.common.ResourceQuickRunController
import com.hive.ui.common.ResourceRunStateStore
import com.hive.utils.utils.IntentUtils
import com.hive.views.resource.ResourceDetailActionStyle
import com.hive.views.resource.ResourceDetailBadgeVariant
import com.hive.views.resource.ResourceDetailType
import com.hive.views.resource.ResourceDetailIntroSectionBinder
import com.hive.views.resource.ResourceDetailTypeStyleResolver
import com.hive.views.resource.ResourceDetailViewFactory
import com.hive.views.resource.ResourceOverflowAction
import com.hive.views.resource.ResourceOverflowMenuHelper
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.FlowLayout

class ActivitySkillDetail : BaseResourceDetailActivity<SkillSpec>() {

    private var tvPrompt: MarkdownTextView? = null
    private var tvMeta: TextView? = null
    private var layoutTools: FlowLayout? = null
    private var introSectionBinder: ResourceDetailIntroSectionBinder? = null

    private val agentProvider: IAgentProvider? by lazy {
        ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
    }

    private val mcpProvider: IMcpProvider? by lazy {
        ComponentManager.getInstance().getProvider(IMcpProvider::class.java) as? IMcpProvider
    }

    private val runStateListener: () -> Unit = {
        updateRunButtonState()
    }

    override fun getLayoutId(): Int = R.layout.activity_skill_detail

    override fun loadDataFromIntent(): SkillSpec? {
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        if (id.isBlank() || name.isBlank()) return null
        return SkillSpec(
            id = id,
            name = name,
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            systemPrompt = intent.getStringExtra(EXTRA_PROMPT).orEmpty(),
            allowedToolNames = intent.getStringArrayListExtra(EXTRA_TOOLS) ?: emptyList(),
            maxRounds = if (intent.hasExtra(EXTRA_MAX_ROUNDS)) intent.getIntExtra(
                EXTRA_MAX_ROUNDS,
                -1
            ) else null,
            timeoutMs = if (intent.hasExtra(EXTRA_TIMEOUT_MS)) intent.getLongExtra(
                EXTRA_TIMEOUT_MS,
                -1L
            ) else null,
            version = intent.getStringExtra(EXTRA_VERSION),
            sourceScriptNames = (intent.getStringArrayListExtra(EXTRA_SOURCE_NAMES)
                ?: arrayListOf())
                .associateBy({ it }, { it })
        )
    }

    override fun bindViews() {
        bindCommonViews()
        ResourceRunStateStore.ensureRegistered()
        ResourceRunStateStore.addListener(runStateListener)
        tvPrompt = findViewById(R.id.tv_prompt)
        tvMeta = findViewById(R.id.tv_meta)
        layoutTools = findViewById(R.id.layout_tools)
        introSectionBinder = ResourceDetailIntroSectionBinder.attach(
            findViewById(R.id.layout_section_description),
            findViewById(R.id.btn_edit_intro),
            findViewById(R.id.layout_intro_content),
            findViewById(R.id.layout_intro_add_host)
        )
        introSectionBinder?.setOnEditListener { editDescription() }
        ResourceDetailViewFactory.styleActionButton(btnEdit, ResourceDetailActionStyle.ACCENT)
        // 更多按钮已移至 CommonHeader 右上角，通过 setRightClickListener 绑定
    }

    override fun onResume() {
        super.onResume()
        currentData?.id?.let { skillId ->
            XAgent.getInstance().listSkills().firstOrNull { it.id == skillId }?.let {
                currentData = it
                render(it)
            }
        }
    }

    override fun bindActions() {
        btnRun?.setOnClickListener { runSkill() }
        btnEdit?.setOnClickListener { openSkillEditor() }
        val header = findViewById<com.carlos.ui.header.CommonHeader>(R.id.header)
        header.setRightClickListener { showMoreMenu(header.getRightLayout()) }
    }

    override fun render(skill: SkillSpec) {
        val custom = isCustom()
        tvName?.text = skill.name
        tvId?.text = skill.id
        tvTypeBadge?.text = getString(
            if (custom) com.hive.i8n.R.string.skill_detail_custom else com.hive.i8n.R.string.skill_detail_builtin
        )
        ResourceDetailTypeStyleResolver.applyBadge(
            tvTypeBadge,
            ResourceDetailType.SKILL,
            if (custom) ResourceDetailBadgeVariant.FILLED else ResourceDetailBadgeVariant.SUBTLE
        )
        tvDescription?.text = skill.description
        introSectionBinder?.bind(skill.description, isCustom())
        renderTools(skill.allowedToolNames)
        tvPrompt?.loadMarkdown(skill.systemPrompt)
        tvMeta?.text = buildMetaText(skill)
        updateActionsVisibility()
        updateRunButtonState()
    }

    private fun renderTools(toolIds: List<String>) {
        val container = layoutTools ?: return
        container.removeAllViews()

        if (toolIds.isEmpty()) {
            container.addView(createEmptyToolTag())
            return
        }

        val registeredTools = mcpProvider?.getRegisteredTools()?.toList() ?: emptyList()
        val toolMap = registeredTools.associateBy { it.name }

        toolIds.forEach { toolId ->
            val tool = toolMap[toolId]
            val displayName = tool?.extraName?.takeIf { it.isNotBlank() } ?: toolId
            container.addView(createToolTag(displayName))
        }
    }

    private fun createToolTag(text: String): TextView {
        return ResourceDetailViewFactory.createChip(this, text)
    }

    private fun createEmptyToolTag(): TextView {
        return ResourceDetailViewFactory.createEmptyText(
            this,
            getString(com.hive.i8n.R.string.skill_link_tools_empty)
        )
    }

    private fun buildMetaText(skill: SkillSpec): String {
        val lines = mutableListOf<String>()
        val sourceNames = skill.sourceScriptNames?.values?.filter { it.isNotBlank() }.orEmpty()
        if (sourceNames.isNotEmpty()) {
            lines.add(
                getString(
                    com.hive.i8n.R.string.skill_detail_meta_sources,
                    sourceNames.joinToString(", ")
                )
            )
        }
        skill.version?.takeIf { it.isNotBlank() }?.let {
            lines.add(getString(com.hive.i8n.R.string.skill_detail_meta_version, it))
        }
        return lines.joinToString("\n").ifEmpty {
            getString(com.hive.i8n.R.string.skill_detail_no_description)
        }
    }

    private fun reloadSkill(skillId: String) {
        val updated = XAgent.getInstance().listSkills().firstOrNull { it.id == skillId }
        if (updated == null) {
            finish()
            return
        }
        currentData = updated
        render(updated)
    }

    override fun isCustom(): Boolean {
        return currentData?.let { skill ->
            skill.id.startsWith("skill.") && !skill.id.startsWith("skill.inline.")
        } ?: false
    }

    override fun performDelete() {
        currentData?.let { skill ->
            runCatching {
                SkillPersistence.removeSkill(skill.id)
                agentProvider?.unregisterSkillSpec(skill.id)
            }.onSuccess {
                finish()
            }
        }
    }

    private fun runSkill() {
        val skill = currentData ?: return
        ResourceQuickRunController.runSkill(this, skill)
    }

    private fun updateRunButtonState() {
        val skill = currentData ?: return
        val running = ResourceRunStateStore.isSkillRunning(skill.id)
        btnRun?.isSelected = running
        btnRun?.setText(
            if (running) com.hive.i8n.R.string.script_state_running
            else com.hive.i8n.R.string.sc_list_item_run
        )
    }

    private fun editDescription() {
        val skill = currentData ?: return
        if (!isCustom()) return
        ResourceDetailIntroEditor.show(this, skill.description) { newDescription ->
            val updated = skill.copy(description = newDescription)
            SkillPersistence.addOrUpdateSkill(updated)
            agentProvider?.registerSkillSpec(updated)
            reloadSkill(updated.id)
        }
    }

    private fun openSkillEditor() {
        currentData?.let { skill ->
            DialogSkillCreate.show(
                fragmentManager = supportFragmentManager,
                initial = skill,
                onSaved = { reloadSkill(skill.id) }
            )
        }
    }

    private fun showMoreMenu(anchor: View) {
        moreMenuPopup?.dismiss()
        val skill = currentData ?: return
        moreMenuPopup = ResourceOverflowMenuHelper.show(
            anchor = anchor,
            actions = listOf(
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_rename)) { renameSkill(skill) },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_copy)) { copySkill(skill) },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.delete), danger = true) {
                    showDeleteConfirm(skill.name)
                }
            )
        )
    }

    private fun renameSkill(skill: SkillSpec) {
        DialogInputMessage(
            this,
            title = getString(com.hive.i8n.R.string.btn_file_rename),
            hint = getString(com.hive.i8n.R.string.sc_dialog_name_hint),
            txtHold = skill.name,
            inputType = android.text.InputType.TYPE_CLASS_TEXT,
            checkInputFun = { editText ->
                val name = editText.text.toString().trim()
                if (name.isBlank()) {
                    throw Exception(getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
            }
        ) { _, input ->
            val newName = input.trim()
            val updated = skill.copy(name = newName)
            SkillPersistence.addOrUpdateSkill(updated)
            agentProvider?.registerSkillSpec(updated)
            reloadSkill(updated.id)
        }.show()
    }

    private fun copySkill(skill: SkillSpec) {
        val existingNames = XAgent.getInstance().listSkills().map { it.name }.toSet()
        val copied = skill.copy(
            id = SkillIdGenerator.generate(),
            name = generateCopyName(skill.name) { it in existingNames }
        )
        runCatching {
            SkillPersistence.addOrUpdateSkill(copied)
            agentProvider?.registerSkillSpec(copied)
        }.onSuccess {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
            finish()
        }.onFailure {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_fail)
        }
    }

    companion object {
        private const val EXTRA_ID = "skill_id"
        private const val EXTRA_NAME = "skill_name"
        private const val EXTRA_DESCRIPTION = "skill_description"
        private const val EXTRA_PROMPT = "skill_prompt"
        private const val EXTRA_TOOLS = "skill_tools"
        private const val EXTRA_MAX_ROUNDS = "skill_max_rounds"
        private const val EXTRA_TIMEOUT_MS = "skill_timeout_ms"
        private const val EXTRA_SOURCE_NAMES = "skill_source_names"
        private const val EXTRA_VERSION = "skill_version"

        fun start(context: Context, skill: SkillSpec) {
            val intent = Intent(context, ActivitySkillDetail::class.java).apply {
                putExtra(EXTRA_ID, skill.id)
                putExtra(EXTRA_NAME, skill.name)
                putExtra(EXTRA_DESCRIPTION, skill.description)
                putExtra(EXTRA_PROMPT, skill.systemPrompt)
                putStringArrayListExtra(EXTRA_TOOLS, ArrayList(skill.allowedToolNames))
                skill.maxRounds?.let { putExtra(EXTRA_MAX_ROUNDS, it) }
                skill.timeoutMs?.let { putExtra(EXTRA_TIMEOUT_MS, it) }
                putStringArrayListExtra(
                    EXTRA_SOURCE_NAMES,
                    ArrayList(skill.sourceScriptNames?.values?.filter { it.isNotBlank() }
                        ?: emptyList())
                )
                skill.version?.let { putExtra(EXTRA_VERSION, it) }
            }
            IntentUtils.safeStartActivity(context, intent)
        }
    }

    override fun onDestroy() {
        ResourceRunStateStore.removeListener(runStateListener)
        super.onDestroy()
    }
}
