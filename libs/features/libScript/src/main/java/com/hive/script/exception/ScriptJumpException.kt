// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.exception

import com.hive.script.base.ScriptCommand

class ScriptJumpException(var cmd: ScriptCommand?) : Exception() {
}