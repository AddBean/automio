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
    private lateinit var tvModelMeta: TextView
    private lateinit var ivModelMore: ImageView

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_model_item, this)

        tvModelName = findViewById(R.id.tvModelName)
        tvModelId = findViewById(R.id.tvModelId)
        tvModelMeta = findViewById(R.id.tvModelMeta)
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

        val meta = mutableListOf<String>()
        val isEnabled = providerData?.provider?.isModelReady(data.modelId) == true
        meta += context.getString(
            if (isEnabled) com.hive.i8n.R.string.agent_model_available_status
            else com.hive.i8n.R.string.agent_model_unavailable_status
        )
        if (data.capabilities.supportsFunctionCall) {
            meta += context.getString(com.hive.i8n.R.string.ai_function_call)
        }
        if (data.capabilities.supportsVision) {
            meta += context.getString(com.hive.i8n.R.string.ai_vision)
        }

        val contextWindow = data.capabilities.contextWindow
        if (contextWindow > 0) {
            meta += when {
                contextWindow >= 1_000_000 -> "${contextWindow / 1_000_000}M"
                contextWindow >= 1_000 -> "${contextWindow / 1_000}K"
                else -> contextWindow.toString()
            }
        }
        meta += context.getString(
            if (data.buildIn) com.hive.i8n.R.string.agent_model_source_discovered
            else com.hive.i8n.R.string.agent_model_source_custom
        )
        tvModelMeta.text = meta.joinToString(" · ")

        tvModelMeta.setCompoundDrawablesRelativeWithIntrinsicBounds(
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
