// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort

/** TalkBack / 描述文案语义；由 View 映射到 i18n 字符串，不读写持久化配置。 */
enum class ReasoningSwitchHint {
    OPTIONAL,
    REQUIRED,
    UNSUPPORTED,
    UNKNOWN,
    /** 尚未选择对话模型：仍允许改全局偏好，选择模型后再生效 */
    NO_MODEL
}

/**
 * 设置页与聊天 BottomSheet 共用的思考模式 UI 状态。
 * 仅描述展示与是否允许用户写入全局配置；不副作用修改 MMKV。
 */
data class ReasoningSettingsUiState(
    val switchChecked: Boolean,
    val switchEnabled: Boolean,
    val switchHint: ReasoningSwitchHint,
    val effortRowVisible: Boolean,
    val effortRowEnabled: Boolean,
    val selectedEffort: ReasoningEffort,
    val supportedEfforts: Set<ReasoningEffort>,
    val canPersistSwitch: Boolean,
    val canPersistEffort: Boolean
)

/**
 * 根据全局已保存配置 + 当前模型能力，计算有效 UI 状态。
 * 切换模型只应重新 [create]，不得改写全局保存值。
 */
object ReasoningSettingsUiStateFactory {

    private val allEfforts = setOf(
        ReasoningEffort.LOW,
        ReasoningEffort.MEDIUM,
        ReasoningEffort.HIGH
    )

    fun create(
        savedEnabled: Boolean,
        savedEffort: ReasoningEffort,
        capabilities: ReasoningCapabilities?,
        modelSelected: Boolean = true
    ): ReasoningSettingsUiState {
        if (!modelSelected) {
            return buildEditable(
                switchChecked = savedEnabled,
                switchHint = ReasoningSwitchHint.NO_MODEL,
                selectedEffort = savedEffort,
                supportedEfforts = allEfforts,
                effortSupported = true
            )
        }

        val caps = capabilities ?: ReasoningCapabilities()
        val efforts = caps.supportedEfforts
        val displayEffort = resolveDisplayEffort(savedEffort, caps)
        val effortSupported = efforts.isNotEmpty()

        return when (caps.availability) {
            ReasoningAvailability.OPTIONAL -> buildEditable(
                switchChecked = savedEnabled,
                switchHint = ReasoningSwitchHint.OPTIONAL,
                selectedEffort = displayEffort,
                supportedEfforts = efforts,
                effortSupported = effortSupported
            )
            ReasoningAvailability.REQUIRED -> {
                // 强制开启：开关不可关，强度在有支持集合时展示
                ReasoningSettingsUiState(
                    switchChecked = true,
                    switchEnabled = false,
                    switchHint = ReasoningSwitchHint.REQUIRED,
                    effortRowVisible = effortSupported,
                    effortRowEnabled = effortSupported,
                    selectedEffort = displayEffort,
                    supportedEfforts = efforts,
                    canPersistSwitch = false,
                    canPersistEffort = effortSupported
                )
            }
            ReasoningAvailability.UNSUPPORTED -> disabledOff(
                hint = ReasoningSwitchHint.UNSUPPORTED,
                displayEffort = displayEffort
            )
            // UNKNOWN：仍允许改全局偏好；关闭时隐藏强度
            ReasoningAvailability.UNKNOWN -> buildEditable(
                switchChecked = savedEnabled,
                switchHint = ReasoningSwitchHint.UNKNOWN,
                selectedEffort = savedEffort,
                supportedEfforts = allEfforts,
                effortSupported = true
            )
        }
    }

    /** 可交互开关：仅在开启时展示强度附属项。 */
    private fun buildEditable(
        switchChecked: Boolean,
        switchHint: ReasoningSwitchHint,
        selectedEffort: ReasoningEffort,
        supportedEfforts: Set<ReasoningEffort>,
        effortSupported: Boolean
    ): ReasoningSettingsUiState {
        val showEffort = switchChecked && effortSupported
        return ReasoningSettingsUiState(
            switchChecked = switchChecked,
            switchEnabled = true,
            switchHint = switchHint,
            effortRowVisible = showEffort,
            effortRowEnabled = showEffort,
            selectedEffort = selectedEffort,
            supportedEfforts = supportedEfforts,
            canPersistSwitch = true,
            canPersistEffort = showEffort
        )
    }

    private fun disabledOff(
        hint: ReasoningSwitchHint,
        displayEffort: ReasoningEffort
    ): ReasoningSettingsUiState = ReasoningSettingsUiState(
        switchChecked = false,
        switchEnabled = false,
        switchHint = hint,
        effortRowVisible = false,
        effortRowEnabled = false,
        selectedEffort = displayEffort,
        supportedEfforts = emptySet(),
        canPersistSwitch = false,
        canPersistEffort = false
    )

    private fun resolveDisplayEffort(
        saved: ReasoningEffort,
        caps: ReasoningCapabilities
    ): ReasoningEffort {
        val supported = caps.supportedEfforts
        if (supported.isEmpty()) return saved
        if (saved in supported) return saved
        caps.defaultEffort?.let { if (it in supported) return it }
        return listOf(ReasoningEffort.MEDIUM, ReasoningEffort.LOW, ReasoningEffort.HIGH)
            .firstOrNull { it in supported }
            ?: saved
    }
}
