// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import com.hive.config.BuildConfigHelper

object ModeControl {
    fun isAgentFeatureEnabled() = BuildConfigHelper.getMapBoolean("enableAgentFeature", true) == true
}
