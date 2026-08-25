// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.hive.script.base.ScriptCommand

class ScriptJumpControl {
    private var targetJumpCommand: ScriptCommand? = null


    fun jumpTo(command: ScriptCommand?) {
        targetJumpCommand = command
    }

    fun isJumpModel(): Boolean {
        return targetJumpCommand != null
    }

    fun getJumpCommand(): ScriptCommand? {
        return targetJumpCommand
    }

    fun checkJump(scriptCommand: ScriptCommand): Boolean {
        return targetJumpCommand == scriptCommand
    }

    fun stopJump() {
        targetJumpCommand = null
    }
}