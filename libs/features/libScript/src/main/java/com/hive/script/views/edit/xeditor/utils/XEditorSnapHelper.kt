// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.extensions.getInfoMap
import com.hive.script.utils.ScriptHelper
import com.hive.script.views.dialog.DialogScriptLoading
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.nodes.XCommandNode
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class XEditorSnapHelper {

    private var snapBitmap: Bitmap? = null

    private var loadingDialog: DialogScriptLoading? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun saveImage(
        command: ScriptCommand?,
        editorView: ScriptXEditorView,
        onSaved: ((path: String?) -> Unit)?
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            loadingDialog?.dismiss()
            loadingDialog = DialogScriptLoading(editorView.context).setCloseEnable(false)
            loadingDialog?.show()
            snapBitmap = createBitmap(editorView)
            if (snapBitmap == null) {
                loadingDialog?.dismiss()
                return@launch
            }
            val canvasSnap = Canvas(snapBitmap!!)
            editorView.saveLayerTransform()
            editorView.isSnapShotMode = true
            val childList = editorView.getChildList().filterIsInstance<XCommandNode>()
            val left = childList.minOfOrNull { it.cell.xIndex } ?: 0
            editorView.translateLayer(left * XEditorHelper.cellWidth, -XEditorHelper.cellHeight)
            editorView.drawView(canvasSnap)
            editorView.restoreLayerTransform()
            editorView.isSnapShotMode = false
            val path = withContext(Dispatchers.IO) {
                delay(100)
                snapBitmap?.let {
                    val bmp = addExtraWaterMark(command, it)
                    val compressBmp = createScaledBitmap(bmp)
                    val path = BitmapUtils.saveBitmap(compressBmp)
                    snapBitmap?.recycle()
                    snapBitmap = null
                    bmp.recycle()
                    compressBmp?.recycle()
                    return@withContext path
                }
            }
            if (path == null) {
                loadingDialog?.dismiss()
                return@launch
            }

            ScriptHelper.saveToGallery(path)
            loadingDialog?.dismiss()
            onSaved?.invoke(path)
            CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.script_edit_save_success))
        }
    }

    /**
     * 如果图片过大，进行缩放
     */
    private fun createScaledBitmap(bmp: Bitmap): Bitmap? {
        val max = 8000
        val width = bmp.width
        val height = bmp.height
        if (width <= max && height <= max) {
            return bmp
        }
        val scale = if (width > height) {
            max.toFloat() / width
        } else {
            max.toFloat() / height
        }
        return Bitmap.createScaledBitmap(
            bmp,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }


    private var extrasMap = mutableMapOf<String, String>()

    /**
     * 生成一张带有水印的图片，并带上logo和app名称，在下面展示
     */
    private fun addExtraWaterMark(command: ScriptCommand?, srcBmp: Bitmap): Bitmap {
        if (command is ScriptCommandRoot) {
            extrasMap = command.scriptMate?.getInfoMap(null) ?: mutableMapOf()
        }
        extrasMap.remove(GlobalApp.getString(com.hive.i8n.R.string.sc_script_info_2))
        extrasMap.remove(GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_view_type_title))
        extrasMap.remove(GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_share_type_title))
        extrasMap.remove(GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_cloud_type_title))
        extrasMap.remove(GlobalApp.getString(com.hive.i8n.R.string.sc_share_ctr_run_type_title))
        val headHeight = 60
        val infoLineHeight = 60
        val barHeight = headHeight + extrasMap.size * infoLineHeight + 46
        val width = if (srcBmp.width > 1024) (srcBmp.width + 128) else 1024
        val height = srcBmp.height + barHeight
        val desBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(desBmp)
        canvas.drawColor(GlobalApp.getColor(com.hive.i8n.R.color.colorPrimary))
        drawWaterMark(canvas)
        canvas.drawBitmap(srcBmp, (width - srcBmp.width) / 2f, 0f, Paint())

        //绘制底部的logo和app名称背景
        canvas.drawRect(
            0f,
            (height - barHeight).toFloat(),
            width.toFloat(),
            height.toFloat(),
            Paint().apply {
                color = 0x5f000000.toInt()
            })
        val logoBmp =
            BitmapFactory.decodeResource(GlobalApp.getResources(), com.hive.i8n.R.drawable.logo)
        val appName = GlobalApp.getString(
            com.hive.i8n.R.string.sc_share_app_info,
            GlobalApp.getString(com.hive.i8n.R.string.app_name)
        )
        val textPaint = Paint().apply {
            color = GlobalApp.getColor(com.hive.i8n.R.color.textColorPrimary)
            textSize = 14f * GlobalApp.DP
            isAntiAlias = true
            isFakeBoldText = true
        }

        val textPaint2 = Paint().apply {
            color = GlobalApp.getColor(com.hive.i8n.R.color.colorTextSecondary)
            textSize = 12f * GlobalApp.DP
            isAntiAlias = true
        }


        val logoRect1 = Rect(0, 0, logoBmp.width, logoBmp.height)
        val logoRect2 = Rect(width - barHeight, height - barHeight, width, height)
        logoRect2.inset(35, 35)
        canvas.drawBitmap(
            logoBmp,
            logoRect1,
            logoRect2,
            Paint()
        )

        val startY = height - barHeight + textPaint.textSize / 2f + 46f
        canvas.drawText(
            appName,
            46f,
            startY,
            textPaint
        )
        val baseY = startY + infoLineHeight
        var index = 0
        extrasMap.forEach {
            canvas.drawText(
                "${it.key} : ${it.value}",
                46f,
                (baseY + index * infoLineHeight),
                textPaint2
            )
            index++
        }

        return desBmp
    }

    private fun drawWaterMark(canvas: Canvas) {
        val watermarkPaint = Paint().apply {
            color = GlobalApp.getColor(com.hive.i8n.R.color.color_666666)
            textSize = 12f * GlobalApp.DP
            alpha = 40
            isAntiAlias = true
        }
        val watermarkText = GlobalApp.getString(com.hive.i8n.R.string.app_name)


        // 获取View的宽高
        val viewWidth = canvas.width
        val viewHeight = canvas.height


        // 计算每个水印的宽高
        val textBounds = Rect()
        watermarkPaint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        // 计算水印之间的间隔
        val spacingPx = 48 * GlobalApp.DP


        // 动态计算水印的行数和列数
        val cols = (viewWidth.toFloat() / (textWidth + spacingPx)).toInt()
        val rows = (viewHeight.toFloat() / (textHeight + spacingPx)).toInt()


        // 计算水印的实际大小，包括间隔
        val totalWidth = cols * textWidth + (cols - 1) * spacingPx
        val totalHeight = rows * textHeight + (rows - 1) * spacingPx

        // 计算水印的缩放比例，确保水印适应Canvas大小
        val scale = minOf(viewWidth.toFloat() / totalWidth, viewHeight.toFloat() / totalHeight)

        // 在Canvas上均匀绘制水印文字
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * (textWidth + spacingPx) * scale + (textWidth * scale / 2)
                val y = row * (textHeight + spacingPx) * scale + (textHeight * scale / 2)

                canvas.drawText(watermarkText, x, y, watermarkPaint)
            }
        }
    }

    private fun createBitmap(editorView: ScriptXEditorView): Bitmap? {
        val childList = editorView.getChildList().filterIsInstance<XCommandNode>()
        val right = (childList.maxOfOrNull { it.cell.xIndex } ?: 0) + 1
        val left = childList.minOfOrNull { it.cell.xIndex } ?: 0
        val bottom = childList.maxOfOrNull { it.cell.yIndex } ?: 0 + 1
        val top = childList.minOfOrNull { it.cell.yIndex } ?: 0
        val w = abs(right - left)
        val h = abs(bottom - top)
        if (w <= 0 || h < 0) {
            return null
        }
        if (w > 10000 || h > 10000) {
            CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.script_edit_too_large))
            return null
        }
        try {
            return Bitmap.createBitmap(
                (w * XEditorHelper.cellWidth).toInt(),
                ((h + 3) * XEditorHelper.cellHeight).toInt(),
                Bitmap.Config.ARGB_8888
            )
        } catch (e: OutOfMemoryError) {
            CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.script_edit_too_large))
            return null
        }
    }
}
