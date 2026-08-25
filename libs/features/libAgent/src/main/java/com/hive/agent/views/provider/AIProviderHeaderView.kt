// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import android.content.Context
import android.widget.TextView
import com.hive.agent.R
import com.hive.views.list_view.ListRecyclerItemView

/**
 * AI Provider头部视图
 * 用于在模型选择页面显示Provider名称
 */
class AIProviderHeaderView(context: Context) : ListRecyclerItemView(context) {

    private lateinit var tvProviderName: TextView

    init {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.ai_provider_header, this)

        tvProviderName = findViewById(R.id.tvProviderName)
    }

    override fun bindData(data: Any?) {
        if (data !is String) return

        val providerId = data

        // 设置Provider名称
        val providerName = when (providerId) {
            "default" -> context.getString(com.hive.i8n.R.string.ai_provider_default)
            "deepseek" -> context.getString(com.hive.i8n.R.string.ai_provider_deepseek)
            "bailian" -> context.getString(com.hive.i8n.R.string.ai_provider_bailian)
            "bailian_code" -> context.getString(com.hive.i8n.R.string.ai_provider_bailian_code)
            "ark_agent_plan" -> context.getString(com.hive.i8n.R.string.ai_provider_ark_agent_plan)
            "ark_coding_plan" -> context.getString(com.hive.i8n.R.string.ai_provider_ark_coding_plan)
            "kimi" -> context.getString(com.hive.i8n.R.string.ai_provider_kimi)
            "siliconflow" -> context.getString(com.hive.i8n.R.string.ai_provider_siliconflow)
            "mimo" -> context.getString(com.hive.i8n.R.string.ai_provider_mimo)
            "minimax" -> context.getString(com.hive.i8n.R.string.ai_provider_minimax)
            "stepfun" -> context.getString(com.hive.i8n.R.string.ai_provider_stepfun)
            "openai_custom" -> context.getString(com.hive.i8n.R.string.ai_provider_openai_custom)
            "openai" -> context.getString(com.hive.i8n.R.string.ai_provider_openai)
            "claude" -> context.getString(com.hive.i8n.R.string.ai_provider_claude)
            "gemini" -> context.getString(com.hive.i8n.R.string.ai_provider_gemini)
            "ollama" -> context.getString(com.hive.i8n.R.string.ai_provider_ollama)
            "openrouter" -> context.getString(com.hive.i8n.R.string.ai_provider_openrouter)
            else -> providerId
        }

        tvProviderName.text = providerName
    }
} 
