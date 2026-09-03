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
    UNKNOWN
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

    fun create(
        savedEnabled: Boolean,
        savedEffort: ReasoningEffort,
        capabilities: ReasoningCapabilities?
    ): ReasoningSettingsUiState {
        val caps = capabilities ?: ReasoningCapabilities()
        val efforts = caps.supportedEfforts
        val displayEffort = resolveDisplayEffort(savedEffort, caps)
        val effortVisible = efforts.isNotEmpty()

        return when (caps.availability) {
            ReasoningAvailability.OPTIONAL -> ReasoningSettingsUiState(
                switchChecked = savedEnabled,
                switchEnabled = true,
                switchHint = ReasoningSwitchHint.OPTIONAL,
                effortRowVisible = effortVisible,
                effortRowEnabled = effortVisible,
                selectedEffort = displayEffort,
                supportedEfforts = efforts,
                canPersistSwitch = true,
                canPersistEffort = effortVisible
            )
            ReasoningAvailability.REQUIRED -> ReasoningSettingsUiState(
                switchChecked = true,
                switchEnabled = false,
                switchHint = ReasoningSwitchHint.REQUIRED,
                effortRowVisible = effortVisible,
                effortRowEnabled = effortVisible,
                selectedEffort = displayEffort,
                supportedEfforts = efforts,
                canPersistSwitch = false,
                canPersistEffort = effortVisible
            )
            ReasoningAvailability.UNSUPPORTED -> disabledOff(
                hint = ReasoningSwitchHint.UNSUPPORTED,
                displayEffort = displayEffort
            )
            ReasoningAvailability.UNKNOWN -> disabledOff(
                hint = ReasoningSwitchHint.UNKNOWN,
                displayEffort = displayEffort
            )
        }
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
