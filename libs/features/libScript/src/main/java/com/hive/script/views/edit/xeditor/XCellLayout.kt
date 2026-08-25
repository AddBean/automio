// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor

import android.graphics.Point
import com.hive.script.base.ScriptCommand
import com.hive.script.extensions.getTreeId
import com.hive.utils.debug.DLog
import com.hive.utils.utils.GsonHelper

class XCellLayout {

    var layouts = mutableMapOf<String, Point>()

    fun getPosition(treeId: String?): Point? {
        treeId ?: return null
        return layouts[treeId]
    }

    fun recordLayout(cells: MutableList<XCellModel>) {
        val setIds = HashMap<String, ScriptCommand>()
        cells.forEach {
            if (setIds.contains(it.cmd?.getTreeId())) {
                DLog.e("********* 出错：重复的id:${it.cmd?.getTreeId()} *********")
                DLog.e("              ：重复的命令:${setIds[it.cmd?.getTreeId()]?.getCommandName()}  ${it.cmd?.getCommandName()}")
            }
            if (it.cmd != null)
                setIds[it.cmd?.getTreeId() ?: ""] = it.cmd!!
            layouts[it.cmd?.getTreeId() ?: ""] = Point(it.xIndex, it.yIndex)
        }
    }

    fun putPosition(treeId: String?, xIndex: Int, yIndex: Int) {
        treeId ?: return
        layouts[treeId ?: ""] = Point(xIndex, yIndex)
    }

    /**
     * 如果已有xIndex、yIndex相同坐标则返回false
     */
    fun addPosition(treeId: String?, xIndex: Int, yIndex: Int): Boolean {
        treeId ?: return false
        val point = Point(xIndex, yIndex)
        return if (layouts.values.count { it == point } >= 1) {
            false
        } else {
            layouts[treeId ?: ""] = point
            true
        }
    }

    fun clear() {
        layouts.clear()
    }

    fun deepCopy(): XCellLayout {
        val layout = XCellLayout()
        layout.layouts = layouts.toMutableMap()
        return layout
    }

    fun getJson(): String {
        return GsonHelper.getInstance().toJson(this)
    }
}