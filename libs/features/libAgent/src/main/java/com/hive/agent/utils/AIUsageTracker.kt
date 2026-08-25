// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.utils

import com.google.gson.reflect.TypeToken
import com.hive.plugin.agent.model.ChatCompletionResponse
import com.hive.utils.global.MMKVTools
import com.hive.utils.utils.GsonHelper
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val MMKV_KEY_RECORDS = "ai_usage_records"
private const val MAX_RECORDS = 50_000
private const val MAX_AGE_MS = 365L * 24 * 60 * 60 * 1000

private val listType = object : TypeToken<List<AIUsageRecord>>() {}.type

/**
 * 统一记录与聚合 AI 使用量（tokens + 费用），支持按日/月/年查询。
 * 费用优先使用 API 返回的 billedCost，否则用 ModelPricingTable 估算。
 */
object AIUsageTracker {

    private val lock = ReentrantLock()

    /**
     * 记录一次调用。仅在 AbstractBaseProvider 成功返回 ChatCompletionResponse 后调用。
     * @param modelId 优先使用请求时的 model（如 request.model），可为空
     */
    @JvmStatic
    fun record(response: ChatCompletionResponse, providerName: String, modelId: String?) {
        val usage = response.usage ?: emptyMap()
        val promptTokens = usage["prompt_tokens"] ?: 0
        val completionTokens = usage["completion_tokens"] ?: 0
        val totalTokens = usage["total_tokens"] ?: (promptTokens + completionTokens).coerceAtLeast(0)
        if (totalTokens <= 0 && response.cost == null) return

        val billedCost = response.cost
        val (estimatedCost, isEstimated) = if (billedCost != null) {
            null to false
        } else {
            val est = ModelPricingTable.calculate(
                providerName,
                modelId ?: response.model,
                promptTokens,
                completionTokens
            )
            est to (est != null)
        }

        val record = AIUsageRecord(
            timestampMs = System.currentTimeMillis(),
            provider = providerName,
            modelId = modelId ?: response.model,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            billedCost = billedCost,
            estimatedCost = estimatedCost,
            isEstimated = isEstimated
        )

        lock.withLock {
            val list = loadLocked().toMutableList()
            list.add(record)
            pruneAndSaveLocked(list)
        }
    }

    @JvmStatic
    fun getTotal(): AIUsageSummary = aggregate(getAllRecords())

    @JvmStatic
    fun getByRange(startMs: Long, endMs: Long): AIUsageSummary {
        val list = getAllRecords().filter { it.timestampMs in startMs..endMs }
        return aggregate(list)
    }

    /** 按日聚合，使用设备默认时区。 */
    @JvmStatic
    fun getByDay(year: Int, month: Int, dayOfMonth: Int): AIUsageSummary {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endMs = cal.timeInMillis - 1
        return getByRange(startMs, endMs)
    }

    /** 按月聚合，使用设备默认时区。 */
    @JvmStatic
    fun getByMonth(year: Int, month: Int): AIUsageSummary {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endMs = cal.timeInMillis - 1
        return getByRange(startMs, endMs)
    }

    /** 按年聚合。 */
    @JvmStatic
    fun getByYear(year: Int): AIUsageSummary {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        val endMs = cal.timeInMillis - 1
        return getByRange(startMs, endMs)
    }

    @JvmStatic
    fun getAllRecords(): List<AIUsageRecord> = lock.withLock { loadLocked() }

    private fun loadLocked(): List<AIUsageRecord> {
        val json = MMKVTools.getInstance().getString(MMKV_KEY_RECORDS, "[]") ?: "[]"
        return try {
            GsonHelper.getInstance().fromJson(json, listType) as? List<AIUsageRecord> ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun pruneAndSaveLocked(list: MutableList<AIUsageRecord>) {
        val now = System.currentTimeMillis()
        val cutoff = now - MAX_AGE_MS
        list.removeAll { it.timestampMs < cutoff }
        while (list.size > MAX_RECORDS) list.removeAt(0)
        val json = GsonHelper.getInstance().toJson(list)
        MMKVTools.getInstance().putStringImmediately(MMKV_KEY_RECORDS, json)
    }

    private fun aggregate(records: List<AIUsageRecord>): AIUsageSummary {
        var promptTokens = 0L
        var completionTokens = 0L
        var totalTokens = 0L
        var billedSum: Double? = null
        var estimatedSum: Double? = null
        for (r in records) {
            promptTokens += r.promptTokens
            completionTokens += r.completionTokens
            totalTokens += r.totalTokens
            r.billedCost?.let { billedSum = (billedSum ?: 0.0) + it }
            r.estimatedCost?.let { estimatedSum = (estimatedSum ?: 0.0) + it }
        }
        return AIUsageSummary(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            billedCostSum = billedSum,
            estimatedCostSum = estimatedSum
        )
    }
}
