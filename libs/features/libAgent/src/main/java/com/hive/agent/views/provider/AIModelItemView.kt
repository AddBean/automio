// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.hive.agent.R
import com.hive.plugin.agent.ModelInfo
import com.hive.views.list_view.ListRecyclerItemView

/**
 * AI模型列表项视图
 */
class AIModelItemView(context: Context) : ListRecyclerItemView(context) {

    private var providerData: AIProviderItemData? = null
    private var parentView: ListRecyclerItemView? = null
    private lateinit var tvModelName: TextView
    private lateinit var tvModelId: TextView
    private lateinit var tvModelTags: TextView
    private lateinit var tvVisionBadge: TextView
    private lateinit var tvContextBadge: TextView
    private lateinit var tvSourceBadge: TextView
    private lateinit var tvModelStatus: TextView
    private lateinit var ivModelMore: ImageView

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_model_item, this)

        tvModelName = findViewById(R.id.tvModelName)
        tvModelId = findViewById(R.id.tvModelId)
        tvModelTags = findViewById(R.id.tvModelTags)
        tvVisionBadge = findViewById(R.id.tvVisionBadge)
        tvContextBadge = findViewById(R.id.tvContextBadge)
        tvSourceBadge = findViewById(R.id.tvSourceBadge)
        tvModelStatus = findViewById(R.id.tvModelStatus)
        ivModelMore = findViewById(R.id.ivModelMore)
        ivModelMore.setOnClickListener { showActions() }
    }

    private fun postEventOutside(eventData: Any?) {
        parentView?.postEvent(eventData)
    }

    override fun bindData(data: Any?) {
        if (data !is ModelInfo) return
        itemData = data

        // 设置模型名称
        tvModelName.text = data.displayName
        tvModelId.text = data.modelId

        // 设置模型标签（仅功能调用）
        val tags = mutableListOf<String>()
        if (data.capabilities.supportsFunctionCall) {
            tags.add(context.getString(com.hive.i8n.R.string.ai_function_call))
        }
        tvModelTags.text = tags.joinToString(" • ")
        tvModelTags.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE

        // 设置视觉能力标签
        tvVisionBadge.visibility = if (data.capabilities.supportsVision) View.VISIBLE else View.GONE

        val contextWindow = data.capabilities.contextWindow
        tvContextBadge.visibility = if (contextWindow > 0) View.VISIBLE else View.GONE
        tvContextBadge.text = when {
            contextWindow >= 1_000_000 -> "${contextWindow / 1_000_000}M"
            contextWindow >= 1_000 -> "${contextWindow / 1_000}K"
            else -> contextWindow.toString()
        }
        tvSourceBadge.setText(
            if (data.buildIn) com.hive.i8n.R.string.agent_model_source_discovered
            else com.hive.i8n.R.string.agent_model_source_custom
        )

        // 设置状态指示器
        val isEnabled = providerData?.provider?.isModelReady(data.modelId) == true
        tvModelStatus.setText(
            if (isEnabled) com.hive.i8n.R.string.agent_model_available_status
            else com.hive.i8n.R.string.agent_model_unavailable_status
        )
        tvModelStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isEnabled) R.drawable.ai_status_indicator_enabled
            else R.drawable.ai_status_indicator_disabled,
            0,
            0,
            0
        )
        ivModelMore.visibility = if (data.buildIn) View.GONE else View.VISIBLE
    }

    // 添加public方法以便外部调用
    fun bindModelData(
        providerData: AIProviderItemData,
        modelData: ModelInfo,
        parentView: ListRecyclerItemView
    ) {
        this.parentView = parentView
        this.providerData = providerData
        bindData(modelData)
    }

    private fun requestDelete() {
        val model = itemData as? ModelInfo ?: return
        val providerId = providerData?.providerId ?: return
        postEventOutside(
            mapOf(
                "eventType" to "delete_custom_model",
                "providerId" to providerId,
                "modelId" to model.modelId,
                "modelName" to model.displayName
            )
        )
    }

    private fun showActions() {
        PopupMenu(context, ivModelMore).apply {
            menu.add(com.hive.i8n.R.string.agent_model_delete_action)
            setOnMenuItemClickListener {
                requestDelete()
                true
            }
            show()
        }
    }
}
