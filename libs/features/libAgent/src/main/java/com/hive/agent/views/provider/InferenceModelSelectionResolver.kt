// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import com.hive.plugin.agent.ModelInfo

/**
 * 设置页推理/视觉模型选择态：只描述展示与是否仍保留本地选择，
 * 不负责写 MMKV。网络失败或列表暂缺时不得当作「未设置」。
 */
enum class InferenceModelSelectionKind {
    /** 本地从未选择 */
    NOT_SET,
    /** 已校验可用 */
    READY,
    /** 已选择但 Provider 关闭/未就绪/不存在 */
    NEEDS_CONFIG,
    /** 已选择且服务就绪，但拉列表失败或暂未匹配到该 modelId */
    REFRESH_FAILED,
    /** 已选择但不满足当前用途（如视觉位选了非视觉模型） */
    INVALID_FOR_TYPE
}

data class InferenceModelSelectionStatus(
    val kind: InferenceModelSelectionKind,
    val model: ModelInfo?
) {
    val displayName: String?
        get() = model?.displayName?.takeIf { it.isNotBlank() } ?: model?.modelId

    /** 是否仍持有本地选择（可用于思考能力解析等） */
    val hasSelection: Boolean
        get() = model != null && kind != InferenceModelSelectionKind.NOT_SET

    /** 视觉识别开关是否应视为「已配置视觉模型」 */
    val countsAsConfiguredVisionModel: Boolean
        get() = kind == InferenceModelSelectionKind.READY ||
            kind == InferenceModelSelectionKind.REFRESH_FAILED
}

object InferenceModelSelectionResolver {

    /**
     * @param findInCatalog 返回刷新后的 [ModelInfo]；找不到返回 null；抛错视为刷新失败。
     */
    suspend fun resolve(
        selected: ModelInfo?,
        requireVision: Boolean,
        providerExists: (providerId: String) -> Boolean,
        providerEnabled: (providerId: String) -> Boolean,
        providerReady: (providerId: String) -> Boolean,
        findInCatalog: suspend (selected: ModelInfo) -> ModelInfo?
    ): InferenceModelSelectionStatus {
        if (selected == null) {
            return InferenceModelSelectionStatus(InferenceModelSelectionKind.NOT_SET, null)
        }
        val providerId = selected.providerId
        if (!providerExists(providerId) ||
            !providerEnabled(providerId) ||
            !providerReady(providerId)
        ) {
            return InferenceModelSelectionStatus(InferenceModelSelectionKind.NEEDS_CONFIG, selected)
        }

        val catalogModel = try {
            findInCatalog(selected)
        } catch (_: Exception) {
            return InferenceModelSelectionStatus(InferenceModelSelectionKind.REFRESH_FAILED, selected)
        }

        if (catalogModel == null) {
            return InferenceModelSelectionStatus(InferenceModelSelectionKind.REFRESH_FAILED, selected)
        }

        if (requireVision && !catalogModel.capabilities.supportsVision) {
            return InferenceModelSelectionStatus(InferenceModelSelectionKind.INVALID_FOR_TYPE, catalogModel)
        }

        return InferenceModelSelectionStatus(InferenceModelSelectionKind.READY, catalogModel)
    }
}
