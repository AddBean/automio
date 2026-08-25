// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.extensions.getTreeId
import com.hive.script.extensions.isUnReachable
import com.hive.script.extensions.traverseCommand
import com.hive.script.extensions.updateAllParent
import com.hive.script.views.edit.xeditor.XCellLayout
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import java.util.Collections

object XEditorHelper {

    private val Max_Wait_Time = 1 * 60 * 1000

    val cellVerPadding = 30f * GlobalApp.DP
    val cellHevPadding = 30f * GlobalApp.DP
    val cellWidth = if (ScriptConst.compatWideScreen()) 240f * GlobalApp.DP else 220f * GlobalApp.DP
    val cellHeight = 120f * GlobalApp.DP
    val arrIconSize = 6f * GlobalApp.DP

    val addIconSize = 14f * GlobalApp.DP

    val addIconSelectedSize = 30f * GlobalApp.DP

    var editMode = false

    private var cellLayout: XCellLayout? = null

    private var mCellList: List<XCellModel>? = null

    private var isStopFlag = false

    fun getCellList(
        cmd: ScriptCommand
    ): List<XCellModel> {
        return Collections.synchronizedList(parseScriptCommand(cmd))
    }

    /**
     * 解析ScriptCommand，并生成cell
     */
    @Synchronized
    private fun parseScriptCommand(
        root: ScriptCommand
    ): List<XCellModel> {
        try {
            isStopFlag = false
            val cells = mutableListOf<XCellModel>()
            root.updateAllParent()
            //遍历所有的command，生成cell
            root.traverseCommand { cmd ->
                if (cmd != root) {
                    cells.add(XCellModel().apply {
                        this.cmd = cmd
                        val cache = cellLayout?.getPosition(cmd.getTreeId())
                        xIndex = cache?.x ?: 0
                        yIndex = cache?.y ?: 0
                        this.indexInQueue = cmd.parentCommand?.commandQueue?.indexOf(cmd) ?: -1
                    })
                }
            }

            //找到无法被执行到的命令
            findUnTouchedCmd(root, cells)

            //为每个cell设置parentCell和nextCell
            fillDefaultCell(cells)

            //为每个cell设置childList
            fillChildList(cells)

            //补足parentCell为空的情况
            fillParentCell(cells)

            //补足prevCell为空的情况
            fillPrevCell(cells)

            //补足nextCall为空的情况,如果nextCell为空，则循环指向parentCell的nextCell
            fillNextCell(cells)

            //如果有冲突，则需要重新计算位置
            if (hasCollision(cells)) {
                //根据在树中的深度来计算xIndex，此处算出来可能有很多位置冲突重叠
                calculateIndex(cells)
                //对齐根节点
                ailginRoot(cells)
                //先通过翻转处理冲突
                handleCollisionInFlip(cells)
                //再通过横向扩展XIndex来处理冲突
                handleCollisionInXIndex(cells)
                //清空之前的cell布局
                cellLayout?.clear()
            }

            //最后根据xIndex和yIndex来计算frame
            calculateFrame(cells)

            //记住cell的布局
            if (cellLayout == null) cellLayout = XCellLayout()
            cellLayout?.recordLayout(cells)
            mCellList = cells
            return cells
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun stopParse() {
        isStopFlag = true
    }

    fun resetCellLayout() {
        cellLayout?.clear()
    }

    fun setCellLayout(layout: XCellLayout?) {
        cellLayout = layout
    }

    fun snapCellLayout(): XCellLayout? {
        return cellLayout?.deepCopy()
    }

    fun putCellPosition(treeId: String?, x: Int, y: Int) {
        cellLayout?.putPosition(treeId, x, y)
    }

    fun getCellPosition(treeId: String?): Point? {
        return cellLayout?.getPosition(treeId)
    }

    /**
     * 找出新增的cell，并计算新位置并写入cellLayout
     */
    fun onScriptChanged(cmd: ScriptCommand?) {
        if (cellLayout == null) return
        val newCmds = mutableListOf<ScriptCommand?>()
        cmd?.forEachAllCommand {
            if (mCellList?.find { cell -> cell.cmd == it } == null) {
                if (it != cmd && cellLayout?.layouts?.contains(it.getTreeId()) == false) {
                    newCmds.add(it)
                }
            }
        }
        newCmds.forEach {
            val parent = it?.parentCommand
            if ((parent?.commandQueue?.size ?: 0) > 1) {
                insertToLines(parent)
            } else {
                parent?.run {
                    addFirstChild(parent)
                }
            }
        }
    }

    /**
     * 增加第一个子命令
     */
    private fun addFirstChild(parent: ScriptCommand) {
        val xMaxIndex = cellLayout?.getPosition(parent.getTreeId())?.x ?: 0
        val yMaxIndex = cellLayout?.getPosition(parent.getTreeId())?.y ?: 0
        if (cellLayout?.addPosition(
                parent.commandQueue.lastOrNull()?.getTreeId(),
                xMaxIndex + 1,
                yMaxIndex
            ) == false
        ) {
            cellLayout?.addPosition(
                parent.commandQueue.lastOrNull()?.getTreeId(),
                xMaxIndex - 1,
                yMaxIndex
            )
        }
    }

    /**
     * 插入到命令指令队列
     */
    private fun insertToLines(parent: ScriptCommand?) {
        val xMaxIndex =
            parent?.commandQueue?.maxOf { cellLayout?.getPosition(it.getTreeId())?.x ?: 0 }
                ?: 0

        val yMaxIndex =
            parent?.commandQueue?.maxOf { cellLayout?.getPosition(it.getTreeId())?.y ?: 0 }
                ?: 0
        if (cellLayout?.addPosition(
                parent?.commandQueue?.lastOrNull()?.getTreeId(),
                xMaxIndex,
                yMaxIndex + 1
            ) == false
        ) {
            cellLayout?.addPosition(
                parent?.commandQueue?.lastOrNull()?.getTreeId(),
                xMaxIndex + 1,
                yMaxIndex
            )
        }
    }

    private fun hasCollision(cells: List<XCellModel>?): Boolean {
        val midCollision = findIndexCollision(cells, null) != null
        val rightCollision = findIndexCollision(cells, false) != null
        val leftCollision = findIndexCollision(cells, true) != null
        return leftCollision || rightCollision || midCollision
    }

    private fun setCellXIndex(cell: XCellModel, xIndex: Int) {
        cell.xIndex = xIndex
    }

    private fun setCellYIndex(cell: XCellModel, yIndex: Int) {
        cell.yIndex = yIndex
    }

    /**
     * 找到无法被执行到的命令
     */
    private fun findUnTouchedCmd(root: ScriptCommand, cells: List<XCellModel>) {
        root.forEachAllCommand {
            it.unReachable = it.isUnReachable()
        }
        cells.forEach {
            it.isDisabled = it.cmd?.unReachable == true
        }
    }


    /**
     * 补足默认的cell,为每个cell设置parentCell和nextCell
     */
    private fun fillDefaultCell(cells: List<XCellModel>) {
        cells.forEach {
            it.parentCell = cells.find { cell -> cell.cmd == it.cmd?.parentCommand }
            val parentQueue = it.cmd?.parentCommand?.commandQueue
            val index = parentQueue?.indexOf(it.cmd) ?: -1
            val targetCmd = parentQueue?.getOrNull(index + 1)
            if (targetCmd != null) {
                it.nextCell = cells.find { cell ->
                    if (index != -1) {
                        cell.cmd == targetCmd
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * 为每个cell设置childList
     */
    private fun fillChildList(cells: List<XCellModel>) {
        cells.forEach {
            it.childList = cells.filter { cell ->
                cell.cmd?.parentCommand == it.cmd
            }
        }
    }

    /**
     * 补足nextCall为空的情况,如果nextCell为空，则循环指向parentCell的nextCell
     */
    private fun fillNextCell(cells: List<XCellModel>) {
        cells.forEach {
            if (it.nextCell == null) {
                it.nextCell = it.findNextCell()
            }
        }
    }


    /**
     * 补足parentCell为空的情况
     */
    private fun fillParentCell(cells: List<XCellModel>) {
        cells.forEach {
            if (it.parentCell == null) {
                it.parentCell = cells.find { cell ->
                    cell.childList?.contains(it) == true
                }
            }
        }
    }

    /**
     * 补足prevCell为空的情况
     */
    private fun fillPrevCell(cells: List<XCellModel>) {
        cells.forEach {
            it.prevCell = cells.find { cell ->
                cell.nextCell == it
            } ?: it.parentCell
        }
    }

    /**
     * 根据在树中的深度来计算xIndex，此处算出来可能有很多位置冲突重叠
     */
    private fun calculateIndex(cells: List<XCellModel>) {
        cells.forEach {
            setCellXIndex(it, calculateXIndex(it.cmd!!) - 1)
            setCellYIndex(it, calculateYIndex(it.cmd!!))
        }
    }

    /**
     * 根据xIndex和yIndex来计算frame
     */
    private fun calculateFrame(cells: List<XCellModel>) {
        cells.forEach {
            it.frame = RectF(
                it.xIndex * cellWidth,
                it.yIndex * cellHeight,
                (it.xIndex + 1) * cellWidth,
                (it.yIndex + 1) * cellHeight
            )
        }
    }

    /**
     * 对齐根节点，即将根节点的xIndex设置为0
     */
    private fun ailginRoot(cells: List<XCellModel>) {
        cells.takeIf { it.isNotEmpty() } ?: return
        if (cells.find { it.xIndex == 0 } == null) {
            val minX = cells.minOf { it.xIndex } ?: 0
            cells.forEach {
                setCellXIndex(it, it.xIndex - minX)
            }
        }
        if (cells.find { it.yIndex == 0 } == null) {
            val minY = cells.minOf { it.yIndex } ?: 0
            cells.forEach {
                setCellYIndex(it, it.yIndex - minY)
            }
        }
    }

    /**
     * 遇到冲突则翻转，应该能解决大多数冲突，如果不行后面继续做兜底解决
     */
    private fun handleCollisionInFlip(cells: List<XCellModel>) {
        val collisionHandledSet = mutableSetOf<XCellModel>()
        for (i in 0 until 2) {
            collisionHandledSet.clear()
            var collision = findIndexCollision2(cells)
            while (collision?.parentCell != null && collisionHandledSet.contains(
                    collision.parentCell
                ).not()
            ) {
                flipCellTreeXIndex(collision.parentCell)
                collision.parentCell?.run {
                    collisionHandledSet.add(this)
                }
                collision = findIndexCollision2(cells)
                if (isStopFlag) {
                    throw RuntimeException("stop")
                }
            }
        }
        cells.forEach {
            if (it.xIndex < 0 && (it.parentCell?.xIndex ?: 0) > 0) {
                flipCellTreeXIndex(it)
            } else if (it.xIndex > 0 && (it.parentCell?.xIndex ?: 0) < 0) {
                flipCellTreeXIndex(it)
            }
        }
    }


    private fun findMaxYNode(list: List<XCellModel>?, xIndex: Int): XCellModel? {
        list ?: return null
        val maxCell = list.filter { it.xIndex == xIndex && it.childList?.isNotEmpty() == true }
            .maxByOrNull { it.yIndex } ?: return list.firstOrNull()?.parentCell
        return findMaxYNode(maxCell?.childList, xIndex + 1)
    }

    /**
     * 遇到冲突则增加XIndex，直到没有冲突,如果计算超过5s，则强制退出
     */
    private fun handleCollisionInXIndex(ls: List<XCellModel>?) {
        val startTime = System.currentTimeMillis()
        val listRight = ls?.filter { it.xIndex > 0 }
        var cell = findIndexCollision(listRight, false)
        val step = 1
        var count = 0
        while (cell != null) {
            if (isStopFlag) {
                throw RuntimeException("stop")
            }
            addCellTreeXIndex(cell, step)
            cell = findIndexCollision(listRight, false)
            DLog.d("XEditorHelper", "find collision cell:$cell")
            count++
            if (System.currentTimeMillis() - startTime > Max_Wait_Time) {
                return
            }
        }

        val listLeft = ls?.filter { it.xIndex < 0 }
        cell = findIndexCollision(listLeft, true)
        while (cell != null) {
            if (isStopFlag) {
                throw RuntimeException("stop")
            }
            decCellTreeXIndex(cell, step)
            cell = findIndexCollision(listLeft, true)
            DLog.d("XEditorHelper", "find collision cell:$cell")
            count++
            if (System.currentTimeMillis() - startTime > Max_Wait_Time) {
                return
            }
        }
        DLog.d("XEditorHelper", "handleCollisionInXIndex count:$count")
    }


    private fun maxTreeXIndex(cell: XCellModel?): Int {
        cell ?: return 0
        var result = cell.xIndex
        cell.childList?.filter { it.xIndex > 0 }?.forEach {
            result = Math.max(result, maxTreeXIndex(it))
        }
        return result
    }


    private fun decCellTreeXIndex(cell: XCellModel?, step: Int) {
        cell ?: return
        cell.childList?.forEach {
            if (it.xIndex < 0) {
                setCellXIndex(it, it.xIndex - step)
            }
        }
        cell.childList?.forEach {
            decCellTreeXIndex(it, step)
        }
    }

    private fun addCellTreeXIndex(cell: XCellModel?, step: Int) {
        cell ?: return
        cell.childList?.forEach {
            if (it.xIndex > 0) {
                setCellXIndex(it, it.xIndex + step)
            }
        }
        cell.childList?.forEach {
            addCellTreeXIndex(it, step)
        }
    }

    /**
     * 找到冲突的cell
     * @param cells
     * @param isLeft 是否是左边冲突,否则是右边冲突,xIndex小于0为左边，大于0为右边,isLeft为null时为中间冲突
     */
    private fun findIndexCollision(
        cells: List<XCellModel>?,
        isLeft: Boolean?
    ): XCellModel? {
        cells ?: return null
        if (isStopFlag) {
            throw RuntimeException("stop")
        }
        if (isLeft == null) {
            return findIndexCollision2(cells.filter { it.xIndex == 0 })
        }

        val positionMap = mutableMapOf<Pair<Int, Int>, XCellModel>()

        var collision2: XCellModel? = null

        val collision1 = cells.find { cell ->
            val position = cell.xIndex to cell.yIndex
            if (positionMap.containsKey(position)) {
                collision2 = positionMap[position]
                true
            } else {
                positionMap[position] = cell
                false
            }
        }

        collision1 ?: return null
        return if ((collision1.parentCell?.yIndex ?: 0) < (collision2?.parentCell?.yIndex ?: 0)) {
            if (isLeft == true) {
                collision2?.parentCell ?: collision1.parentCell
            } else {
                collision1.parentCell ?: collision2?.parentCell
            }
        } else {
            if (isLeft == false) {
                collision2?.parentCell ?: collision1.parentCell
            } else {
                collision1.parentCell ?: collision2?.parentCell
            }
        }
    }

    private fun findIndexCollision2(list: List<XCellModel>?): XCellModel? {
        list ?: return null
        if (isStopFlag) {
            throw RuntimeException("stop")
        }
        var cells = list.sortedByDescending { it.yIndex }
        cells = cells.sortedBy { it.xIndex }
        val collision1 = cells.find { cell ->
            cells.any { it != cell && it.xIndex == cell.xIndex && it.yIndex == cell.yIndex }
        }
        return collision1
    }

    private fun flipCellTreeXIndex(cell: XCellModel?) {
        cell ?: return
        if (isStopFlag) {
            throw RuntimeException("stop")
        }
        setCellXIndex(cell, -cell.xIndex)
        cell.childList?.forEach {
            flipCellTreeXIndex(it)
        }
    }


    private fun calculateXIndex(cmd: ScriptCommand, isLeft: Boolean = false): Int {
        var result = 0
        var parent = cmd.parentCommand
        while (parent != null) {
            if (isLeft) {
                result--
            } else {
                result++
            }

            parent = parent.parentCommand
        }
        return result
    }

    /**
     * 从根节点开始计算，根据cmd在parentCommand中commandQueue位置来计算其yIndex，即cmd在树中的深度(此深度包含父级的深度)
     */
    private fun calculateYIndex(cmd: ScriptCommand?): Int {
        var result = 0
        var parent = cmd?.parentCommand
        var current = cmd
        while (parent != null) {
            val index = parent.commandQueue.indexOf(current)
            if (index != -1) {
                result += index
            }
            current = current?.parentCommand
            parent = current?.parentCommand
        }
        return result
    }


    fun clipString(text: String, paint: Paint, width: Float): String {
        var w = paint.measureText(text)
        if (w <= width) {
            return text
        }
        var i = 0
        while (i < text.length) {
            w = paint.measureText(text.substring(0, i))
            if (w > width) {
                return text.substring(0, i - 1) + "..."
            }
            i++
        }
        return text
    }

}