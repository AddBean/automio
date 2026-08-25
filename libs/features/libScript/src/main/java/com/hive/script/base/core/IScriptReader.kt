// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

interface IScriptReader {

    fun readLine(): String?

    fun getCurrentLine():Int

    fun reset()

    fun backLine()
}