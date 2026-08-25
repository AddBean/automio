// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.net.data

import java.io.Serializable

/**
 * Custom/public skill 的 SP 引用，仅存基本信息，具体内容在 .skill 文件中。
 * 类似 [ScriptCustomMcpTool] 的路径指向模式。
 */
data class ScriptCustomSkill(
    val skillId: String,
    val skillName: String,
    val skillPath: String
) : Serializable
