// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.exception

import java.lang.Exception

/**
 *
 * @author jiadou
 * @date 6/8/21
 */
class ScriptException(var type: ExceptionType, message: String?) : Exception(message) {
    enum class ExceptionType {
        ERROR_CODE
    }
}