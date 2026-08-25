// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.carlos.ui.header.CommonHeader
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.hive.agent.skill.SkillIdGenerator
import com.hive.agent.skill.SkillPersistence
import com.hive.app.script.R
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.scope.GlobalScriptRegistry
import com.hive.script.scope.ScriptScopeRepository
import com.hive.script.scope.ScriptScopeSnapshot
import com.hive.script.scope.ScopedSkillSpec
import com.hive.ui.skill.SkillDraft
import java.io.File

class DialogSkillCreate : DialogFragment() {

    private var initial: SkillSpec? = null
    private var prefill: SkillDraft? = null
    private var onSaved: (() -> Unit)? = null
    private var scopeScriptPath: String? = null

    private var editSkillId: EditText? = null
    private var editSkillName: EditText? = null
    private var editSkillDesc: EditText? = null
    private var editSkillPrompt: EditText? = null
    private var editMaxRounds: EditText? = null
    private var editTimeoutMs: EditText? = null
    private var headerView: CommonHeader? = null
    private var tvToolsSummary: TextView? = null
    private var btnLinkTools: View? = null

    private val selectedAllowedToolNames = mutableListOf<String>()

    private val agentProvider: IAgentProvider? by lazy {
        ComponentManager.getInstance().getProvider(IAgentProvider::class.java) as? IAgentProvider
    }

    private val toolSelectorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val selected = data.getStringArrayListExtra(ActivityToolSelector.EXTRA_SELECTED_TOOLS)
                ?: arrayListOf()
            selectedAllowedToolNames.clear()
            selectedAllowedToolNames.addAll(selected.distinct())
            updateToolsSummary()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_skill_create, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindViews(view)
        applyInitial()
        bindActions()

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.BOTTOM)
        }
    }

    private fun bindViews(root: View) {
        headerView = root.findViewById(R.id.header)
        editSkillId = root.findViewById(R.id.edit_skill_id)
        editSkillName = root.findViewById(R.id.edit_skill_name)
        editSkillDesc = root.findViewById(R.id.edit_skill_desc)
        editSkillPrompt = root.findViewById(R.id.edit_skill_prompt)
        editMaxRounds = root.findViewById(R.id.edit_max_rounds)
        editTimeoutMs = root.findViewById(R.id.edit_timeout_ms)
        btnLinkTools = root.findViewById(R.id.btn_link_tools)
        tvToolsSummary = root.findViewById(R.id.tv_tools_summary)
    }

    private fun applyInitial() {
        val spec = initial
        val isEdit = spec != null

        headerView?.setCenterText(getString(
            if (isEdit) com.hive.i8n.R.string.skill_edit_title else com.hive.i8n.R.string.skill_create_title
        ))
        headerView?.setRightText(getString(com.hive.i8n.R.string.skill_save))

        if (spec != null) {
            editSkillId?.setText(spec.id)
            editSkillName?.setText(spec.name)
            editSkillDesc?.setText(spec.description)
            editSkillPrompt?.setText(spec.systemPrompt)
            editMaxRounds?.setText(spec.maxRounds?.toString() ?: "")
            editTimeoutMs?.setText(timeoutMsToInputSeconds(spec.timeoutMs))
            selectedAllowedToolNames.clear()
            selectedAllowedToolNames.addAll(spec.allowedToolNames)
        } else if (prefill != null) {
            val draft = requireNotNull(prefill)
            editSkillId?.setText(generateDefaultSkillId())
            editSkillName?.setText(draft.name)
            editSkillDesc?.setText(draft.description)
            editSkillPrompt?.setText(draft.systemPrompt)
            editMaxRounds?.setText(draft.maxRounds?.toString() ?: "-1")
            editTimeoutMs?.setText(timeoutMsToInputSeconds(draft.timeoutMs ?: -1L))
            selectedAllowedToolNames.clear()
            selectedAllowedToolNames.addAll(draft.allowedToolNames)
        } else {
            editSkillId?.setText(generateDefaultSkillId())
            editMaxRounds?.setText("-1")
            editTimeoutMs?.setText("-1")
        }

        val canEditId = !isEdit
        editSkillId?.isEnabled = canEditId
        updateToolsSummary()
    }

    private fun bindActions() {
        headerView?.setLeftClickListener { dismissAllowingStateLoss() }
        headerView?.setRightClickListener { save() }
        btnLinkTools?.setOnClickListener { openToolSelector() }
    }

    private fun openToolSelector() {
        val intent = Intent(requireContext(), ActivityToolSelector::class.java).apply {
            putStringArrayListExtra(
                ActivityToolSelector.EXTRA_PRESELECTED_TOOLS,
                ArrayList(selectedAllowedToolNames)
            )
            putExtra(ActivityToolSelector.EXTRA_SCOPE_SCRIPT_PATH, scopeScriptPath)
        }
        toolSelectorLauncher.launch(intent)
    }

    private fun updateToolsSummary() {
        val count = selectedAllowedToolNames.size
        tvToolsSummary?.text = if (count <= 0) {
            getString(com.hive.i8n.R.string.skill_link_tools_empty)
        } else {
            getString(com.hive.i8n.R.string.skill_link_tools_summary, count)
        }
    }

    private fun save() {
        val rawId = editSkillId?.text?.toString()?.trim().orEmpty()
        val id = normalizeSkillId(rawId)
        val name = editSkillName?.text?.toString()?.trim().orEmpty()
        val desc = editSkillDesc?.text?.toString()?.trim().orEmpty()
        val prompt = editSkillPrompt?.text?.toString()?.trim().orEmpty()

        if (id.isBlank() || name.isBlank() || desc.isBlank() || prompt.isBlank()) {
            Toast.makeText(
                requireContext(),
                getString(com.hive.i8n.R.string.skill_required_fields_tip),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val maxRounds = editMaxRounds?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val timeoutMs = parseTimeoutInputToMs()

        val spec = SkillSpec(
            id = id,
            name = name,
            description = desc,
            systemPrompt = prompt,
            allowedToolNames = selectedAllowedToolNames.distinct(),
            maxRounds = maxRounds,
            timeoutMs = timeoutMs
        )

        if (scopeScriptPath.isNullOrBlank()) {
            SkillPersistence.addOrUpdateSkill(spec)
            agentProvider?.registerSkillSpec(spec)
        } else {
            saveScopedSkill(spec)
        }

        onSaved?.invoke()
        dismissAllowingStateLoss()
    }

    private fun timeoutMsToInputSeconds(timeoutMs: Long?): String {
        if (timeoutMs == null) return ""
        if (timeoutMs <= 0L) return timeoutMs.toString()
        return ((timeoutMs + 999L) / 1000L).toString()
    }

    private fun parseTimeoutInputToMs(): Long? {
        val raw = editTimeoutMs?.text?.toString()?.trim().orEmpty()
        if (raw.isBlank()) return null
        val seconds = raw.toLongOrNull() ?: return null
        if (seconds <= 0L) return seconds
        return seconds * 1000L
    }

    private fun normalizeSkillId(id: String): String {
        if (id.isBlank()) return ""
        return SkillIdGenerator.normalizeSkillId(id)
    }

    private fun generateDefaultSkillId(): String {
        val scopedPath = scopeScriptPath
        return if (scopedPath.isNullOrBlank()) {
            SkillIdGenerator.generate()
        } else {
            SkillIdGenerator.generate()
        }
    }

    private fun saveScopedSkill(spec: SkillSpec) {
        val scriptPath = scopeScriptPath ?: return
        val scriptDir = File(scriptPath)
        val scriptUid = resolveScopeScriptUid(scriptPath) ?: return
        val current = loadScopeSnapshot(scriptPath)
            ?: ScriptScopeSnapshot(
                scopeId = ScriptScopeRepository.scopeId(scriptUid),
                scriptUid = scriptUid,
                tools = emptyList(),
                skills = emptyList()
            )
        val updatedSkills = current.skills.toMutableList().apply {
            removeAll { it.id == spec.id }
            add(ScopedSkillSpec.from(spec))
        }
        ScriptScopeRepository.save(scriptDir, current.copy(skills = updatedSkills))
        runCatching { GlobalScriptRegistry.registerFromWorkflow(scriptDir) }
    }

    private fun loadScopeSnapshot(path: String): ScriptScopeSnapshot? {
        return runCatching {
            ScriptScopeRepository.load(File(path), validate = false)
        }.getOrNull()
    }

    private fun resolveScopeScriptUid(path: String): String? {
        val dir = File(path)
        return loadScopeSnapshot(path)?.scriptUid
            ?: com.hive.script.views.beans.ScriptInfoModel().parseInfoFile(dir).scriptMate?.scriptUid
            ?: com.hive.script.views.beans.ScriptInfoModel().parseMainFile(dir).scriptMate?.scriptUid
    }

    companion object {
        fun show(
            fragment: Fragment,
            initial: SkillSpec?,
            onSaved: () -> Unit,
            scopeScriptPath: String? = null,
            prefill: SkillDraft? = null
        ) {
            show(fragment.parentFragmentManager, initial, onSaved, scopeScriptPath, prefill)
        }

        fun show(
            fragmentManager: FragmentManager,
            initial: SkillSpec?,
            onSaved: () -> Unit,
            scopeScriptPath: String? = null,
            prefill: SkillDraft? = null
        ) {
            val dialog = DialogSkillCreate()
            dialog.initial = initial
            dialog.prefill = prefill
            dialog.onSaved = onSaved
            dialog.scopeScriptPath = scopeScriptPath
            dialog.show(fragmentManager, "DialogSkillCreate")
        }
    }
}
