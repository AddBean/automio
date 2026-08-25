// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.session

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hive.agent.R
import com.hive.agent.storage.AgentSessionStorage
import com.hive.agent.storage.SessionMeta
import com.hive.views.SampleDialog
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.ListRecyclerView
import com.hive.script.extensions.submitDataSetsWithType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 会话列表 BottomSheet：展示历史会话，支持「新建对话」与点击切换会话。
 * 选中会话或新建时通过 onSessionSelected 回调通知 Fragment。
 */
class AgentSessionListBottomSheet : BottomSheetDialogFragment() {

    var onSessionSelected: ((sessionKey: String?) -> Unit)? = null
    /** 当在历史列表中删除某条会话时回调，便于外部在删除的是当前会话时清空界面 */
    var onSessionDeleted: ((sessionKey: String) -> Unit)? = null
    /** 当用户点击「转为工作流」时回调，参数为 sessionKey，由外部加载 session 并调用 ScriptProvider 保存 */
    var onConvertToWorkflow: ((sessionKey: String) -> Unit)? = null
    /** 为 true 时禁止切换会话（如 agent 正在执行），仅提示用户 */
    var isAgentRunning: Boolean = false
    /** 当前会话 key（用于列表高亮） */
    var currentSessionKey: String? = null

    private var storage: AgentSessionStorage? = null
    private var listRecyclerView: ListRecyclerView? = null
    private var layoutEmpty: View? = null
    private var tvEmptyTitle: TextView? = null
    private var tvEmptyDesc: TextView? = null
    private var tvSubtitle: TextView? = null
    private var layoutRunningHint: View? = null
    private var tvRunningHint: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_agent_sessions, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = context?.applicationContext?.let { AgentSessionStorage(it) }
        val ctx = context ?: return

        view.findViewById<TextView>(R.id.tvSessionListTitle)?.text = getString(com.hive.i8n.R.string.agent_session_history)
        tvSubtitle = view.findViewById(R.id.tvSessionListSubtitle)
        tvSubtitle?.text = ""

        view.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnNewSession)?.apply {
            text = getString(com.hive.i8n.R.string.agent_session_new)
            setOnClickListener {
                onSessionSelected?.invoke(null)
                dismiss()
            }
        }

        listRecyclerView = view.findViewById(R.id.listSessions)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc)
        tvEmptyTitle?.text = getString(com.hive.i8n.R.string.agent_session_empty_list)
        tvEmptyDesc?.text = getString(com.hive.i8n.R.string.agent_session_empty_desc)

        layoutRunningHint = view.findViewById(R.id.layoutRunningHint)
        tvRunningHint = view.findViewById(R.id.tvRunningHint)
        if (isAgentRunning) {
            layoutRunningHint?.visibility = View.VISIBLE
            tvRunningHint?.text = getString(com.hive.i8n.R.string.agent_session_switch_disabled)
        } else {
            layoutRunningHint?.visibility = View.GONE
        }

        listRecyclerView?.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
            setItemViewFactory(SessionListViewFactory(ctx, currentSessionKey, isAgentRunning))
            setOnItemEventListener(object : ListRecyclerItemView.OnItemEventListener {
                override fun onItemEvent(itemData: Any?, eventData: Any?) {
                    if (itemData !is SessionListUiItem) return
                    val meta = itemData.meta
                    when (eventData) {
                        "select" -> {
                            if (isAgentRunning) {
                                Toast.makeText(context, getString(com.hive.i8n.R.string.agent_session_switch_disabled), Toast.LENGTH_SHORT).show()
                                return
                            }
                            onSessionSelected?.invoke(meta.sessionKey)
                            dismiss()
                        }
                        "delete" -> {
                            if (isAgentRunning) {
                                Toast.makeText(context, getString(com.hive.i8n.R.string.agent_session_switch_disabled), Toast.LENGTH_SHORT).show()
                                return
                            }
                            confirmDelete(meta)
                        }
                        "convert" -> {
                            if (isAgentRunning) {
                                Toast.makeText(context, getString(com.hive.i8n.R.string.agent_session_switch_disabled), Toast.LENGTH_SHORT).show()
                                return
                            }
                            confirmConvert(meta)
                        }
                    }
                }
            })
        }

        view.post { loadSessions() }
    }

    override fun onResume() {
        super.onResume()
        view?.post { loadSessions() }
    }

    private fun confirmDelete(meta: SessionMeta) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(getString(com.hive.i8n.R.string.agent_session_delete_confirm_title))
            .setMessage(getString(com.hive.i8n.R.string.agent_session_delete_confirm_message))
            .setNegativeButton(getString(com.hive.i8n.R.string.cancel), null)
            .setPositiveButton(getString(com.hive.i8n.R.string.delete)) { _, _ ->
                val key = meta.sessionKey
                storage?.deleteSession(key)
                onSessionDeleted?.invoke(key)
                loadSessions()
            }
            .show()
    }

    private fun confirmConvert(meta: SessionMeta) {
        val ctx = context ?: return
        val dialog = SampleDialog(ctx)
        dialog.setDialogTitle(getString(com.hive.i8n.R.string.agent_session_convert_confirm_title))
        dialog.setDialogContent(getString(com.hive.i8n.R.string.agent_session_convert_confirm_message))
        dialog.setLeftText(getString(com.hive.i8n.R.string.cancel))
        dialog.setRightText(getString(com.hive.i8n.R.string.agent_session_convert_confirm_action))
        dialog.setOnDialogListener { isRight ->
            dialog.dismiss()
            if (!isRight) return@setOnDialogListener
            onConvertToWorkflow?.invoke(meta.sessionKey)
        }
        dialog.show()
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                val metas = storage?.listSessions() ?: emptyList()
                metas.map { meta ->
                    SessionListUiItem(
                        meta = meta,
                        preview = buildSessionPreview(meta)
                    )
                }
            }
            tvSubtitle?.text = if (list.isEmpty()) "" else getString(com.hive.i8n.R.string.agent_session_count_format, list.size)

            if (list.isEmpty()) {
                listRecyclerView?.visibility = View.GONE
                layoutEmpty?.visibility = View.VISIBLE
            } else {
                listRecyclerView?.visibility = View.VISIBLE
                layoutEmpty?.visibility = View.GONE
                val viewType = 0
                val pairs = list.map { viewType to it }
                listRecyclerView?.submitDataSetsWithType(pairs)
                listRecyclerView?.notifyDataSetChanged()
            }
        }
    }

    private fun buildSessionPreview(meta: SessionMeta): String {
        val loaded = storage?.loadSession(meta.sessionKey)
        val preview = loaded?.messages
            ?.asReversed()
            ?.mapNotNull { it.content?.trim()?.takeIf { content -> content.isNotEmpty() } }
            ?.firstOrNull()
            ?: meta.title
        return preview.take(80)
    }
}
