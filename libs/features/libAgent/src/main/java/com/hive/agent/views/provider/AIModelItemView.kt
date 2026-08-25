// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import android.view.View
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
    private lateinit var tvModelTags: TextView
    private lateinit var tvVisionBadge: TextView
    private lateinit var statusIndicator: View

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_model_item, this)

        tvModelName = findViewById(R.id.tvModelName)
        tvModelTags = findViewById(R.id.tvModelTags)
        tvVisionBadge = findViewById(R.id.tvVisionBadge)
        statusIndicator = findViewById(R.id.statusIndicator)
    }

    private var isProgrammaticChange = false


    private fun postEventOutside(eventData: Any?) {
        parentView?.postEvent(eventData)
    }

    override fun bindData(data: Any?) {
        if (data !is ModelInfo) return
        itemData = data

        // 设置模型名称
        tvModelName.text = data.displayName

        // 设置模型标签（仅功能调用）
        val tags = mutableListOf<String>()
        if (data.capabilities.supportsFunctionCall) {
            tags.add(context.getString(com.hive.i8n.R.string.ai_function_call))
        }
        tvModelTags.text = tags.joinToString(" • ")
        tvModelTags.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE

        // 设置视觉能力标签
        tvVisionBadge.visibility = if (data.capabilities.supportsVision) View.VISIBLE else View.GONE

        // 设置状态指示器
        val isEnabled = providerData?.provider?.isModelReady(data.modelId) == true
        statusIndicator.setBackgroundResource(
            if (isEnabled) R.drawable.ai_status_indicator_enabled
            else R.drawable.ai_status_indicator_disabled
        )
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
}