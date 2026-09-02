// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.agent.R
import com.hive.utils.GlobalApp
import com.hive.utils.GlobalApp.getString
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.widgets.TextDrawableView

/**
 * AI Provider列表项视图
 */
class AIProviderItemView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvProviderName: TextView
    private lateinit var ivProviderIcon: ImageView
    private lateinit var llProviderHeader: LinearLayout
    private lateinit var tvProviderDes: TextView
    private lateinit var tvSettings: TextView
    private lateinit var tvModelCount: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvShowModels: TextDrawableView
    private lateinit var ivAddModel: ImageView
    private lateinit var llModelsContainer: LinearLayout
    private lateinit var llModelSummary: LinearLayout

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_provider_item_new, this)

        tvProviderName = findViewById(R.id.tvProviderName)
        ivProviderIcon = findViewById(R.id.ivProviderIcon)
        llProviderHeader = findViewById(R.id.llProviderHeader)
        tvProviderDes = findViewById(R.id.tvProviderDes)
        tvSettings = findViewById(R.id.tvSettings)
        tvModelCount = findViewById(R.id.tvModelCount)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvShowModels = findViewById(R.id.tvShowModels)
        ivAddModel = findViewById(R.id.ivAddModel)
        llModelsContainer = findViewById(R.id.llModelsContainer)
        llModelSummary = findViewById(R.id.llModelSummary)

        // 设置点击事件
        tvSettings.setOnClickListener {
            handleSettingsClick()
        }
        llProviderHeader.setOnClickListener {
            if (tvSettings.isEnabled) handleSettingsClick()
        }

        tvShowModels.setOnClickListener {
            handleShowModelsClick()
        }

        llModelSummary.setOnClickListener {
            handleShowModelsClick()
        }

        ivAddModel.setOnClickListener {
            handleAddModelClick()
        }
    }

    override fun bindData(data: Any?) {
        if (data !is AIProviderItemData) return

        val providerData = data

        // 设置Provider名称
        tvProviderName.text = providerData.provider?.getProviderInfo()?.displayName
        ivProviderIcon.setImageResource(providerIcon(providerData.providerId))

        // 设置Provider标签
        tvProviderDes.text = providerData.providerTags.joinToString(" • ")

        // 设置API Key状态
        if (!providerData.hasValidApiKey) {
            tvSettings.text = getString(com.hive.i8n.R.string.agent_settings_not_set)
            tvSettings.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_accent_amber))
        } else {
            tvSettings.text = getString(com.hive.i8n.R.string.agent_settings_modify)
            tvSettings.setTextColor(GlobalApp.getColor(com.hive.i8n.R.color.design_text_tertiary))
        }
        tvSettings.alpha = if (providerData.providerInfo.apikeyEnabled) 1.0f else 0.4f
        tvSettings.isEnabled = providerData.providerInfo.apikeyEnabled
        val isConnected = providerData.isEnabled && providerData.provider?.isProviderReady() == true
        tvConnectionStatus.setText(
            if (isConnected) com.hive.i8n.R.string.agent_provider_connected
            else com.hive.i8n.R.string.agent_provider_not_connected
        )
        tvConnectionStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isConnected) R.drawable.ai_status_indicator_enabled
            else R.drawable.ai_status_indicator_disabled,
            0,
            0,
            0
        )
        // 设置模型数量
        val modelCount = providerData.models.size
        tvModelCount.text = context.getString(com.hive.i8n.R.string.ai_model_count, modelCount)

        tvShowModels.isSelected = providerData.isExpanded
        tvShowModels.setDrawableRight(
            GlobalApp.getDrawable(
                if (providerData.isExpanded) R.drawable.icon_arr_up else R.drawable.icon_arr_down
            )
        )

        // 设置模型列表
        setupModelsList(providerData, providerData.isExpanded)
    }

    private fun providerIcon(providerId: String): Int = when {
        providerId.contains("openrouter", ignoreCase = true) -> R.drawable.ic_openrouter
        providerId.contains("openai", ignoreCase = true) -> R.drawable.ic_openai
        providerId.contains("claude", ignoreCase = true) -> R.drawable.ic_claude
        providerId.contains("gemini", ignoreCase = true) -> R.drawable.ic_gemini
        providerId.contains("deepseek", ignoreCase = true) -> R.drawable.ic_deepseek
        providerId.contains("ollama", ignoreCase = true) -> R.drawable.ic_ollama
        else -> R.drawable.ic_model_default
    }

    private fun setupModelsList(itemData: AIProviderItemData, isExpanded: Boolean) {
        llModelsContainer.removeAllViews()

        if (isExpanded) {
            itemData.models.forEach { model ->
                val modelView = AIModelItemView(context)
                modelView.bindModelData(itemData, model, this)
                llModelsContainer.addView(modelView)
            }
        }
    }

    private fun handleSettingsClick() {
        val providerData = itemData as? AIProviderItemData
        if (providerData != null) {
            postEvent(
                mapOf(
                    "eventType" to "show_settings_dialog",
                    "providerId" to providerData.providerId,
                    "providerName" to providerData.providerName
                )
            )
        }
    }

    private fun handleShowModelsClick() {
        val providerData = itemData as? AIProviderItemData
        if (providerData != null) {
            postEvent(
                mapOf(
                    "eventType" to "toggle_models_expanded", "providerId" to providerData.providerId
                )
            )
        }
    }

    private fun handleAddModelClick() {
        val providerData = itemData as? AIProviderItemData
        if (providerData != null) {
            postEvent(
                mapOf(
                    "eventType" to "show_add_model_dialog",
                    "providerId" to providerData.providerId,
                    "providerName" to providerData.providerName
                )
            )
        }
    }
}
