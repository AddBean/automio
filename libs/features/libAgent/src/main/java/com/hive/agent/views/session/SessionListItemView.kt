// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.session

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.hive.agent.R
import com.hive.views.list_view.ListRecyclerItemView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SessionListUiItem(
    val meta: com.hive.agent.storage.SessionMeta,
    val preview: String
)

/**
 * 会话列表项：展示 title + 摘要 + 更新时间 + 操作按钮
 */
class SessionListItemView(
    context: Context,
    private val currentSessionKey: String?,
    private val isAgentRunning: Boolean
) : ListRecyclerItemView(context) {

    private lateinit var tvSessionTitle: TextView
    private lateinit var tvSessionPreview: TextView
    private lateinit var tvSessionTime: TextView
    private lateinit var viewCurrentIndicator: View
    private lateinit var itemRoot: View
    private lateinit var btnMore: View

    init {
        inflate(context, R.layout.item_agent_session, this)
        itemRoot = findViewById(R.id.itemSessionRoot)
        tvSessionTitle = findViewById(R.id.tvSessionTitle)
        tvSessionPreview = findViewById(R.id.tvSessionPreview)
        tvSessionTime = findViewById(R.id.tvSessionTime)
        viewCurrentIndicator = findViewById(R.id.viewCurrentIndicator)
        btnMore = findViewById(R.id.btnSessionMore)

        itemRoot.setOnClickListener { postEvent("select") }
        btnMore.setOnClickListener { anchor -> showActions(anchor) }
    }

    override fun bindData(data: Any?) {
        if (data !is SessionListUiItem) return
        val meta = data.meta
        tvSessionTitle.text = meta.title.ifEmpty { context.getString(com.hive.i8n.R.string.agent_session_empty_title) }
        tvSessionPreview.text = data.preview
        tvSessionTime.text = formatSessionTime(meta.updateTime)
        tvSessionTime.visibility = View.VISIBLE

        val isCurrent = currentSessionKey != null && currentSessionKey == meta.sessionKey
        viewCurrentIndicator.visibility = if (isCurrent) View.VISIBLE else View.GONE
        itemRoot.isSelected = isCurrent

        val enabled = !isAgentRunning
        itemRoot.isEnabled = enabled
        itemRoot.alpha = if (enabled) 1f else 0.55f
        btnMore.isEnabled = enabled
        btnMore.alpha = if (enabled) 1f else 0.45f
    }

    private fun showActions(anchor: View) {
        PopupMenu(context, anchor).apply {
            menu.add(com.hive.i8n.R.string.agent_session_convert).setOnMenuItemClickListener {
                postEvent("convert")
                true
            }
            menu.add(com.hive.i8n.R.string.agent_session_delete).setOnMenuItemClickListener {
                postEvent("delete")
                true
            }
            show()
        }
    }

    /**
     * 格式化会话时间：无效时间显示 "-"；60秒内显示"刚刚"；24 小时内用相对时间（如「5分钟前」）；更早用绝对日期（如「03-01 14:30」）。
     */
    private fun formatSessionTime(updateTime: Long): CharSequence {
        if (updateTime <= 0) return "—"
        val now = System.currentTimeMillis()
        val diff = now - updateTime
        return when {
            diff < 0 -> formatAbsoluteTime(updateTime)
            diff < 60_000 -> context.getString(com.hive.i8n.R.string.agent_session_time_just_now)
            diff < DateUtils.DAY_IN_MILLIS ->
                DateUtils.getRelativeTimeSpanString(
                    updateTime,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            else -> formatAbsoluteTime(updateTime)
        }
    }

    private fun formatAbsoluteTime(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
