// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import com.hive.script.base.ScriptCommand
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView

class XEditorAnimHelper {

    private var lastCmdCacheSet = mutableSetOf<ScriptCommand>()

    @Synchronized
    private fun setLastCmdCache(cmd: ScriptCommand) {
        cmd.forEachAllCommand { lastCmdCacheSet.add(it) }
    }

    @Synchronized
    fun doAnim(cellList: List<SCAbsLayerItemView>?, cmd: ScriptCommand, startAnim: Boolean) {
        if ((cellList?.size ?: 0) > 2000) return
        val list = mutableSetOf<ScriptCommand>()
        cmd.forEachAllCommand { list.add(it) }
        val animCmds = getNewCmdList(list.toList())
        if (startAnim && animCmds.isNotEmpty()) {
            cellList?.filter { animCmds.contains(it.getMainCell()?.cmd) }?.forEach {
                it.startAnim(300f)
            }
        }
        setLastCmdCache(cmd)
    }

    @Synchronized
    private fun getNewCmdList(newList: List<ScriptCommand>): List<ScriptCommand> {
        if (lastCmdCacheSet.isEmpty()) {
            return mutableListOf()
        }
        val newCmdList = mutableListOf<ScriptCommand>()
        newList.forEach {
            if (!lastCmdCacheSet.contains(it)) {
                newCmdList.add(it)
            }
        }
        return newCmdList
    }
}