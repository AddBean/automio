// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.params

class ScriptParamGroup(
    var id: String,
    var name: String,
    var params: MutableList<ScriptParam>
) {

    fun getCommandLines(): String {
        val sb = StringBuilder()
        sb.append("def group $id #$name")
        params.forEach {
            sb.append("\n")
            sb.append(it.getCommandLines())
        }
        return sb.toString()
    }

    fun hasParam(id: String): Boolean {
        return params.any { it.id == id }
    }
}