// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.net.data

import java.io.Serializable

data class ScriptCustomMcpTool(
    val scriptId: String,
    val scriptName: String,
    val scriptDesc: String,
    val scriptPath: String
) : Serializable