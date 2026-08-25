// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor

import android.graphics.RectF
import com.hive.annotation.NotProguard
import com.hive.script.base.ScriptCommand

@NotProguard
class XCellModel {

    var xIndex = 0

    var yIndex = 0

    var frame = RectF()

    var prevCell: XCellModel? = null

    var nextCell: XCellModel? = null

    var parentCell: XCellModel? = null

    var childList: List<XCellModel>? = null

    var cmd: ScriptCommand? = null

    var indexInQueue = 0

    var isInTouchMove = false

    var isDisabled = false


    fun copy(): XCellModel {
        val cell = XCellModel()
        cell.xIndex = xIndex
        cell.yIndex = yIndex
        cell.frame = RectF(frame)
        cell.prevCell = prevCell
        cell.nextCell = nextCell
        cell.parentCell = parentCell
        cell.childList = childList
        cell.cmd = cmd
        cell.indexInQueue = indexInQueue
        cell.isInTouchMove = isInTouchMove
        cell.isDisabled = isDisabled
        return cell
    }

    fun traverseCell(
        cmd: XCellModel? = null, innerLoop: Boolean = false, callback: (cmd: XCellModel) -> Unit
    ) {
        val targetCmd = cmd ?: if (innerLoop) null else this
        targetCmd?.run {
            callback.invoke(targetCmd)
        }
        if (targetCmd?.childList?.isNotEmpty() == true) {
            repeat(targetCmd.childList!!.size) {
                traverseCell(targetCmd.childList!![it], true, callback)
            }
        }
    }

    /**
     * 查找next cell,如果当前cell是最后一个cell,则查找parent cell的next cell
     */
    fun findNextCell(): XCellModel? {
        var currentCell: XCellModel? = this
        while (currentCell != null) {
            if (currentCell.nextCell != null) {
                return currentCell.nextCell
            }
            currentCell = currentCell.parentCell
        }
        return null
    }

    fun getIndexInQueue(): Int? {
        val parentQueue = parentCell?.childList
        parentQueue ?: return null
        return parentQueue.indexOf(this)
    }

    fun isLastCell(): Boolean {
        val parentQueue = parentCell?.childList
        parentQueue ?: return false
        val index = parentQueue.indexOf(this)
        return index == parentQueue.size - 1
    }

    override fun toString(): String {
        return "XCellModel(xIndex=$xIndex, yIndex=$yIndex,isDisabled=$isDisabled)"
    }


}