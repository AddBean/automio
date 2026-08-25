// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.skill

import android.content.Context
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import com.hive.ui.widgets.ResourceListItemView
import android.widget.TextView
import com.hive.agent.XAgent
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.ui.creation.ActivityCreationCenter
import com.hive.plugin.agent.model.SkillSpec
import com.hive.ui.common.ResourceQuickRunController
import com.hive.ui.common.ResourceRunStateStore
import com.hive.utils.debug.DLog
import com.hive.views.fragment.PagerFragment
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView

class FragmentSkillList : PagerFragment() {

    private val skills = mutableListOf<SkillSpec>()
    private var recyclerView: ListRecyclerView? = null
    private var emptyLayout: View? = null
    private var emptyMessage: TextView? = null
    private var emptyAction: Button? = null
    private var headerView: SkillHeaderItemView? = null
    private val runStateListener: () -> Unit = {
        recyclerView?.notifyDataSetChanged()
    }

    override fun getLayoutId(): Int = R.layout.fragment_skill_list

    override fun initView() {
        recyclerView = mView?.findViewById(R.id.recycler_view)
        emptyLayout = mView?.findViewById(R.id.layout_empty)
        emptyMessage = mView?.findViewById(R.id.tv_empty_message)
        emptyAction = mView?.findViewById(R.id.btn_empty_action)

        // Set up header view
        ResourceRunStateStore.ensureRegistered()
        ResourceRunStateStore.addListener(runStateListener)
        headerView = SkillHeaderItemView(requireContext()).apply {
            setDescription(getString(i8nR.string.workflow_section_skill_desc))
        }
        recyclerView?.setHeaderView(headerView)
        recyclerView?.setItemViewFactory(SkillFactory())
        recyclerView?.submitDataSetsWithType(skills.map { android.util.Pair(0, it) })

        emptyAction?.setOnClickListener {
            context?.let { ActivityCreationCenter.start(it) }
        }

        loadSkills()
    }

    override fun onResume() {
        super.onResume()
        loadSkills()
    }

    private fun loadSkills() {
        try {
            val list = XAgent.getInstance().listSkills()
            skills.clear()
            skills.addAll(list)
            updateUI()
        } catch (e: Exception) {
            DLog.e("FragmentSkillList", "loadSkills failed: ${e.message}")
            updateUI()
        }
    }

    private fun updateUI() {
        val hasData = skills.isNotEmpty()
        recyclerView?.visibility = if (hasData) View.VISIBLE else View.GONE
        emptyLayout?.visibility = if (hasData) View.GONE else View.VISIBLE
        recyclerView?.submitDataSetsWithType(skills.map { Pair(0, it) })
        recyclerView?.notifyDataSetChanged()
    }

    private fun isCustomSkill(spec: SkillSpec): Boolean {
        return spec.id.startsWith("skill.") && !spec.id.startsWith("skill.inline.")
    }

    private inner class SkillFactory : IListRecyclerViewFactory {
        override fun createItemView(viewType: Int): ListRecyclerItemView {
            return SkillItemView(requireContext())
        }
    }

    private inner class SkillItemView(context: Context) : ListRecyclerItemView(context) {
        private val itemRoot: ResourceListItemView

        init {
            LayoutInflater.from(context).inflate(R.layout.item_skill, this, true)
            itemRoot = findViewById(R.id.item_skill_root)
            itemRoot.configure(
                resourceType = "skill",
                showDescription = true,
                showDeleteButton = false,
                showArrow = false,
                showStatusDot = false
            )
            itemRoot.setItemClickListener {
                val spec = itemData as? SkillSpec
                if (spec != null) {
                    context?.let { ctx -> ActivitySkillDetail.start(ctx, spec) }
                }
            }
            itemRoot.setPlayClickListener {
                val spec = itemData as? SkillSpec
                if (spec != null) {
                    ResourceQuickRunController.runSkill(requireContext(), spec)
                }
            }
        }

        override fun bindData(data: Any?) {
            val spec = data as? SkillSpec ?: return
            val running = ResourceRunStateStore.isSkillRunning(spec.id)

            itemRoot.bindData(
                name = spec.name,
                description = getString(
                    if (isCustomSkill(spec)) i8nR.string.skill_detail_custom
                    else i8nR.string.skill_detail_builtin
                ),
                isRunning = running,
                isCustom = isCustomSkill(spec)
            )
        }
    }

    override fun onDestroyView() {
        ResourceRunStateStore.removeListener(runStateListener)
        super.onDestroyView()
    }
}
