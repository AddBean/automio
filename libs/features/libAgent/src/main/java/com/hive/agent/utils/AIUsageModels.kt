// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * 单次 AI 调用使用记录（带时间戳，便于按日/月/年聚合）
 */
@Keep
data class AIUsageRecord(
    @SerializedName("timestampMs") val timestampMs: Long,
    @SerializedName("provider") val provider: String,
    @SerializedName("modelId") val modelId: String?,
    @SerializedName("promptTokens") val promptTokens: Int,
    @SerializedName("completionTokens") val completionTokens: Int,
    @SerializedName("totalTokens") val totalTokens: Int,
    @SerializedName("billedCost") val billedCost: Double? = null,
    @SerializedName("estimatedCost") val estimatedCost: Double? = null,
    @SerializedName("isEstimated") val isEstimated: Boolean = false
)

/**
 * 聚合后的使用汇总（token 与费用合计）
 */
@Keep
data class AIUsageSummary(
    @SerializedName("promptTokens") val promptTokens: Long,
    @SerializedName("completionTokens") val completionTokens: Long,
    @SerializedName("totalTokens") val totalTokens: Long,
    @SerializedName("billedCostSum") val billedCostSum: Double?,
    @SerializedName("estimatedCostSum") val estimatedCostSum: Double?
)
