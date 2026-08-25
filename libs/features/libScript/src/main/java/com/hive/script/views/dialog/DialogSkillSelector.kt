// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.hive.plugin.ComponentManager
import com.hive.plugin.agent.model.SkillSpec
import com.hive.plugin.provider.IAgentProvider
import com.hive.script.scope.ScriptVisibilityRegistry
import com.hive.script.R
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.script.views.cards.SkillItemSelectorView

/**
 * 技能选择弹框，通过 IAgentProvider.listSkills() 获取列表并展示。
 */
class DialogSkillSelector(context: Context) : BaseScriptDialog(context),
    ListRecyclerItemView.OnItemEventListener {

    private var cancelBySubmit = false
    private var title: String? = null
    private var onSkillSelectListener: OnSkillSelectListener? = null
    private var scriptPath: String? = null
    private var includeGlobalSkills: Boolean = true

    private var ivClose: View? = null
    private var layoutState: StatefulLayout? = null
    private var recyclerView: ListRecyclerView? = null
    private var tvTitle: TextView? = null

    override fun initWindow() {
        ivClose = findViewById(R.id.iv_close)
        layoutState = findViewById(R.id.layout_state)
        recyclerView = findViewById(R.id.recycler_view)
        tvTitle = findViewById(R.id.tvTitle)
        recyclerView?.setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int): ListRecyclerItemView =
                SkillItemSelectorView(context!!).apply {
                    onItemEventListener = this@DialogSkillSelector
                }
        })

        ivClose?.setOnClickListener { dismiss() }
        layoutState?.setOnClickListener { dismiss() }
        title?.let { tvTitle?.text = it }
        post { updateSkillList() }
    }

    fun setTitle(title: String): DialogSkillSelector {
        this.title = title
        tvTitle?.text = title
        return this
    }

    private fun updateSkillList() {
        val provider = ComponentManager.getInstance()
            .getProvider(IAgentProvider::class.java) as? IAgentProvider
        val allSkills = provider?.listSkills().orEmpty()
        val publicIds = ScriptVisibilityRegistry.getPublicSkillIds()
        val list = allSkills.filter { it.id in publicIds }
        recyclerView?.submitDataSets(list.toMutableList())
        if (list.isEmpty()) {
            layoutState?.showEmpty()
        } else {
            layoutState?.showContent()
        }
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        (eventData as? SkillSpec)?.let { spec ->
            cancelBySubmit = true
            onSkillSelectListener?.onSelected(this@DialogSkillSelector, spec)
        }
    }

    override fun getWindowLayoutId(): Int = R.layout.view_skill_list_selector

    override fun getMarginParams(): Array<Int> =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 160 * DP, 0, 0)

    fun setOnSkillSelectListener(listener: OnSkillSelectListener): DialogSkillSelector {
        onSkillSelectListener = listener
        return this
    }

    fun setScopeScriptPath(scriptPath: String?, includeGlobal: Boolean = true): DialogSkillSelector {
        this.scriptPath = scriptPath
        this.includeGlobalSkills = includeGlobal
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        if (!cancelBySubmit) {
            onSkillSelectListener?.onDismissed()
        }
        cancelBySubmit = false
    }

    interface OnSkillSelectListener {
        fun onSelected(dialog: DialogSkillSelector, spec: SkillSpec)
        fun onDismissed()
    }
}
