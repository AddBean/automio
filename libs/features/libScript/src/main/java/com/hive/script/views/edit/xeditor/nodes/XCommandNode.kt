// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.nodes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptParser
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdIf
import com.hive.script.cmd.CmdJump
import com.hive.script.cmd.CmdJumpPoint
import com.hive.script.condition.ConditionColor
import com.hive.script.condition.ConditionImage
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellModel
import com.hive.script.views.edit.xeditor.core.SCAbsLayerItemView
import com.hive.script.views.edit.xeditor.core.model.SCDrawEditRect
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dp
import com.hive.utils.extends.dpf
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.BitmapUtils
import com.hive.views.popmenu.PopMenuManager
import kotlin.math.PI
import kotlin.math.sin
import androidx.core.graphics.createBitmap

open class XCommandNode protected constructor(context: Context, var cell: XCellModel) :
    SCAbsLayerItemView(context) {

    private val moveBgPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorInMove)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val animBgPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.color_red)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimaryLight)
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val imagePaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.color_black)
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint().apply {
        strokeWidth = 4f
        alpha = 160
        isAntiAlias = true

        colorFilter = PorterDuffColorFilter(
            GlobalApp.getColor(com.hive.i8n.R.color.color_white),
            PorterDuff.Mode.SRC_IN
        )
    }

    private val cellPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorPrimaryLight)
        alpha = 200
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val cellMaskPaint = Paint().apply {
        color = Color.BLACK
        alpha = 80
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val cellDisablePaint = Paint().apply {
        color = Color.BLACK
        alpha = 150
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
        textSize = 12f * GlobalApp.DP
        isAntiAlias = true
    }

    private val textDesPaint = Paint().apply {
        color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
        textSize = 10f * GlobalApp.DP
        isAntiAlias = true
    }

    private val editDesRect = Rect()

    private val editSrcRect = Rect().apply {
        set(0, 0, editIconBmp.width, editIconBmp.height)
    }

    private val editPadding = 8 * GlobalApp.DP

    private val editSize = 12 * GlobalApp.DP

    private val dotSize = 12 * GlobalApp.DP

    private val labelImageSize = 20 * GlobalApp.DP

    private val labelPadding = 2 * GlobalApp.DP

    private val labelPadding2 = 6 * GlobalApp.DP

    private val iconImageSize = 14 * GlobalApp.DP

    private val iconPadding = 6 * GlobalApp.DP

    private var displayTitle = ""

    private var titleWidth = 0f

    private var diaplayDesc = ""

    private var desWidth = 0f

    private var editClickRect = RectF()

    private val innerBgFrame = RectF()

    private val innerFrame = RectF()

    private var labelImageRect = RectF()

    private var iconImageRect = RectF()

    private val xFermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    private var roundBmp: Bitmap? = null

    private var iconBmp: Bitmap? = null

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * GlobalApp.DP
        isAntiAlias = true
    }

    init {
        onDataRefresh(cell.cmd)
    }

    override fun onDataRefresh(cmd: ScriptCommand?) {
        cell.cmd = cmd
        val innerCell = RectF(cell.frame)
        innerCell.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
        val comment = cell.cmd?.comment?.trim()
        val commandName = cell.cmd?.getCommandName()?.trim()
        val displayName = if (TextUtils.isEmpty(comment)) commandName else comment
        displayTitle =
            XEditorHelper.clipString(
                displayName ?: "",
                textPaint,
                innerCell.width() - 16 * GlobalApp.DP
            )
        diaplayDesc = XEditorHelper.clipString(
            cell.cmd?.getCommandDescribe() ?: "",
            textDesPaint,
            innerCell.width() - 16 * GlobalApp.DP
        )
        titleWidth = textPaint.measureText(displayTitle)
        desWidth = textDesPaint.measureText(diaplayDesc)
        editDesRect.set(
            (innerCell.right - editSize).toInt() - editPadding,
            (innerCell.top).toInt() + editPadding,
            innerCell.right.toInt() - editPadding,
            (innerCell.top + editSize).toInt() + editPadding
        )
        editClickRect.set(editDesRect)
        editClickRect.inset(-10f * GlobalApp.DP, -10f * GlobalApp.DP)
    }


    override fun getRenderRect(): RectF? {
        return cell.frame
    }

    override fun onDraw(canvas: Canvas) {
        drawNode(canvas, true)
    }

    override fun onDrawAnim(canvas: Canvas, animPercent: Float, type: String?) {
        //动画
        innerFrame.set(cell.frame)
        innerFrame.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
        animBgPaint.alpha = (sin(animPercent * PI) * 0.9f * 255f).toInt()
        cellPaint.color = getCellColor(cell.cmd)
        canvas.drawRoundRect(innerFrame, 12f * GlobalApp.DP, 12f * GlobalApp.DP, cellPaint)
        canvas.drawRoundRect(innerFrame, 12f * GlobalApp.DP, 12f * GlobalApp.DP, cellMaskPaint)
        canvas.drawRoundRect(innerFrame, 12f * GlobalApp.DP, 12f * GlobalApp.DP, animBgPaint)
        drawNode(canvas, false)
    }

    private fun drawNode(canvas: Canvas, drawBg: Boolean) {
        if (cell.isInTouchMove) {
            canvas.translate(ScriptXEditorView.touchX, ScriptXEditorView.touchY)
            innerBgFrame.set(cell.frame)
            innerBgFrame.inset(4f * dp, 4f * dp)
            canvas.drawRoundRect(innerBgFrame, 12f * dp, 12f * dp, moveBgPaint)
        }
        innerFrame.set(cell.frame)
        innerFrame.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
        if (drawBg) {
            if (cell.cmd?.isGroupCommand() == true) {
                drawGroupCommandBg(canvas, cellPaint, cell)
            } else {
                drawSingleCommandBg(canvas, cellPaint, cell)
            }
        }
        if (isNormalSize()) {
            if (XEditorHelper.editMode) {
                canvas.drawText(
                    displayTitle,
                    cell.frame.centerX() - titleWidth / 2f,
                    cell.frame.centerY() - textPaint.textSize / 2 + 1 * GlobalApp.DP,
                    textPaint
                )

                canvas.drawText(
                    diaplayDesc,
                    cell.frame.centerX() - desWidth / 2f,
                    cell.frame.centerY() + textDesPaint.textSize + 3 * GlobalApp.DP,
                    textDesPaint
                )
            } else {
                canvas.drawText(
                    displayTitle,
                    cell.frame.centerX() - titleWidth / 2f,
                    cell.frame.centerY() + 3.dp,
                    textPaint
                )
            }

            drawLabelIcon(canvas)

            if (mParentView?.isSnapShotMode != true) {
                if (XEditorHelper.editMode) {
                    canvas.drawBitmap(editIconBmp, editSrcRect, editDesRect, iconPaint)
                }
            }
        }

        //如果是禁用状态，绘制禁用遮罩
        if (cell.isDisabled) {
            canvas.drawRoundRect(
                innerFrame,
                12f * GlobalApp.DP,
                12f * GlobalApp.DP,
                cellDisablePaint
            )
        }
    }

    private fun drawGroupCommandBg(canvas: Canvas, cellPaint: Paint, cell: XCellModel) {
        val radius = 12f * GlobalApp.DP
        val offset = 4f * GlobalApp.DP // 单层位移
        val baseColor = getCellColor(cell.cmd)

        // --- 第3层（最底层，偏移最大，最淡） ---
        cellPaint.color = baseColor
        cellPaint.alpha = 40
        val layer3 = RectF(innerFrame).apply { offset(offset * 2, offset * 2) }
        canvas.drawRoundRect(layer3, radius, radius, cellPaint)

        // --- 第2层（中间层，偏移适中） ---
        cellPaint.alpha = 80
        val layer2 = RectF(innerFrame).apply { offset(offset, offset) }
        canvas.drawRoundRect(layer2, radius, radius, cellPaint)

        // --- 第1层（主背景层） ---
        cellPaint.color = baseColor
        cellPaint.alpha = 255
        canvas.drawRoundRect(innerFrame, radius, radius, cellPaint)
        canvas.drawRoundRect(innerFrame, radius, radius, cellMaskPaint) // 莫兰迪灰调遮罩

        // 2. 主层内部亮色轮廓：增加精致感
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 1f.dp
        linePaint.color = Color.WHITE
        linePaint.alpha = 110
        val innerStroke = RectF(innerFrame).apply {
            inset(0.5f.dpf,0.5f.dpf)
        }
        canvas.drawRoundRect(innerStroke, radius, radius, linePaint)
    }

    private fun drawSingleCommandBg(
        canvas: Canvas,
        cellPaint: Paint,
        cell: XCellModel
    ) {
        val radius = 12f * GlobalApp.DP
        val baseColor = getCellColor(cell.cmd)

        // 1. 绘制主体背景
        cellPaint.color = baseColor
        cellPaint.alpha = 200
        canvas.drawRoundRect(innerFrame, radius, radius, cellPaint)
        canvas.drawRoundRect(innerFrame, radius, radius, cellMaskPaint)

        // 2. 绘制精致的内描边线条
        linePaint.color = Color.WHITE
        linePaint.alpha = 40 // 极淡的白色内边框
        val strokeRect = RectF(innerFrame).apply { inset(0.5f.dp, 0.5f.dp) }
        canvas.drawRoundRect(strokeRect, radius, radius, linePaint)
    }

    private fun drawLabelIcon(canvas: Canvas) {
        val cmd = cell.cmd
        labelImageRect.set(
            innerFrame.left + labelPadding2,
            innerFrame.top + labelPadding2,
            innerFrame.left + labelPadding2 + labelImageSize,
            innerFrame.top + labelPadding2 + labelImageSize
        )

        iconImageRect.set(
            innerFrame.left + iconPadding,
            innerFrame.top + iconPadding,
            innerFrame.left + iconPadding + iconImageSize,
            innerFrame.top + iconPadding + iconImageSize
        )
        when (cmd) {
            is CmdIf -> {
                val imageCod = cmd.conditionList?.filterIsInstance<ConditionImage>()?.firstOrNull()
                val colorCod = cmd.conditionList?.filterIsInstance<ConditionColor>()?.firstOrNull()
                if (imageCod != null) {
                    drawBitmap(canvas, imageCod.getAttachmentRelativePaths()?.firstOrNull() ?: "")
                } else if (colorCod != null) {
                    drawColorDot(canvas, colorCod.color ?: Color.BLACK)
                } else {
                    drawDefaultIcon(canvas, cmd)
                }
            }

            is CmdClickImage -> {
                drawBitmap(canvas, cmd.getAttachmentRelativePaths()?.firstOrNull() ?: "")
            }

            is CmdClickColor -> {
                drawColorDot(canvas, cmd.targetColor)
            }

            else -> {
                drawDefaultIcon(canvas, cmd)
            }
        }

    }

    private fun drawDefaultIcon(canvas: Canvas, cmd: ScriptCommand?) {
        cmd ?: return
        iconMap[cmd::class.java.name]?.let {
            canvas.save()
            if (iconBmp == null) iconBmp = it
            canvas.drawBitmap(
                iconBmp!!,
                Rect(0, 0, iconBmp!!.width, iconBmp!!.height),
                iconImageRect,
                iconPaint
            )
            canvas.restore()
        }
    }

    private fun drawBitmap(canvas: Canvas, path: String) {
        bmpMap[path]?.let {
            canvas.save()
            if (roundBmp == null) roundBmp = createRoundBitmap(it)
            canvas.drawBitmap(
                roundBmp!!,
                Rect(0, 0, roundBmp!!.width, roundBmp!!.height),
                labelImageRect,
                dotPaint
            )
            canvas.restore()
        }
    }

    private fun drawColorDot(canvas: Canvas, targetColor: Int) {
        dotPaint.color = targetColor
        canvas.drawCircle(
            labelImageRect.centerX(),
            labelImageRect.centerY(),
            labelImageRect.width() * 1f / 2f,
            dotPaint
        )
    }

    private fun createRoundBitmap(it: Bitmap): Bitmap {
        val bm = createBitmap(it.width, it.width)
        val canvasRound = Canvas(bm)
        canvasRound.drawARGB(0, 0, 0, 0)
        canvasRound.drawCircle(
            bm.width / 2f,
            bm.height / 2f,
            bm.width / 2f,
            imagePaint
        )
        imagePaint.xfermode = xFermode
        canvasRound.drawBitmap(
            it,
            Rect(0, 0, it.width, it.height),
            Rect(0, 0, it.width, it.width),
            imagePaint
        )

        imagePaint.xfermode = null
        return bm
    }

    override fun onLongClick(event: MotionEvent) {
        super.onLongClick(event)
        postEvent(ScriptXEditorView.XEditorEvent.TOUCH_MOVE)
    }

    override fun getMainCell() = cell

    override fun onClick(event: MotionEvent) {
        super.onClick(event)
        if (editClickRect.contains(event.x, event.y)) {
            val p = mEditModel.layerModel.getTransformPosition(PointF(event.x, event.y))
            val anchorView =
                if (mParentView is ScriptXEditorView) (mParentView as ScriptXEditorView).anchorView else mParentView
            val isGroup = cell.cmd?.isGroupCommand() == true
            val list = GlobalApp.getResources()
                .getStringArray(if (isGroup) com.hive.i8n.R.array.sc_xcmd_sub_cmd_menu_array_group else com.hive.i8n.R.array.sc_xcmd_sub_cmd_menu_array)
                .toList().map { it.split("_") }
            PopMenuManager.instance.showMenu(
                anchorView!!,
                p.x.toInt() - 80 * GlobalApp.DP,
                p.y.toInt(),
                list.map { it.getOrNull(1) ?: "" }.toList(),
                object : PopMenuManager.OnItemClickListener<String> {
                    override fun onItemClicked(view: View, data: String, pos: Int) {
                        val listKey = list.map { it.getOrNull(0) ?: "" }.toList()
                        when (listKey[pos].toIntOrNull()) {
                            1 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.COMMENT)
                            }

                            2 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.UP)
                            }

                            3 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.DOWN)
                            }

                            4 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.MANGER_SUB_TASK)
                            }

                            5 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.COPY)
                            }

                            6 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.DELETE)
                            }

                            7 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.RUN_CMD)
                            }

                            8 -> {
                                postEvent(ScriptMenuEditHelper.ClickType.RUN_NEXT_ALL)
                            }
                        }
                    }
                })
        } else {
            startAnim(200f)
            ScriptMenuEditHelper.showEditDialog(
                mParentView?.context!!,
                cell.cmd!!,
                false,
                { event, cmd ->
                    postEvent(event)
                }) { newCmd ->
                postEvent(ScriptXEditorView.XEditorEvent.REFRESH_DATA, newCmd)
                requestParentInvalidate()
            }
        }
    }

    /**
     * 获取节点颜色
     */
    private fun getCellColor(cmd: ScriptCommand?): Int {
        return when (cmd) {
            is CmdJump -> {
                ScriptParser.getJumpCellColor(cmd.id)
            }

            is CmdJumpPoint -> {
                ScriptParser.getJumpCellColor(cmd.id)
            }

            else -> {
                ScriptParser.getXCellColor(cmd)
            }
        }
    }

    override fun onMeasure(rect: SCDrawEditRect) {
        rect.lb = PointF(cell.frame.left, cell.frame.bottom)
        rect.lt = PointF(cell.frame.left, cell.frame.top)
        rect.rb = PointF(cell.frame.right, cell.frame.bottom)
        rect.rt = PointF(cell.frame.right, cell.frame.top)
        rect.inset(XEditorHelper.cellHevPadding, XEditorHelper.cellVerPadding)
    }

    companion object {

        private val editIconBmp = BitmapUtils.drawableToBitmap(R.drawable.sc_icon_more)

        private val bmpMap = mutableMapOf<String, Bitmap?>()

        private val iconMap = mutableMapOf<String, Bitmap?>()

        private fun loadBitmap(path: String, cmd: ScriptCommand) {
            var imgPath = cmd.getScriptBasePath() + path
            if (!FileUtils.isFileExist(imgPath)) {
                imgPath = ScriptConst.Save_Script_Temp_Path + path
            }
            if (!TextUtils.isEmpty(imgPath)) {
                bmpMap[path] = BitmapFactory.decodeFile(imgPath)
            }
        }

        private fun loadBmpIcon(cmd: ScriptCommand) {
            if (iconMap[cmd::class.java.name] == null) {
                cmd.getCommandIcon().run {
                    iconMap[cmd::class.java.name] =
                        BitmapFactory.decodeResource(GlobalApp.getResources(), this)
                }
            }
        }

        fun createNode(context: Context, cell: XCellModel): XCommandNode {
            val node = XCommandNode(context, cell)
            val targetBmp = bmpMap[cell.cmd?.getAttachmentRelativePaths() ?: ""]
            if (targetBmp == null && cell.cmd is CmdClickImage) {
                cell.cmd?.getAttachmentRelativePaths()?.firstOrNull()?.run {
                    loadBitmap(this, cell.cmd!!)
                }
            }
            cell.cmd?.run {
                loadBmpIcon(this)
            }
            cell.cmd?.conditionList?.forEach {
                if (it is ConditionImage) {
                    it.getAttachmentRelativePaths()?.firstOrNull()?.run {
                        loadBitmap(this, cell.cmd!!)
                    }
                }
            }
            return node
        }
    }
}