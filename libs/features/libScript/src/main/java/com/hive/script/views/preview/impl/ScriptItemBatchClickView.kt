// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.preview.impl

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.utils.ScriptCommonDrawer
import com.hive.script.utils.ScriptCommonUtils
import com.hive.script.views.preview.ScriptItemView

/**
 *
 * @author jiadou
 * @date 6/9/21
 */
class ScriptItemBatchClickView : ScriptItemView() {

    private var locPoint: Point? = null

    private var screenRect: Rect? = null

    private var screenRectF = RectF()

    override fun onDraw(canvas: Canvas) {
        val cmd = command as CmdPatternTap
        screenRect = ScriptCommonUtils.covertToScreenRect(cmd.limitRect)
        screenRectF.set(screenRect!!)
        screenRect?.run {
            ScriptCommonDrawer.drawBatchRect(canvas, this, (0.4f * 255).toInt())
        }
        cmd.run {
            ScriptCommonUtils.forEachRect(screenRectF, clickType, clickHrz, clickVer) { x, y ->
                ScriptCommonDrawer.drawDot2(canvas, x, y, 50)
            }
        }
        locPoint ?: return
        ScriptCommonDrawer.drawDot(
            canvas,
            locPoint?.x?.toFloat() ?: 0f,
            locPoint?.y?.toFloat() ?: 0f,
            200
        )
    }

    override fun onExecuteUpdate(type: Int, cmd: ScriptCommand, obj: Any?) {
        locPoint = obj as Point
        parentView?.postInvalidate()
    }


    override fun onExecuteEnd(cmd: ScriptCommand) {
        parentView?.postInvalidate()
    }
}