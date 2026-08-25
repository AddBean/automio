// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

class ScripRunningEnv(var scriptInterpreter: ScriptInterpreter) {

    private val jumpControl = ScriptJumpControl()

    fun getJumpControl() = jumpControl

}