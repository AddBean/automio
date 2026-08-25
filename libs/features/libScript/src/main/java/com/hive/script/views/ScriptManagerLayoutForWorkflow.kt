// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.script.R
import com.hive.script.base.ScriptConst
import com.hive.script.views.cards.ScriptItemView
import com.hive.script.views.cards.ScriptWorkflowItemView
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.utils.IntentUtils
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerView
import java.io.File

/**
 * 主 Tab「工作流」页：对齐 script-desgin [WorkflowPage]，不修改 [ScriptManagerLayoutForFrame] / [script_manager_list_frame]。
 */
class ScriptManagerLayoutForWorkflow(context: Context, attributes: AttributeSet?) :
    ScriptManagerLayout(context, attributes) {

    constructor(context: Context) : this(context, null)

    override fun initView(p0: View?) {
        super.initView(p0)
        post {
            getLayoutTaskAddView()?.setOnClickListener {
                openCreationCenterOrFallback()
            }
            bindEmptyImportAction()
        }
    }

    private fun bindEmptyImportAction() {
        getLayoutStateView()?.findViewById<View>(R.id.btn_empty_import)?.setOnClickListener {
            importScript()
        }
    }

    private fun openCreationCenterOrFallback() {
        if (openCreationCenter()) {
            return
        }
        ScriptManager.createScriptDialog(context) { scriptPath ->
            DialogScriptEdit.create(null)
                ?.setScriptPath(scriptPath)
                ?.setTitleName(File(scriptPath).name)
                ?.setFromSource(ScriptConst.From.FROM_SCRIPT_LIST)
                ?.show()
        }
    }

    private fun openCreationCenter(): Boolean {
        return runCatching {
            val activityClass = Class.forName("com.hive.ui.creation.ActivityCreationCenter")
            IntentUtils.safeStartActivity(context, Intent(context, activityClass))
        }.isSuccess
    }

    override fun getFilterButtonView(): TextView? = null

    override fun getCancelButtonView(): View? = findViewById(R.id.tv_btn_cancel)

    override fun getDeleteButtonView(): View? = findViewById(R.id.tv_btn_delete)

    override fun getSelectButtonView(): TextView? = findViewById(R.id.tv_btn_selected)

    override fun getEditButtonView(): View? = findViewById(R.id.btn_edit)

    override fun getImportButtonView(): View? = findViewById(R.id.btn_import)

    override fun getCloseButtonView(): View? = findViewById(R.id.iv_close)

    override fun getLayoutTaskAddView(): View? = findViewById(R.id.layoutTaskAdd)

    override fun getLayoutStateView(): StatefulLayout? = findViewById(R.id.layout_state)

    override fun getLayoutTitleView(): View? = findViewById(R.id.layout_title)

    override fun getLayoutSelectView(): View? = findViewById(R.id.layout_edit_opt)

    override fun getListRecyclerView(): ListRecyclerView = findViewById(R.id.recycler_view)

    override fun getListItemView(): ScriptItemView = ScriptWorkflowItemView(context)

    override fun getManagerLayout(): Int = R.layout.script_manager_list_workflow_spec
}
