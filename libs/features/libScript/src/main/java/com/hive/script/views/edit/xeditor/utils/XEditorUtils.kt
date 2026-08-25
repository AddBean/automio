// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.Point
import android.graphics.RectF
import com.hive.script.setting.ScriptSetting
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.nodes.XCommandNode
import com.hive.script.views.edit.xeditor.nodes.XLineNode

object XEditorUtils {

    /**
     * 查找目标连接点，每个cell的frameRect 4边共12个连接点，每个边分别三个连接点，根据起始cell和终点cell的xIndex、yIndex的位置，确定终点cell的连接点
     */
    fun findTargetConnectPoint(
        start: XCellModel, end: XCellModel, posIndex: Int, margin: Int = 0
    ): Point {
        val endFrame = RectF(end.frame)
        endFrame.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
        var pos = posIndex
        if (posIndex == 0) {
            pos = findPositionInCell(start, end)
        }
        return when (pos) {
            1 -> Point(
                (endFrame.left + endFrame.width() * 3 / 4).toInt(),
                endFrame.top.toInt() - margin
            )

            2 -> Point(
                endFrame.centerX().toInt(), endFrame.top.toInt() - margin
            )


            3 -> Point(
                (endFrame.left + endFrame.width() * 1 / 4).toInt(),
                endFrame.top.toInt() - margin
            )

            4 -> Point(
                endFrame.right.toInt() + margin, endFrame.centerY().toInt()
            )

            6 -> Point(
                endFrame.left.toInt() - margin, endFrame.centerY().toInt()
            )

            7 -> Point(
                (endFrame.left + endFrame.width() * 3 / 4).toInt(),
                endFrame.bottom.toInt() + margin
            )

            8 -> Point(
                endFrame.centerX().toInt(), endFrame.bottom.toInt() + margin
            )

            9 -> Point(
                (endFrame.left + endFrame.width() * 1 / 4).toInt(),
                endFrame.bottom.toInt() + margin
            )

            else -> Point(0, 0)
        }
    }

    /**
     * 计算折叠按钮在 group 上的放置点，与「第一个子 → group」连接线终点一致（含冲突避让后的位置）。
     * @param groupCell 有子节点的 group cell
     * @param lineNodes 当前已添加且已做过冲突优化的 XLineNode 列表，可为 null
     */
    fun getOperateNodeConnectPoint(groupCell: XCellModel, lineNodes: List<XLineNode>?): Point {
        val firstChild = groupCell.childList?.firstOrNull() ?: return Point(0, 0)
        val endPos = lineNodes?.find { it.startCell == firstChild && it.endCell == groupCell }?.endPos
            ?: 0
        val pos = if (endPos in 1..9) endPos else findPositionInCell(firstChild, groupCell)
        return findTargetConnectPoint(firstChild, groupCell, pos)
    }

    fun findPositionInCell(start: XCellModel, end: XCellModel): Int {

        val xIndex = when (end.xIndex - start.xIndex) {
            0 -> 0
            else -> if (end.xIndex - start.xIndex > 0) 1 else -1
        }
        val yIndex = when (end.yIndex - start.yIndex) {
            0 -> 0
            else -> if (end.yIndex - start.yIndex > 0) 1 else -1
        }

        /**
         * 通过paddingStep来调节xIndex,yIndex的值，按顺序编号1-9
         * -1,1     0,1     1,1
         * -1,0     0,0     1,0
         * -1,-1    0,-1    1,-1
         */

        val pos = when {
            -1 == xIndex && 1 == yIndex -> 1
            0 == xIndex && 1 == yIndex -> 2
            1 == xIndex && 1 == yIndex -> 3
            -1 == xIndex && 0 == yIndex -> 4
            1 == xIndex && 0 == yIndex -> 6
            -1 == xIndex && -1 == yIndex -> 7
            0 == xIndex && -1 == yIndex -> 8
            1 == xIndex && -1 == yIndex -> 9
            else -> 0
        }
        return pos
    }

    fun getEndArrPath(
        end: XCellModel, posIndex: Int, arrSize: Int
    ): Path {
        val endFrame = RectF(end.frame)
        endFrame.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
        when (posIndex) {
            1 -> {
                //起始cell在终点cell的右上方,终点cell的连接点在上边框3/4处,箭头在终点cell的上边框,并且向上和上边框相接
                return Path().apply {
                    moveTo(endFrame.left + endFrame.width() * 3 / 4, endFrame.top)
                    lineTo(
                        endFrame.left + endFrame.width() * 3 / 4 - arrSize, endFrame.top - arrSize
                    )
                    lineTo(
                        endFrame.left + endFrame.width() * 3 / 4 + arrSize, endFrame.top - arrSize
                    )
                }

            }


            2 -> {
                //起始cell在终点cell的上方,终点cell的连接点在上边框1/2处,箭头在终点cell的上边框,并且向上和上边框相接
                return Path().apply {
                    moveTo(endFrame.centerX(), endFrame.top)
                    lineTo(endFrame.centerX() - arrSize, endFrame.top - arrSize)
                    lineTo(endFrame.centerX() + arrSize, endFrame.top - arrSize)
                }

            }

            3 -> {
                //起始cell在终点cell的左上方,终点cell的连接点在上边框1/4处,箭头在终点cell的上边框,并且向下和上边框相接
                return Path().apply {
                    moveTo(endFrame.left + endFrame.width() * 1 / 4, endFrame.top)
                    lineTo(
                        endFrame.left + endFrame.width() * 1 / 4 - arrSize, endFrame.top - arrSize
                    )
                    lineTo(
                        endFrame.left + endFrame.width() * 1 / 4 + arrSize, endFrame.top - arrSize
                    )
                }

            }

            4 -> {
                //起始cell在终点cell的右方,终点cell的连接点在右边框1/2处,箭头在终点cell的右边框,并且向左和右边框相接
                return Path().apply {
                    moveTo(endFrame.right, endFrame.centerY())
                    lineTo(endFrame.right + arrSize, endFrame.centerY() - arrSize)
                    lineTo(endFrame.right + arrSize, endFrame.centerY() + arrSize)
                }

            }

            6 -> {
                //起始cell在终点cell的左方,终点cell的连接点在左边框1/2处,箭头在终点cell的左边框,并且向右和左边框相接
                return Path().apply {
                    moveTo(endFrame.left, endFrame.centerY())
                    lineTo(endFrame.left - arrSize, endFrame.centerY() - arrSize)
                    lineTo(endFrame.left - arrSize, endFrame.centerY() + arrSize)
                }

            }

            7 -> {
                //起始cell在终点cell的右下方,终点cell的连接点在下边框3/4处,箭头在终点cell的下边框,并且向上和下边框相接
                return Path().apply {
                    moveTo(endFrame.left + endFrame.width() * 3 / 4, endFrame.bottom)
                    lineTo(
                        endFrame.left + endFrame.width() * 3 / 4 - arrSize,
                        endFrame.bottom + arrSize
                    )
                    lineTo(
                        endFrame.left + endFrame.width() * 3 / 4 + arrSize,
                        endFrame.bottom + arrSize
                    )
                }

            }

            8 -> {
                //起始cell在终点cell的下方,终点cell的连接点在下边框1/2处,箭头在终点cell的下边框,并且向上和下边框相接
                return Path().apply {
                    moveTo(endFrame.centerX(), endFrame.bottom)
                    lineTo(endFrame.centerX() - arrSize, endFrame.bottom + arrSize)
                    lineTo(endFrame.centerX() + arrSize, endFrame.bottom + arrSize)
                }

            }

            9 -> {
                //起始cell在终点cell的左下方,终点cell的连接点在下边框1/4处,箭头在终点cell的下边框,并且向下和下边框相接
                return Path().apply {
                    moveTo(endFrame.left + endFrame.width() * 1 / 4, endFrame.bottom)
                    lineTo(
                        endFrame.left + endFrame.width() * 1 / 4 - arrSize,
                        endFrame.bottom + arrSize
                    )
                    lineTo(
                        endFrame.left + endFrame.width() * 1 / 4 + arrSize,
                        endFrame.bottom + arrSize
                    )
                }

            }

        }
        return Path()
    }

    private fun createPath(start: Point, mid: Point, end: Point): Path {
        return Path().apply {
            moveTo(start.x.toFloat(), start.y.toFloat())
            lineTo(end.x.toFloat(), end.y.toFloat())
        }
    }

    /**
     * 查找cell旁边的空cell，创建个新的cell
     */
    fun findEmptyAroundCell(view: ScriptXEditorView, cell: XCellModel): XCellModel {
        val rightCell =
            view.getChildList().filterIsInstance<XCommandNode>()
                .find { it.cell.xIndex == cell.xIndex + 1 && it.cell.yIndex == cell.yIndex }
        if (rightCell == null) {
            val emptyCell = cell.copy().apply {
                xIndex = cell.xIndex + 1
                yIndex = cell.yIndex
                frame = RectF(cell.frame).apply {
                    left += XEditorHelper.cellWidth
                    right += XEditorHelper.cellWidth
                }

            }
            return emptyCell
        }

        val leftCell =
            view.getChildList().filterIsInstance<XCommandNode>()
                .find { it.cell.xIndex == cell.xIndex - 1 && it.cell.yIndex == cell.yIndex }
        if (leftCell == null) {
            val emptyCell = cell.copy().apply {
                xIndex = cell.xIndex - 1
                yIndex = cell.yIndex
                frame = RectF(cell.frame).apply {
                    left -= XEditorHelper.cellWidth
                    right -= XEditorHelper.cellWidth
                }
            }
            return emptyCell
        }


        val topCell =
            view.getChildList().filterIsInstance<XCommandNode>()
                .find { it.cell.xIndex == cell.xIndex && it.cell.yIndex == cell.yIndex - 1 }
        if (topCell == null) {
            val emptyCell = cell.copy().apply {
                xIndex = cell.xIndex
                yIndex = cell.yIndex - 1
                frame = RectF(cell.frame).apply {
                    top -= XEditorHelper.cellHeight
                    bottom -= XEditorHelper.cellHeight
                }
            }
            return emptyCell
        }

        val bottomCell =
            view.getChildList().filterIsInstance<XCommandNode>()
                .find { it.cell.xIndex == cell.xIndex && it.cell.yIndex == cell.yIndex + 1 }
        if (bottomCell == null) {
            val emptyCell = cell.copy().apply {
                xIndex = cell.xIndex
                yIndex = cell.yIndex + 1
                frame = RectF(cell.frame).apply {
                    top += XEditorHelper.cellHeight
                    bottom += XEditorHelper.cellHeight
                }
            }
            return emptyCell
        }
        return cell.copy().apply {
            xIndex = cell.xIndex + 1
            yIndex = cell.yIndex
            frame = RectF(cell.frame).apply {
                left += XEditorHelper.cellWidth
                right += XEditorHelper.cellWidth
            }
        }
    }

    fun getLinePath(start: Point, end: Point): Path {
        return Path().apply {
            moveTo(start.x.toFloat(), start.y.toFloat())
            lineTo(end.x.toFloat(), end.y.toFloat())
        }
    }


    fun getLinePath(
        start: Point,
        end: Point,
        mid1: Point,
        mid2: Point,
        startPos: Int,
        endPos: Int
    ): Path {
        if ((startPos == 3 && endPos == 4) ||
            (startPos == 1 && endPos == 6)
        ) {
            val dx = mid1.x - mid2.x
            mid1.x += (-dx / 3)
            mid2.x -= (-dx / 3)
//            return Path().apply {
//                moveTo(start.x.toFloat(), start.y.toFloat())
//                lineTo(mid1.x.toFloat(), mid1.y.toFloat())
//                lineTo(mid2.x.toFloat(), mid2.y.toFloat())
//                lineTo(end.x.toFloat(), end.y.toFloat())
//            }
        }
        if (ScriptSetting.script_setting_editor_bizer_enable) {//是否启用贝塞尔曲线
            //要过mid1和mid2中点的贝塞尔曲线·
            return Path().apply {
                moveTo(start.x.toFloat(), start.y.toFloat())
                cubicTo(
                    mid1.x.toFloat(), mid1.y.toFloat(),
                    mid2.x.toFloat(), mid2.y.toFloat(),
                    end.x.toFloat(), end.y.toFloat()
                )
            }
        } else {
            return Path().apply {
                moveTo(start.x.toFloat(), start.y.toFloat())
                lineTo(mid1.x.toFloat(), mid1.y.toFloat())
                lineTo(mid2.x.toFloat(), mid2.y.toFloat())
                lineTo(end.x.toFloat(), end.y.toFloat())
            }
        }

    }

    class NodeConnectPoint(
        val point: Point,
        val isStart: Boolean,
        val node: XLineNode
    )

    /**
     * 找出所有冲突的连接点,遍历比较lineNode.startConnectPoint和lineNode.endConnectPoint，如果有冲突，返回冲突的连接点
     */
    fun findAllConflictConnectPoints(
        lineNodes: List<XLineNode>?
    ): List<Pair<NodeConnectPoint, NodeConnectPoint>> {
        val conflictList = mutableListOf<Pair<NodeConnectPoint, NodeConnectPoint>>()
        lineNodes?.forEach { lineNode ->
            val startConnectPoint = lineNode.startPoint
            lineNodes.forEach {
                if (startConnectPoint == it.endPoint) {
                    conflictList.add(
                        Pair(
                            NodeConnectPoint(lineNode.startPoint, true, lineNode),
                            NodeConnectPoint(it.endPoint, false, it)
                        )
                    )
                }
            }
        }
//        DLog.e(
//            GsonHelper.getInstance().toFormatJson(lineNodes?.map { it.startPoint to it.endPoint })
//        )
        return conflictList
    }

    /**
     * 两个冲突的连接点，调整位置，使其不重叠
     */
    fun justifyConflictConnectPoints(listConflict: List<Pair<NodeConnectPoint, NodeConnectPoint>>) {
        listConflict.forEach {
            val endPos = it.second.node.endPos
            val isClockwise = it.second.node.startCell.xIndex >= it.first.node.startCell.xIndex
            var targetPos = endPos
            if (isClockwise) {
                targetPos = when (endPos) {
                    1 -> 2
                    2 -> 3
                    3 -> 6
                    4 -> 1
                    6 -> 9
                    7 -> 4
                    8 -> 7
                    9 -> 8
                    else -> endPos
                }
            } else {
                targetPos = when (endPos) {
                    1 -> 4
                    2 -> 1
                    3 -> 2
                    4 -> 7
                    6 -> 3
                    7 -> 8
                    8 -> 9
                    9 -> 6
                    else -> endPos
                }
            }

            it.second.node.endPos = targetPos
            it.second.node.updateLine()
        }
    }

    fun getDottedPathEffect(): PathEffect {
        return DashPathEffect(floatArrayOf(4f, 6f), 0f)
    }
}