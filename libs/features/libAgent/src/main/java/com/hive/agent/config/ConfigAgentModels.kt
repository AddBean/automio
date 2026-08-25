// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.config;

import androidx.annotation.Keep
import com.hive.annotation.NotProguard
import com.hive.global.GlobalConfig
import com.hive.plugin.agent.ProviderInfo

@NotProguard
@Keep
object ConfigAgentModels {

    @JvmStatic
    fun read(): List<ProviderInfo>? {
        return GlobalConfig.getInstance().getListObject(
            "config.agent.models",
            ProviderInfo::class.java, mutableListOf()
        )
    }

    fun findProviderInfo(id: String): ProviderInfo? {
        return read()?.firstOrNull { it.name == id }
    }
}

