// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.hive.agent.config.AIAgentConfig
import com.hive.agent.config.ReasoningRunPolicy
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.model.ReasoningOptions
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务级思考策略上下文：在任务开始时冻结 [ReasoningRunPolicy]，
 * 任务中途的设置变更只影响下一次任务。
 */
class ReasoningRunContext(
    val policy: ReasoningRunPolicy
) {
    fun resolve(
        providerId: String,
        modelId: String,
        dynamicMetadata: DynamicReasoningMetadata? = null
    ): ResolvedReasoning =
        ReasoningCapabilityResolver.resolve(providerId, modelId, dynamicMetadata, policy)

    fun resolveOptions(
        providerId: String,
        modelId: String,
        dynamicMetadata: DynamicReasoningMetadata? = null
    ): ReasoningOptions? = resolve(providerId, modelId, dynamicMetadata).effectiveOptions

    companion object {
        fun snapshotNow(): ReasoningRunContext =
            ReasoningRunContext(AIAgentConfig.ReasoningConfig.snapshot())
    }
}

/**
 * 按 rootTaskId 绑定思考上下文，供嵌套 Skill 继承父任务快照。
 */
object ReasoningRunContexts {
    private val byRootTaskId = ConcurrentHashMap<String, ReasoningRunContext>()

    fun bind(rootTaskId: String, context: ReasoningRunContext) {
        byRootTaskId[rootTaskId] = context
    }

    fun get(rootTaskId: String): ReasoningRunContext? = byRootTaskId[rootTaskId]

    fun unbind(rootTaskId: String) {
        byRootTaskId.remove(rootTaskId)
    }

    fun clear() {
        byRootTaskId.clear()
    }

    /**
     * 嵌套 Skill 继承已绑定的父任务快照；独立 Skill（或找不到父快照时）创建新快照并绑定。
     */
    fun resolveForSkill(
        rootTaskId: String,
        isStandalone: Boolean,
        createSnapshot: () -> ReasoningRunContext = { ReasoningRunContext.snapshotNow() }
    ): ReasoningRunContext {
        if (!isStandalone) {
            get(rootTaskId)?.let { return it }
        }
        val created = createSnapshot()
        bind(rootTaskId, created)
        return created
    }
}

/**
 * 将任务级 [ReasoningRunContext] 解析为 [AIRequest.reasoning]。
 */
object ReasoningRequestFactory {

    fun dynamicMetadataFrom(model: ModelInfo): DynamicReasoningMetadata? {
        val caps = model.capabilities.reasoning ?: return null
        return DynamicReasoningMetadata(capabilities = caps)
    }

    fun optionsFor(
        runContext: ReasoningRunContext,
        providerId: String,
        model: ModelInfo
    ): ReasoningOptions? =
        runContext.resolveOptions(providerId, model.modelId, dynamicMetadataFrom(model))

    /** 摘要 / 脚本命名等轻量请求显式保持 null。 */
    fun lightweightOptions(): ReasoningOptions? = null
}
