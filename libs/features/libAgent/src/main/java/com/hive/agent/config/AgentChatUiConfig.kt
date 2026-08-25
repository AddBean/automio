// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.config

import com.google.gson.annotations.SerializedName
import com.hive.global.GlobalConfig
import com.hive.utils.GlobalApp

data class AgentChatUiConfig(
    @SerializedName("emptyState")
    val emptyState: EmptyState = EmptyState(),
) {

    data class EmptyState(
        @SerializedName("title")
        val title: String = "",
        @SerializedName("subtitle")
        val subtitle: String = "",
        @SerializedName("sectionLabel")
        val sectionLabel: String = "",
        @SerializedName("examples")
        val examples: List<Example> = emptyList(),
    )

    data class Example(
        @SerializedName("title")
        val title: String = "",
        @SerializedName("description")
        val description: String = "",
        @SerializedName("prompt")
        val prompt: String = "",
        @SerializedName("icon")
        val icon: String? = null,
        @SerializedName("autoSubmit")
        val autoSubmit: Boolean = false,
    )

    companion object {
        private const val CONFIG_KEY = "config.ai.chat.ui"

        fun read(): AgentChatUiConfig {
            val config = GlobalConfig.getInstance()
                .getObject(CONFIG_KEY, AgentChatUiConfig::class.java, null)
                ?.sanitize()
            return if (config == null || config.emptyState.examples.isEmpty()) {
                default()
            } else {
                config
            }
        }

        /** 云配缺失时的本地默认示例，与对话空态设计稿一致 */
        fun default(): AgentChatUiConfig {
            return AgentChatUiConfig(
                emptyState = EmptyState(
                    title = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_empty_title),
                    subtitle = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_empty_subtitle),
                    sectionLabel = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_empty_section_label),
                    examples = listOf(
                        Example(
                            title = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_douyin_title),
                            description = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_douyin_desc),
                            prompt = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_douyin_prompt),
                        ),
                        Example(
                            title = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_xhs_title),
                            description = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_xhs_desc),
                            prompt = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_xhs_prompt),
                        ),
                        Example(
                            title = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_engage_title),
                            description = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_engage_desc),
                            prompt = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_engage_prompt),
                        ),
                        Example(
                            title = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_phone_title),
                            description = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_phone_desc),
                            prompt = GlobalApp.getString(com.hive.i8n.R.string.agent_chat_example_phone_prompt),
                        ),
                    ),
                )
            )
        }
    }

    private fun sanitize(): AgentChatUiConfig {
        val sanitizedExamples = emptyState.examples.filter { it.title.isNotBlank() && it.prompt.isNotBlank() }
        return copy(
            emptyState = emptyState.copy(
                examples = sanitizedExamples,
            )
        )
    }
}
