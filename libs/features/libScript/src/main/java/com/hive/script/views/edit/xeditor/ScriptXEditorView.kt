// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.blankj.utilcode.util.VibrateUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.extensions.count
import com.hive.script.extensions.getTreeId
import com.hive.script.extensions.isLastCommand
import com.hive.script.extensions.isParent
import com.hive.script.extensions.updateParent
import com.hive.script.views.dialog.DialogScriptLoading
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.SCEditOperateView
import com.hive.script.views.edit.xeditor.nodes.XCommandDelayNode
import com.hive.script.views.edit.xeditor.nodes.XCommandEndNode
import com.hive.script.views.edit.xeditor.nodes.XCommandNode
import com.hive.script.views.edit.xeditor.nodes.XCommandOperateNode
import com.hive.script.views.edit.xeditor.nodes.XLineNode
import com.hive.script.views.edit.xeditor.nodes.XStartEndNode
import com.hive.script.views.edit.xeditor.utils.XEditorAnimHelper
import com.hive.script.views.edit.xeditor.utils.XEditorDrawHelper
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.script.views.edit.xeditor.utils.XEditorSnapHelper
import com.hive.script.views.edit.xeditor.utils.XEditorSnapManager
import com.hive.script.views.edit.xeditor.utils.XEditorUtils
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.extends.dp
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.synchronizedList
import java.util.Collections.synchronizedMap
import kotlin.math.pow
import kotlin.math.sqrt


open class ScriptXEditorView(context: Context?, attrs: AttributeSet?) :
    SCEditOperateView(context, attrs) {

    private var dialogLoading: DialogScriptLoading? = null

    var dialogView: DialogScriptEdit? = null

    private val snapHelper = XEditorSnapHelper()

    private var numberColumn: Int = -1

    private var numberRow: Int = -1

    private var cellList: List<XCellModel>? = null

    private var currentCommand: ScriptCommand? = null

    private var currentMoveNode: XCommandNode? = null

    private var startTouchX = 0f

    private var startTouchY = 0f

    private val callsIntouch = synchronizedList(mutableListOf<XCellModel>())

    private var nodeInTouch = synchronizedList(mutableListOf<SCAbsLayerItemView>())

    private var nodeNotInTouch = synchronizedList(mutableListOf<SCAbsLayerItemView>())

    private var targetSelectCell: SCAbsLayerItemView? = null

    private val Min_Distance_To_Select = 80 * GlobalApp.DP

    private var isInDeleteMode: Boolean = false

    private val operateNodeMap = synchronizedMap(mutableMapOf<ScriptCommand, XCellOperateModel?>())

    private val animHelper = XEditorAnimHelper()

    private var Max_Command_Count_Show_Loading = 1000

    private var targetPosition: Point? = null

    private val enableFreePin = true //EngineerHelper.getSwitcherValue("script_move_pin") == true

    var isLoading = false

    var anchorView: View? = null


    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(command: ScriptCommand, anim: Boolean, onFinish: (() -> Unit)? = null) {
        isLoading = true
        GlobalScope.launch(Dispatchers.Main) {
            currentCommand = command
            val count = command.count()
            if (count > Max_Command_Count_Show_Loading) {
                dialogLoading = DialogScriptLoading(ScriptProvider.getViewContext())
                dialogLoading?.setMessage(GlobalApp.getString(com.hive.i8n.R.string.sc_to_many_command_tip))
                    ?.show()
            } else {
                dialogLoading = null
            }
            withContext(Dispatchers.IO) {
                var startTime = System.currentTimeMillis()
                cellList = XEditorHelper.getCellList(
                    command
                )
                DLog.e(
                    "ScriptXEditorView",
                    "getCellList Cost Time:${System.currentTimeMillis() - startTime}ms"
                )
                cellList ?: return@withContext null
                if (cellList?.isNotEmpty() == true) {
                    val p = Pair(cellList!!.maxOf { it.xIndex }, cellList!!.maxOf { it.yIndex })
                    numberColumn = p.first + 1
                    numberRow = p.second + 1
                    mVirtualEditLayerModel.mOriginRect.rt =
                        PointF(XEditorHelper.cellWidth * numberColumn, 0f)
                    mVirtualEditLayerModel.mOriginRect.rb = PointF(
                        XEditorHelper.cellWidth * numberColumn, XEditorHelper.cellHeight * numberRow
                    )
                    mVirtualEditLayerModel.mOriginRect.lb =
                        PointF(0f, XEditorHelper.cellHeight * numberRow)
                }
                startTime = System.currentTimeMillis()
                renderAllNodes()
                DLog.e(
                    "ScriptXEditorView",
                    "createAllNode Cost Time:${System.currentTimeMillis() - startTime}ms"
                )
                command.updateParent()
            }
            isLoading = false
            animHelper.doAnim(getChildList(), command, anim)
            changeOperateFoldState()
            postInvalidate()
            dialogLoading?.dismiss()
            onFinish?.invoke()
        }

    }

    @Synchronized
    fun notifyData() {
        currentCommand ?: return
        loadData(currentCommand!!, true)
    }

    fun autoLayout() {
        XEditorSnapManager.get().save(currentCommand)
        XEditorHelper.resetCellLayout()
        notifyData()
        CommonToast.show(com.hive.i8n.R.string.sc_auto_layout_success)
        dialogView?.updateUndoRedoStatus()
        dialogView?.setSaveEnable(true)
    }

    /**
     * 重新定位位置，以第一个节点为基准
     */
    @Synchronized
    fun resetLocation() {
        mVirtualEditLayerModel.mLayerTransX = 0f
        mVirtualEditLayerModel.mLayerTransY = 0f
        mVirtualEditLayerModel.mLayerScaleX = 1f
        mVirtualEditLayerModel.mLayerScaleY = 1f
        val paddingX = -width / 2f + XEditorHelper.cellWidth / 2f
        val paddingY = -XEditorHelper.cellHeight - 60f.dp
        cellList?.firstOrNull()?.let {
            mVirtualEditLayerModel.mLayerTransX =
                -it.xIndex * XEditorHelper.cellWidth - paddingX
            mVirtualEditLayerModel.mLayerTransY =
                -(it.yIndex - 1) * XEditorHelper.cellHeight
        } ?: run {
            mVirtualEditLayerModel.mLayerTransX = -paddingX
            mVirtualEditLayerModel.mLayerTransY = -paddingY
        }
        invalidate()
    }

    @Synchronized
    private fun renderAllNodes() {
        removeAllView()
        cellList?.filter { it.nextCell != null && !it.isLastCell() }?.forEach {
            addChildView(
                XLineNode.createLine(
                    this,
                    XLineNode.Companion.LineType.CMD_TO_CMD,
                    it,
                    it.nextCell!!
                )
            )
        }

        cellList?.filter { it.prevCell != null }?.forEach {
            if (it.prevCell?.nextCell != it) {
                addChildView(
                    XLineNode.createLine(
                        this,
                        XLineNode.Companion.LineType.GROUP_TO_CMD,
                        it,
                        it.prevCell!!
                    )
                )
            }
        }
        cellList?.forEach {
            when (it.cmd) {
                is CmdScriptStart -> {
                    val start = XStartEndNode.createStartNode(context, it)
                    addChildView(start)
                }

                is CmdScriptEnd -> {
                    val start = XStartEndNode.createEndNode(context, it)
                    addChildView(start)
                }

                else -> {
                    addChildView(XCommandNode.createNode(context, it))

                    if (it.cmd?.isLastCommand() == true) {
                        val end = XCommandEndNode.createNode(context, it)
                        addChildView(
                            XLineNode.createLine(
                                this,
                                XLineNode.Companion.LineType.CMD_TO_END,
                                it,
                                end.drawCell
                            )
                        )
                        addChildView(end)
                    }
                }
            }
        }

        optimizeLineNode()

        cellList?.forEach {
            if (it.cmd?.isSupportDelay() == true) {
                addChildView(XCommandDelayNode.create(context, it))
            }
        }

        cellList?.filter { it.childList?.isNotEmpty() == true }?.forEach {
            if (operateNodeMap[it.cmd] == null)
                operateNodeMap[it.cmd!!] = XCellOperateModel()
            val point = XEditorUtils.getOperateNodeConnectPoint(it, getChildList().filterIsInstance<XLineNode>())
            addChildView(XCommandOperateNode.create(context, it, operateNodeMap[it.cmd], point))
        }

        cellList?.filter { it.cmd?.isGroupCommand() == true && it.childList.isNullOrEmpty() }
            ?.forEach {
                addChildView(
                    XLineNode.createLine(
                        this, XLineNode.Companion.LineType.GROUP_EMPTY_TO_ADD, it, it
                    )
                )
            }

        requestReLayout()
    }

    /**
     * 优化线条节点
     */
    private fun optimizeLineNode() {
        val listConflict =
            XEditorUtils.findAllConflictConnectPoints(getChildList().filterIsInstance<XLineNode>())
        XEditorUtils.justifyConflictConnectPoints(listConflict)
    }

    @Synchronized
    override fun onItemEvent(itemView: SCAbsLayerItemView, eventData: Any?, eventData2: Any?) {
        if (!XEditorHelper.editMode) {
            return
        }
        when (eventData) {
            is ScriptMenuEditHelper.ClickType -> {
                val cmd = itemView.getMainCell()?.cmd
                cmd ?: return
                ScriptMenuEditHelper.handleMenuEdit(
                    context,
                    cmd,
                    null,
                    eventData,
                    ScriptMenuEditHelper.InsertType.INSERT_AFTER,
                    dialogView
                )
            }

            XEditorEvent.REFRESH_DATA -> {
                if (eventData2 != null && eventData2 is ScriptCommand) {
                    itemView.onDataRefresh(eventData2)
                }
                dialogView?.updateData()
            }

            XEditorEvent.TOUCH_MOVE -> {
                if (itemView is XCommandNode) {
                    startMove(itemView)
                }
            }

            XEditorEvent.OPERATE_CHANGE -> {
                changeOperateFoldState()
            }
        }
    }

    /**
     * 改变收起状态
     */
    @Synchronized
    private fun changeOperateFoldState() {
        getChildList().forEach {
            it.visibility = true
        }
        operateNodeMap.filter { it.value?.isFold == true }.map {
            it.key
        }.forEach { foldCell ->
            cellList?.map { it.cmd }?.forEach { cmd ->
                if (cmd?.isParent(foldCell) == true) {
                    getChildList().filter { it.getMainCell()?.cmd == cmd }.forEach {
                        it.visibility = false
                    }
                }
            }
        }
        invalidate()
    }

    /**
     * 折叠所有
     */
    @Synchronized
    fun foldAll(): Boolean {
        var hasUnFold = false
        operateNodeMap.forEach {
            if (it.value?.isFold == false) {
                hasUnFold = true
            }
            it.value?.isFold = true
        }
        changeOperateFoldState()
        return hasUnFold
    }

    /**
     * 展开所有
     */
    @Synchronized
    fun unfoldAll(): Boolean {
        var hasFold = false
        operateNodeMap.forEach {
            if (it.value?.isFold == true) {
                hasFold = true
            }
            it.value?.isFold = false
        }
        changeOperateFoldState()
        return hasFold
    }


    @Synchronized
    fun unfold(cmd: ScriptCommand) {
        operateNodeMap.forEach {
            if (it.key == cmd) {
                it.value?.isFold = false
            }
        }
        changeOperateFoldState()
    }

    @Synchronized
    fun fold(cmd: ScriptCommand) {
        operateNodeMap.forEach {
            if (it.key == cmd) {
                it.value?.isFold = true
            }
        }
        changeOperateFoldState()
    }


    @Synchronized
    private fun setTouchCells() {
        callsIntouch.clear()
        nodeNotInTouch.clear()
        currentMoveNode?.cell?.traverseCell {
            it.isInTouchMove = true
            callsIntouch.add(it)
        }
        nodeInTouch.clear()
        nodeInTouch.addAll(
            getChildList().filterIsInstance<XCommandNode>()
                .filter { callsIntouch.contains(it.cell) })
        nodeInTouch.addAll(
            getChildList().filterIsInstance<XCommandDelayNode>()
                .filter { callsIntouch.contains(it.cell) })
        nodeInTouch.forEach {
            it.isInTouchMove = true
        }
        nodeNotInTouch.clear()
        val nodeNotInTouch1 = getChildList().filterIsInstance<XLineNode>()
            .filter { !callsIntouch.contains(it.startCell) && !callsIntouch.contains(it.endCell) }
        val nodeNotInTouch2 = getChildList().filterIsInstance<XCommandEndNode>()
            .filter { !callsIntouch.contains(it.cell) }
        nodeNotInTouch.addAll(nodeNotInTouch1)
        nodeNotInTouch.addAll(nodeNotInTouch2)
    }

    @Synchronized
    private fun topNodeList(node: List<SCAbsLayerItemView>) {
        node.forEach { it.indexZ += 100 }
        requestReLayout()
    }

    @Synchronized
    private fun restoreTopNode(node: List<SCAbsLayerItemView>) {
        node.forEach { it.indexZ -= 100 }
        requestReLayout()
    }

    @Synchronized
    private fun clearTouchCell() {
        nodeInTouch.forEach {
            it.isInTouchMove = false
        }
        cellList?.forEach {
            it.isInTouchMove = false
        }
    }

    @Synchronized
    private fun clearTouchSelectCell() {
        nodeNotInTouch.forEach {
            it.isInTouchSelected = false
        }
    }

    @Synchronized
    private fun startMove(node: XCommandNode) {
        VibrateUtils.vibrate(100L)
        currentMoveNode = node
        touchX = 0f
        touchY = 0f
        startTouchX = 0f
        startTouchY = 0f
        isInTouchMove = true
        setTouchCells()
        topNodeList(nodeInTouch)
    }


    override fun onTouchMove(transEvent: MotionEvent, originEvent: MotionEvent) {
        when (transEvent.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                VibrateUtils.vibrate(100L)
                isInTouchMove = false
                touchX = 0f
                touchY = 0f
                startTouchX = 0f
                startTouchY = 0f
                if (isInDeleteMode) {
                    deleteCommand()
                } else {
                    refractCommand()
                }
                restoreTopNode(nodeInTouch)
                setTargetPosition()
                clearTouchCell()
                clearTouchSelectCell()
                currentMoveNode = null
            }

            MotionEvent.ACTION_DOWN -> {
                startTouchX = transEvent.x
                startTouchY = transEvent.y
                touchX = 0f
                touchY = 0f
                clearTargetNodePosition()
            }

            MotionEvent.ACTION_MOVE -> {
                if (startTouchX != 0f && startTouchY != 0f) {
                    touchX += (transEvent.x - startTouchX)
                    touchY += (transEvent.y - startTouchY)
                }
                startTouchX = transEvent.x
                startTouchY = transEvent.y
                if (updateTargetLinePoint(transEvent)) {
                    clearTargetNodePosition()
                } else {
                    updateTargetNodePosition(transEvent)
                }
            }
        }
        isInDeleteMode = checkIsDelete(originEvent.x, originEvent.y)
        invalidate()
    }

    override fun onRender(canvas: Canvas?) {
        super.onRender(canvas)
        if (isInTouchMove) {
            XEditorDrawHelper.drawDeleteArea(canvas, isInDeleteMode)
        }
    }

    override fun onItemDrawBefore(canvas: Canvas) {
        canvas.save()
        val matrix = mVirtualEditLayerModel.getTransformMatrix()
        canvas.setMatrix(matrix)
        if (targetPosition != null) {
            XEditorDrawHelper.drawSelectedPosition(canvas, targetPosition!!)
        }
        canvas.restore()
    }


    /**
     * 是否是拖动到deleteOptRect删除,返回true表示删除
     */
    private fun checkIsDelete(touchX: Float, touchY: Float): Boolean {
        currentMoveNode ?: return false
        return XEditorDrawHelper.deleteArea.contains(touchX, touchY)
    }

    @Synchronized
    private fun deleteCommand() {
        currentMoveNode ?: return
        ScriptMenuEditHelper.handleMenuEdit(
            context,
            currentMoveNode?.cell?.cmd,
            null,
            ScriptMenuEditHelper.ClickType.DELETE,
            ScriptMenuEditHelper.InsertType.INSERT_AFTER,
            dialogView
        )
    }

    private fun refractCommand() {
        val selectLineNode = nodeNotInTouch.find { it.isInTouchSelected }
        selectLineNode ?: return
        currentMoveNode ?: return
        val cmd = selectLineNode.getMainCell()!!.cmd
        var insertAfter = ScriptMenuEditHelper.InsertType.INSERT_AFTER
        if (selectLineNode is XLineNode) {
            insertAfter = when (selectLineNode.lineType) {

                XLineNode.Companion.LineType.GROUP_TO_CMD -> {
                    ScriptMenuEditHelper.InsertType.INSERT_BEFORE
                }

                XLineNode.Companion.LineType.GROUP_EMPTY_TO_ADD -> {
                    ScriptMenuEditHelper.InsertType.INSERT_INNER
                }

                else -> {
                    ScriptMenuEditHelper.InsertType.INSERT_AFTER
                }
            }
        }
        ScriptMenuEditHelper.handleMenuEdit(
            context,
            currentMoveNode?.cell?.cmd,
            cmd,
            ScriptMenuEditHelper.ClickType.INSERT_REFRACT,
            insertAfter,
            dialogView
        )
    }

    /**
     * 离touch xy最近的被选中
     */
    @Synchronized
    private fun updateTargetLinePoint(e: MotionEvent): Boolean {
        var hasTarget = false
        clearTouchSelectCell()
        //找出list里的frameRect离e.xy最近的那个
        val selectCell = nodeNotInTouch.minByOrNull {
            sqrt(
                (it.mItemRect.centerX() - e.x).toDouble()
                    .pow(2) + (it.mItemRect.centerY() - e.y).toDouble().pow(2)
            )
        }
        var hasChanged = false
        //发生变化
        if (selectCell != null && selectCell != targetSelectCell) {
            hasChanged = true
        }
        targetSelectCell = selectCell
        targetSelectCell?.run {
            val dis = sqrt(
                (this.mItemRect.centerX() - e.x).toDouble()
                    .pow(2) + (this.mItemRect.centerY() - e.y).toDouble().pow(2)
            )
            if (dis < Min_Distance_To_Select) {
                this.isInTouchSelected = true
                if (hasChanged) {
                    VibrateUtils.vibrate(50L)
                }
                hasTarget = true
            }
        }
        return hasTarget
    }

    /**
     * 更新选中位置
     */
    private fun updateTargetNodePosition(e: MotionEvent) {
        if (!enableFreePin) return
        val itemCellWidth = XEditorHelper.cellWidth
        val itemCellHeight = XEditorHelper.cellHeight
        //计算理滑动最近的xIndex和yIndex
        var xIndex = (e.x / itemCellWidth).toInt()
        var yIndex = (e.y / itemCellHeight).toInt()
        if (e.x < 0) {
            xIndex = ((e.x - itemCellWidth) / itemCellWidth).toInt()
            yIndex = (e.y / itemCellHeight).toInt()
        }
        val pos = Point(xIndex, yIndex)
        cellList?.filter { !callsIntouch.contains(it) }
            ?.firstOrNull { it.xIndex == xIndex && it.yIndex == yIndex }?.run {
                targetPosition = null
            } ?: run {
            val startEndNodes = getChildList().filterIsInstance<XStartEndNode>()
            startEndNodes.firstOrNull { it.cell.xIndex == xIndex && it.cell.yIndex == yIndex }
                ?.run {
                    targetPosition = null
                } ?: run {
                if (targetPosition?.x != xIndex || targetPosition?.y != yIndex) {
                    VibrateUtils.vibrate(50L)
                }
                targetPosition = pos
            }
        }
    }

    private fun setTargetPosition() {
        if (!enableFreePin) return
        targetPosition?.let { pos ->
            val nodePos =
                XEditorHelper.getCellPosition(currentMoveNode?.getMainCell()?.cmd?.getTreeId())
            nodePos?.run {
                val dx = pos.x - nodePos.x
                val dy = pos.y - nodePos.y
                //如果没有移动，则直接返回
                if (dx == 0 && dy == 0) {
                    clearTargetNodePosition()
                    return
                }
                callsIntouch.forEach {
                    XEditorHelper.putCellPosition(
                        it.cmd?.getTreeId(),
                        it.xIndex + dx,
                        it.yIndex + dy
                    )
                }
            }

            notifyData()
        }
        XEditorSnapManager.get().save(currentCommand)
        dialogView?.updateUndoRedoStatus()
        dialogView?.setSaveEnable(true)
        clearTargetNodePosition()
    }

    private fun clearTargetNodePosition() {
        targetPosition = null
    }

    @Synchronized
    fun saveShareImage(command: ScriptCommand?, onSaved: ((path: String?) -> Unit)?) {
        snapHelper.saveImage(command, this, onSaved)
    }


    enum class XEditorEvent {
        REFRESH_DATA, TOUCH_MOVE, OPERATE_CHANGE
    }

    companion object {

        var touchX = 0f

        var touchY = 0f
    }
}