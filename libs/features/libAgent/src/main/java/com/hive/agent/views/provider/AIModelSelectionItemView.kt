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
 * AI模型选择项视图
 * 复用AIModelItemView的逻辑，但简化为只显示模型信息
 */
class AIModelSelectionItemView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvModelName: TextView
    private lateinit var tvModelTags: TextView
    private lateinit var statusIndicator: View

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_model_selection_item, this)

        tvModelName = findViewById(R.id.tvModelName)
        tvModelTags = findViewById(R.id.tvModelTags)
        statusIndicator = findViewById(R.id.statusIndicator)

        // 设置点击事件
        setOnClickListener {
            handleModelClick()
        }
    }

    override fun bindData(data: Any?) {
        if (data !is ModelInfo) return
        itemData = data

        val modelData = data

        // 设置模型名称
        tvModelName.text = modelData.displayName

        // 设置模型标签
        val tags = mutableListOf<String>()
        if (modelData.capabilities.supportsVision) {
            tags.add(context.getString(com.hive.i8n.R.string.ai_vision_recognition))
        }
        if (modelData.capabilities.supportsFunctionCall) {
            tags.add(context.getString(com.hive.i8n.R.string.ai_function_call))
        }
        tvModelTags.text = tags.joinToString(" • ")

        // 设置状态指示器（始终显示为可选状态）
        statusIndicator.setBackgroundResource(R.drawable.ai_status_indicator_enabled)
    }

    private fun handleModelClick() {
        val modelData = itemData as? ModelInfo
        if (modelData != null) {
            postEvent(
                mapOf(
                    "eventType" to "select_model",
                    "data" to modelData
                )
            )
        }
    }
} 