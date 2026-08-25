// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.carlos.ui.header.CommonHeader
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.utils.GlobalApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivitySkillAiCreate : BaseFragmentActivity() {

    private var header: CommonHeader? = null
    private var requirementInput: EditText? = null
    private var generateButton: View? = null
    private var statusView: View? = null
    private var overlay: View? = null
    private var scopeScriptPath: String? = null
    private var isGenerating = false

    override fun getLayoutId(): Int = R.layout.activity_skill_ai_create

    override fun doOnCreate(savedState: Bundle?) {
        header = findViewById(R.id.header)
        requirementInput = findViewById(R.id.edit_requirement)
        generateButton = findViewById(R.id.btn_generate)
        statusView = findViewById(R.id.tv_status)
        overlay = findViewById(R.id.layout_loading_overlay)

        scopeScriptPath = intent.getStringExtra(EXTRA_SCOPE_SCRIPT_PATH)

        header?.setLeftClickListener { onBackPressed() }
        generateButton?.setOnClickListener { submitDraft() }
        bindExampleSuggestions()
    }

    private fun submitDraft() {
        val requirement = requirementInput?.text?.toString()?.trim().orEmpty()
        if (requirement.isBlank()) {
            android.widget.Toast.makeText(
                this,
                getString(com.hive.i8n.R.string.skill_ai_create_requirement_hint),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isGenerating) return
        renderLoading(true, getString(com.hive.i8n.R.string.skill_ai_create_generating))

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SkillAiDraftGenerator.generate(requirement, scopeScriptPath)
            }
            renderLoading(false, result.errorMessage)
            result.draft?.let { draft ->
                setResult(Activity.RESULT_OK, draft.toIntent())
                finish()
                return@launch
            }
            android.widget.Toast.makeText(
                this@ActivitySkillAiCreate,
                result.errorMessage ?: getString(com.hive.i8n.R.string.skill_ai_create_failed),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderLoading(loading: Boolean, status: String?) {
        isGenerating = loading
        overlay?.visibility = if (loading) View.VISIBLE else View.GONE
        statusView?.visibility = if (status.isNullOrBlank()) View.GONE else View.VISIBLE
        (statusView as? android.widget.TextView)?.text = status.orEmpty()
        generateButton?.isEnabled = !loading
        requirementInput?.isEnabled = !loading
    }

    private fun bindExampleSuggestions() {
        val editText = requirementInput ?: return
        val examples = listOf(
            R.id.example_1,
            R.id.example_2,
            R.id.example_3,
            R.id.example_4
        ).mapNotNull { findViewById<View>(it) as? android.widget.TextView }
        examples.forEach { example ->
            example.setOnClickListener {
                val text = example.text?.toString().orEmpty()
                editText.setText(text)
                editText.setSelection(text.length)
            }
        }
    }

    companion object {
        private const val EXTRA_SCOPE_SCRIPT_PATH = "extra_scope_script_path"

        fun createIntent(context: Context, scopeScriptPath: String? = null): Intent {
            return Intent(context, ActivitySkillAiCreate::class.java).apply {
                putExtra(EXTRA_SCOPE_SCRIPT_PATH, scopeScriptPath)
            }
        }
    }
}
