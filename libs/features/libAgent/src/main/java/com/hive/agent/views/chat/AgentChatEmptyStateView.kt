// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.agent.config.AgentChatUiConfig
import com.hive.base.BaseLayout

class AgentChatEmptyStateView(context: Context?, attrs: android.util.AttributeSet?) :
    BaseLayout(context, attrs) {

    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var sectionLabelView: TextView
    private lateinit var examplesContainer: LinearLayout

    var onExampleClick: ((AgentChatUiConfig.Example) -> Unit)? = null

    override fun initView(view: View?) {
        titleView = findViewById(R.id.tvEmptyTitle)
        subtitleView = findViewById(R.id.tvEmptySubtitle)
        sectionLabelView = findViewById(R.id.tvSectionLabel)
        examplesContainer = findViewById(R.id.layoutExampleContainer)
    }

    override fun getLayoutId() = R.layout.view_agent_chat_empty_state

    fun render(config: AgentChatUiConfig.EmptyState) {
        titleView.text = config.title
        subtitleView.text = config.subtitle
        sectionLabelView.text = config.sectionLabel
        titleView.visibility = if (config.title.isBlank()) View.GONE else View.VISIBLE
        subtitleView.visibility = if (config.subtitle.isBlank()) View.GONE else View.VISIBLE
        sectionLabelView.visibility =
            if (config.sectionLabel.isBlank() || config.examples.isEmpty()) View.GONE else View.VISIBLE
        examplesContainer.removeAllViews()
        config.examples.forEachIndexed { index, example ->
            examplesContainer.addView(createExampleView(example, index != config.examples.lastIndex))
        }
    }

    private fun createExampleView(example: AgentChatUiConfig.Example, addBottomSpacing: Boolean): View {
        val itemView = View.inflate(context, R.layout.item_agent_chat_empty_example, null)
        val bottomSpacing = if (addBottomSpacing) {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                context.resources.displayMetrics
            ).toInt()
        } else {
            0
        }
        val layoutParams = (itemView.layoutParams as? ViewGroup.MarginLayoutParams)
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.bottomMargin = bottomSpacing
        itemView.layoutParams = layoutParams
        itemView.findViewById<ImageView>(R.id.ivExampleIcon)
            .setImageResource(resolveExampleIcon(example))
        itemView.findViewById<TextView>(R.id.tvExampleTitle).text = example.title
        itemView.findViewById<TextView>(R.id.tvExampleDescription).text = example.description
        itemView.setOnClickListener { onExampleClick?.invoke(example) }
        return itemView
    }

    private fun resolveExampleIcon(example: AgentChatUiConfig.Example): Int {
        return when {
            example.title.contains("抖音") -> com.hive.i8n.R.drawable.ic_repeat
            example.title.contains("小红书") || example.title.contains("发帖") || example.title.contains("笔记") ->
                com.hive.i8n.R.drawable.ic_book_open
            example.title.contains("点赞") || example.title.contains("评论") ->
                com.hive.i8n.R.drawable.ic_thumbs_up
            example.title.contains("手机") || example.title.contains("微信") ->
                com.hive.i8n.R.drawable.ic_smartphone
            else -> com.hive.i8n.R.drawable.ic_wrench
        }
    }
}
