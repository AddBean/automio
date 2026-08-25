// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import com.hive.plugin.agent.AIServiceProvider
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ProviderInfo

/**
 * AI Provider列表项数据模型
 */
data class AIProviderItemData(
    val provider: AIServiceProvider?,
    val isEnabled: Boolean,
    val hasValidApiKey: Boolean,
    val providerId: String,
    val providerInfo: ProviderInfo,
    val providerName: String,
    val providerDescription: String,
    val providerTags: List<String> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val isExpanded: Boolean = false
) {
    val id: String get() = providerId
    val name: String get() = providerName
}
